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
package com.chrism.api.google.model.representatives

import cats.effect.IO
import org.http4s.EntityDecoder
import org.http4s.json4s.native.jsonExtract
import org.json4s.FieldSerializer.{renameFrom, renameTo}
import org.json4s.{FieldSerializer, Formats}

/** Represents a (social) media channel for an official/candidate.
  *
  * @param id the unique public identifier for the candidate's channel
  * @param channelType the type of channel, e.g., GooglePlus, YouTube, Facebook, Twitter
  */
final case class Channel(id: String, channelType: String) {}

object Channel {

  @transient
  lazy val fieldSerializer: FieldSerializer[Channel] =
    FieldSerializer[Channel](
      serializer = renameTo("channelType", "type"),
      deserializer = renameFrom("type", "channelType"))

  implicit def decoder(implicit formats: Formats): EntityDecoder[IO, Channel] = jsonExtract[IO, Channel]
}
