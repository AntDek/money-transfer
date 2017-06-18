package antdek.moneytransfer.http

import akka.http.scaladsl.marshalling.{Marshaller, _}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling._
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag


trait JacksonSupport {

  implicit def JacksonMarshaller: ToEntityMarshaller[AnyRef] = {
    Marshaller.withFixedContentType(`application/json`) { obj =>
      HttpEntity(`application/json`, Json.generate(obj).getBytes("UTF-8"))
    }
  }

  implicit def JacksonUnmarshaller[T <: AnyRef: Manifest](implicit c: ClassTag[T]): FromRequestUnmarshaller[T] = {
    new FromRequestUnmarshaller[T] {
      override def apply(request: HttpRequest)(implicit ec: ExecutionContext, materializer: Materializer): Future[T] = {
        import scala.concurrent.duration._
        import scala.language.postfixOps

        request.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8")).map { str =>
          Json.parse[T](str)
        }
      }
    }
  }

  implicit def JacksonResponseUnmarshaller[T <: AnyRef: Manifest](implicit c: ClassTag[T]): FromResponseUnmarshaller[T] = {
    new FromResponseUnmarshaller[T] {
      override def apply(response: HttpResponse)(implicit ec: ExecutionContext, materializer: Materializer): Future[T] = {
        import scala.concurrent.duration._
        import scala.language.postfixOps

        response.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8")).map { str =>
          Json.parse[T](str)
        }
      }
    }
  }

}

