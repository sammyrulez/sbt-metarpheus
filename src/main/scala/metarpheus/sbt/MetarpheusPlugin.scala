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
    val metarpheusConfigPath = settingKey[String]("configPath")
    val metarpheusOutputFile = settingKey[String]("outputFile")
    val openApiOutputFile = settingKey[String]("swaggerFile")
    val metarpheusTargets =  settingKey[List[String]]("targets")
    val metarpheus = taskKey[Unit]("run metarpheus")


    val apiName = settingKey[String]("API Name")
    val apiVersion = settingKey[String]("API version")
    val apiDescription = settingKey[String]("API Description")



    lazy val baseMetaSettings: Seq[Def.Setting[_]] = Seq(

      apiName in metarpheus := name.value + " API",
      apiVersion in metarpheus := version.value,
      apiDescription in metarpheus := description.value,
      metarpheus := {
        MorpheusToSwagger.log = streams.value.log
        MetaBiz(metarpheusTargets.value,metarpheusOutputFile.value,openApiOutputFile.value,(apiName in metarpheus).value,(apiVersion in metarpheus).value,(apiDescription in metarpheus).value)
      },
      metarpheusTargets := List(new File(".").getCanonicalPath() +  "/src/main/scala"),
      metarpheusOutputFile := "src/main/resources/metarpheus-api-spec.json",
      openApiOutputFile := "src/main/resources/swagger.json"


    )


  }
  import autoImport._


  override def trigger = allRequirements

  override val projectSettings =
    inConfig(Compile)(baseMetaSettings)

  object MetaBiz {

    def apply(targets:List[String],metarpheusOut:String,openApiOut:String, apiName:String, apiVersion:String,apiDescr:String ) = {


      implicit val parseConfig: Configuration = Configuration.default.withDefaults
      val config = Config.default.copy(wiro = true, verbose = false)


     // println("current config " + config)

      val api = Metarpheus.run(targets, config)

    //  println("Api " + api )

      val serializedAPI = repr.serializeAPI(api)





      val openApi = MorpheusToSwagger.morpheusToSwagger(api,apiName,apiDescr,apiVersion)


      val serializedOpenAPI = repr.serializeOpenAPI(openApi)


  //    println(" \n ----- \n " + config)

      writeFile(serializedAPI, metarpheusOut)

      writeFile(serializedOpenAPI, openApiOut)

    }
  }
  def writeFile(serializedAPI: String, path: String) = {
    val f = new File(path)
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
