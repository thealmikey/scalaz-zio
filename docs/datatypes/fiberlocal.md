---
id: datatypes_fiberlocal
title:  "FiberLocal"
---

A `FiberLocal[A]` is a container for fiber-local storage that enables you to bind a value of type `A` to a fiber. Fiber-local data can be accessed only by the fiber it is bound to, for as long as it remains bound, via a `FiberLocal` reference.

`FiberLocal` is the pure equivalent of thread-local storage (e.g. Java's `ThreadLocal`) on a fiber architecture.

```scala mdoc:silent
import scalaz.zio._

for {
  local <- FiberLocal.make[Int]
  _     <- local.set(10)
  v     <- local.get
  _     <- local.empty
} yield v == Some(10)
```

## Operations

The basic operations on a `FiberLocal` are `get`, `set` and `empty`.

- `get` returns the current, possibly non-existent, fiber-bound `A` in an `Option[A]`
- `set` binds an `A` to the current fiber
- `empty` clears the current, fiber-bound `A` if it exists

When binding data to a fiber manually, it is important to always unbind that data. Otherwise, you will start "leaking" memory because old fiber-local data will never be evicted. However, in the face of cancellation and failure, using `set` and `empty` with `flatMap` is insufficient.

Instead of using `set` and `empty`, you should always use another method, `locally`, that automatically runs a specified `IO` with fiber-bound data, and guarantees that any bound data is unbound, avoiding possible leaks.

```scala mdoc:silent
for {
  local <- FiberLocal.make[Int]
  f     <- local.locally(10)(local.get).fork
  v     <- f.join
} yield v == Some(10)
```
