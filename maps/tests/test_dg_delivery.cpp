#include <maps/factory/libs/delivery/dg_delivery.h>
#include <maps/factory/libs/delivery/statistics.h>

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/dataset/dataset.h>
#include <maps/factory/libs/dataset/rpc_transformation.h>
#include <maps/factory/libs/storage/local_storage.h>

#include <boost/filesystem/operations.hpp>

namespace maps::factory::delivery::tests {
using namespace testing;
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(dg_delivery_should) {

const std::string src = ArcadiaSourceRoot() + "/maps/factory/test_data/dg_deliveries/058800151040_01";

Y_UNIT_TEST(parse_delivery_index_file)
{
    DgDelivery dl{src + "/058800151040_01_README.XML"};
    EXPECT_EQ(dl.defaultDirectoryName(), "058800151040_01_Kahramanmaras_AC");
    EXPECT_EQ(dl.deliveryIndexFileName().string(), "058800151040_01_README.XML");
    EXPECT_EQ(dl.orderNumber(), "058800151040_01");
    EXPECT_EQ(dl.metadataVersion(), "28.3");
    EXPECT_EQ(dl.areaDescription(), "Kahramanmaras_AC");
    EXPECT_EQ(dl.dgOrderNo(), "058800151");
    EXPECT_EQ(dl.dgOrderItemNo(), "058800151040");
    EXPECT_EQ(dl.customerOrderNo(), "YANDEX_1");
    EXPECT_EQ(dl.parseOrderId(), 1);
    EXPECT_EQ(dl.customerOrderItemNo(), "0");
    EXPECT_EQ(dl.cloudCover(), 0.0);
    EXPECT_EQ(dl.collectionStartTime(), chrono::parseIsoDateTime("2018-09-10T08:29:42.627182Z"));
    EXPECT_EQ(dl.collectionStopTime(), chrono::parseIsoDateTime("2018-09-10T08:29:44.422328Z"));
    EXPECT_DOUBLE_EQ(dl.boundsGeo().min().x(), 36.88893600);
    EXPECT_DOUBLE_EQ(dl.boundsGeo().min().y(), 37.49429000);
    EXPECT_DOUBLE_EQ(dl.boundsGeo().max().x(), 37.01030100);
    EXPECT_DOUBLE_EQ(dl.boundsGeo().max().y(), 37.51193700);
    const auto& files = dl.files(DgFiles::Indexed);
    ASSERT_EQ(files.size(), 47u);
    EXPECT_EQ(files.count("GIS_FILES/18SEP10082942-M2AS-058800151040_01_P001_PIXEL_SHAPE.shp"), 1u);
    EXPECT_EQ(files.count("058800151040_01_README.XML"), 1u);
}

Y_UNIT_TEST(load_from_directory_name)
{
    EXPECT_EQ(DgDelivery(src).areaDescription(), "Kahramanmaras_AC");
    EXPECT_EQ(DgDelivery{src + "/"}.areaDescription(), "Kahramanmaras_AC");
}

Y_UNIT_TEST(find_index_files)
{
    DgDelivery dl(src);
    EXPECT_EQ(dl.deliveryIndexFileName().string(), "058800151040_01_README.XML");
    auto prods = dl.products();
    ASSERT_EQ(prods.size(), 1u);
    EXPECT_THAT(prods.front().pan().productComponentIndexFile().string(),
        EndsWith("058800151040_01_P001_PAN/18SEP10082942-P2AS-058800151040_01_P001.XML"));
    EXPECT_THAT(prods.front().mul().productComponentIndexFile().string(),
        EndsWith("058800151040_01_P001_MUL/18SEP10082942-M2AS-058800151040_01_P001.XML"));
}

Y_UNIT_TEST(parse_areas)
{
    DgDelivery dl(src);
    auto areas = dl.areas();
    EXPECT_THAT(areas.productShapeFile().string(),
        EndsWith("GIS_FILES/058800151040_01_PRODUCT_SHAPE.shp"));
    EXPECT_THAT(areas.stripShapeFile().string(),
        EndsWith("GIS_FILES/058800151040_01_STRIP_SHAPE.shp"));
    EXPECT_THAT(areas.tileShapeFile().string(),
        EndsWith("GIS_FILES/058800151040_01_TILE_SHAPE.shp"));
    EXPECT_THAT(areas.orderShapeFile().string(),
        EndsWith("GIS_FILES/058800151040_01_ORDER_SHAPE.shp"));
}

Y_UNIT_TEST(parse_pan_file)
{
    auto prods = DgDelivery(src).products();
    ASSERT_EQ(prods.size(), 1u);
    auto pan = prods.front().pan();

    EXPECT_THAT(pan.tilFile().string(), EndsWith(
        "058800151040_01/058800151040_01_P001_PAN/18SEP10082942-P2AS-058800151040_01_P001.TIL"));
    EXPECT_TRUE(pan.tilFilePtr()->exists());

    EXPECT_THAT(pan.pixelShapeFile().string(), EndsWith(
        "058800151040_01/GIS_FILES/18SEP10082942-P2AS-058800151040_01_P001_PIXEL_SHAPE.shp"));
    EXPECT_TRUE(pan.pixelShapeFilePtr()->exists());

    EXPECT_THAT(pan.browseFile().string(), EndsWith(
        "058800151040_01/058800151040_01_P001_PAN/18SEP10082942-P2AS-058800151040_01_P001-BROWSE.JPG"));
    EXPECT_TRUE(pan.browseFilePtr()->exists());

    EXPECT_EQ(pan.satelliteMnemonic(), "WV02");
    EXPECT_EQ(pan.satelliteId(), SatelliteId::WorldView02);
    EXPECT_EQ(pan.productCatalogId(), "A0100103E56BF100");
    EXPECT_EQ(pan.childCatalogId(), "20300103E56BF000");
    EXPECT_EQ(pan.imageCatalogId(), "10300100855F3700");

    auto bands = pan.bands();
    ASSERT_EQ(bands.size(), 1u);
    EXPECT_EQ(bands[0].name(), "P");
    EXPECT_EQ(bands[0].id(), BandId::Pan);
    EXPECT_EQ(bands[0].index(), 0u);
    EXPECT_NEAR(bands[0].absCalFactor(), 5.678345000000000e-02, 1e-4);
    EXPECT_NEAR(bands[0].effectiveBandwidth(), 2.846000000000000e-01, 1e-4);

    auto pos = pan.position();
    EXPECT_EQ(pos.firstLineTime(), chrono::parseIsoDateTime("2018-09-10T08:29:42.627907Z"));
    EXPECT_EQ(pos.meanSunCoords().azimuth, 1.536e+02_deg);
    EXPECT_EQ(pos.meanSunCoords().elevation, 5.48e+01_deg);
    EXPECT_EQ(pos.meanSatelliteCoords().azimuth, 1.418e+02_deg);
    EXPECT_EQ(pos.meanSatelliteCoords().elevation, 7.43e+01_deg);
    EXPECT_EQ(pos.meanSatelliteCoords().elevation, 7.43e+01_deg);
}

Y_UNIT_TEST(parse_mul_file)
{
    auto prods = DgDelivery(src).products();
    ASSERT_EQ(prods.size(), 1u);
    auto mul = prods.front().mul();

    EXPECT_THAT(mul.tilFile().string(), EndsWith(
        "058800151040_01/058800151040_01_P001_MUL/18SEP10082942-M2AS-058800151040_01_P001.TIL"));
    EXPECT_TRUE(mul.tilFilePtr()->exists());

    EXPECT_THAT(mul.pixelShapeFile().string(), EndsWith(
        "058800151040_01/GIS_FILES/18SEP10082942-M2AS-058800151040_01_P001_PIXEL_SHAPE.shp"));
    EXPECT_TRUE(mul.pixelShapeFilePtr()->exists());

    EXPECT_THAT(mul.browseFile().string(), EndsWith(
        "058800151040_01/058800151040_01_P001_MUL/18SEP10082942-M2AS-058800151040_01_P001-BROWSE.JPG"));
    EXPECT_TRUE(mul.browseFilePtr()->exists());

    EXPECT_EQ(mul.satelliteMnemonic(), "WV02");
    EXPECT_EQ(mul.satelliteId(), SatelliteId::WorldView02);
    EXPECT_TRUE(mul.hasAtmosphericCompensation());
    EXPECT_EQ(mul.imageCatalogId(), "10300100855F3700");

    auto bands = mul.bands();
    ASSERT_EQ(bands.size(), 4u);
    EXPECT_EQ(bands[0].name(), "B");
    EXPECT_EQ(bands[0].id(), BandId::Blue);
    EXPECT_EQ(bands[0].index(), 0u);
    EXPECT_NEAR(bands[0].absCalFactor(), 1.260825e-02, 1e-4);
    EXPECT_NEAR(bands[0].effectiveBandwidth(), 5.43e-02, 1e-4);
    EXPECT_EQ(bands[1].name(), "G");
    EXPECT_EQ(bands[1].id(), BandId::Green);
    EXPECT_EQ(bands[1].index(), 1u);
    EXPECT_NEAR(bands[1].absCalFactor(), 9.713071e-03, 1e-4);
    EXPECT_NEAR(bands[1].effectiveBandwidth(), 6.3e-02, 1e-4);
    EXPECT_EQ(bands[2].name(), "R");
    EXPECT_EQ(bands[2].id(), BandId::Red);
    EXPECT_EQ(bands[2].index(), 2u);
    EXPECT_EQ(bands[3].name(), "N");
    EXPECT_EQ(bands[3].id(), BandId::NearInfrared1);
    EXPECT_EQ(bands[3].index(), 3u);

    auto pos = mul.position();
    EXPECT_EQ(pos.firstLineTime(), chrono::parseIsoDateTime("2018-09-10T08:29:42.627832Z"));
    EXPECT_EQ(pos.meanSunCoords().azimuth, 1.536e+02_deg);
    EXPECT_EQ(pos.meanSunCoords().elevation, 5.48e+01_deg);
    EXPECT_EQ(pos.meanSatelliteCoords().azimuth, 1.415e+02_deg);
    EXPECT_EQ(pos.meanSatelliteCoords().elevation, 7.43e+01_deg);
}

Y_UNIT_TEST(parse_product)
{
    auto prod = DgDelivery(src).products().front();
    EXPECT_EQ(prod.id(), DgProductId("058800151040_01", "P001"));
    EXPECT_THAT(prod.descriptors(), UnorderedElementsAre("PAN", "MUL"));
}

Y_UNIT_TEST(read_rpc_coeffs)
{
    auto rpc = DgDelivery(src).products().front().pan().rpc();
    const std::string meta[] = {
        "HEIGHT_OFF=618", "HEIGHT_SCALE=501",
        "LAT_OFF=3.750300000000000e+01", "LAT_SCALE=1.610000000000000e-02",
        "LINE_DEN_COEFF=1.000000000000000e+00 1.404342000000000e-05 8.193828000000000e-04 -9.984743000000000e-05 -1.338738000000000e-08 2.561502000000000e-07 4.406963000000000e-08 -6.952442000000000e-08 6.301772000000000e-07 -2.318709000000000e-07 -1.002680000000000e-08 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 -2.428525000000000e-08 0.000000000000000e+00",
        "LINE_NUM_COEFF=1.655740000000000e-03 6.660743000000000e-02 -9.991736000000000e-01 -6.298181999999999e-02 5.065316000000000e-05 1.086569000000000e-04 -1.966885000000000e-04 -1.003975000000000e-03 -8.199030000000000e-04 -5.864764000000000e-06 2.280123000000000e-07 1.792515000000000e-08 5.493621000000000e-08 8.013461999999999e-08 -9.061240000000000e-07 -6.260373000000000e-07 1.835017000000000e-07 -4.794927000000000e-07 -3.595780000000000e-07 1.807638000000000e-08",
        "LINE_OFF=2099", "LINE_SCALE=3576",
        "LONG_OFF=3.694970000000000e+01", "LONG_SCALE=6.180000000000000e-02",
        "SAMP_DEN_COEFF=1.000000000000000e+00 1.277709000000000e-03 1.129670000000000e-04 -3.793100000000000e-04 1.476953000000000e-08 3.624085000000000e-08 -2.406772000000000e-08 1.593155000000000e-06 3.170956000000000e-08 -1.097195000000000e-07 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00 0.000000000000000e+00",
        "SAMP_NUM_COEFF=-2.544603000000000e-03 1.012064000000000e+00 7.208193000000000e-03 -1.574559000000000e-02 -9.413625000000000e-05 3.353786000000000e-04 -4.013606000000000e-05 1.288007000000000e-03 2.143439000000000e-06 -6.858407000000000e-06 -6.100384000000000e-08 1.655502000000000e-06 -3.339688000000000e-08 8.199067000000000e-08 -2.414086000000000e-07 0.000000000000000e+00 -2.079640000000000e-08 7.458418000000000e-07 0.000000000000000e+00 0.000000000000000e+00",
        "SAMP_OFF=10734", "SAMP_SCALE=10797",
        "ERR_BIAS=1.180000000000000e+00", "ERR_RAND=2.200000000000000e-01"
    };
    EXPECT_EQ(rpc, TRpc::fromGdalMetadata(meta));
}

Y_UNIT_TEST(get_missing_files)
{
    DgDelivery dl(src);
    EXPECT_THAT(toStrings(dl.files(DgFiles::Missing)), IsEmpty());
}

Y_UNIT_TEST(get_extra_files)
{
    EXPECT_THAT(toStrings(DgDelivery(src).files(DgFiles::Extra)),
        ElementsAre("058800151040_01_P001_FITTED.GEOJSON", "058800151040_01_P001_MUL.TIF",
            "058800151040_01_P001_PAN.TIF", "058800151040_01_P001_STATS.JSON",
            "058800151040_01_P001_WARPED.GEOJSON"));
}

Y_UNIT_TEST(get_ftp_specific_files)
{
    EXPECT_THAT(toStrings(DgDelivery(src).files(DgFiles::FtpSpecific)),
        UnorderedElementsAre("../058800151040_01.MAN", "../058800151040_01_EOT.TXT"));
}

Y_UNIT_TEST(get_all_files)
{
    EXPECT_THAT(toStrings(DgDelivery(src).files(DgFiles::All)), SizeIs(52u));
}

Y_UNIT_TEST(get_indexed_files)
{
    EXPECT_THAT(toStrings(DgDelivery(src).files(DgFiles::Indexed)), SizeIs(47u));
}

Y_UNIT_TEST(list_all_deliveries_in_folder)
{
    auto all = DgDelivery::listAll(fs::path(src).parent_path());
    ASSERT_EQ(all.size(), 2u);
    EXPECT_EQ(all[0].orderNumber(), "058800151040_01");
    EXPECT_EQ(all[1].orderNumber(), "059011233030_01");
}

Y_UNIT_TEST(list_all_deliveries_in_subfolders)
{
    auto all = DgDelivery::listAllDirs(fs::path(src).parent_path());
    ASSERT_EQ(all.size(), 2u);
    EXPECT_EQ(all[0].orderNumber(), "058800151040_01");
    EXPECT_EQ(all[1].orderNumber(), "059011233030_01");
}

Y_UNIT_TEST(copy_delivery)
{
    DgDelivery dl(src);
    auto copy = dl.copyIndexedTo("tmp_11");
    EXPECT_EQ(copy.areaDescription(), "Kahramanmaras_AC");
    EXPECT_THAT(toStrings(copy.files(DgFiles::All)), SizeIs(47u));
    EXPECT_THAT(copy.products().front().pan().productComponentIndexFile().string(),
        EndsWith("tmp_11/058800151040_01_P001_PAN/18SEP10082942-P2AS-058800151040_01_P001.XML"));

}

Y_UNIT_TEST(remove_delivery)
{
    auto dst = "tmp_12";
    {
        DgDelivery dl(src);
        auto copy = dl.copyIndexedTo(dst);
        EXPECT_TRUE(fs::exists(dst));
        copy.removeAll();
    }
    EXPECT_FALSE(fs::exists(dst));
}

Y_UNIT_TEST(check_equal)
{
    auto otherSrc = fs::path(src).parent_path() / "059011233030_01";
    EXPECT_TRUE(DgDelivery(src).isEqual(DgDelivery(src)));
    EXPECT_TRUE(DgDelivery(src).isEqual(DgDelivery(src), true));
    EXPECT_FALSE(DgDelivery(src).isEqual(DgDelivery{ otherSrc }));
    EXPECT_FALSE(DgDelivery(src).isEqual(DgDelivery{ otherSrc }, true));
}

Y_UNIT_TEST(match_statistics_version)
{
    EXPECT_FALSE(Statistics::versionMatches(""));
    EXPECT_FALSE(Statistics::versionMatches("{}"));
    EXPECT_FALSE(Statistics::versionMatches("test test"));
    EXPECT_FALSE(Statistics::versionMatches("{\"test\" : \"test\"}"));
    EXPECT_FALSE(Statistics::versionMatches("{\"version\" : \"test\"}"));
    EXPECT_FALSE(Statistics::versionMatches("{\"version\" : \"2\"}"));
    EXPECT_FALSE(Statistics::versionMatches("{\"version\" : -1}"));
    EXPECT_FALSE(Statistics::versionMatches("\"version\" : 2"));
    const auto v = std::to_string(Statistics::
    VERSION);
    EXPECT_TRUE(Statistics::versionMatches("{\"version\" : " + v + "}"));
}

Y_UNIT_TEST(parse_order_id)
{
    using impl::parseOrderId;
    EXPECT_EQ(parseOrderId("YANDEX_123"), 123);
    EXPECT_EQ(parseOrderId("YANDEX_1"), 1);
    EXPECT_EQ(parseOrderId("YANDEX_100000000000"), 100000000000l);
    EXPECT_EQ(parseOrderId("YANDEX__123"), 123);

    EXPECT_EQ(parseOrderId("YANDEX_0"), 0);

    EXPECT_EQ(parseOrderId(""), std::nullopt);
    EXPECT_EQ(parseOrderId("YANDEX"), std::nullopt);
    EXPECT_EQ(parseOrderId("YANDEX_"), std::nullopt);
    EXPECT_EQ(parseOrderId("123"), std::nullopt);
    EXPECT_EQ(parseOrderId("_123"), std::nullopt);
    EXPECT_EQ(parseOrderId("YANDEX_123.45"), std::nullopt);
    EXPECT_EQ(parseOrderId("YANDEX_-123"), std::nullopt);
}

Y_UNIT_TEST(remove_invalid_url_symbols)
{
    using impl::removeInvalidUrlSymbols;
    EXPECT_EQ(removeInvalidUrlSymbols(""), "");
    EXPECT_EQ(removeInvalidUrlSymbols("abc_123_xyz"), "abc_123_xyz");
    EXPECT_EQ(removeInvalidUrlSymbols("a\r\nstoan2394q-+_naors*!&$^*&!@esatnd//\\h"),
        "astoan2394q_naorsesatndh");
}

Y_UNIT_TEST(parse_default_directory_name)
{
    using namespace std::string_literals;
    EXPECT_THROW(splitDirectoryToOrderNumberAndAoi(""s), RuntimeError);
    EXPECT_THROW(splitDirectoryToOrderNumberAndAoi("058800151040"s), RuntimeError);
    EXPECT_EQ(splitDirectoryToOrderNumberAndAoi("058800151040_01"s),
        std::make_pair("058800151040_01"s, ""s));
    EXPECT_EQ(splitDirectoryToOrderNumberAndAoi("058800151040_01_"s),
        std::make_pair("058800151040_01"s, ""s));
    EXPECT_EQ(splitDirectoryToOrderNumberAndAoi("058800151040_01_Some_Name"s),
        std::make_pair("058800151040_01"s, "Some_Name"s));
}

Y_UNIT_TEST(parse_product_id)
{
    using namespace std::string_literals;
    EXPECT_THROW(DgProductId(""s), RuntimeError);
    EXPECT_THROW(DgProductId("058800151040"s), RuntimeError);
    EXPECT_THROW(DgProductId("058800151040_01"s), RuntimeError);
    EXPECT_THROW(DgProductId("058800151040_01_"s), RuntimeError);
    EXPECT_EQ(DgProductId("058800151040_01_P001"s), DgProductId("058800151040_01"s, "P001"s));
    EXPECT_EQ(DgProductId("058800151040_99_P999"s), DgProductId("058800151040_99"s, "P999"s));
}

Y_UNIT_TEST(load_from_default_directory)
{
    DgDelivery dl = DgDelivery::fromDefaultDirectory(storage::storageFromDir(src));
    EXPECT_EQ(dl.orderNumber(), "058800151040_01");
}

Y_UNIT_TEST(copy_one_product)
{
    auto dst = storage::localStorage("./tmp")->dir(Name_);
    DgDelivery dl(src);
    auto dstDl = dl.copyProductTo(dl.products().front(), dst);

    EXPECT_THAT(toStrings(dst->list(storage::Select::FilesRecursive)),
        UnorderedElementsAreArray(toStrings(dl.files(DgFiles::Indexed))));
    EXPECT_TRUE(dstDl.isEqual(dl));
}

Y_UNIT_TEST(calculate_HAE)
{
    DgDelivery dl(src);
    for (const auto& prod: {dl.products()[0].pan(), dl.products()[0].mul()}) {
        dataset::TDataset ds = dataset::OpenDataset(prod.tilFile());
        double el = dataset::calculateElevation(ds);
        EXPECT_NEAR(el, prod.terrainHeightAboveEllipsoid(), 5);
    }
}

Y_UNIT_TEST(check_uploaded)
{
    const DgDelivery dl(src);
    EXPECT_TRUE(dl.isUploaded());
}

Y_UNIT_TEST(check_files)
{
    const DgDelivery dl(src);
    EXPECT_NO_THROW(dl.check());
}

Y_UNIT_TEST(collect_product_metadata)
{
    const DgDelivery dl(src);
    const ProductMetadata m = dl.productMetadata(dl.productFromPartId("P001"));

    EXPECT_DOUBLE_EQ(m.clouds, 0.0);
    EXPECT_EQ(m.datetime, chrono::parseIsoDateTime("2018-09-10T08:29:42.627907Z"));
    EXPECT_EQ(m.imageId, "058800151040_01_P001");
    EXPECT_EQ(m.stripId, "10300100855F3700");
    EXPECT_DOUBLE_EQ(m.sunAzum, 153.6);
    EXPECT_DOUBLE_EQ(m.sunElev, 54.8);
    EXPECT_DOUBLE_EQ(m.azimAngle, 141.8);
    EXPECT_DOUBLE_EQ(m.elev, 74.3);
    EXPECT_EQ(m.satellite, "WV02");
    EXPECT_DOUBLE_EQ(m.errBias, 1.18);
    EXPECT_DOUBLE_EQ(m.errRand, 0.22);
    EXPECT_EQ(m.dgOrderNo, "058800151");
    EXPECT_EQ(m.dgOrderItemNo, "058800151040");
    EXPECT_EQ(m.areaDescription, "Kahramanmaras_AC");
    EXPECT_DOUBLE_EQ(m.terrainHae, 562.21);
    EXPECT_DOUBLE_EQ(m.rpcDHeightPix, 0.56322484231063352);
}

} //suite
} //namespace maps::factory::delivery::tests
