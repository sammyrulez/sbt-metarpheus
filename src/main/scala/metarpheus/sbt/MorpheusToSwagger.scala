package metarpheus.sbt

import io.buildo.metarpheus.core.intermediate.Type.{Apply, Name}
import io.buildo.metarpheus.core.intermediate.{API, CaseClass, RouteParam}
import io.swagger.models.parameters.{AbstractSerializableParameter, _}
import io.swagger.models._
import io.swagger.models.auth.ApiKeyAuthDefinition

import scala.collection.JavaConverters._
import io.swagger.models.auth.In
import io.swagger.models.properties._
import sbt.internal.util.ManagedLogger

object MorpheusToSwagger {

  var log: ManagedLogger = _




  def morpheusToSwagger(input: API, name: String, description: String, version: String): Swagger = {


    val swagger = new Swagger()
    swagger.setBasePath("/api")
    swagger.setHost("localhost")
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

      log.info("processing route " + route.name.mkString(""))
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

      operation.setProduces(List("application/json").asJava)

      val response: Response = new Response

      response.setDescription("Response for " + route.name.mkString("_"))

      val outputName = swaggerType(route.returns).getOrElse("type","string")


      val schemaResponse = outputName match  {
        case "array" => {
          val itemRef = new RefProperty()
          itemRef.set$ref( route.returns.asInstanceOf[Apply].args.head.asInstanceOf[Name].name)
          new ArrayProperty().items(itemRef)
        }
        case "object" => {
          val itemRef = new RefProperty()
          itemRef.set$ref( route.returns.asInstanceOf[Name].name)
          itemRef
        }
        case _ => new AbstractProperty(){}
      }

      schemaResponse.setType(outputName)
      response.setSchema(schemaResponse)

      operation.response(200,response)



      val path = new Path
      route.method match {
        case "post" => path.setPost(operation)
        case "get" => path.setGet(operation)
      }
      ("/"+route.name.mkString("/"), path)
    }).toMap

    val definitions : Map[String,Model] = input.models.map{
      model => {
        val impl = new ModelImpl()
        impl.setType("object")
        model.asInstanceOf[CaseClass].members.foreach( member => impl.addProperty(member.name,new StringProperty().rename(member.name))) //TODO right types
        (model.name,impl)}
    }.toMap


    swagger.setDefinitions(definitions.asJava)
    swagger.info(info).paths(paths.asJava)


  }

  def typeNameToSwaggerType(innerTypeName :String):Map[String,String] = {



    innerTypeName match {
      case "String" => Map("type" -> "string")
      case "Int" => Map("type" -> "integer", "format" -> "int32")
      case "Long" => Map("type" -> "integer", "format" -> "int64")
      case "Float" => Map("type" -> "number", "format" -> "float")
      case "Double" => Map("type" -> "number", "format" -> "double")
      case "Date" => Map("type" -> "string", "format" -> "date")
      case "DateTime" => Map("type" -> "string", "format" -> "date-time")
      case "Boolean" => Map("type" -> "boolean")
      case "List" => Map("type" -> "array")
      case _ => Map("type" -> "object")
    }
  }


  private def buildReqParameter(  param: RouteParam):QueryParameter = {
    buildParameter(param,new QueryParameter).asInstanceOf[QueryParameter]
  }

  private def buildBodyParameter(  param: RouteParam):Parameter = {

    swaggerType(param.tpe).getOrElse("type","string") match {
      case "object" => {
        val ref = new RefModel()
        ref.set$ref(param.tpe.asInstanceOf[Name].name)
        buildParameter(param,new BodyParameter).asInstanceOf[BodyParameter].schema(ref)
      }

      case _  =>  buildParameter(param,new QueryParameter).asInstanceOf[QueryParameter]
    }

  }


  private def swaggerType( tpe: io.buildo.metarpheus.core.intermediate.Type ):Map[String,String] = {

    val  typeName =tpe match {
      case n:Name =>  n.name
      case a:Apply => a.name

    }

    log.info("\t processing typeName " +typeName)

    typeName match {
        case "String" => Map("type" -> "string")
        case "Int" => Map("type" -> "integer", "format" -> "int32")
        case "Long" => Map("type" -> "integer", "format" -> "int64")
        case "Float" => Map("type" -> "number", "format" -> "float")
        case "Double" => Map("type" -> "number", "format" -> "double")
        case "Date" => Map("type" -> "string", "format" -> "date")
        case "DateTime" => Map("type" -> "string", "format" -> "date-time")
        case "Boolean" => Map("type" -> "boolean")
        case "Option" => Map("required" -> "false") ++ typeNameToSwaggerType(tpe.toString)
        case "List" => Map("type" -> "array")
        case _ => Map("type" -> "object")
      }



  }


  private def buildParameter(  param: RouteParam , parameter: AbstractParameter):AbstractParameter = {

    val name = param.name.getOrElse("param")
    val descr = param.desc.getOrElse(name +" parameter")
    parameter.setName(name)
    parameter.setDescription(descr)

      parameter.setRequired(param.required)



    parameter match {
      case sp: AbstractSerializableParameter[_] => {
        val swaggerParamType = swaggerType(param.tpe)
        sp.setType(swaggerParamType.getOrElse("type","object"))
        sp.setFormat(swaggerParamType.getOrElse("format",null))

      }
      case _ => println()
    }

    return parameter
  }



}
