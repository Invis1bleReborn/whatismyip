package com.github.invis1ble.whatismyip.info.providers

import com.github.invis1ble.whatismyip.info.Info

import scala.concurrent.Future

trait InfoProvider {
  def info: Future[Info]
}
