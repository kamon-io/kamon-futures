/* =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */


import AspectJ._
import Settings._
import Dependencies._

lazy val `kamon-futures` = (project in file("."))
  .settings(moduleName := "kamon-futures")
  .settings(basicSettings: _*)
  .settings(formatSettings: _*)
  .settings(noPublishing: _*)
  .aggregate(`kamon-scala`, `kamon-scalaz`, `kamon-twitter`, `kamon-futures-testkit`)

lazy val `kamon-scala` = (project in file("kamon-scala"))
  .settings(moduleName := "kamon-scala")
  .settings(basicSettings: _*)
  .settings(formatSettings: _*)
  .settings(aspectJSettings: _*)
  .dependsOn(`kamon-futures-testkit` % "test->compile")
  .settings(
    libraryDependencies
      ++= compileScope(kamonCore)
      ++ providedScope(aspectJ)
      ++ testScope(scalatest, logback)
  )

lazy val `kamon-scalaz` = (project in file("kamon-scalaz"))
  .settings(moduleName := "kamon-scalaz")
  .settings(basicSettings: _*)
  .settings(formatSettings: _*)
  .settings(aspectJSettings: _*)
  .dependsOn(`kamon-futures-testkit` % "test->compile")
  .settings(
    libraryDependencies
      ++= compileScope(kamonCore, scalazConcurrent)
      ++ providedScope(aspectJ)
      ++ testScope(scalatest, logback)
  )

lazy val `kamon-twitter` = (project in file("kamon-twitter"))
  .settings(moduleName := "kamon-twitter")
  .settings(basicSettings: _*)
  .settings(formatSettings: _*)
  .settings(aspectJSettings: _*)
  .dependsOn(`kamon-futures-testkit` % "test->compile")
  .settings(
    libraryDependencies
      ++= compileScope(kamonCore, twitterDependency("core").value)
      ++ providedScope(aspectJ)
      ++ testScope(scalatest, logback)
  )

lazy val `kamon-futures-testkit` = (project in file("kamon-futures-testkit"))
  .settings(basicSettings: _*)
  .settings(formatSettings: _*)
  .settings(noPublishing: _*)
  .settings(
    libraryDependencies ++=
      compileScope(
        kamonCore,
        scalatest,
        akkaDependency("testkit").value,
        akkaDependency("slf4j").value
      )
  )

lazy val noPublishing = Seq(publish := (), publishLocal := (), publishArtifact := false)
