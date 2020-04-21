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

import cats.effect.IO
import org.http4s.Response

final class ApiException(response: Response[IO]) extends Exception(ExceptionUtils.formatErrorMessage(response))

final class NoResultsException(name: String) extends Exception(s"No contribution record found for the candidate $name")

private[this] object ExceptionUtils {

  private[api] def formatErrorMessage(response: Response[IO]): String =
    s"""Status code ${response.status.code} returned from the server!
       |Reason: ${response.status.reason}""".stripMargin
}
