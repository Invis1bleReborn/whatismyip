package com.github.invis1ble.whatismyip.info.providers

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.MediaType.WithFixedCharset
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import com.github.invis1ble.whatismyip.info._
import spray.json._

import scala.concurrent.duration.{DurationInt, FiniteDuration}
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

  protected val jsonMediaType: WithFixedCharset = MediaTypes.`application/json`

  override def info(interval: FiniteDuration): Source[Option[Info], _] = {
    implicit val ec: ExecutionContextExecutor = system.executionContext

    val request = HttpRequest(uri = "https://api.myip.com", headers = Seq(Accept(jsonMediaType)))
    val responseFutureF: NotUsed => Future[Option[HttpResponse]] = _ => Http().singleRequest(request)
      .map { response =>
        response.status match {
          case StatusCodes.OK => Some(response)
          case _ =>
            response.discardEntityBytes()
            throw new RuntimeException(s"Expected 200 HTTP status, got ${response.status}.")
        }
      }
      .recover(_ => None)

    Source.tick(0.millisecond, interval, NotUsed)
      .log("before request")
      .mapAsync(1)(responseFutureF)
      .log("before unmarshalling")
      .mapAsync(1) {
        case Some(response) => Unmarshal(response.entity.withContentType(jsonMediaType)).to[Info]
          .map(Some(_))
        case _ => Future(None)
      }
      .log("before info providing")
  }
}

object MyipcomInfoProvider {
  def apply()(implicit system: ActorSystem[Nothing]) = new MyipcomInfoProvider
}