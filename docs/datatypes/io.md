---
id: datatypes_io
title:  "IO"
---

A value of type `IO[E, A]` describes an effect that may fail with an `E`, run forever, or produce a single `A`.

`IO` values are immutable, and all `IO` functions produce new `IO` values, enabling `IO` to be reasoned about and used like any ordinary Scala immutable data structure.

`IO` values do not actually _do_ anything; they are just values that _model_ or _describe_ effectful interactions.

`IO` can be _interpreted_ by the ZIO runtime system into effectful interactions with the external world. Ideally, this occurs at a single time, in your application's `main` function. The `App` class provides this functionality automatically.

## Pure Values

You can lift pure values into `IO` with `IO.succeedLazy`:

```scala mdoc:silent
import scalaz.zio._

val lazyValue: UIO[String] = IO.succeedLazy("Hello World")
```

The constructor uses non-strict evaluation, so the parameter will not be evaluated until when and if the `IO` action is executed at runtime, which is useful if the construction is costly and the value may never be needed.

Alternately, you can use the `IO.succeed` constructor to perform strict evaluation of the value:

```scala mdoc:silent
val value: UIO[String] = IO.succeed("Hello World")
```

You should never use either constructor for importing impure code into `IO`. The result of doing so is undefined.

## Infallible IO

`IO` values of type `UIO[A]` (where the error type is `Nothing`) are considered _infallible_,
because the `Nothing` type is _uninhabitable_, i.e. there can be no actual values of type `Nothing`. Values of this type may produce an `A`, but will never fail with an `E`.

## Unproductive IO

`IO` values of type `IO[E, Nothing]` (where the value type is `Nothing`) are considered _unproductive_,
because the `Nothing` type is _uninhabitable_, i.e. there can be no actual values of type `Nothing`. Values of this type may fail with an `E`, but will never produce a value.

## Impure Code

You can use the `effectTotal` method of `IO` to import effectful synchronous code into your purely functional program:

```scala mdoc:silent
val effectTotalTask: Task[Long] = IO.effectTotal(System.nanoTime())
```

```scala mdoc:invisible
import java.io.File
import org.apache.commons.io.FileUtils
import java.io.IOException
```

The resulting effect may fail for any `Throwable`.

If this is too broad, the `refineOrDie` method of `ZIO` may be used to retain only certain types of exceptions, and to die on any other types of exceptions:

```scala mdoc:silent
def readFile(name: String): IO[String, Array[Byte]] =
  IO.effect(FileUtils.readFileToByteArray(new File(name))).refineOrDie {
    case e : IOException => "Could not read file"
  }
```

You can use the `effectAsync` method of `IO` to import effectful asynchronous code into your purely functional program:

```scala mdoc:invisible
case class HttpException()
case class Request()
case class Response()

object Http {
  def req(req: Request, k: IO[HttpException, Response] => Unit): Unit =
    k(IO.succeed(Response()))
}
```

```scala mdoc:silent
def makeRequest(req: Request): IO[HttpException, Response] =
  IO.effectAsync[Any, HttpException, Response](k => Http.req(req, k))
```

In this example, it's assumed the `Http.req` method will invoke the specified callback when the result has been asynchronously computed.

## Mapping

You can change an `IO[E, A]` to an `IO[E, B]` by calling the `map` method with a function `A => B`. This lets you transform values produced by actions into other values.

```scala mdoc:silent
import scalaz.zio._

val mappedLazyValue: UIO[Int] = IO.succeedLazy(21).map(_ * 2)
```

You can transform an `IO[E, A]` into an `IO[E2, A]` by calling the `mapError` method with a function `E => E2`:

```scala mdoc:silent
val mappedError: IO[Exception, String] = IO.fail("No no!").mapError(msg => new Exception(msg))
```

## Chaining

You can execute two actions in sequence with the `flatMap` method. The second action may depend on the value produced by the first action.

```scala mdoc:silent
val chainedActionsValue: UIO[List[Int]] = IO.succeedLazy(List(1, 2, 3)).flatMap { list =>
  IO.succeedLazy(list.map(_ + 1))
}
```

You can use Scala's `for` comprehension syntax to make this type of code more compact:

```scala mdoc:silent
val chainedActionsValueWithForComprehension: UIO[List[Int]] = for {
  list <- IO.succeedLazy(List(1, 2, 3))
  added <- IO.succeedLazy(list.map(_ + 1))
} yield added
```

## Brackets

Brackets are a built-in primitive that let you safely acquire and release resources.

Brackets are used for a similar purpose as try/catch/finally, only brackets work with synchronous and asynchronous actions, work seamlessly with fiber interruption, and are built on a different error model that ensures no errors are ever swallowed.

Brackets consist of an *acquire* action, a *utilize* action (which uses the acquired resource), and a *release* action.

The release action is guaranteed to be executed by the runtime system, even if the utilize action throws an exception or the executing fiber is interrupted.

```scala mdoc:silent
import scalaz.zio._
```

```scala mdoc:invisible
import java.io.{ File, IOException }

def openFile(s: String): IO[IOException, File] = IO.succeedLazy(???)
def closeFile(f: File): UIO[Unit] = IO.succeedLazy(???)
def decodeData(f: File): IO[IOException, Unit] = IO.unit
def groupData(u: Unit): IO[IOException, Unit] = IO.unit
```

```scala mdoc:silent
val groupedFileData: IO[IOException, Unit] = openFile("data.json").bracket(closeFile(_)) { file =>
  for {
    data    <- decodeData(file)
    grouped <- groupData(data)
  } yield grouped
}
```

Brackets have compositional semantics, so if a bracket is nested inside another bracket, and the outer bracket acquires a resource, then the outer bracket's release will always be called, even if, for example, the inner bracket's release fails.

A helper method called `ensuring` provides a simpler analogue of `finally`:

```scala mdoc:silent
var i: Int = 0
val action: Task[String] = Task.effectTotal(i += 1) *> Task.fail(new Throwable("Boom!"))
val cleanupAction: UIO[Unit] = UIO.effectTotal(i -= 1)
val composite = action.ensuring(cleanupAction)
```
