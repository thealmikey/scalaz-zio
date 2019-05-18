// Copyright (C) 2017-2018 Łukasz Biały, Paul Chiusano, Michael Pilquist,
// Oleg Pyzhcov, Fabio Labella, Alexandru Nedelcu, Pavel Chlupacek. All rights reserved.

package scalaz.zio

import scalaz.zio.duration._

class SemaphoreSpec(implicit ee: org.specs2.concurrent.ExecutionEnv) extends TestRuntime {

  def is =
    "SemaphoreSpec".title ^ s2"""
    Make a Semaphore and
    verify that
      `acquire` permits sequentially $e1
      `acquire` permits in parallel $e2
      `acquireN`s can be parallel with `releaseN`s $e3
      individual `acquireN`s can be parallel with individual `releaseN`s $e4
      semaphores and fibers play ball together $e5
      `acquire` doesn't leak permits upon cancellation $e6
      `withPermit` does not leak fibers or permits upon cancellation $e7
    """

  def e1 = {
    val n = 20L
    unsafeRun(for {
      semaphore <- Semaphore.make(n)
      available <- IO.foreach((0L until n).toList)(_ => semaphore.acquire) *> semaphore.available
    } yield available must_=== 0)
  }

  def e2 = {
    val n = 20L
    unsafeRun(for {
      semaphore <- Semaphore.make(n)
      available <- IO.foreachPar((0L until n).toList)(_ => semaphore.acquire) *> semaphore.available
    } yield available must_=== 0)
  }

  def e3 =
    offsettingReleasesAcquires(
      (s, permits) => IO.foreach(permits)(s.acquireN).unit,
      (s, permits) => IO.foreach(permits.reverse)(s.releaseN).unit
    )

  def e4 =
    offsettingReleasesAcquires(
      (s, permits) => IO.foreachPar(permits)(amount => s.acquireN(amount)).unit,
      (s, permits) => IO.foreachPar(permits.reverse)(amount => s.releaseN(amount)).unit
    )

  def e5 = {
    val n = 1L
    unsafeRun(for {
      s <- Semaphore.make(n).tap(_.acquire)
      _ <- s.release.fork
      _ <- s.acquire
    } yield () must_=== (()))
  }

  /**
   * Ported from @mpilquist work in cats-effects (https://github.com/typelevel/cats-effect/pull/403)
   */
  def e6 = {
    val n = 1L
    unsafeRun(for {
      s       <- Semaphore.make(n)
      _       <- s.acquireN(2).timeout(1.milli).either
      permits <- s.release *> clock.sleep(10.millis) *> s.available
    } yield permits) must_=== 2
  }

  /**
   * Ported from @mpilquist work in cats-effects (https://github.com/typelevel/cats-effect/pull/403)
   */
  def e7 = {
    val n = 0L
    unsafeRun(for {
      s       <- Semaphore.make(n)
      _       <- s.withPermit(s.release).timeout(1.milli).either
      permits <- s.release *> clock.sleep(10.millis) *> s.available
    } yield permits must_=== 1L)
  }

  def offsettingReleasesAcquires(
    acquires: (Semaphore, Vector[Long]) => UIO[Unit],
    releases: (Semaphore, Vector[Long]) => UIO[Unit]
  ) = {
    val permits = Vector(1L, 0L, 20L, 4L, 0L, 5L, 2L, 1L, 1L, 3L)

    unsafeRun(for {
      semaphore     <- Semaphore.make(0L)
      acquiresFiber <- acquires(semaphore, permits).fork
      releasesFiber <- releases(semaphore, permits).fork
      _             <- acquiresFiber.join
      _             <- releasesFiber.join
      count         <- semaphore.available
    } yield count must_=== 0)
  }

}
