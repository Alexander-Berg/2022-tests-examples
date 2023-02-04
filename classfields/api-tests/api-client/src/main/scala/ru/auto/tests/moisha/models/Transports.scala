package ru.auto.tests.moisha.models

object Transports extends Enumeration {
  val Cars = Value(15, "cars")
  val Moto = Value(17, "moto")
  val Commercial = Value(29, "commercial")
  val Special = Value(35, "special")

  val Motorcycle = Value(1, "motorcycle")
  val ATV = Value(3, "atv")
  val Snowmobile = Value(4, "snowmobile")
  val Scooters = Value(55, "scooters")
  val Carting = Value(10, "carting")
  val Amphibious = Value(12, "amphibious")
  val Baggi = Value(14, "baggi")

  val Artic = Value(33, "artic")
  val Bus = Value(34, "bus")
  val Trailer = Value(16, "trailer")
  val LCV = Value(31, "lcv")
  val Trucks = Value(32, "trucks")
  val Swapbody = Value(62, "swapbody")

  val Agricultural = Value(36, "agricultural")
  val Construction = Value(37, "construction")
  val Autoloader = Value(38, "autoloader")
  val Crane = Value(43, "crane")
  val Dredge = Value(44, "dredge")
  val Bulldozers = Value(45, "bulldozers")
  val CraneHydraulics = Value(53, "crane_hydraulics")
  val Municipal = Value(54, "municipal")

  private val parentCategories = Map(
    Cars -> Cars,
    Moto -> Moto,
    Commercial -> Commercial,
    Special -> Special,
    Motorcycle -> Moto,
    ATV -> Moto,
    Snowmobile -> Moto,
    Carting -> Moto,
    Amphibious -> Moto,
    Baggi -> Moto,
    Scooters -> Moto,
    Trailer -> Commercial,
    LCV -> Commercial,
    Trucks -> Commercial,
    Artic -> Commercial,
    Bus -> Commercial,
    Swapbody -> Commercial,
    Agricultural -> Special,
    Construction -> Special,
    Autoloader -> Special,
    Crane -> Special,
    Dredge -> Special,
    Bulldozers -> Special,
    CraneHydraulics -> Special,
    Municipal -> Special
  )

  def parent(category: Transports.Value): Transports.Value =
    parentCategories.getOrElse(category, throw new NoSuchElementException(s"No value found for '$category'"))
}
