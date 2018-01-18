// this bit is important
sbtPlugin := true

organization := "org.github.sammyrulez"

name := "sbt-metarpheus"

version := "1.0.0"

//scalaVersion := "2.10.4"

scalaVersion := "2.12.2"

scalacOptions ++= Seq("-deprecation", "-feature")

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.bintrayRepo("buildo", "maven")

libraryDependencies ++= Seq(
  "io.buildo" %% "metarpheus-core" % "0.1.6-SNAPSHOT",
  "org.json4s" %% "json4s-jackson" % "3.5.0",
  "io.circe" %% "circe-core" % "0.8.0",
  "io.circe" %% "circe-generic-extras" % "0.8.0",
  "io.circe" %% "circe-parser" % "0.8.0",
  "io.swagger" %% "swagger-scala-module" % "1.0.3"
)





publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))