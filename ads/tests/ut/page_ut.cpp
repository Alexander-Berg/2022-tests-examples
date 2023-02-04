#include <ads/tensor_transport/lib_2/page.h>
#include <ads/tensor_transport/proto/page.pb.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NTsarTransport;

Y_UNIT_TEST_SUITE(TPageEntityTest) {
    Y_UNIT_TEST(DirectCreationTest) {
        ui64 pageID = 186465;
        TPage page(pageID);
        TVector<ui64> features = page.GetFeatures();
        UNIT_ASSERT_VALUES_EQUAL(features[0], 7826035404431730673);
        THashMap<TString, TVector<ui64>> namedFeatures = page.GetNamedFeatures();
        UNIT_ASSERT_VALUES_EQUAL(namedFeatures["PageID"][0], 186465);
    }

    Y_UNIT_TEST(PageImpHashTest) {
        ui64 pageID = 186465;
        ui64 impID = 123123;
        TPage page(pageID, impID);
        TVector<ui64> features = page.GetFeatures();
        UNIT_ASSERT_VALUES_EQUAL(features[1], 10131922319278468396ul);
        THashMap<TString, TVector<ui64>> namedFeatures = page.GetNamedFeatures();
        UNIT_ASSERT_VALUES_EQUAL(namedFeatures["ImpID"][0], 123123);
        UNIT_ASSERT_VALUES_EQUAL(namedFeatures["ImpID,PageID"][0], 3440320412808);
    }

    Y_UNIT_TEST(ProtoCreationTest) {
        ui64 pageID = 186465;
        TensorTransport::TPageRecord pageRecord;
        pageRecord.set_pageid(pageID);
        TPage page(pageRecord);
        TVector<ui64> features = page.GetFeatures();
        UNIT_ASSERT_VALUES_EQUAL(features[0], 7826035404431730673);
        THashMap<TString, TVector<ui64>> namedFeatures = page.GetNamedFeatures();
        UNIT_ASSERT_VALUES_EQUAL(namedFeatures["PageID"][0], 186465);
    }

    Y_UNIT_TEST(CheckPageToken) {
        ui64 pageID = 186465;
        TensorTransport::TPageRecord pageRecord;
        pageRecord.set_pageid(pageID);
        TPage page(pageRecord);
        THashMap<TString, TVector<ui64>> namedFeatures = page.GetNamedFeatures();
        UNIT_ASSERT(namedFeatures.contains("PageToken"));
    }
};


