package com.github.invis1ble.whatismyip.info.providers

import akka.stream.scaladsl.Source
import com.github.invis1ble.whatismyip.info.Info

import scala.concurrent.duration.FiniteDuration

trait InfoProvider {
  def info(interval: FiniteDuration): Source[Option[Info], _]
}
