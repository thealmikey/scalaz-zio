package scalaz.zio

import scala.{ Array, Boolean, Int, Unit }

object ScalazIOArray {

  def bubbleSort[A](lessThanEqual0: (A, A) => Boolean)(array: Array[A]): UIO[Unit] = {

    type IndexValue   = (Int, A)
    type IJIndex      = (Int, Int)
    type IJIndexValue = (IndexValue, IndexValue)

    val lessThanEqual =
      FunctionIO.fromFunction[IJIndexValue, Boolean] {
        case ((_, ia), (_, ja)) => lessThanEqual0(ia, ja)
      }

    val extractIJAndIncrementJ = FunctionIO.fromFunction[IJIndexValue, IJIndex] {
      case ((i, _), (j, _)) => (i, j + 1)
    }

    val extractIAndIncrementI = FunctionIO.fromFunction[IJIndex, Int](_._1 + 1)

    val innerLoopStart = FunctionIO.fromFunction[Int, IJIndex]((i: Int) => (i, i + 1))

    val outerLoopCheck: FunctionIO[Nothing, Int, Boolean] =
      FunctionIO.fromFunction((i: Int) => i < array.length - 1)

    val innerLoopCheck: FunctionIO[Nothing, IJIndex, Boolean] =
      FunctionIO.fromFunction { case (_, j) => j < array.length }

    val extractIJIndexValue: FunctionIO[Nothing, IJIndex, IJIndexValue] =
      FunctionIO.effectTotal {
        case (i, j) => ((i, array(i)), (j, array(j)))
      }

    val swapIJ: FunctionIO[Nothing, IJIndexValue, IJIndexValue] =
      FunctionIO.effectTotal {
        case v @ ((i, ia), (j, ja)) =>
          array.update(i, ja)
          array.update(j, ia)

          v
      }

    val sort = FunctionIO
      .whileDo[Nothing, Int](outerLoopCheck)(
        innerLoopStart >>>
          FunctionIO.whileDo[Nothing, IJIndex](innerLoopCheck)(
            extractIJIndexValue >>>
              FunctionIO.ifNotThen[Nothing, IJIndexValue](lessThanEqual)(swapIJ) >>>
              extractIJAndIncrementJ
          ) >>>
          extractIAndIncrementI
      )
    sort.run(0).unit
  }
}

object CatsIOArray {
  import cats.effect.IO

  def bubbleSort[A](lessThanEqual0: (A, A) => Boolean)(array: Array[A]): IO[Unit] = {
    def outerLoop(i: Int): IO[Unit] =
      if (i >= array.length - 1) IO.unit else innerLoop(i, i + 1).flatMap(_ => outerLoop(i + 1))

    def innerLoop(i: Int, j: Int): IO[Unit] =
      if (j >= array.length) IO.unit
      else
        IO((array(i), array(j))).flatMap {
          case (ia, ja) =>
            val maybeSwap = if (lessThanEqual0(ia, ja)) IO.unit else swapIJ(i, ia, j, ja)

            maybeSwap.flatMap(_ => innerLoop(i, j + 1))
        }

    def swapIJ(i: Int, ia: A, j: Int, ja: A): IO[Unit] =
      IO { array.update(i, ja); array.update(j, ia) }

    outerLoop(0)
  }
}

object MonixIOArray {
  import monix.eval.Task

  def bubbleSort[A](lessThanEqual0: (A, A) => Boolean)(array: Array[A]): Task[Unit] = {
    def outerLoop(i: Int): Task[Unit] =
      if (i >= array.length - 1) Task.unit else innerLoop(i, i + 1).flatMap(_ => outerLoop(i + 1))

    def innerLoop(i: Int, j: Int): Task[Unit] =
      if (j >= array.length) Task.unit
      else
        Task.eval((array(i), array(j))).flatMap {
          case (ia, ja) =>
            val maybeSwap = if (lessThanEqual0(ia, ja)) Task.unit else swapIJ(i, ia, j, ja)

            maybeSwap.flatMap(_ => innerLoop(i, j + 1))
        }

    def swapIJ(i: Int, ia: A, j: Int, ja: A): Task[Unit] =
      Task.eval { array.update(i, ja); array.update(j, ia) }

    outerLoop(0)
  }
}
