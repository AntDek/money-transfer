package antdek.moneytransfer.http


import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object Json {
  private lazy val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m registerModule DefaultScalaModule
    m configure (DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
    m
  }

  def parse[A](json: String)(implicit m : Manifest[A]): A = mapper.readValue[A](json)

  def generate(x: Any): String = mapper writeValueAsString x
}

