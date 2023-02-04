package ru.yandex.realty.componenttest.http

case class ExternalHttpStubConfig(
  host: String,
  port: Int
) {

  def url: String = s"http://$host:$port"

}
