package com.github.invis1ble.whatismyip

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.event.Logging
import akka.stream.Attributes
import com.github.invis1ble.whatismyip.info.providers.MyipcomInfoProvider
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object App {
  private val logger = LoggerFactory.getLogger(getClass.getName.replaceFirst("\\$$", ""))

  def main(args: Array[String]): Unit = {
    logger.info("App started")

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "WhatIsMyIpAddress")
    implicit val ec: ExecutionContextExecutor = system.executionContext

    MyipcomInfoProvider().info(1.minute)
      .addAttributes(Attributes.logLevels(
        onElement = Logging.DebugLevel,
        onFinish = Logging.InfoLevel,
        onFailure = Logging.ErrorLevel,
      ))
      .runForeach {
        case Some(info) =>
          val ccLabel = info.location.flatMap(_.countryCode).getOrElse("n/a")
          println(s"[$ccLabel] ${info.address}")
        case _ => println("n/a")
      }
  }
}
