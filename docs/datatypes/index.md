---
id: datatypes_index
title:  "Summary"
---

ZIO contains a small number of data types that can help you solve complex problems in asynchronous and concurrent programming.

 - **[Fiber](fiber.md)** — A fiber value models an `IO` value that has started running, and is the moral equivalent of a green thread.
 - **[FiberLocal](fiberlocal.md)** — A `FiberLocal` is a variable whose value depends on the fiber that accesses it, and is the moral equivalent of Java's `ThreadLocal`.
 - **[ZIO](io.md)** — A `ZIO` is a value that models an effectful program, which might fail or succeed.
 - **[Managed](managed.md)** — A `Managed` is a value that describes an `IO` perishable resource that may be consumed only once inside a given scope.
 - **[Promise](promise.md)** — A `Promise` is a model of a variable that may be set a single time, and awaited on by many fibers.
 - **[Queue](queue.md)** — A `Queue` is an asynchronous queue that never blocks, which is safe for multiple concurrent producers and consumers.
 - **[Ref](ref.md)** — `Ref[A]` models a mutable reference to a value of type `A`. The two basic operations are `set`, which fills the `Ref` with a new value, and `get`, which retrieves its current content. All operations on a `Ref` are atomic and thread-safe, providing a reliable foundation for synchronizing concurrent programs.
 - **[Schedule](schedule.md)** — A `Schedule` is a model of a recurring schedule, which can be used for repeating successful `IO` values, or retrying failed `IO` values.
 - **[Semaphore](semaphore.md)** — A `Semaphore` is an asynchronous (non-blocking) semaphore that plays well with ZIO's interruption.

Besides the core datatypes, the following datatypes can be found in ZIO streams library:

 - **[Sink](sink.md)** — A `Sink` is a consumer of values from a `Stream`, which may produces a value when it has consumed enough.
 - **[Stream](stream.md)** — A `Stream` is a lazy, concurrent, asynchronous source of values.

To learn more about these data types, please explore the pages above, or check out the Scaladoc documentation.
