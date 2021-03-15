package com.github.invis1ble.whatismyip

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.event.Logging
import akka.stream.Attributes
import ch.qos.logback.classic.LoggerContext
import com.github.invis1ble.whatismyip.info.providers.MyipcomInfoProvider
import org.slf4j.LoggerFactory
import scopt.{DefaultOEffectSetup, DefaultOParserSetup, OParser}

import java.io.{File, PrintWriter}
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object App {
  private val logger = LoggerFactory.getLogger(getClass.getName.replaceFirst("\\$$", ""))

  def main(args: Array[String]): Unit = {
    logger.info("App started")

    val config = parseArgs(args) match {
      case Some(config) => config
      case _ => terminate(); sys.exit(1)
    }

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "WhatIsMyIpAddress")
    implicit val ec: ExecutionContextExecutor = system.executionContext

    MyipcomInfoProvider().info(1.minute)
      .addAttributes(Attributes.logLevels(
        onElement = Logging.DebugLevel,
        onFinish = Logging.InfoLevel,
        onFailure = Logging.ErrorLevel,
      ))
      .map {
        case Some(info) =>
          val ccLabel = info.location.flatMap(_.countryCode).getOrElse("n/a")
          s"[$ccLabel] ${info.address}"
        case _ => "n/a"
      }
      .runForeach(writeln(_, config.file))
  }

  private def writeln(str: String, file: Option[File] = None): Unit = {
    file match {
      case Some(file) =>
        val writer = new PrintWriter(file)
        writer.println(str)
        writer.close()
      case None => println(str)
    }
  }

  private def parseArgs(args: Array[String]): Option[Config] = {
    OParser.runParser(createParser, args, Config(), new DefaultOParserSetup {
      override def showUsageOnError: Some[Boolean] = Some(true)
    }) match {
      case (result, effects) =>
        OParser.runEffects(effects, new DefaultOEffectSetup {
          override def terminate(exitState: Either[String, Unit]): Unit = {
            App.terminate()
            super.terminate(exitState)
          }
        })
        result
    }
  }

  private def createParser: OParser[_, Config] = {
    val builder = OParser.builder[Config]
    import builder._

    OParser.sequence(
      programName("whatismyip"),
      head("whatismyip", "1.2.0"),
      opt[Option[File]]('f', "file")
        .action((x, c) => c.copy(file = x))
        .validate {
          case Some(r) =>
            if (r.exists) {
              if (r.isFile) {
                if (!r.canWrite) failure(s"""Path "${r.getAbsolutePath}" must be writable by current user""")
              } else {
                failure(s"""Path "${r.getAbsolutePath}" is not a file""")
              }
            }
            success
          case None => success
        }
        .text("Output file (will be created if not exists)"),
      help("help").text("prints this usage text"),
      version("version")
    )
  }

  private def terminate(): Unit = {
    LoggerFactory.getILoggerFactory match {
      case ctx: LoggerContext => ctx.stop()
    }
  }
}
