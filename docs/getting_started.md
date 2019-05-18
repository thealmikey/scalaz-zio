---
id: getting_started
title:  "Getting Started"
---

Include ZIO in your project by adding the following to your `build.sbt` file:
```scala mdoc:passthrough
println(s"""```""")
if (scalaz.zio.BuildInfo.isSnapshot)
  println(s"""resolvers += Resolver.sonatypeRepo("snapshots")""")
println(s"""libraryDependencies += "org.scalaz" %% "scalaz-zio" % "${scalaz.zio.BuildInfo.version}"""")
println(s"""```""")
```

In case you want to have ZIO streams at your disposal, the following dependency has to be included:

```scala mdoc:passthrough
println(s"""```""")
if (scalaz.zio.BuildInfo.isSnapshot)
  println(s"""resolvers += Resolver.sonatypeRepo("snapshots")""")
println(s"""libraryDependencies += "org.scalaz" %% "calaz-zio-streams" % "${scalaz.zio.BuildInfo.version}"""")
println(s"""```""")
```

## Main

Your application can extend `App`, which provides a complete runtime system and allows you to write your whole program using ZIO:

```scala mdoc:silent
import scalaz.zio.App
import scalaz.zio.console._

object MyApp extends App {

  def run(args: List[String]) =
    myAppLogic.fold(_ => 1, _ => 0)

  val myAppLogic =
    for {
      _ <- putStrLn("Hello! What is your name?")
      n <- getStrLn
      _ <- putStrLn(s"Hello, ${n}, welcome to ZIO!")
    } yield ()
}
```

If you are integrating ZIO into an existing application, using dependency injection, or do not control your main function, then you can use a custom runtime system in order to execute your ZIO programs:

```scala mdoc:silent
import scalaz.zio._
import scalaz.zio.console._

object IntegrationExample {
  val runtime = new DefaultRuntime {}

  runtime.unsafeRun(putStrLn("Hello World!"))
}
```

Ideally, your application should have a single runtime, because each runtime has its own resources (including thread pool and unhandled error reporter).

## Console

ZIO provides a module for interacting with the console. You can import the functions in this module with the following code snippet:

```scala mdoc:silent
import scalaz.zio.console._
```

### Printing Output

If you need to print text to the console, you can use `putStr` and `putStrLn`:

```scala mdoc
// Print without trailing line break
putStr("Hello World")

// Print string and include trailing line break
putStrLn("Hello World")
```

### Reading Input

If you need to read input from the console, you can use `getStrLn`:

```scala mdoc
val echo = getStrLn flatMap putStrLn
```

## Learning More

To learn more about ZIO, see the [Overview](overview/index.md).
