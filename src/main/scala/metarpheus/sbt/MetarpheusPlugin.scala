package metarpheus.sbt

import java.io.File

import io.buildo.metarpheus.core.Config
import io.buildo.metarpheus.core.Metarpheus
import io.circe.parser.decode
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import sbt._
import sbt.Keys._

import scala.io.Source
import scala.util.{Left, Right}

object MetarpheusPlugin extends AutoPlugin {

  object autoImport {
    val greeting = settingKey[String]("greeting")
    val metarpheusConfigPath = settingKey[String]("configPath")
    val metarpheusOutputFile = settingKey[String]("outputFile")
    val metarpheusWiro = settingKey[Boolean]("wiro")
    val metarpheusTargets =  settingKey[List[String]]("targets")

    val metarpheus = taskKey[Unit]("run metarpheus")
  }
  import autoImport._
  override def trigger = allRequirements



  override lazy val globalSettings = Seq(
    greeting := "Hi!",
    metarpheusWiro := true,
    metarpheusTargets := List(new File(".").getCanonicalPath() +  "/src/main/scala"),
    metarpheusOutputFile := "src/main/resources/metarpheus-api-spec.json",
    metarpheus := metarpheusTask.value
  )
  lazy val metarpheusTask =
    Def.task {
      println("Processing metarpheus verbose getCanonicalPath ")


      implicit val parseConfig: Configuration = Configuration.default.withDefaults
      val config = Config.default.copy(wiro = metarpheusWiro.value, verbose = false)


     // println("current config " + config)

      val api = Metarpheus.run(metarpheusTargets.value, config)

    //  println("Api " + api )

      val serializedAPI = repr.serializeAPI(api)

  //    println(" \n ----- \n " + config)

      val f = new File(metarpheusOutputFile.value)
      val p = new java.io.PrintWriter(f)
      try {
        p.println(serializedAPI)
      } finally {
        p.close()
      }

    }

  def toOption(e:Either[_,_]) = e match {
    case Left(a) => Some(a)
    case Right(_) => None
  }

}
