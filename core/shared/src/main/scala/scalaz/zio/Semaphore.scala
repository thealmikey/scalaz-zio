/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
 * Copyright 2017-2018 Łukasz Biały, Paul Chiusano, Michael Pilquist,
 * Oleg Pyzhcov, Fabio Labella, Alexandru Nedelcu, Pavel Chlupacek.
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalaz.zio

import internals._

import scala.annotation.tailrec
import scala.collection.immutable.{ Queue => IQueue }

/**
 * An asynchronous semaphore, which is a generalization of a mutex. Semaphores
 * have a certain number of permits, which can be held and released
 * concurrently by different parties. Attempts to acquire more permits than
 * available result in the acquiring fiber being suspended until the specified
 * number of permits become available.
 **/
final class Semaphore private (private val state: Ref[State]) extends Serializable {

  /**
   * The number of permits currently available.
   */
  final def available: UIO[Long] = state.get.map {
    case Left(_)  => 0
    case Right(n) => n
  }

  /**
   * Acquires a single permit. This must be paired with `release` in a safe
   * fashion in order to avoid leaking permits.
   *
   * If a permit is not available, the fiber invoking this method will be
   * suspended until a permit is available.
   */
  final def acquire: UIO[Unit] = acquireN(1)

  /**
   * Releases a single permit.
   */
  final def release: UIO[Unit] = releaseN(1)

  final def withPermit[R, E, A](task: ZIO[R, E, A]): ZIO[R, E, A] =
    prepare(1L).bracket(_.release)(_.awaitAcquire *> task)

  /**
   * Acquires a specified number of permits.
   *
   * If the specified number of permits are not available, the fiber invoking
   * this method will be suspended until the permits are available.
   *
   * Ported from @mpilquist work in cats-effects (https://github.com/typelevel/cats-effect/pull/403)
   */
  final def acquireN(n: Long): UIO[Unit] = {
    // TODO: Dotty doesn't infer this properly
    val i0: ZIO.BracketExitRelease[Any, Nothing, Nothing, Acquisition, Unit] = IO.bracketExit(prepare(n))(cleanup)
    val i1: UIO[Unit]                                                        = i0(_.awaitAcquire)
    assertNonNegative(n) *> i1
  }

  /**
   * Ported from @mpilquist work in cats-effects (https://github.com/typelevel/cats-effect/pull/403)
   */
  final private def prepare(n: Long): UIO[Acquisition] = {
    def restore(p: Promise[Nothing, Unit], n: Long): UIO[Unit] =
      IO.flatten(state.modify {
        case Left(q) =>
          q.find(_._1 == p).fold(releaseN(n) -> Left(q))(x => releaseN(n - x._2) -> Left(q.filter(_._1 != p)))
        case Right(m) => IO.unit -> Right(m + n)
      })

    if (n == 0)
      IO.succeed(Acquisition(IO.unit, IO.unit))
    else
      Promise.make[Nothing, Unit].flatMap { p =>
        state.modify {
          case Right(m) if m >= n => Acquisition(IO.unit, releaseN(n))   -> Right(m - n)
          case Right(m)           => Acquisition(p.await, restore(p, n)) -> Left(IQueue(p -> (n - m)))
          case Left(q)            => Acquisition(p.await, restore(p, n)) -> Left(q.enqueue(p -> n))
        }
      }
  }

  final private def cleanup[E, A](ops: Acquisition, res: Exit[E, A]): UIO[Unit] =
    res match {
      case Exit.Failure(c) if c.interrupted => ops.release
      case _                                => IO.unit
    }

  final def releaseN(toRelease: Long): UIO[Unit] = {

    @tailrec def loop(n: Long, state: State, acc: UIO[Unit]): (UIO[Unit], State) = state match {
      case Right(m) => acc -> Right(n + m)
      case Left(q) =>
        q.dequeueOption match {
          case None => acc -> Right(n)
          case Some(((p, m), q)) =>
            if (n > m)
              loop(n - m, Left(q), acc <* p.succeed(()))
            else if (n == m)
              (acc <* p.succeed(())) -> Left(q)
            else
              acc -> Left((p -> (m - n)) +: q)
        }
    }

    IO.flatten(assertNonNegative(toRelease) *> state.modify(loop(toRelease, _, IO.unit))).uninterruptible

  }

}

object Semaphore extends Serializable {

  /**
   * Creates a new `Sempahore` with the specified number of permits.
   */
  final def make(permits: Long): UIO[Semaphore] = Ref.make[State](Right(permits)).map(new Semaphore(_))
}

private object internals {

  final case class Acquisition(awaitAcquire: UIO[Unit], release: UIO[Unit])

  type Entry = (Promise[Nothing, Unit], Long)

  type State = Either[IQueue[Entry], Long]

  def assertNonNegative(n: Long): UIO[Unit] =
    if (n < 0)
      IO.die(new NegativeArgument(s"Unexpected negative value `$n` passed to acquireN or releaseN."))
    else IO.unit

  class NegativeArgument(message: String) extends IllegalArgumentException(message)
}
