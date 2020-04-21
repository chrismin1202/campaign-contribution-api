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
package com.chrism.api.config

trait SystemPropertiesHandle {

  protected final def withSystemPropertiesIfSet[A, B](
    key: String,
    moreKeys: String*
  )(
    ifSet: Map[String, String] => A
  )(
    ifNotSet: => B
  ): Either[A, B] = {
    val props = sys.props
    val values = (key +: moreKeys).map(k => k -> props.get(k))
    if (values.forall(_._2.isDefined)) Left(ifSet(values.map(kv => kv._1 -> kv._2.get).toMap))
    else Right(ifNotSet)
  }

  protected final def withSystemPropertiesIfSetOrDefault[A](
    key: String,
    moreKeys: String*
  )(
    ifSet: Map[String, String] => A
  )(
    default: => A
  ): A = {
    val props = sys.props
    val values = (key +: moreKeys).map(k => k -> props.get(k))
    if (values.forall(_._2.isDefined)) ifSet(values.map(kv => kv._1 -> kv._2.get).toMap)
    else default
  }

  protected final def withSystemPropertiesIfSetOrNone[A](
    key: String,
    moreKeys: String*
  )(
    ifSet: Map[String, String] => A
  ): Option[A] = {
    val props = sys.props
    val values = (key +: moreKeys).map(k => k -> props.get(k))
    if (values.forall(_._2.isDefined)) Some(ifSet(values.map(kv => kv._1 -> kv._2.get).toMap))
    else None
  }

  protected final def withSystemProperties[A](key: String, moreKeys: String*)(func: Map[String, String] => A): A = {
    val props = sys.props
    func((key +: moreKeys).map(k => k -> props(k)).toMap)
  }
}
