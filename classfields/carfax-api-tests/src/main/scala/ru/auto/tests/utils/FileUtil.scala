package ru.auto.tests.utils

import com.google.gson.Gson
import ru.auto.tests.commons.util.Utils.getResourceAsString
import ru.auto.tests.model.AutoApiOffer

object FileUtil {

  def loadOfferFromFile(filePath: String): AutoApiOffer = {
    val gson = new Gson
    val reader = getResourceAsString(filePath)

    gson.fromJson(reader, classOf[AutoApiOffer])
  }

}
