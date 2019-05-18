package scalaz.zio

import java.io._

class SerializableSpec(implicit ee: org.specs2.concurrent.ExecutionEnv) extends TestRuntime {

  def serializeAndBack[T](a: T): IO[_, T] = {
    import SerializableSpec._

    for {
      obj       <- IO.effectTotal(serializeToBytes(a))
      returnObj <- IO.effectTotal(getObjFromBytes[T](obj))
    } yield returnObj
  }

  def is =
    "SerializableSpec".title ^ s2"""
    Test all classes are Serializable
    verify that
      Semaphore is serializable $e1
      Clock is serializable $e2
      Queue is serializable $e3
      Ref is serializable $e4
      IO is serializable $e5
      FunctionIO is serializable $e6
      FiberStatus is serializable $e7
      Duration is serializable $e8
    """

  def e1 = {
    val n = 20L
    unsafeRun(
      for {
        semaphore   <- Semaphore.make(n)
        count       <- semaphore.available
        returnSem   <- serializeAndBack(semaphore)
        returnCount <- returnSem.available
      } yield returnCount must_=== count
    )
  }

  def e2 =
    unsafeRun(
      for {
        time1 <- clock.nanoTime
        time2 <- serializeAndBack(clock.nanoTime).flatten
      } yield (time1 <= time2) must beTrue
    )

  def e3 = unsafeRun(
    for {
      queue       <- Queue.bounded[Int](100)
      _           <- queue.offer(10)
      returnQueue <- serializeAndBack(queue)
      v1          <- returnQueue.take
      _           <- returnQueue.offer(20)
      v2          <- returnQueue.take
    } yield (v1 must_=== 10) and (v2 must_=== 20)
  )

  def e4 = {
    val current = "This is some value"
    unsafeRun(
      for {
        ref       <- Ref.make(current)
        returnRef <- serializeAndBack(ref)
        value     <- returnRef.get
      } yield value must_=== current
    )
  }

  def e5 = {
    val list = List("1", "2", "3")
    val io   = IO.succeedLazy(list)
    unsafeRun(
      for {
        returnIO <- serializeAndBack(io)
        result   <- returnIO
      } yield result must_=== list
    )
  }

  def e6 = {
    import FunctionIO._
    val v = fromFunction[Int, Int](_ + 1)
    unsafeRun(
      for {
        returnKleisli <- serializeAndBack(v)
        computeV      <- returnKleisli.run(9)
      } yield computeV must_=== 10
    )
  }

  def e7 = {
    val list = List("1", "2", "3")
    val io   = IO.succeed(list)
    val exit = unsafeRun(
      for {
        fiber          <- io.fork
        status         <- fiber.await
        returnedStatus <- serializeAndBack(status)
      } yield returnedStatus
    )
    val result = exit match {
      case Exit.Success(value) => value
      case _                   => List.empty
    }
    result must_=== list
  }

  def e8 = {
    import scalaz.zio.duration.Duration
    val duration = Duration.fromNanos(1)
    val returnDuration = unsafeRun(
      for {
        returnDuration <- serializeAndBack(duration)
      } yield returnDuration
    )

    returnDuration must_=== duration
  }
}

object SerializableSpec {
  def serializeToBytes[T](a: T): Array[Byte] = {
    val bf  = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bf)
    oos.writeObject(a)
    oos.close()
    bf.toByteArray
  }

  def getObjFromBytes[T](bytes: Array[Byte]): T = {
    val ios = new ObjectInputStream(new ByteArrayInputStream(bytes))
    ios.readObject().asInstanceOf[T]
  }

  def serializeAndDeserialize[T](a: T): T = getObjFromBytes(serializeToBytes(a))
}
