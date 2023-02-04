#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/store_internal/lib/common/helpers.h>
#include <maps/automotive/store_internal/lib/validation.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/infra/yacare/include/yacare.h>

namespace maps::automotive::store_internal {

namespace {

const auto goodName = "aftermarket-mts-vendor-model-caska-imx6";
const auto goodVersion = ToString(std::time(nullptr) - 1);

HeadUnits makeHeadUnits()
{
    HeadUnits headunits;
    headunits.add_type("aftermarket-mts");
    headunits.add_type("aftermarket");
    auto* model = headunits.add_model();
    model->set_vendor("vendor");
    model->set_model("model");
    model = headunits.add_model();
    model->set_vendor("complex-vendor");
    model->set_model("very-complex-model");
    model = headunits.add_model();
    model->set_vendor("complex-vendor");
    model->set_model("vendor-model");
    headunits.add_mcu("caska-imx6");
    headunits.add_mcu("caska_t3");
    return headunits;
}

const HeadUnits headunits = makeHeadUnits();

} // namespace

TEST(validation, parseHeadUnitId)
{
    struct {
        TString firmwareName;
        struct {
            TString type;
            TString vendor;
            TString model;
            TString mcu;
        } headUnitSelector;
    } validNames[] = {
        {"aftermarket-mts-vendor-model-caska-imx6",
         {"aftermarket-mts", "vendor", "model", "caska-imx6"}},
        {"aftermarket-mts-complex-vendor-vendor-model-caska-imx6",
         {"aftermarket-mts", "complex-vendor", "vendor-model", "caska-imx6"}},
        {"aftermarket-complex-vendor-very-complex-model-caska-imx6",
         {"aftermarket",
          "complex-vendor",
          "very-complex-model",
          "caska-imx6"}}};

    for (const auto& validName: validNames) {
        auto headUnitSelector =
            parseHeadUnitId(validName.firmwareName, headunits);
        EXPECT_EQ(headUnitSelector.type(), validName.headUnitSelector.type);
        EXPECT_EQ(
            headUnitSelector.vendor(), validName.headUnitSelector.vendor);
        EXPECT_EQ(headUnitSelector.model(), validName.headUnitSelector.model);
        EXPECT_EQ(headUnitSelector.mcu(), validName.headUnitSelector.mcu);
    }

    const TString invalidNames[] = {
        "unknown-vendor-model-caska",
        "taxi-vendor-model-unknown",
        "taxi--model-caska",
        "taxi-vendor--caska",
        "-vendor-model-caska",
        "taxi-vendor-model-",
        "taxi-vendor-model-caska-t3",
        "aftermarket_mts-vendor-model-caska",
        "",
        "---",
        "aftermarket-complex-vendor-model-caska",
        "aftermarket-vendor-model-caska_t3-",
        "-aftermarket-vendor-model-caska_t3",
        "aftermarket--vendor-model-caska_t3",
        "aftermarket-vendor-model-caska_t3X",
        "aftermarket-vendor-complex-model-caska_t3"};

    for (const auto& invalidName: invalidNames) {
        EXPECT_THROW(
            parseHeadUnitId(invalidName, headunits),
            yacare::errors::BadRequest);
    }
}

TEST(validation, firmwareName)
{
    Firmware fw;
    fw.mutable_id()->set_name(goodName);
    fw.mutable_id()->set_version(goodVersion);
    (*fw.mutable_metadata()->mutable_title())["ru_RU"] = "T";

    validate(fw, headunits);

    fw.mutable_id()->set_name("aftermarket-vendor-model-caska_t3");
    validate(fw, headunits);

    fw.mutable_id()->set_name("unknown-vendor-model-caska");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_name("taxi-vendor-model-unknown");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_name("taxi--model-caska");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_name("taxi-vendor--caska");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_name("-vendor-model-caska");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_name("taxi-vendor-model-");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_name("taxi-vendor-model-caska-t3");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_name("aftermarket_mts-vendor-model-caska");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
}

TEST(validation, firmwareVersion)
{
    Firmware fw;
    fw.mutable_id()->set_name(goodName);
    fw.mutable_id()->set_version(goodVersion);
    (*fw.mutable_metadata()->mutable_title())["ru_RU"] = "T";

    validate(fw, headunits);

    fw.mutable_id()->set_version("11ss");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_version("123");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_version(" " + goodVersion);
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_version(goodVersion + " ");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_version("+" + goodVersion);
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    fw.mutable_id()->set_version("0" + goodVersion);
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    // timestamp from future
    fw.mutable_id()->set_version(ToString(std::time(nullptr) + 24*60*60));
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
    // too old timestamp (2001-09-09)
    fw.mutable_id()->set_version("1000000000");
    EXPECT_THROW(validate(fw, headunits), yacare::errors::BadRequest);
}

TEST(validation, validateFirmwareVersions)
{
    Firmware fw;
    buildFirmware(fw, goodName, "1530461423");
    fw.mutable_metadata()->set_from_min_version("1530461422");
    validate(fw);

    buildFirmware(fw, goodName, "1530461422");
    EXPECT_THROW(validate(fw), yacare::errors::BadRequest);

    buildFirmware(fw, goodName, "1530461421");
    EXPECT_THROW(validate(fw), yacare::errors::BadRequest);

    fw.mutable_metadata()->clear_from_min_version();
    validate(fw);
}

TEST(validation, validateFirmwareFromVersion)
{
    Firmware fw;
    buildFirmware(fw, goodName, "1598367991");
    fw.mutable_id()->set_from_version("1598367990");
    fw.mutable_metadata()->clear_from_min_version();
    validate(fw);

    fw.mutable_metadata()->set_from_min_version("1598367990");
    EXPECT_THROW(validate(fw), yacare::errors::BadRequest);

    fw.mutable_metadata()->clear_from_min_version();
    validate(fw);

    fw.mutable_id()->set_from_version("1598367991");
    EXPECT_THROW(validate(fw), yacare::errors::BadRequest);
}

TEST(validation, packageVersionCode)
{
    auto pkg = buildPackage("a", 1);
    validate(pkg);

    pkg.mutable_id()->set_version_code((1ul << 31) - 1);
    validate(pkg);

    pkg.mutable_id()->set_version_code(1ul << 31);
    EXPECT_THROW(validate(pkg), yacare::errors::BadRequest);

    pkg.mutable_id()->set_version_code(0);
    EXPECT_THROW(validate(pkg), yacare::errors::BadRequest);
}

TEST(validation, validateFirmwareRollout)
{
    FirmwareRollout goodRollout;
    goodRollout.set_branch("br");
    goodRollout.mutable_firmware_id()->set_name(
        "aftermarket-mts-complex-vendor-vendor-model-caska-imx6");
    goodRollout.mutable_headunit()->set_type("aftermarket-mts");
    goodRollout.mutable_headunit()->set_mcu("caska-imx6");
    goodRollout.mutable_headunit()->set_vendor("complex-vendor");
    goodRollout.mutable_headunit()->set_model("vendor-model");
    validateRollout(goodRollout, headunits);
    {
        FirmwareRollout ro = goodRollout;
        ro.mutable_headunit()->set_type("unsupported_type");
        EXPECT_THROW(validateRollout(ro, headunits), yacare::errors::BadRequest);
    }
    {
        FirmwareRollout ro = goodRollout;
        ro.mutable_headunit()->set_mcu("unsupported_mcu");
        EXPECT_THROW(validateRollout(ro, headunits), yacare::errors::BadRequest);
    }
    {
        FirmwareRollout ro = goodRollout;
        ro.mutable_headunit()->set_model("unsupported_model");
        EXPECT_THROW(validateRollout(ro, headunits), yacare::errors::BadRequest);
    }
    {
        FirmwareRollout ro = goodRollout;
        ro.mutable_headunit()->set_vendor("unsupported_vendor");
        EXPECT_THROW(validateRollout(ro, headunits), yacare::errors::BadRequest);
    }
}

}
