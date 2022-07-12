import sbt.Keys.libraryDependencies

lazy val V = _root_.scalafix.sbt.BuildInfo
inThisBuild(
  List(
    scalaVersion := V.scala212,
    crossScalaVersions := List(V.scala213, V.scala212, V.scala211),
    organization := "com.example",
    homepage := Some(url("https://github.com/com/example")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "example-username",
        "Example Full Name",
        "example@email.com",
        url("https://example.com")
      )
    ),
    addCompilerPlugin(scalafixSemanticdb),
    scalacOptions ++= List(
      "-Yrangepos",
      "-P:semanticdb:synthetics:on",
      "-Ywarn-unused"
    )
  )
)

skip in publish := true

lazy val rules = project.settings(
  moduleName := "scalameta",
  libraryDependencies += "org.scalameta" %% "scalameta" % "4.3.22",
  moduleName := "scalafix",
  libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % V.scalafixVersion

)

lazy val input = project.settings(
  skip in publish := true,
  libraryDependencies := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      // if Scala 2.12+ is used, use scala-swing 2.x
      case Some((2, scalaMajor)) if scalaMajor >= 12 =>
        libraryDependencies.value ++ Seq(
          "org.apache.spark" %% "spark-sql" % "2.4.4",
          "org.apache.spark" %% "spark-streaming" % "2.4.4" % "provided",
          "org.apache.spark" %% "spark-mllib" % "2.4.4" % "runtime")
      case Some((2, scalaMajor)) if scalaMajor >= 11 =>
        libraryDependencies.value ++ Seq(
          "org.apache.spark" %% "spark-core" % "1.6.2",
          "org.apache.spark" %% "spark-mllib" % "1.4.0" % "runtime")
      case _ =>
        // or just libraryDependencies.value if you don't depend on scala-swing
        libraryDependencies.value :+ "org.scala-lang" % "scala-swing" % scalaVersion.value
    }
  }
)

lazy val output = project.settings(
  skip in publish := true,
  libraryDependencies := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      // if Scala 2.12+ is used, use scala-swing 2.x
      case Some((2, scalaMajor)) if scalaMajor >= 12 =>
        libraryDependencies.value ++ Seq(
          "org.apache.spark" %% "spark-sql" % "2.4.4",
          "org.apache.spark" %% "spark-streaming" % "2.4.4" % "provided",
          "org.apache.spark" %% "spark-mllib" % "2.4.4" % "runtime")
      case Some((2, scalaMajor)) if scalaMajor >= 11 =>
        libraryDependencies.value ++ Seq(
          "org.apache.spark" %% "spark-core" % "1.6.2",
          "org.apache.spark" %% "spark-mllib" % "1.4.0" % "runtime")
      case _ =>
        // or just libraryDependencies.value if you don't depend on scala-swing
        libraryDependencies.value :+ "org.scala-lang" % "scala-swing" % scalaVersion.value
    }
  }
)

lazy val tests = project
  .settings(
    skip in publish := true,
    libraryDependencies += "ch.epfl.scala" % "scalafix-testkit" % V.scalafixVersion % Test cross CrossVersion.full,
    compile.in(Compile) := 
      compile.in(Compile).dependsOn(compile.in(input, Compile)).value,
    scalafixTestkitOutputSourceDirectories :=
      sourceDirectories.in(output, Compile).value,
    scalafixTestkitInputSourceDirectories :=
      sourceDirectories.in(input, Compile).value,
    scalafixTestkitInputClasspath :=
      fullClasspath.in(input, Compile).value,
  )
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)
