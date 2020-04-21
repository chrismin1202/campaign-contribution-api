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

import java.nio.{file => jnf}

import cats.effect.{ContextShift, IO, Timer}
import com.chrism.api.config.SystemPropertiesHandle
import com.chrism.api.log.Logging
import com.chrism.api.model.ContributionsResponse
import com.chrism.api.spark.TestSparkSessionLike
import org.apache.commons.io.FileUtils
import org.http4s.{Method, Request}
import org.json4s.native.Serialization

final class CampaignContributionServiceTest
    extends FunTestSuite
    with TestSparkSessionLike
    with CampaignContributionJson4sFormatsLike
    with SystemPropertiesHandle
    with Logging {

  import com.chrism.api.google.CivicInformationClient.ApiKeyPropKey

  test("aggregating campaign contributions for all parties") {
    withSystemPropertiesIfSet(ApiKeyPropKey) { props =>
      import org.http4s.implicits._

      import scala.concurrent.ExecutionContext.Implicits.global

      logger.info(s"The system properties '$ApiKeyPropKey' is set. Running the test case...'")

      val apiKey = props(ApiKeyPropKey)
      logger.info(s"Retrieved the Google API key: ${apiKey.head +: ("*" * (apiKey.length - 1))}")

      val baseDir = jnf.Files.createTempDirectory("test_")
      sys.addShutdownHook(FileUtils.forceDeleteOnExit(baseDir.toFile))
      val basePath = baseDir.toString
      logger.info(s"Setting the base path to $basePath")

      implicit val cs: ContextShift[IO] = IO.contextShift(global)
      implicit val timer: Timer[IO] = IO.timer(global)

      val response = CampaignContributionService
        .buildApp(apiKey, basePath)
        .run(Request[IO](Method.GET, uri"/contributions/all"))
        .unsafeRunSync()
        .as[ContributionsResponse]
        .unsafeRunSync()
      println(Serialization.write(response))
    } {
      logger.info(s"The system properties '$ApiKeyPropKey' is not set. Skipping the test case...'")
    }
  }
}
