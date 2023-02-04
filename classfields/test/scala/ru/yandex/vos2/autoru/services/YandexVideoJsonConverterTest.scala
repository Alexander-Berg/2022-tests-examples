package ru.yandex.vos2.autoru.services

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

/**
  * Created by andrey on 8/19/16.
  */
@RunWith(classOf[JUnitRunner])
class YandexVideoJsonConverterTest extends AnyFunSuite {

  test("testGetVideoUrls") {
    val jsonStr =
      """[
                    |            {
                    |                "name":"sq.mp4",
                    |                "size":5300851,
                    |                "getUrl":"https:\/\/storage.mds.yandex.net\/get-video-autoru-office\/54402\/1568197e350\/ca4f9a689829f93b\/sq.mp4?redirect=yes&sign=7efaa9d851584956ee0bc9e3518cce0b15b0efd5f9ec112d0f3e22dbb4214a02&ts=6a7a8fce"
                    |            },
                    |            {
                    |                "name":"360p.m3u8",
                    |                "size":983,
                    |                "getUrl":"https:\/\/proxy.video.yandex.net\/get-hls\/autoru-office\/38552\/15681995709\/6fb33123e87564d0\/360p.m3u8?sign=19feec1bdc3.18ba25545c603bc998c81fd9b74814a9c8b5067137a375c03b4670d29e594ae6"
                    |            },
                    |            {
                    |                "name":"hls_master_playlist.m3u8",
                    |                "size":580,
                    |                "getUrl":"https:\/\/proxy.video.yandex.net\/get-master-hls\/autoru-office\/50464\/156819c5a70\/318e895ee3a00110\/hls_master_playlist.m3u8?sign=19feec1bdd3.a282c69f8559e6c2afb905826d1d92219ef44ccc2d5323f96a352d5417b989e7"
                    |            }]""".stripMargin
    val videoUrls = YandexVideoJsonConverter.getVideoUrls(jsonStr)
    assert(videoUrls.length == 3)
    val video1 = videoUrls.head
    assert(video1.name == "sq.mp4")
    assert(video1.size == 5300851)
    assert(
      video1.getUrl == "https://storage.mds.yandex.net/get-video-autoru-office/54402/1568197e350/ca4f9a689829f93b/sq.mp4?redirect=yes&sign=7efaa9d851584956ee0bc9e3518cce0b15b0efd5f9ec112d0f3e22dbb4214a02&ts=6a7a8fce"
    )
    val video2 = videoUrls(1)
    assert(video2.name == "360p.m3u8")
    assert(video2.size == 983)
    assert(
      video2.getUrl == "https://proxy.video.yandex.net/get-hls/autoru-office/38552/15681995709/6fb33123e87564d0/360p.m3u8?sign=19feec1bdc3.18ba25545c603bc998c81fd9b74814a9c8b5067137a375c03b4670d29e594ae6"
    )
    val video3 = videoUrls(2)
    assert(video3.name == "hls_master_playlist.m3u8")
    assert(video3.size == 580)
    assert(
      video3.getUrl == "https://proxy.video.yandex.net/get-master-hls/autoru-office/50464/156819c5a70/318e895ee3a00110/hls_master_playlist.m3u8?sign=19feec1bdd3.a282c69f8559e6c2afb905826d1d92219ef44ccc2d5323f96a352d5417b989e7"
    )
  }

  test("testGetVideoThumbs") {
    val jsonStr =
      """[
                    |            {
                    |                "url":"https:\/\/static.video.yandex.ru\/get\/office-autoru\/m-40909-156819756e5-5df77322b97b8029\/120x90.jpg",
                    |                "width":120,
                    |                "height":90
                    |            },
                    |            {
                    |                "url":"https:\/\/static.video.yandex.ru\/get\/office-autoru\/m-40909-156819756e5-5df77322b97b8029\/320x240.jpg",
                    |                "width":320,
                    |                "height":240
                    |            },
                    |            {
                    |                "url":"https:\/\/static.video.yandex.ru\/get\/office-autoru\/m-40909-156819756e5-5df77322b97b8029\/450x334.jpg",
                    |                "width":450,
                    |                "height":334
                    |            },
                    |            {
                    |                "url":"https:\/\/static.video.yandex.ru\/get\/office-autoru\/m-40909-156819756e5-5df77322b97b8029\/1.450x334.jpg",
                    |                "width":450,
                    |                "height":334
                    |            },
                    |            {
                    |                "url":"https:\/\/static.video.yandex.ru\/get\/office-autoru\/m-40909-156819756e5-5df77322b97b8029\/2.450x334.jpg",
                    |                "width":450,
                    |                "height":334
                    |            },
                    |            {
                    |                "url":"https:\/\/static.video.yandex.ru\/get\/office-autoru\/m-40909-156819756e5-5df77322b97b8029\/3.450x334.jpg",
                    |                "width":450,
                    |                "height":334
                    |            }]""".stripMargin
    val videoThumbs = YandexVideoJsonConverter.getVideoThumbs(jsonStr)
    assert(videoThumbs.length == 6)
    val Seq(thumb1, thumb2, thumb3, thumb4, thumb5, thumb6) = videoThumbs
    assert(
      thumb1.url == "https://static.video.yandex.ru/get/office-autoru/m-40909-156819756e5-5df77322b97b8029/120x90.jpg"
    )
    assert(thumb1.width == 120)
    assert(thumb1.height == 90)
    assert(
      thumb2.url == "https://static.video.yandex.ru/get/office-autoru/m-40909-156819756e5-5df77322b97b8029/320x240.jpg"
    )
    assert(thumb2.width == 320)
    assert(thumb2.height == 240)
    assert(
      thumb3.url == "https://static.video.yandex.ru/get/office-autoru/m-40909-156819756e5-5df77322b97b8029/450x334.jpg"
    )
    assert(thumb3.width == 450)
    assert(thumb3.height == 334)
    assert(
      thumb4.url == "https://static.video.yandex.ru/get/office-autoru/m-40909-156819756e5-5df77322b97b8029/1.450x334.jpg"
    )
    assert(thumb4.width == 450)
    assert(thumb4.height == 334)
    assert(
      thumb5.url == "https://static.video.yandex.ru/get/office-autoru/m-40909-156819756e5-5df77322b97b8029/2.450x334.jpg"
    )
    assert(thumb5.width == 450)
    assert(thumb5.height == 334)
    assert(
      thumb6.url == "https://static.video.yandex.ru/get/office-autoru/m-40909-156819756e5-5df77322b97b8029/3.450x334.jpg"
    )
    assert(thumb6.width == 450)
    assert(thumb6.height == 334)
  }

}
