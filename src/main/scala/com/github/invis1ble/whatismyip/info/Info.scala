package com.github.invis1ble.whatismyip.info

final case class Location(country: Option[String], countryCode: Option[String])
final case class Info(address: String, location: Option[Location])
