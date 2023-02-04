package ru.auto.salesman.billing

/** Common props for client testing
  *
  * @author alesavin
  */
package object impl {

  // up billing-indexer on dev machine
  val HostPort = "dev55h.vs.os.yandex.net:34120"
  val InternalApiBaseUrl = "http://localhost:34150"
//  val InternalApiBaseUrl = "http://csback01ht.vs.yandex.net:34150"
//  val HostPort = "dev04i.vs.os.yandex.net:34120"

  val Uri = s"http://$HostPort/api/1.x/service/realty/binding"
  val CampaignsUri = s"http://$HostPort/api/1.x/service/realty/campaigns"
}
