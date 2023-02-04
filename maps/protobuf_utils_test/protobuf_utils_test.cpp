#include <library/cpp/testing/unittest/env.h>

#include <maps/mobile/server/init/lib/protobuf_utils.h>
#include <google/protobuf/unknown_field_set.h>
#include <maps/mobile/server/init/lib/settings_storage.h>
#include <maps/mobile/server/init/lib/tstring_helper.h>
#include <yandex/maps/proto/mobile_config/config.pb.h>
#include <maps/libs/common/include/file_utils.h>

#include <boost/test/unit_test.hpp>

#include <iostream>
#include <fstream>

BOOST_AUTO_TEST_CASE(test_protobuf_replace_text)
{
    Settings settings(SRC_("sample_input.pb"));
    replaceText("HOSTNAME_PLACEHOLDER", "spdy.maps.yandex.ru", &settings);
    TString result;
    Y_PROTOBUF_SUPPRESS_NODISCARD settings.SerializeToString(&result);

    auto sampleOutput = maps::common::readFileToString(SRC_("sample_output.pb"));
    BOOST_CHECK_MESSAGE(result.equal(TStringBuf(sampleOutput.data(), sampleOutput.size())),
        "Result protobuf doesn't match to sample_output.pb");
}
