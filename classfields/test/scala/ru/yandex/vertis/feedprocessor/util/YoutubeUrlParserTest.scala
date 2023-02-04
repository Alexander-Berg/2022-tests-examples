package ru.yandex.vertis.feedprocessor.util

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.Inspectors

/**
  * Created by sievmi on 16.02.18
  */

class YoutubeUrlParserTest extends AnyFunSuite with Inspectors {

  val testData: List[(String, Option[String])] = List(
    ("https://youtu.be/MJIC9MWhrrs", Some("MJIC9MWhrrs")),
    ("youtu.be/_HVhaVB8olU", Some("_HVhaVB8olU")),
    ("http://youtu.be/fRr0ubZqXXM", Some("fRr0ubZqXXM")),
    ("http://youtube.com/watch?v=UWYjvpfLuVs", Some("UWYjvpfLuVs")),
    ("youtube.com/watch?v=UWYjvpfLuVs", Some("UWYjvpfLuVs")),
    ("https://youtube.com/watch?v=UWYjvpfLuVs", Some("UWYjvpfLuVs")),
    ("http://www.youtube.com/watch?v=aqXlrMMT5qM&feature=youtu.be", Some("aqXlrMMT5qM")),
    ("http://www.youtube.com/watch?v=joFv-TjTNC8", Some("joFv-TjTNC8")),
    ("https://www.youtube.com/watch?v=0FwTBdZCMBQ", Some("0FwTBdZCMBQ")),
    ("www.youtube.com/watch?v=fYPz68fMoAw&t=422s", Some("fYPz68fMoAw")),
    ("https://m.youtube.com/watch?v=AGhfnXZiFSs", Some("AGhfnXZiFSs")),
    ("https://www.youtube.com/embed/4ZDLoBp8gK8", Some("4ZDLoBp8gK8")),
    ("https://www.youtube.com/edit?o=U&video_id=M3Kr-PAy1uI", None),
    ("M2U00132 https://youtu.be/IiQ1fdRlLuU", None),
    ("89267355684", None),
    ("3C1ZSkLjaQA", None),
    (" https://youtu.be/AmtJ5cry0xQ", Some("AmtJ5cry0xQ")),
    ("//www.youtube.com/watch?v=8dfli6pklnY", None),
    ("http://www.youtube.com/https://youtu.be/jLT5sWWvwGI", None),
    ("ttps://www.youtube.com/watch?v=l4BlrYoERVY", None),
    ("http:// https://youtu.be/SYDl2XMfEWg", None),
    ("http://s3-eu-west-1.amazonaws.com/s3.ovidoapp/video/c675162bbd5e4f5f9b478c7edacf0575.mp4", None),
    ("[11:05, 5.9.2016] Антон Хнурин: е. От официального дилера", None),
    ("", None)
  )

  test("parseYoutubeUrl") {
    forEvery(testData) {
      case (url, hash) => assert(YoutubeUrlParser.parseYoutubeId(url) == hash, "Url = " + url)
    }
  }
}
