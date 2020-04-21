/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chrism.api

import org.http4s.EntityDecoder
import org.http4s.client.middleware.Logger

private[api] trait ApiClientLike {

  import cats.effect._
  import org.http4s.client.blaze._
  import org.http4s.client._
  import scala.concurrent.ExecutionContext.global

  protected implicit final lazy val cs: ContextShift[IO] = IO.contextShift(global)

  private[this] lazy val resource: Resource[IO, Client[IO]] =
    BlazeClientBuilder[IO](global).resource.map(Logger(true, true)(_))

  protected final def call[A](callFunc: Client[IO] => IO[A])(implicit d: EntityDecoder[IO, A]): IO[A] =
    resource.use(callFunc)

  protected final def callSync[A](callFunc: Client[IO] => IO[A])(implicit d: EntityDecoder[IO, A]): A =
    call(callFunc).unsafeRunSync()
}
