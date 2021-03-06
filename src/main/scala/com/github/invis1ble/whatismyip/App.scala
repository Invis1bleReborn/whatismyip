package com.github.invis1ble.whatismyip

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.github.invis1ble.whatismyip.info.providers.MyipcomInfoProvider

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object App {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "WhatIsMyIpAddress")
    implicit val ec: ExecutionContextExecutor = system.executionContext

    val provider = MyipcomInfoProvider()

    system.scheduler.scheduleWithFixedDelay(0.millisecond, 1.minute) {
      () => {
        provider.info
          .onComplete {
            case Success(info) =>
              val ccLabel = info.location.flatMap(_.countryCode).getOrElse("n/a")
              println(s"[$ccLabel] ${info.address}")
            case Failure(_) => sys.error(_)
          }
      }
    }
  }
}
