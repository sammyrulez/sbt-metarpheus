package metarpheus.sbt

import io.buildo.metarpheus.core.intermediate.Type.{Apply, Name}
import io.buildo.metarpheus.core.intermediate.{API, RouteParam}
import io.swagger.models.parameters.{AbstractSerializableParameter, _}
import io.swagger.models._
import io.swagger.models.auth.ApiKeyAuthDefinition

import scala.collection.JavaConverters._
import io.swagger.models.auth.In

object MorpheusToSwagger {


  def morpheusToSwagger(input: API, name: String, description: String, version: String): Swagger = {


    val swagger = new Swagger()
    swagger.setBasePath("/api")
    swagger.setHost("") //TODO PARAMS
    val JSON = "application/json"
    swagger.produces(JSON)
    val schemes = List(Scheme.forValue("https"), Scheme.forValue("http"))
    swagger.schemes(schemes.asJava)
    swagger.securityDefinition("api_key",new ApiKeyAuthDefinition("api_key",In.HEADER))
    val securityRequirement = new SecurityRequirement("api_key")
    swagger.security(securityRequirement)


    val info = new Info().description(description)
    info.setTitle(name)
    info.setVersion(version)

   // val security = new SecurityRequirement().requirement("")

    val paths:Map[String,Path] = input.routes.map(route => {

      println("processing route " + route.name.mkString(""))
      val operation = new Operation()

      operation.setOperationId(name + "__" +route.name.mkString("_"))
      operation.setProduces(List(JSON).asJava)
      operation.setSchemes(schemes.asJava)
      if(route.authenticated){
        operation.security(securityRequirement)
      }


      val bodyParams = route.params.filter(_.inBody)
      val reqParams = route.params.filter(!_.inBody)
      bodyParams.foreach(param => {
        operation.addParameter(buildBodyParameter(param)) //TODO type
      })
      reqParams.foreach(param => {
        operation.consumes("plain/text")
        operation.addParameter(buildReqParameter(param))
      })



      val path = new Path
      println("method" + route.method)
      route.method match {
        case "post" => path.setPost(operation)
        case "get" => path.setGet(operation)
      }
      (route.name.mkString("/"), path)
    }).groupBy(_._1).map { case (k,v) => (k,v.head._2)}


    swagger.info(info).paths(paths.asJava)


  }


  private def buildReqParameter(  param: RouteParam):QueryParameter = {
    buildParameter(param,new QueryParameter).asInstanceOf[QueryParameter]
  }

  private def buildBodyParameter(  param: RouteParam):BodyParameter = {
    buildParameter(param,new BodyParameter).asInstanceOf[BodyParameter]
  }

  private def buildParameter(  param: RouteParam , parameter: AbstractParameter):AbstractParameter = {

    val name = param.name.getOrElse("param")
    val descr = param.desc.getOrElse(name +" parameter")
    parameter.setName(name)
    parameter.setDescription(descr)
    println("\t processing param "+  name + " " +param.tpe.toString)
    val  typeName =param.tpe match {
      case n:Name =>  n.name
      case a:Apply => a.name

    }

    if(typeName.contains("Option"))
      parameter.setRequired(false)
    else
      parameter.setRequired(true)


    param match {
      case sp: AbstractSerializableParameter[_] => {
        val swaggerParamType = typeName match  {
          case "Int" => Map("type" -> "integer", "format" -> "int32" )
          case "Long" => Map("type" -> "integer", "format" -> "int64" )
          case "Float" => Map("type" -> "number", "format" -> "float" )
          case "Double" => Map("type" -> "number", "format" -> "double" )
          case "Date" => Map("type" -> "string", "format" -> "date" )
          case "DateTime" => Map("type" -> "string", "format" -> "date-time" )
          case "Boolean" => Map("type" -> "boolean" )
          case _ => Map("type" -> "object" )
          //case "Option" => return toSwaggerType(args[0]);
          //case "List":
        }
        sp.setType(swaggerParamType.getOrElse("type","object"))
        sp.setFormat(swaggerParamType.getOrElse("format",""))

      }
    }

    return parameter
  }



}
