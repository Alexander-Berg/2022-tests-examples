#include "tests/boost-tests/include/tools/map_tools.h"
#include "../../include/contexts.hpp"

#include "postgres/LabelSerializer.h"
#include "postgres/PostgresBinaryResult.h"
#include <maps/renderer/libs/font/include/loader.h>
#include <maps/renderer/libs/font/include/scaled_font_set.h>
#include <yandex/maps/renderer/feature/placed_label.h>
#include <maps/renderer/libs/base/include/geom/wkb_reader.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::postgres;
namespace font = maps::renderer::font;
namespace rbase = maps::renderer::base;

namespace {

void ptfc_label_version_test(pqxx::result &res, int rowID)
{
    PostgresBinaryResult binRes(res);

    // get bbox
    rbase::BoxD bbox;
    {
        pqxx::binarystring bstr(res[rowID]["bbox"]);
        rbase::Vertices vertices;
        rbase::readWkb({bstr.c_ptr(), bstr.size()}, &vertices);
        bbox = rbase::BoxD(vertices);
    }

    // get object id
    const unsigned int objectID =
        static_cast<unsigned int>(binRes.getIntegerValue(rowID, L"id"));

    // get labeldata
    pqxx::binarystring bstr(res[rowID]["data"]);

    // get labeltext
    std::wstring text = binRes.getStringValue(rowID, L"text");

    auto& font = font::sysLoader()("arial.ttf");
    font::ScaledFontSet scaledFont(font, font.front().capHeight());

    io::IStream stream(io::Reader(bstr.c_ptr(), io::Range(0, bstr.size())));
    auto label = labelDeserialize(
        bbox,
        objectID,
        stream,
        text,
        scaledFont,
        scaledFont);
}

}  // namespace

BOOST_AUTO_TEST_SUITE( ptfc )

BOOST_FIXTURE_TEST_CASE( label_versions_test, TransactionContext<> )
{
    // get data
    std::stringstream strQuery;
    strQuery << "SELECT "
             <<   "ST_AsBinary(bbox) as bbox, "
             <<   "labelData as data, "
             <<   "objectID as id, "
             <<   "labeltext as text "
             << "FROM testpgversions ";
    pqxx::result pqres = trans->exec(strQuery.str());

    // test
    for (size_t i = 0; i < pqres.size(); ++i)
        BOOST_CHECK_NO_THROW(ptfc_label_version_test(pqres, static_cast<int>(i)));
}

// void test::postgres::ptfc_test()
// {
//     PQXXConnectionPtr conn = PQXXConnectionPtr(new pqxx::connection(options));
//     PQXXTransactionPtr trans = PQXXTransactionPtr(new pqxx::work(*conn));
//
//     PostgresTransactionProviderPtr transactionProvider(
//             new SinglePostgresTransactionProvider(*trans));
//
//     labeler::FontStoragePtr fontStorage(new labeler::FontStorage(false));
//
//     PostgresTextFeatureContainer ptfc(
//         transactionProvider,
//         1,
//         L"testptfc",
//         true,
//         fontStorage);
//
//     ptfc.create();
//
//     rbase::BoxD bb = ptfc.getExtent();
//
//     // create feature
//     maps::renderer5::labeler::PositionedGlyph pa;
//     pa.glyphIndex = L'А';
//
//     maps::renderer5::labeler::PositionedGlyph pb;
//     pb.glyphIndex = L'Б';
//     pb.position.translate(10, 0);
//
//     maps::renderer5::labeler::PositionedTextPtr lg1(new maps::renderer5::labeler::PositionedText());
//     lg1->addLetter(pa);
//     lg1->addLetter(pb);
//     rbase::BoxD bb1(0, 0, 10, 10);
//     maps::renderer5::labeler::PlacedLabel pl1(
//         lg1, 1, bb1, 1, L"", std::optional<base::Vec2>(), 0.0);
//
//     maps::renderer5::labeler::PositionedTextPtr lg2(new maps::renderer5::labeler::PositionedText());
//     lg2->addLetter(pa);
//     rbase::BoxD bb2(10, 10, 20, 20);
//     maps::renderer5::labeler::PlacedLabel pl2(
//         lg2, 2, bb2, 2, L"", std::optional<base::Vec2>(), 0.0);
//
//     // test add feature
//     //
//     ptfc.startTransaction();
//     ptfc.addFeature(pl1);
//     ptfc.addFeature(pl2);
//     ptfc.finishTransaction();
//
//     // test get feature by bbox
//     //
//     core::FeatureCapabilities fc;
//     fc.add(core::CapabilityFeaturePlacedLabel);
//
//     auto itPtr = ptfc.findFeatures(bb, fc);
//     unsigned int num = 0;
//     for (itPtr->reset(); itPtr->hasNext(); itPtr->next())
//     {
//         ++num;
//     }
//
//     BOOST_CHECK(num == 2);
//
//     // test delete feature
//     //
//     ptfc.startTransaction();
//     ptfc.deleteFeature(1);
//     ptfc.finishTransaction();
//
//
//     // test feature by id
//     //
//     BOOST_CHECK_NO_THROW(ptfc.featureById(1, fc));
//
//     // test get extent
//     rbase::BoxD bbExtent = ptfc.getExtent();
//     BOOST_CHECK(bbExtent.x1 == bb2.x1 &&
//                 bbExtent.x2 == bb2.x2 &&
//                 bbExtent.y1 == bb2.y1 &&
//                 bbExtent.y2 == bb2.y2);
//
//     ptfc.close();
// }

BOOST_AUTO_TEST_SUITE_END()
