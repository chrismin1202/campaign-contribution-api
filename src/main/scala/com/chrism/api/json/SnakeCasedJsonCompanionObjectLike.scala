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
package com.chrism.api.json

import cats.effect.IO
import org.http4s.json4s.native.jsonDecoder
import org.http4s.{DecodeFailure, DecodeResult, EntityDecoder, InvalidMessageBodyFailure}
import org.json4s.Formats

trait SnakeCasedJsonCompanionObjectLike {

  import cats.implicits._

  implicit def decodeSnakeCasedJson[A](
    implicit
    formats: Formats,
    manifest: Manifest[A]
  ): EntityDecoder[IO, A] =
    jsonDecoder[IO].flatMapR(json =>
      DecodeResult(
        IO.delay[Either[DecodeFailure, A]](Right(json.camelizeKeys.extract[A]))
          .handleError(e => Left(InvalidMessageBodyFailure("Could not extract JSON", Some(e))))))
}
