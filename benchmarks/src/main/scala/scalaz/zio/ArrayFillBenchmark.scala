package scalaz.zio

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

import scala.collection.immutable.Range

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class ArrayFillBenchmarks {
  @Param(Array("10000"))
  var size: Int = _

  def createTestArray: Array[Int] = Range.inclusive(1, size).toArray.reverse

  @Benchmark
  def scalazArrayFill() = {
    import IOBenchmarks.unsafeRun

    def arrayFill(array: Array[Int]): FunctionIO[Nothing, Int, Int] = {
      val condition = FunctionIO.fromFunction[Int, Boolean]((i: Int) => i < array.length)

      FunctionIO.whileDo[Nothing, Int](condition)(FunctionIO.effectTotal[Int, Int] { (i: Int) =>
        array.update(i, i)

        i + 1
      })
    }

    unsafeRun(
      for {
        array <- IO.effectTotal[Array[Int]](createTestArray)
        _     <- arrayFill(array).run(0)
      } yield ()
    )
  }

  @Benchmark
  def monoArrayFill() = {
    import reactor.core.publisher.Mono

    def arrayFill(array: Array[Int])(i: Int): Mono[Unit] =
      if (i >= array.length) Mono.fromSupplier(() => ())
      else
        Mono
          .fromSupplier(() => array.update(i, i))
          .flatMap(_ => arrayFill(array)(i + 1))

    (for {
      array <- Mono.fromSupplier(() => createTestArray)
      _     <- arrayFill(array)(0)
    } yield ())
      .block()
  }

  @Benchmark
  def catsArrayFill() = {
    import cats.effect.IO

    def arrayFill(array: Array[Int])(i: Int): IO[Unit] =
      if (i >= array.length) IO.unit
      else IO(array.update(i, i)).flatMap(_ => arrayFill(array)(i + 1))

    (for {
      array <- IO(createTestArray)
      _     <- arrayFill(array)(0)
    } yield ()).unsafeRunSync()
  }

  @Benchmark
  def monixArrayFill() = {
    import IOBenchmarks.monixScheduler
    import monix.eval.Task

    def arrayFill(array: Array[Int])(i: Int): Task[Unit] =
      if (i >= array.length) Task.unit
      else Task.eval(array.update(i, i)).flatMap(_ => arrayFill(array)(i + 1))

    (for {
      array <- Task.eval(createTestArray)
      _     <- arrayFill(array)(0)
    } yield ()).runSyncUnsafe(scala.concurrent.duration.Duration.Inf)
  }
}
