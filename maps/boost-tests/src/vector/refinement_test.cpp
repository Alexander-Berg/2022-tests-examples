#include "tests/boost-tests/include/tools/map_tools.h"
#include "tests/boost-tests/include/tools/vector_tools.h"

#include "refiner/tools.h"
#include <yandex/maps/renderer5/core/geometry/comparations.h>
#include <yandex/maps/renderer5/refiner/refiner.h>

#include <maps/renderer/libs/base/include/json_fwd.h>
#include <yandex/maps/renderer/feature/feature.h>
#include <yandex/maps/renderer/geojson/serializer.h>
#include <yandex/maps/renderer/geometry/integer_types_tools.h>

#include <boost/test/unit_test.hpp>
#include <boost/filesystem.hpp>

#include <fstream>
#include <sstream>

DISABLE_MS_WARNING(4267)

using namespace maps::renderer;
using namespace maps::renderer::base;
using namespace maps::renderer::geometry;

namespace maps { namespace renderer5 { namespace test {

namespace {

typedef std::vector<base::PointD> PointsD;

enum class Sort {
    ByArea,
    ByLength
};

class RefinementTest {

    inline PointsD jsonToPointsD(const rapidjson::Value& jsonPoints) const
    {
        PointsD result;

        for (size_t i = 0; i < jsonPoints.Size(); i++) {
            base::PointD point;
            if (integerSource) {
                auto x = jsonPoints[i][0].GetInt();
                auto y = jsonPoints[i][1].GetInt();
                point = refiner::converters[zoom].i2d(PointI32(x, y));
            } else {
                point.x = jsonPoints[i][0].GetDouble();
                point.y = jsonPoints[i][1].GetDouble();
            }
            result.push_back(point);
        }

        return result;
    }

    feature::FeatureType codeToType(std::string code) const
    {
        return code == "polygon"
            ? feature::FeatureType::Polygon
            : feature::FeatureType::Polyline;
    }

    void locateCommon(const rjhelper::ValueRef& json,
                      feature::Feature& dst) const
    {
        auto zOrderBegin = json.GetMemberDefault<int>("zOrderBegin", 0);
        auto zOrderEnd   = json.GetMemberDefault<int>("zOrderEnd",   0);

        dst.zOrder() = {zOrderBegin, zOrderEnd};
    }

    void locatePolygon(const rjhelper::ValueRef& json,
                       feature::Feature& dst) const
    {
        locateCommon(json, dst);

        for (const auto& pointsRaw: json["points"].GetArray()) {
            size_t idBegin = dst.geom().shapes().size();

            for (auto pt: jsonToPointsD(pointsRaw))
                dst.geom().shapes().addLineTo(pt);

            dst.geom().shapes().push_back(dst.geom().shapes()[idBegin]);
            dst.geom().shapes()[idBegin].cmd = agg::path_cmd_move_to;
        }
    }

    void locatePolyline(const rjhelper::ValueRef& json,
                        feature::Feature& dst) const
    {
        locateCommon(json, dst);

        for (const auto& pointsRaw: json["points"].GetArray()) {
            size_t idBegin = dst.geom().shapes().size();

            for (auto pt: jsonToPointsD(pointsRaw))
                dst.geom().shapes().addLineTo(pt);

            dst.geom().shapes()[idBegin].cmd = agg::path_cmd_move_to;
        }
    }

    void initRefiner(const rjhelper::ValueRef& json)
    {
        auto type = codeToType(json["type"].Get<std::string>());

        Vertices border;
        for (auto pt: jsonToPointsD(*json["border"].ptr()))
            border.addLineTo(pt);

        if (!border.empty()) {
            border.push_back(border[0]);
            border[0].cmd = agg::path_cmd_move_to;
        }

        auto orientCCW = json["orientCCW"].Get<bool>();
        zoom = json["zoom"].Get<int>();
        integerSource = json.GetMemberDefault("source", std::string()) == "integer";

        refiner.reset(new refiner::Refiner(type, border, orientCCW, zoom, false));
    }

    void initInput(const rjhelper::ValueRef& json)
    {
        auto type = codeToType(json["type"].Get<std::string>());

        inputFeature.reset(new feature::Feature(type));

        if (type == feature::FeatureType::Polygon)
            locatePolygon(json, *inputFeature);

        if (type == feature::FeatureType::Polyline)
            locatePolyline(json, *inputFeature);
    }

    void initOutput(const rjhelper::ValueRef& json)
    {
        for (const auto& featureRaw: json.GetArray()) {
            rjhelper::ValueRef feature(&featureRaw);

            auto type = codeToType(feature["type"].Get<std::string>());

            needFeatures.emplace_back(new feature::Feature(type));

            if (type == feature::FeatureType::Polygon) {
                locatePolygon(feature, *needFeatures.back());
                sortParam = Sort::ByArea;
            }

            if (type == feature::FeatureType::Polyline) {
                locatePolyline(feature, *needFeatures.back());
                sortParam = Sort::ByLength;
            }
        }
    }

    geojson::Geometry toLine(const Vertices& vs) const
    {
        geojson::LineString result;

        for (auto& v: vs) {
            base::PointD res(v.x, v.y);

            if (integerSource)
                res = base::PointD(refiner::converters[zoom].d2i(res));

            result.push_back(res);
        }

        return result;
    }

