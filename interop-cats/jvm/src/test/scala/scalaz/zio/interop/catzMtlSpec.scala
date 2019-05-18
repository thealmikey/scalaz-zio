package scalaz.zio
package interop

import cats.Eq
import cats.implicits._
import cats.mtl.{ ApplicativeAsk, ApplicativeLocal }
import cats.mtl.laws.discipline.{
  ApplicativeAskTests,
  ApplicativeHandleTests,
  ApplicativeLocalTests,
  FunctorRaiseTests
}
import org.scalacheck.{ Arbitrary, Cogen }
import org.scalatest.prop.Checkers
import org.scalatest.{ BeforeAndAfterAll, FunSuite, Matchers }
import org.typelevel.discipline.scalatest.Discipline
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalaz.zio.internal.PlatformLive
import scalaz.zio.interop.catz._
import scalaz.zio.interop.catz.mtl._
import scalaz.zio.random.Random
import scalaz.zio.system.System

class catzMtlSpec extends FunSuite with BeforeAndAfterAll with Matchers with Checkers with Discipline with GenIO {

  type Env = Clock with Console with System with Random with Blocking

  implicit val rts: Runtime[Env] = new DefaultRuntime {
    override val Platform = PlatformLive.makeDefault().withReportFailure(_ => ())
  }

  type Ctx   = String
  type Error = String

  checkAll("ApplicativeAsk[ZIO[Ctx, Error, ?]]", ApplicativeAskTests[ZIO[Ctx, Error, ?], Ctx].applicativeAsk[Ctx])
  checkAll(
    "ApplicativeLocal[ZIO[Ctx, Error, ?]]",
    ApplicativeLocalTests[ZIO[Ctx, Error, ?], Ctx].applicativeLocal[Ctx, Int]
  )
  checkAll("FunctorRaise[ZIO[Ctx, Error, ?]]", FunctorRaiseTests[ZIO[Ctx, Error, ?], Error].functorRaise[Int])
  checkAll(
    "ApplicativeHandle[ZIO[Ctx, Error, ?]]",
    ApplicativeHandleTests[ZIO[Ctx, Error, ?], Error].applicativeHandle[Int]
  )

  def askSummoner[R, E]   = ApplicativeAsk[ZIO[R, E, ?], R]
  def localSummoner[R, E] = ApplicativeLocal[ZIO[R, E, ?], R]

  implicit def catsEQ[R: Arbitrary, E, A: Eq]: Eq[ZIO[R, E, A]] =
    new Eq[ZIO[R, E, A]] {
      import scalaz.zio.duration._

      def eqv(io1: ZIO[R, E, A], io2: ZIO[R, E, A]): Boolean = {
        val ctx = Arbitrary.arbitrary[R].sample.get
        val v1  = rts.unsafeRunSync(io1.provide(ctx).timeout(20.seconds)).map(_.get)
        val v2  = rts.unsafeRunSync(io2.provide(ctx).timeout(20.seconds)).map(_.get)
        val res = v1 === v2
        if (!res) {
          println(s"Mismatch: $v1 != $v2")
        }
        res
      }
    }

  implicit def zioArbitrary[E, A: Arbitrary: Cogen, R: Arbitrary: Cogen]: Arbitrary[ZIO[R, E, A]] =
    Arbitrary(genSuccess[E, A])

}
