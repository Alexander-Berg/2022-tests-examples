package ru.auto.api.util

import ru.auto.api._
import ru.auto.api.model.CategorySelector.{Cars, Moto, Trucks}
import ru.auto.api.search.SearchModel.TrucksSearchRequestParameters.TrailerType
import ru.auto.api.search.SearchModel._
import ru.auto.api.ui.UiModel.Stock
import ru.auto.api.util.search.{SearcherFieldNames, SearcherProtoUtils}

class SearcherProtoUtilsTest extends BaseSpec {
  "convert auto params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setCarsParams(
        CarsSearchRequestParameters
          .newBuilder()
          .addTransmission(CarsModel.Car.Transmission.AUTO)
          .addTransmission(CarsModel.Car.Transmission.MECHANICAL)
          .addFeedingType(CarsModel.Car.FeedingType.TURBO)
      )
      .setWithWarranty(true)
      .setInStock(Stock.IN_STOCK)
      .addMarkModelNameplate("HONDA#CIVIC#4569475")
      .addMarkModelNameplate("HONDA#CIVIC")
      .addMarkModelNameplate("BMW")
      .setHasImage(true)
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Cars, params)
    searcherMap("image") shouldBe Set("true")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Cars, searcherMap)
    params shouldBe backwardConvert
  }

  "convert moto.atv params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setMotoParams(
        MotoSearchRequestParameters
          .newBuilder()
          .addTransmission(MotoModel.Moto.Transmission.TRANSMISSION_4_FORWARD_AND_BACK)
          .addTransmission(MotoModel.Moto.Transmission.AUTOMATIC_3_SPEED)
          .setMotoCategory(MotoModel.MotoCategory.ATV)
          .addGearType(MotoModel.Moto.GearType.CARDAN)
          .addCylinders(MotoModel.Moto.Cylinders.CYLINDERS_4)
          .addStrokes(MotoModel.Moto.Strokes.STROKES_2)
          .addAtvType(MotoModel.Atv.Type.BUGGI)
      )
      .setHasImage(true)
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Moto, params)
    searcherMap("image") shouldBe Set("true")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Moto, searcherMap)
    params shouldBe backwardConvert
  }

  "convert moto.moto params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setMotoParams(
        MotoSearchRequestParameters
          .newBuilder()
          .addTransmission(MotoModel.Moto.Transmission.TRANSMISSION_4_FORWARD_AND_BACK)
          .addTransmission(MotoModel.Moto.Transmission.AUTOMATIC_3_SPEED)
          .setMotoCategory(MotoModel.MotoCategory.MOTORCYCLE)
          .addGearType(MotoModel.Moto.GearType.CARDAN)
          .addCylinders(MotoModel.Moto.Cylinders.CYLINDERS_4)
          .addStrokes(MotoModel.Moto.Strokes.STROKES_2)
          .addMotoType(MotoModel.Moto.Type.MINIBIKE)
      )
      .addColor("color1")
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Moto, params)
    searcherMap(SearcherFieldNames.MOTO_COLOR).head shouldBe "color1"
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Moto, searcherMap)
    params shouldBe backwardConvert
  }

  "vipe some moto.moto params when from searcher" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setMotoParams(
        MotoSearchRequestParameters
          .newBuilder()
          .addTransmission(MotoModel.Moto.Transmission.TRANSMISSION_4_FORWARD_AND_BACK)
          .addTransmission(MotoModel.Moto.Transmission.AUTOMATIC_3_SPEED)
          .setMotoCategory(MotoModel.MotoCategory.MOTORCYCLE)
          .addMotoType(MotoModel.Moto.Type.ALLROUND)
          .addGearType(MotoModel.Moto.GearType.CARDAN)
          .addCylinders(MotoModel.Moto.Cylinders.CYLINDERS_4)
          .addStrokes(MotoModel.Moto.Strokes.STROKES_2)
          .addMotoType(MotoModel.Moto.Type.MINIBIKE)
      )
      .addColor("color1")
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Moto, params)
    searcherMap(SearcherFieldNames.MOTO_COLOR).head shouldBe "color1"
    val motoCat = searcherMap(SearcherFieldNames.MOTO_TYPE)
    val searcherMapModified =
      searcherMap + (SearcherFieldNames.MOTO_TYPE -> (motoCat ++ Set("ROAD_GROUP", "OFF_ROAD_GROUP")))
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Moto, searcherMapModified)
    params shouldBe backwardConvert
  }

  "convert moto.snowmobile params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setMotoParams(
        MotoSearchRequestParameters
          .newBuilder()
          .addTransmission(MotoModel.Moto.Transmission.TRANSMISSION_4_FORWARD_AND_BACK)
          .addTransmission(MotoModel.Moto.Transmission.AUTOMATIC_3_SPEED)
          .setMotoCategory(MotoModel.MotoCategory.SNOWMOBILE)
          .addGearType(MotoModel.Moto.GearType.CARDAN)
          .addCylinders(MotoModel.Moto.Cylinders.CYLINDERS_4)
          .addStrokes(MotoModel.Moto.Strokes.STROKES_2)
          .addSnowmobileType(MotoModel.Snowmobile.Type.SPORTS_CROSS)
      )
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Moto, params)
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Moto, searcherMap)
    params shouldBe backwardConvert
  }

  "convert trucks.lcv params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(
        TrucksSearchRequestParameters
          .newBuilder()
          .addTransmission(TrucksModel.Transmission.SEMI_AUTOMATIC)
          .addWheelDrive(TrucksModel.WheelDrive.WD_4x4)
          .addEuroClass(TrucksModel.EuroClass.EURO_2)
          .addEngineType(TrucksModel.Engine.DIESEL)
          .setHaggle(TrucksModel.HaggleType.HAGGLE_POSSIBLE)
          .addSaddleHeight(TrucksModel.SaddleHeight.SH_185)
          .setTrucksCategory(TrucksModel.TruckCategory.LCV)
          .addLightTruckType(TrucksModel.LightTruck.BodyType.AMBULANCE)
          .addCabinKey(TrucksModel.CabinType.SEAT_2_WO_SLEEP)
      )
      .addMarkModelNameplate("VOLVO")
      .setHasImage(true)
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params)
    searcherMap("image") shouldBe Set("true")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    params shouldBe backwardConvert
  }

  "convert trucks.trucks params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(
        TrucksSearchRequestParameters
          .newBuilder()
          .addTransmission(TrucksModel.Transmission.SEMI_AUTOMATIC)
          .addWheelDrive(TrucksModel.WheelDrive.WD_4x4)
          .addEuroClass(TrucksModel.EuroClass.EURO_2)
          .setHaggle(TrucksModel.HaggleType.HAGGLE_POSSIBLE)
          .addSaddleHeight(TrucksModel.SaddleHeight.SH_185)
          .setTrucksCategory(TrucksModel.TruckCategory.TRUCK)
          .addTruckType(TrucksModel.Truck.BodyType.CATTLE_CARRIER)
          .addCabinKey(TrucksModel.CabinType.SEAT_2_WO_SLEEP)
      )
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params)
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    params shouldBe backwardConvert
  }

  "check engine type in trucks.trucks" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(
        TrucksSearchRequestParameters
          .newBuilder()
          .addTransmission(TrucksModel.Transmission.SEMI_AUTOMATIC)
          .addWheelDrive(TrucksModel.WheelDrive.WD_4x4)
          .addEuroClass(TrucksModel.EuroClass.EURO_2)
          .setHaggle(TrucksModel.HaggleType.HAGGLE_POSSIBLE)
          .addSaddleHeight(TrucksModel.SaddleHeight.SH_185)
          .setTrucksCategory(TrucksModel.TruckCategory.TRUCK)
          .addEngineType(TrucksModel.Engine.DIESEL)
          .addTruckType(TrucksModel.Truck.BodyType.CATTLE_CARRIER)
          .addCabinKey(TrucksModel.CabinType.SEAT_2_WO_SLEEP)
      )
      .build()
    val searcherMap =
      SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params) + (SearcherFieldNames.ENGINE_TYPE -> Set("DIESEL"))
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    params shouldBe backwardConvert
  }

  "convert trucks.agricultural params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(
        TrucksSearchRequestParameters
          .newBuilder()
          .setTrucksCategory(TrucksModel.TruckCategory.AGRICULTURAL)
          .addAgriculturalType(TrucksModel.Agricultural.Type.COMBAIN_HARVESTER)
      )
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params)
    searcherMap("body_key") shouldBe Set("COMBAIN_HARVESTER")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    params shouldBe backwardConvert
  }

  "convert trucks.construction params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(
        TrucksSearchRequestParameters
          .newBuilder()
          .setTrucksCategory(TrucksModel.TruckCategory.CONSTRUCTION)
          .addConstructionType(TrucksModel.Construction.Type.CABLE_LAYER)
      )
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params)
    searcherMap("body_key") shouldBe Set("CABLE_LAYER")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    params shouldBe backwardConvert
  }

  "convert trucks.autoloader params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(
        TrucksSearchRequestParameters
          .newBuilder()
          .setTrucksCategory(TrucksModel.TruckCategory.AUTOLOADER)
          .addAutoloaderType(TrucksModel.Autoloader.Type.FORKLIFTS)
      )
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params)
    searcherMap("body_key") shouldBe Set("FORKLIFTS")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    params shouldBe backwardConvert
  }

  "convert trucks.dredge params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(
        TrucksSearchRequestParameters
          .newBuilder()
          .setTrucksCategory(TrucksModel.TruckCategory.DREDGE)
          .addDredgeType(TrucksModel.Dredge.Type.CRAWLER_EXCAVATOR)
      )
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params)
    searcherMap("body_key") shouldBe Set("CRAWLER_EXCAVATOR")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    params shouldBe backwardConvert
  }

  "convert trucks.bulldozer params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(
        TrucksSearchRequestParameters
          .newBuilder()
          .setTrucksCategory(TrucksModel.TruckCategory.BULLDOZERS)
          .addBulldozerType(TrucksModel.Bulldozer.Type.CRAWLERS_BULLDOZER)
      )
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params)
    searcherMap("body_key") shouldBe Set("CRAWLERS_BULLDOZER")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    params shouldBe backwardConvert
  }

  "convert trucks.municipal params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(
        TrucksSearchRequestParameters
          .newBuilder()
          .setTrucksCategory(TrucksModel.TruckCategory.MUNICIPAL)
          .addMunicipalType(TrucksModel.Municipal.Type.FIRE_TRUCK)
      )
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params)
    searcherMap("body_key") shouldBe Set("FIRE_TRUCK")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    params shouldBe backwardConvert
  }

  "convert trucks.trailer and swap body params to searcher format and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(
        TrucksSearchRequestParameters
          .newBuilder()
          .setTrucksCategory(TrucksModel.TruckCategory.SWAP_BODY)
          .addTrailerType(TrailerType.ST_ASSORTMENT)
          .addTrailerType(TrailerType.BULK_CARGO)
      )
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params)
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    backwardConvert.getTrucksParams.getTrucksCategory shouldBe TrucksModel.TruckCategory.TRAILER
    backwardConvert.getTrucksParams.getTrailerType(0) shouldBe TrailerType.ST_ASSORTMENT
    backwardConvert.getTrucksParams.getTrailerType(1) shouldBe TrailerType.BULK_CARGO
  }

  "convert exclude catalog filter to searcher and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setCarsParams(CarsSearchRequestParameters.newBuilder())
      .addExcludeCatalogFilter(CatalogFilter.newBuilder().setMark("mark").setModel("model"))
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Cars, params)
    searcherMap("not_catalog_filter") shouldBe Set("mark=mark,model=model")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Cars, searcherMap)
    params shouldBe backwardConvert
  }

  "convert trucks exclude catalog filter to searcher and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setTrucksParams(TrucksSearchRequestParameters.newBuilder())
      .addExcludeCatalogFilter(CatalogFilter.newBuilder().setMark("mark").setModel("model"))
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Trucks, params)
    searcherMap("not_catalog_filter") shouldBe Set("mark=mark,model=model")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Trucks, searcherMap)
    params shouldBe backwardConvert
  }

  "convert moto exclude catalog filter to searcher and back" in {
    val params = SearchRequestParameters
      .newBuilder()
      .setMotoParams(MotoSearchRequestParameters.newBuilder())
      .addExcludeCatalogFilter(CatalogFilter.newBuilder().setMark("mark").setModel("model"))
      .build()
    val searcherMap = SearcherProtoUtils.fromProtoToSearcherFilter(Moto, params)
    searcherMap("not_catalog_filter") shouldBe Set("mark=mark,model=model")
    val backwardConvert = SearcherProtoUtils.fromSearcherFilterToProto(Moto, searcherMap)
    params shouldBe backwardConvert
  }
}