    geojson::Geometry toPoly(const Vertices& vs) const
    {
        geojson::Polygon result;

        for (auto& v: vs) {
            if (v.isMoveTo())
                result.emplace_back();

            base::PointD res(v.x, v.y);
            if (integerSource)
                res = base::PointD(refiner::converters[zoom].d2i(res));

            result.back().push_back(res);
        }

        return result;
    }

public:
    RefinementTest(const std::string& file) : testName(file)
    {
        std::string content = io::file::open(file).readAllToString();
        rapidjson::Document jsonRaw;
        jsonRaw.Parse<rapidjson::kParseDefaultFlags>(content.data());
        rjhelper::ValueRef json(&jsonRaw);

        initRefiner(json["ref"]);
        initInput(json["input"]);
        initOutput(json["output"]);
    }

    json::Document visualize(const feature::Feature& feature,
                             json::Allocator& alloc) const
    {
        json::Document result;

        auto geometry = (sortParam == Sort::ByLength)
            ? toLine(feature.geom().shapes())
            : toPoly(feature.geom().shapes());

        geojson::Serializer().dumpGeometry(
            geometry, json::MutValueRef{&result, &alloc});

        result.AddMember("zOrder.begin", feature.zOrder().begin, alloc);
        result.AddMember("zOrder.end", feature.zOrder().end, alloc);

        return std::move(result);
    }

    const std::string                 testName;
    std::unique_ptr<refiner::Refiner> refiner;
    feature::FeaturePtr               inputFeature;
    refiner::Features                 needFeatures;
    int                               zoom;
    bool                              integerSource;
    Sort                              sortParam;

};

inline Vec2PointI32 id() {
    return [](const PointD& v) {
        return PointI32(std::round(v.x), std::round(v.y));
    };
}

geometry::PolylineI32 asLine(const Vertices& vs)
{
    return LineTypes32::polylines(vs, id())[0];
}

geometry::PolygonI32 asPoly(const Vertices& vs)
{
    return unite(RingTypes32::rings(vs, id()))[0];
}

bool equal(const feature::Feature& have,
           const feature::Feature& need,
           const Sort param)
{
    if (have.zOrder().begin != need.zOrder().begin)
        return false;

    if (have.zOrder().end != need.zOrder().end)
        return false;

    return (param == Sort::ByLength)
        ? asLine(have.geom().contours()) == asLine(need.geom().contours())
        : asPoly(have.geom().shapes()) == asPoly(need.geom().shapes());
}

bool equalVis(
    const feature::Feature& have,
    const feature::Feature& need,
    const std::string& diffFileName,
    const std::string& diffFile,
    const Sort param)
{
    if (equal(have, need, param))
        return true;

    std::ofstream fout(diffFileName);
    fout << diffFile;

    return false;
}

double areaOf(const feature::FeatureUPtr& feature)
{
    const auto& vs = feature->geom().shapes();
    return math::area2(vs.begin(), vs.end());
}

double lengthOf(const feature::FeatureUPtr& feature)
{
    const auto& vs = feature->geom().contours();
    double len = 0;

    auto prev = vs.begin();
    for (auto cur = std::next(vs.begin()); cur != vs.end(); ++cur) {
        len += normL2(*cur - *prev);
        prev = cur;
    }

    return len * 100 + feature->zOrder().begin * 10 + feature->zOrder().end;
}

void sort(refiner::Features& features, Sort param)
{
    auto comp = [=](const feature::FeatureUPtr& lhs,
                    const feature::FeatureUPtr& rhs) {
        double l = 0;
        double r = 0;

        if (param == Sort::ByArea) {
            l = areaOf(lhs);
            r = areaOf(rhs);
        } else {
            l = lengthOf(lhs);
            r = lengthOf(rhs);
        }

        return l > r;
    };

    std::sort(features.begin(), features.end(), comp);
}

std::string toGeoJson(const std::string& test)
{
    auto gj = boost::filesystem::path(test).replace_extension(".geo.json");
    std::stringstream stream;
    stream << gj.filename();
    std::string result = stream.str();

    if (result.front() == '"') {
        result.erase(0, 1);
        result.erase(result.size() - 1);
    }

    return result;
}

} // anonymous

BOOST_AUTO_TEST_SUITE( refinement_test )

BOOST_AUTO_TEST_CASE( refinement )
{
    auto testNames = filesInDirectory("tests/boost-tests/data/refinement/");

    for (const auto& testName: testNames) {
        RefinementTest test(testName);
        auto haveFeatures = (*test.refiner)(*test.inputFeature);

        sort(haveFeatures, test.sortParam);
        sort(test.needFeatures, test.sortParam);

        BOOST_REQUIRE_MESSAGE(
            haveFeatures.size() == test.needFeatures.size(),
            "sizes are not equal:"
            << " have " << haveFeatures.size() << " features,"
            << " need " << test.needFeatures.size() << " features @ "
            << testName);

        for (size_t id = 0; id < haveFeatures.size(); id++) {
            std::string location = testName + ", feature #" + std::to_string(id);
            auto& have = *haveFeatures[id];
            auto& need = *test.needFeatures[id];

            rapidjson::Document info;

            json::ObjectBuilder o(&info);

            o("test", testName);
            o("feature #", id);
            o("have", test.visualize(have, info.GetAllocator()));
            o("need", test.visualize(need, info.GetAllocator()));

            auto gjName = toGeoJson(testName);
            auto gj = rjhelper::ToStringPretty(info);

            BOOST_CHECK_MESSAGE(equalVis(have, need, gjName, gj, test.sortParam),
                                testName << " failed, see diff in " << gjName);
        }
    }
}

BOOST_AUTO_TEST_SUITE_END()

} } } // namespace maps::renderer5::test

RESET_MS_WARNING(4267)
