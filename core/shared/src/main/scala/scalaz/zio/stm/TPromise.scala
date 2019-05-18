/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
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

package scalaz.zio.stm

class TPromise[E, A] private (val ref: TRef[Option[Either[E, A]]]) extends AnyVal {
  final def await: STM[E, A] =
    ref.get.collect {
      case Some(e) => STM.fromEither(e)
    }.flatten

  final def done(v: Either[E, A]): STM[Nothing, Boolean] =
    for {
      value <- ref.get
      change <- value match {
                 case Some(_) => STM.succeed(false)
                 case None =>
                   ref.set(Some(v)) *> STM.succeed(true)
               }
    } yield change

  final def fail(e: E): STM[Nothing, Boolean] =
    done(Left(e))

  final def poll: STM[Nothing, Option[STM[E, A]]] =
    ref.get.map {
      case Some(e) => Some(STM.fromEither(e))
      case None    => None
    }

  final def succeed(a: A): STM[Nothing, Boolean] =
    done(Right(a))
}

object TPromise {
  final def make[E, A]: STM[Nothing, TPromise[E, A]] =
    TRef.make[Option[Either[E, A]]](None).map(ref => new TPromise(ref))
}
