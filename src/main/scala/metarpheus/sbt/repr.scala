package metarpheus.sbt

import io.buildo.metarpheus.core.intermediate.API

package object repr {

  import org.json4s._
  import org.json4s.jackson.JsonMethods._
  import org.json4s.jackson.Serialization

  implicit val formats = Serialization.formats(NoTypeHints)

  def serializeAPI(api: API): String = pretty(Extraction.decompose(api))

}
