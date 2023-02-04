package ru.yandex.auto

import java.nio.charset.Charset

import org.apache.http.client.utils.URLEncodedUtils
import org.mortbay.jetty.HttpOnlyCookie
import org.springframework.mock.web.MockHttpServletRequest
import ru.yandex.auto.core.util.ProfileDataStorage
import ru.yandex.common.framework.http.HttpServRequest

import scala.collection.JavaConverters._

trait Fixtures {

  def requestFromQuery(q: String): MockHttpServletRequest = {
    val request = new MockHttpServletRequest

    val valuePair = URLEncodedUtils
      .parse(q, Charset.forName("UTF-8"))
      .asScala
      .toList

    // we kinda need at least one cookie downstream
    request.setCookies(new HttpOnlyCookie("123", "123"))

    for (p <- valuePair) {
      request.addParameter(p.getName, p.getValue)
    }

    request
  }

  def emptyProfileData: ProfileDataStorage = {
    val profileDataStorage = new ProfileDataStorage
    profileDataStorage
  }

  def emptyRequest: HttpServRequest = {
    val httpServRequest = new HttpServRequest(
      1L,
      "redir",
      ""
    )
    httpServRequest.setHttpHeaders("")
    httpServRequest.setRequestUrl("")
    httpServRequest
  }
}
