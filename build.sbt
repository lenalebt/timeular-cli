name := """timeular-cli"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.1"

//TODO: there is more; debian, windows, ...
enablePlugins(DockerPlugin)

val catsVersion   = "2.0.0"
val circeVersion  = "0.12.3"
val playWsVersion = "2.1.2"
libraryDependencies ++= Seq(
  "org.typelevel"              %% "cats-core"               % catsVersion,
  "org.typelevel"              %% "cats-macros"             % catsVersion,
  "org.typelevel"              %% "cats-kernel"             % catsVersion,
  "org.typelevel"              %% "cats-laws"               % catsVersion,
  "io.circe"                   %% "circe-core"              % circeVersion,
  "io.circe"                   %% "circe-generic"           % circeVersion,
  "io.circe"                   %% "circe-parser"            % circeVersion,
  "com.typesafe.play"          %% "play-ahc-ws-standalone"  % playWsVersion,
  "com.typesafe.play"          %% "play-ws-standalone-json" % playWsVersion,
  "com.typesafe.play"          %% "play-json"               % "2.8.1",
  "com.typesafe.scala-logging" %% "scala-logging"           % "3.9.2",
  "com.github.scopt"           %% "scopt"                   % "4.0.0-RC2",
  "ch.qos.logback"             % "logback-classic"          % "1.2.3",
  "com.typesafe"               % "config"                   % "1.4.0",
  "org.apache.poi"             % "poi"                      % "4.1.1" //for editing excel files
)

//test dependencies
libraryDependencies ++= Seq(
  "org.scalatest"              %% "scalatest"                 % "3.1.0",
  "org.scalacheck"             %% "scalacheck"                % "1.14.1",
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3"
).map(_ % "test")

dockerBaseImage := "openjdk:11"
dockerUpdateLatest := true
