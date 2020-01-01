name := """timeular-cli"""

version := "0.2-SNAPSHOT"

scalaVersion := "2.13.1"

maintainer := "Lena Brüder <oss@lena-brueder.de>"

enablePlugins(JavaAppPackaging)
mainClass in Compile := Some("de.lenabrueder.timeular.Main")
discoveredMainClasses in Compile := Seq()

//TODO: this one needs more configuration and an oracle JDK8
//see https://www.scala-sbt.org/sbt-native-packager/formats/jdkpackager.html
enablePlugins(JDKPackagerPlugin)

val catsVersion   = "2.0.0"
val circeVersion  = "0.12.3"
val playWsVersion = "2.1.2"
val sttpVersion   = "2.0.0-RC5"
libraryDependencies ++= Seq(
  "org.typelevel"                %% "cats-core"      % catsVersion,
  "org.typelevel"                %% "cats-macros"    % catsVersion,
  "org.typelevel"                %% "cats-kernel"    % catsVersion,
  "org.typelevel"                %% "cats-laws"      % catsVersion,
  "com.chuusai"                  %% "shapeless"      % "2.3.3",
  "io.circe"                     %% "circe-core"     % circeVersion,
  "io.circe"                     %% "circe-generic"  % circeVersion,
  "io.circe"                     %% "circe-parser"   % circeVersion,
  "io.circe"                     %% "circe-yaml"     % "0.12.0",
  "com.softwaremill.sttp.client" %% "core"           % sttpVersion,
  "com.softwaremill.sttp.client" %% "circe"          % sttpVersion,
  "com.typesafe.scala-logging"   %% "scala-logging"  % "3.9.2",
  "com.github.scopt"             %% "scopt"          % "4.0.0-RC2",
  "ch.qos.logback"               % "logback-classic" % "1.2.3",
  "com.typesafe"                 % "config"          % "1.4.0",
  "org.apache.poi"               % "poi"             % "4.1.1" //for editing excel files
)

//test dependencies
libraryDependencies ++= Seq(
  "org.scalatest"              %% "scalatest"                 % "3.1.0",
  "org.scalacheck"             %% "scalacheck"                % "1.14.1",
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3"
).map(_ % "test")

fork in run := true

dockerBaseImage := "openjdk:11"
dockerUpdateLatest := true
