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
package com.chrism.api.spark

import cats.effect.{IO, Resource}
import com.chrism.api.MainLike
import org.apache.spark.sql.SparkSession

trait SparkApplicationLike extends MainLike with SparkSessionLike {

  def runSpark(args: Array[String])(implicit spark: SparkSession): Unit

  override protected final def getOrCreateSparkSession(): SparkSession =
    SparkSession.builder().master("local[*]").getOrCreate()

  override final def run(args: Array[String]): Unit =
    makeSparkSession().use(spark => IO(runSpark(args)(spark))).unsafeRunSync()

  private[this] def makeSparkSession(): Resource[IO, SparkSession] =
    Resource.make(IO(getOrCreateSparkSession()))(spark => IO(spark.stop()))
}
