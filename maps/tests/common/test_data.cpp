#include "test_data.h"

#include <maps/factory/libs/db/aoi.h>
#include <maps/factory/libs/db/aoi_gateway.h>
#include <maps/factory/libs/db/delivery.h>
#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/mosaic.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_pipeline.h>
#include <maps/factory/libs/db/mosaic_source_status_event_gateway.h>
#include <maps/factory/libs/db/order.h>
#include <maps/factory/libs/db/order_gateway.h>
#include <maps/factory/libs/db/source.h>
#include <maps/factory/libs/db/source_gateway.h>
#include <maps/factory/libs/dataset/vector_dataset.h>
#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <maps/libs/log8/include/log8.h>
#include <maps/libs/img/include/raster.h>
#include <maps/libs/tile/include/tile.h>

#include <library/cpp/testing/unittest/env.h>
#include <maps/factory/libs/delivery/dg_delivery.h>
#include <maps/factory/libs/db/dg_delivery.h>
#include <maps/factory/libs/db/dg_delivery_gateway.h>

namespace maps::factory::sputnica::tests {

namespace {

std::string makeReferenceFilePath(
    const tile::Tile& tile,
    const std::string& prefix,
    const std::string& suffix)
{
    static const std::string REFERENCE_IMAGES_ROOT = "/maps/factory/services/sputnica_back/tests/data/";
    return ArcadiaSourceRoot() +
           REFERENCE_IMAGES_ROOT + prefix +
           std::to_string(tile.x()) + "." +
           std::to_string(tile.y()) + "." +
           std::to_string(tile.z()) + suffix;
}

template <typename Gateway>
std::string makeNewName(std::string name, Gateway& gateway)
{
    size_t count = gateway.count();
    if (count == 0) {
        return name;
    }
    return name + "_" + std::to_string(count);
}

} // namespace

db::Delivery makeTestDelivery(pqxx::transaction_base& txn)
{
    auto sourceGateway = db::SourceGateway(txn);
    db::Source source(makeNewName("test_source", sourceGateway), db::SourceType::Local, "/");
    sourceGateway.insert(source);

    auto deliveryGateway = db::DeliveryGateway(txn);
    db::Delivery delivery(source.id(), "2019-01-01", makeNewName("test_delivery", deliveryGateway), "/");
    deliveryGateway.insert(delivery);
    return delivery;
}

db::MosaicSource makeNewMosaicSource(pqxx::transaction_base& txn, const std::string& name)
{
    db::Order order(ORDER_YEAR, ORDER_TYPE);
    db::OrderGateway(txn).insert(order);

    auto delivery = makeTestDelivery(txn);

    db::Aoi aoi(order.id(), FIRST_AOI_NAME, AOI_GEOMETRY);
    db::AoiGateway(txn).insert(aoi);

    return makeNewMosaicSource(aoi.orderId(), aoi.id(), delivery.id(), name, txn);
}

db::MosaicSource makeNewMosaicSource(
    int64_t orderId,
    int64_t aoiId,
    int64_t deliveryId,
    const std::string& name,
    pqxx::transaction_base& txn)
{
    db::MosaicSource ms(name);
    ms.setDeliveryId(deliveryId);
    ms.setMercatorGeom(MOSAIC_GEOMETRY);
    ms.setMinMaxZoom(MOSAIC_ZMIN, MOSAIC_ZMAX);
    ms.setSatellite(MOSAIC_SATELLITE);
    ms.setOrderId(orderId);
    ms.setAoiId(aoiId);
    ms.setMetadata(json::Value{json::repr::ObjectRepr{
        {"SUNELEVATION", json::Value("31337")},
        {"dg_order_no", json::Value(MOSAIC_DG_ORDER_NO)},
        {"dg_order_item_no", json::Value(MOSAIC_DG_ORDER_ITEM_NO)}
    }});
    db::MosaicSourcePipeline{txn}.insertNew(ms);
    return ms;
}

db::MosaicSource makeNewDigitalGlobeMosaicSource(pqxx::transaction_base& txn)
{
    std::string dgPath = ArcadiaSourceRoot() + "/maps/factory/test_data/dg_deliveries/058800151040_01";
    std::string cogPath = ArcadiaSourceRoot() + "/maps/factory/test_data/cog/dg_058800151040_01_P001";
    const delivery::DgDelivery dl{dgPath};
    const auto product = dl.products().at(0);

    auto geom = dataset::toGeolibPolygon(
        dataset::VectorDataset(dataset::OpenDataset(product.pan().pixelShapeFile()))
            .singleGeometry(geometry::mercatorSr()));
    auto geomMp = geolib3::MultiPolygon2{{geom}};
    db::Order order(ORDER_YEAR, ORDER_TYPE);
    db::OrderGateway(txn).insert(order);

    auto delivery = makeTestDelivery(txn);

    db::Aoi aoi(order.id(), dl.areaDescription(), geom);
    db::AoiGateway(txn).insert(aoi);

    auto dgDeliveryGateway = db::DgDeliveryGateway(txn);
    db::DgDelivery dgDelivery(makeNewName(dl.orderNumber(), dgDeliveryGateway), dgPath);
    dgDeliveryGateway.insert(dgDelivery);

    auto meta = json::Value{json::repr::ObjectRepr{
        {"SUNELEVATION", json::Value(product.pan().position().meanSunCoords().elevation.value())},
        {"dg_order_no", json::Value(dl.dgOrderNo())},
        {"dg_order_item_no", json::Value(dl.dgOrderItemNo())}
    }};

    db::MosaicSource ms(dl.orderNumber());
    ms.setDeliveryId(delivery.id());
    ms.setMercatorGeom(geomMp);
    ms.setMinZoom(MOSAIC_ZMIN);
    ms.setMaxZoom(MOSAIC_ZMAX);
    ms.setSatellite(MOSAIC_SATELLITE);
    ms.setOrderId(order.id());
    ms.setAoiId(aoi.id());
    ms.setCogPath(cogPath);
    ms.setMetadata(meta);
    db::MosaicSourcePipeline{txn}.insertNew(ms);
    auto dgGateway = db::DgProductGateway(txn);
    db::DgProduct dgProduct({makeNewName(dl.orderNumber(), dgGateway), product.id().partId()}, ms.id());
    dgGateway.insert(dgProduct);
    return ms;
}

void promoteMosaicSourceToReady(db::MosaicSource& ms, pqxx::transaction_base& txn)
{
    db::Mosaic m(
        ms.id(),
        MOSAIC_ZORDER,
        MOSAIC_ZMIN,
        MOSAIC_ZMAX,
        ms.mercatorGeom()
    );
    db::MosaicGateway(txn).insert(m);
    db::MosaicSourcePipeline pp{txn};
    pp.transition(ms,
        db::MosaicSourceStatus::Ready, db::UserRole{unittest::TEST_CUSTOMER_USER_ID, db::Role::Customer});
}

img::RasterImage loadReferenceImage(const tile::Tile& tile, const std::string& prefix)
{
    return img::RasterImage::fromPngFile(makeReferenceFilePath(tile, prefix, ".png"));
}

json::Value loadReferenceJson(const tile::Tile& tile, const std::string& prefix)
{
    return json::Value::fromFile(makeReferenceFilePath(tile, prefix, ".json"));
}

int64_t makeOrder(pqxx::transaction_base& txn)
{
    db::Order order(2019, db::OrderType::Tasking);
    db::OrderGateway(txn).insert(order);
    return order.id();
}

int64_t makeAoi(
    pqxx::transaction_base& txn, int64_t orderId, std::string name, geolib3::Polygon2 mercatorGeom)
{
    db::Aoi aoi(orderId, std::move(name), std::move(mercatorGeom));
    db::AoiGateway(txn).insert(aoi);
    return aoi.id();
}

} //namespace maps::factory::sputnica::tests
