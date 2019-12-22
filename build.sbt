name := """timeular-cli"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.1"

//TODO: there is more; debian, windows, ...
enablePlugins(DockerPlugin)

val catsVersion = "2.0.0"
libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-macros" % catsVersion,
  "org.typelevel" %% "cats-kernel" % catsVersion,
  "org.typelevel" %% "cats-laws" % catsVersion,
  "com.typesafe" % "config" % "1.4.0",
  "org.apache.poi" % "poi" % "4.1.1" //for editing excel files
)

//test dependencies
libraryDependencies ++= Seq(
  "org.scalatest"              %% "scalatest"                 % "3.1.0",
  "org.scalacheck"             %% "scalacheck"                % "1.14.1",
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3"
).map(_ % "test")

dockerBaseImage := "openjdk:11"
dockerUpdateLatest := true
