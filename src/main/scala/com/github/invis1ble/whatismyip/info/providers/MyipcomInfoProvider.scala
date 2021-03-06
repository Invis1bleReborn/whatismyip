package com.github.invis1ble.whatismyip.info.providers

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{HttpRequest, MediaTypes, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.github.invis1ble.whatismyip.info._
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}

object InfoProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val locationFormat: JsonFormat[Location] = jsonFormat(Location, "country", "cc")

  implicit object InfoFormat extends RootJsonFormat[Info] {
    def read(json: JsValue): Info = {
      val jsObject = json.asJsObject
      jsObject.getFields("ip", "country", "cc") match {
        case List(JsString(ip), _*) => Info(
          ip,
          jsObject.convertTo[Option[Location]]
        )
        case unrecognized => deserializationError(s"JSON deserialization error: $unrecognized.")
      }
    }

    def write(info: Info): JsValue = ???
  }
}

class MyipcomInfoProvider(implicit system: ActorSystem[Nothing]) extends InfoProvider {
  import com.github.invis1ble.whatismyip.info.providers.InfoProtocol._

  def info: Future[Info] = {
    implicit val ec: ExecutionContextExecutor = system.executionContext
    val acceptedMediaType = MediaTypes.`application/json`

    Http().singleRequest(
      HttpRequest(uri = "https://api.myip.com", headers = Seq(Accept(acceptedMediaType)))
    )
      .flatMap { response =>
        response.status match {
          case StatusCodes.OK => Unmarshal(response.entity.withContentType(acceptedMediaType)).to[Info]
          case _ =>
            response.discardEntityBytes()
            throw new RuntimeException(s"Expected 200 HTTP status, got ${response.status}.")
        }
      }
  }
}

object MyipcomInfoProvider {
  def apply()(implicit system: ActorSystem[Nothing]) = new MyipcomInfoProvider
}