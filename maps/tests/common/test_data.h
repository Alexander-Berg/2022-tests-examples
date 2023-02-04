#pragma once

#include <maps/factory/libs/db/delivery.h>
#include <maps/factory/libs/db/order.h>
#include <maps/factory/libs/db/mosaic_source.h>
#include <maps/factory/libs/sproto_helpers/order.h>

#include <maps/libs/json/include/value.h>
#include <maps/libs/img/include/raster.h>
#include <maps/libs/tile/include/tile.h>

#include <string>

namespace maps::factory::sputnica::tests {

constexpr uint32_t ORDER_YEAR = 2019;
constexpr db::OrderType ORDER_TYPE = db::OrderType::Tasking;
const std::string ORDER_NAME = "test order";
const auto SPROTO_ORDER_TYPE = sproto_helpers::convertToSproto(ORDER_TYPE);

const geolib3::Polygon2 AOI_GEOMETRY(
    geolib3::PointsVector{
        {12019966.3541429, 6904062.66721244},
        {12039319.7036828, 6904965.89392846},
        {12039442.3817891, 6890407.40886827},
        {12020140.7466942, 6889491.27668234},
        {12019966.3541429, 6904062.66721244}
    }
);

const geolib3::BoundingBox AOI_BOUNDING_BOX = AOI_GEOMETRY.boundingBox();
const std::string FIRST_AOI_NAME = "Moscow";
const std::string SECOND_AOI_NAME = "Sochi";

//FIXME: generate proper zOrder for Mosaic
constexpr int64_t MOSAIC_ZORDER = 65535;

const std::string MOSAIC_NAME = "NameOnShe";
const std::string MOSAIC_DG_ORDER_NO = "dgorder123";
const std::string MOSAIC_DG_ORDER_ITEM_NO = "dgorderItem1234";
const geolib3::MultiPolygon2 MOSAIC_GEOMETRY({
    geolib3::Polygon2(
        geolib3::PointsVector{
            {12020066.3529682, 6904063.15192575},
            {12039323.8689762, 6904865.98071446},
            {12039342.3818711, 6890407.28082854},
            {12020136.5013354, 6889591.18652634},
            {12020066.3529682, 6904063.15192575}
        }
    )
});

const geolib3::BoundingBox MOSAIC_SOURCE_GEODETIC_BOUNDING_BOX =
    geolib3::convertMercatorToGeodetic(MOSAIC_GEOMETRY).boundingBox();
constexpr size_t MOSAIC_ZMIN = 10;
constexpr size_t MOSAIC_ZMAX = 20;
const std::string MOSAIC_SATELLITE = "Sputnik";

constexpr uint32_t IMAGE_ZOOM = 12;
const geolib3::Point2 IMAGE_TIEPOINT(
    12019218.042000,
    6905048.136000
);

db::Delivery makeTestDelivery(pqxx::transaction_base& txn);

db::MosaicSource makeNewMosaicSource(
    int64_t orderId,
    int64_t aoiId,
    int64_t deliveryId,
    const std::string& name,
    pqxx::transaction_base& txn);

/// Creates Order, Aoi and MosaicSource object.
db::MosaicSource makeNewMosaicSource(pqxx::transaction_base& txn, const std::string& name = MOSAIC_NAME);

/// Create mosaic source without associated mosaic but with associated digital globe product.
/// @see /maps/factory/test_data/dg_deliveries/058800151040_01
db::MosaicSource makeNewDigitalGlobeMosaicSource(pqxx::transaction_base& txn);

/// Promotes MosaicSource to MosaicSourceStatus::Ready,
/// and creates Mosaic, MosaicImage and Image objects in database
void promoteMosaicSourceToReady(db::MosaicSource& mosaicSource, pqxx::transaction_base& txn);

img::RasterImage loadReferenceImage(const tile::Tile& tile, const std::string& prefix = {});

json::Value loadReferenceJson(const tile::Tile& tile, const std::string& prefix = {});

int64_t makeOrder(pqxx::transaction_base& txn);

int64_t makeAoi(
    pqxx::transaction_base& txn, int64_t orderId, std::string name, geolib3::Polygon2 mercatorGeom);

} //namespace maps::factory::sputnica::tests
