package scalaz.zio.stream

import org.scalacheck.Arbitrary
import scala.reflect.ClassTag
import ArbitraryChunk._
import ArbitraryStream._
import org.scalacheck.Gen
import scalaz.zio.Chunk

object ArbitraryStreamChunk {

  implicit def arbStreamChunk[T: ClassTag: Arbitrary]: Arbitrary[StreamChunk[String, T]] =
    Arbitrary {
      Gen.oneOf(
        genFailingStream[Chunk[T]].map(ZStreamChunk(_)),
        genPureStream[Chunk[T]].map(StreamChunkPure(_)),
        genSucceededStream[Chunk[T]].map(ZStreamChunk(_))
      )
    }

  implicit def arbSucceededStreamChunk[T: ClassTag: Arbitrary]: Arbitrary[StreamChunk[Nothing, T]] =
    Arbitrary {
      Gen.oneOf(
        genPureStream[Chunk[T]].map(StreamChunkPure(_)),
        genSucceededStream[Chunk[T]].map(ZStreamChunk(_))
      )
    }
}
