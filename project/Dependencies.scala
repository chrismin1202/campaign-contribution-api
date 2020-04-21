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
object Dependencies {

  import sbt._

  private val Json4sGroupId: String = "org.json4s"
  private val Http4sGroupId: String = "org.http4s"
  private val SparkGroupId: String = "org.apache.spark"

  // Note that is project is used in Spark applications; therefore,
  // the versions of some of these dependencies are set to match those of Spark dependencies.
  private val SparkVersion: String = "2.4.5"
  private val CommonsLang3Version: String = "3.5"
  private val CommonsIoVersion: String = "2.6"
  private val Json4sVersion: String = "3.5.3"
  private val Http4sVersion: String = "0.21.3"

  private val ScalacheckVersion: String = "1.14.0"
  private val ScalatestVersion: String = "3.0.8"
  private val Specs2CoreVersion: String = "4.7.0"

  val SourceCode: ModuleID = "com.lihaoyi" %% "sourcecode" % "0.2.1"
  val CommonsLang3: ModuleID = "org.apache.commons" % "commons-lang3" % CommonsLang3Version
  val CommonsIo: ModuleID = "commons-io" % "commons-io" % CommonsIoVersion
  val Json4sNative: ModuleID = Json4sGroupId %% "json4s-native" % Json4sVersion

  val Http4sDsl: ModuleID = "org.http4s" %% "http4s-dsl" % Http4sVersion
  val Http4sBlazeServer: ModuleID = Http4sGroupId %% "http4s-blaze-server" % Http4sVersion
  val Http4sBlazeClient: ModuleID = Http4sGroupId %% "http4s-blaze-client" % Http4sVersion

  val Http4sJson4sNative: ModuleID = (Http4sGroupId %% "http4s-json4s-native" % Http4sVersion)
    .excludeAll(ExclusionRule(Json4sGroupId))

  val Log4s: ModuleID = "org.log4s" %% "log4s" % "1.8.2"

  val SparkCore: ModuleID = SparkGroupId %% "spark-core" % SparkVersion
  val SparkSql: ModuleID = SparkGroupId %% "spark-sql" % SparkVersion
  val SparkHive: ModuleID = SparkGroupId %% "spark-hive" % SparkVersion

  val SaajImpl: ModuleID = "com.sun.xml.messaging.saaj" % "saaj-impl" % "1.5.1"
  val Scalacheck: ModuleID = "org.scalacheck" %% "scalacheck" % ScalacheckVersion
  val Scalatest: ModuleID = "org.scalatest" %% "scalatest" % ScalatestVersion
  val Specs2Core: ModuleID = "org.specs2" %% "specs2-core" % Specs2CoreVersion
}
