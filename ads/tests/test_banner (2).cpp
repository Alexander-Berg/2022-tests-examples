#include <ads/bigkv/bko/entities/banner.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/yson/parser.h>


Y_UNIT_TEST_SUITE(BannerEntityTests) {
    Y_UNIT_TEST(ParseEmptyTest) {
        // TBaseEntity constructor will fail if some of
        // CategoricalFeatures, UnravelledCategoricalFeatures, RealvalueFeatures
        // do not belong to schema or there is no proper converter for feature type.
        NBkoTsar::TBannerEntity().GetParser()->Parse(
            NProfilePreprocessing::TProfilesPack(NCSR::TBannerProfileProto()),
            NProfilePreprocessing::TArgs(static_cast<ui64>(12345678))
        );
    }

    // https://st.yandex-team.ru/AUTOPARSER-175#628cf3aae04e425ba05a6cd8
    Y_UNIT_TEST(ParseBadUtf8Test) {
        TFileInput inputData("banner_bad_utf8.yson");
        NYT::TNode currentNode;
        NYT::TNodeBuilder builder(&currentNode);
        NYson::TYsonListParser parser(&builder, &inputData);

        parser.Parse();
        currentNode = currentNode.AsList().front();

        NCSR::TBannerProfileProto banner;
        banner.ParseFromStringOrThrow(currentNode["BannerProfileNoTsar"].AsString());

        NBkoTsar::TBannerEntity().GetParser()->Parse(
            NProfilePreprocessing::TProfilesPack(banner),
            NProfilePreprocessing::TArgs(static_cast<ui64>(12345678))
        );
        
    }
}
