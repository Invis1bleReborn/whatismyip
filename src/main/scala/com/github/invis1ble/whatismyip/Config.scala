package com.github.invis1ble.whatismyip

import java.io.File
import scala.concurrent.duration.{DurationInt, FiniteDuration}

final case class Config(
  file: Option[File] = None,
  interval: FiniteDuration = 1.minute,
)
