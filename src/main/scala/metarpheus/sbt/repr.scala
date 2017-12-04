package metarpheus.sbt

import com.fasterxml.jackson.annotation.JsonInclude.Include
import io.buildo.metarpheus.core.intermediate.API
import io.swagger.models.Swagger

package object repr {

  import org.json4s._
  import org.json4s.jackson.JsonMethods._
  import org.json4s.jackson.Serialization
  import com.fasterxml.jackson.databind.ObjectMapper

  implicit val formats = Serialization.formats(NoTypeHints)

  def serializeAPI(api: API): String = pretty(Extraction.decompose(api))

  def serializeOpenAPI(api: Swagger): String = {
    val mapper1 = new ObjectMapper()
    mapper1.setSerializationInclusion(Include.NON_NULL)
    mapper1.writerWithDefaultPrettyPrinter().writeValueAsString(api)
  }

}
