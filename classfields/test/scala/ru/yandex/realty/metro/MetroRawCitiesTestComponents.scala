package ru.yandex.realty.metro

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.metro.MetroRawCitiesTestComponents.metroStorage
import ru.yandex.realty.storage.{MetroRawCitiesStorage, MetroRawCity, MetroRawLine, MetroRawName, MetroRawStation}

trait MetroRawCitiesTestComponents {
  lazy val metroRawCitiesProvider: Provider[MetroRawCitiesStorage] = () => metroStorage
}

object MetroRawCitiesTestComponents {

  val komsomolskaya = MetroRawStation(
    20484,
    Seq(55.775672, 37.654772),
    None,
    Seq("213_5", "213_1"),
    None,
    Some(Seq("213_14")),
    MetroRawName("Комсомольская")
  )

  val parkKulturi = MetroRawStation(
    20490,
    Seq(55.736077, 37.595061),
    None,
    Seq("213_5", "213_1"),
    None,
    Some(Seq("213_14")),
    MetroRawName("Парк Культуры")
  )

  val sokolniki = MetroRawStation(
    20402,
    Seq(55.789198, 37.6797),
    None,
    Seq("213_1"),
    None,
    Some(Seq("213_14")),
    MetroRawName("Сокольники")
  )

  val msk = MetroRawCity(
    213,
    Seq(55.75396, 37.620393),
    Seq(
      MetroRawLine(
        "213_1",
        Some("#e4402d"),
        Seq(komsomolskaya, parkKulturi, sokolniki),
        MetroRawName("Сокольническая линия")
      )
    )
  )

  val metroStorage = MetroRawCitiesStorage(Seq(msk))
}
