#include <maps/jams/renderer2/common/yacare/lib/tests/common/map_data_builder.h>

#include <maps/jams/renderer2/common/yacare/lib/map_data.h>
#include <maps/jams/renderer2/common/yacare/lib/i18n.h>
#include <maps/jams/renderer2/common/yacare/lib/util.h>

#include <maps/libs/chrono/include/time_point.h>

#include <maps/libs/common/include/temporary_dir.h>

#include <maps/libs/edge_persistent_index/packer/lib/include/persistent_index_builder.h>
#include <maps/libs/road_graph/serialization/include/serialization.h>

#include <maps/renderer/libs/data_sets/data_set/include/data_set.h>
#include <maps/renderer/libs/data_sets/data_set/include/view_queriable.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace data_set = maps::renderer::data_set;
namespace feature = maps::renderer::feature;

using namespace testing;

namespace maps::jams::renderer::tests {

namespace {

size_t countFeatures(const data_set::FeatureIterable& view)
{
    size_t result = 0;
    auto it = view.iterator();
    while (it->hasNext()) {
        it->next();
        ++result;
    }
    return result;
}

std::set<feature::FeatureType> collectFeatureTypes(
    const data_set::FeatureIterable& view)
{
    std::set<feature::FeatureType> result;
    auto it = view.iterator();
    while (it->hasNext()) {
        const auto& ft = it->next();
        result.insert(ft.type());
    }
    return result;
}

std::unique_ptr<feature::Feature> findJamFeature(
    const data_set::FeatureIterable& view,
    double speedAsId)
{
    auto it = view.iterator();
    while (it->hasNext()) {
        const auto& ft = it->next();
        const auto attrs = ft.attr.get();
        if (attrs->IsObject() && attrs->HasMember("speed")) {
            double speed = (*attrs)["speed"].GetDouble();
            if (std::abs(speed - speedAsId) < 0.01) {
                return ft.clone();
            }
        }
    }
    throw RuntimeError() << "Can't find jam feature with speed " << speedAsId;
}

std::vector<std::string> readArray(const rapidjson::Value& value)
{
    std::vector<std::string> result;
    for (size_t i = 0; i < value.Size(); ++i) {
        result.emplace_back(value[i].GetString());
    }
    return result;
}

std::vector<std::string> readEventDescriptions(
    std::unique_ptr<feature::FeatureIter> it)
{
    std::vector<std::string> result;
    while (it->hasNext()) {
        const auto& feature = it->next();
        auto attrs = feature.attr.get();
        result.emplace_back((*attrs)["description"].GetString());
    }
    return result;
}

} // namespace

Y_UNIT_TEST_SUITE(data_sets_should) {

Y_UNIT_TEST(read_features)
{
    setupi18n();
    const time_t now = std::time(nullptr);

    maps::common::TemporaryDir tempDir;
    MapDataPaths paths(tempDir.path().string());

    std::vector<TestJam> jams;
    auto* jam = &jams.emplace_back();
    jam->speed = 30;
    jam->category = 3;
    jam->severity = common::Severity::Light;
    jam->geometry = {{10, 20}, {30, 40}, {35, 45}};

    jam = &jams.emplace_back();
    jam->speed = 25;
    jam->category = 3;
    jam->severity = common::Severity::Light;
    jam->geometry = {{15, 25}, {35, 45}};

    jam = &jams.emplace_back();
    jam->speed = 20;
    jam->category = 2;
    jam->severity = common::Severity::Hard;
    jam->geometry = {{35, 45}, {60, 45}};

    jam = &jams.emplace_back();
    jam->speed = 5;
    jam->category = 4;
    jam->severity = common::Severity::VeryHard;
    jam->geometry = {{60, 45}, {60, 80}};
    jam->oneWayRoad = false;

    std::vector<TestEvent> events;
    auto* event = &events.emplace_back();
    event->category = 1;
    event->geometry = {15, 25};
    event->type = "RECONSTRUCTION";
    event->tags = {"reconstruction"};
    event->description = "Road works";
    event->lanes = {"left"};
    event->moderated = false;

    event = &events.emplace_back();
    event->category = 3;
    event->geometry = {60, 35};
    event->type = "CLOSED";
    event->tags = {"closed"};
    event->description = "Parade";
    event->moderated = true;
    event->startTime = now - 300;
    event->endTime = std::chrono::system_clock::to_time_t(
        std::chrono::time_point_cast<std::chrono::seconds>(
            chrono::parseIsoDate("2090-07-02")));

    buildMapData(jams, events, paths, now);

    auto mapData = createMapData(paths)[TRF_DATA_NAME];
    ASSERT_TRUE(mapData);
    EXPECT_EQ(mapData->info().layers.size(), 2u);

    data_set::ViewQueryParams params({0, 0, 100, 100}, {0, 23});
    params.auxData = MapDataQueryParams();
    data_set::ViewQueryContext ctx(&params);
    params.layers.emplace();
    params.layers->insert(JAMS_SOURCE_LAYER);
    auto jamsView = mapData->asViewQueriable().queryView(ctx);
    ASSERT_EQ(jamsView.size(), 1u);
    EXPECT_EQ(jamsView[0].info().id(), JAMS_SOURCE_LAYER);
    EXPECT_EQ(jamsView[0].info().featureType(), feature::FeatureType::Polyline);
    EXPECT_THAT(
        collectFeatureTypes(jamsView[0]),
        ElementsAre(feature::FeatureType::Polyline));
    EXPECT_EQ(countFeatures(jamsView[0]), 4u);

    auto feature = findJamFeature(jamsView[0], 30);
    EXPECT_EQ(feature->detailRange().min(), 8u);
    EXPECT_EQ(feature->detailRange().max(), 21u);
    auto geom = feature->geom().contours();
    ASSERT_EQ(geom.size(), 3u);
    EXPECT_THAT(geom[0].x, DoubleNear(10, 0.1));
    EXPECT_THAT(geom[0].y, DoubleNear(20, 0.1));
    EXPECT_THAT(geom[1].x, DoubleNear(30, 0.1));
    EXPECT_THAT(geom[1].y, DoubleNear(40, 0.1));
    EXPECT_THAT(geom[2].x, DoubleNear(35, 0.1));
    EXPECT_THAT(geom[2].y, DoubleNear(45, 0.1));
    auto attrs = feature->attr.get();
    ASSERT_TRUE(attrs->IsObject());
    ASSERT_TRUE(attrs->HasMember("speed"));
    ASSERT_TRUE(attrs->HasMember("category"));
    ASSERT_TRUE(attrs->HasMember("severity"));
    ASSERT_TRUE(attrs->HasMember("oneWayRoad"));
    EXPECT_THAT((*attrs)["speed"].GetDouble(), DoubleNear(30, 0.01));
    EXPECT_THAT((*attrs)["category"].GetInt(), Eq(3));
    EXPECT_THAT((*attrs)["severity"].GetString(), StrEq("light"));
    EXPECT_THAT((*attrs)["oneWayRoad"].GetString(), StrEq("1"));

    feature = findJamFeature(jamsView[0], 25);
    geom = feature->geom().contours();
    ASSERT_EQ(geom.size(), 2u);
    EXPECT_THAT(geom[0].x, DoubleNear(15, 0.1));
    EXPECT_THAT(geom[0].y, DoubleNear(25, 0.1));
    EXPECT_THAT(geom[1].x, DoubleNear(35, 0.1));
    EXPECT_THAT(geom[1].y, DoubleNear(45, 0.1));

    feature = findJamFeature(jamsView[0], 5);
    EXPECT_EQ(feature->detailRange().min(), 10u);
    EXPECT_EQ(feature->detailRange().max(), 21u);
    attrs = feature->attr.get();
    ASSERT_TRUE(attrs->IsObject());
    EXPECT_THAT((*attrs)["speed"].GetDouble(), DoubleNear(5, 0.01));
    EXPECT_THAT((*attrs)["category"].GetInt(), Eq(4));
    EXPECT_THAT((*attrs)["severity"].GetString(), StrEq("very_hard"));
    EXPECT_THAT((*attrs)["oneWayRoad"].GetString(), StrEq("0"));

    feature = findJamFeature(jamsView[0], 20);
    attrs = feature->attr.get();
    EXPECT_THAT((*attrs)["severity"].GetString(), StrEq("hard"));

    params.bbox = {0, 0, 50, 50};
    jamsView = mapData->asViewQueriable().queryView(ctx);
    ASSERT_EQ(jamsView.size(), 1u);
    EXPECT_EQ(countFeatures(jamsView[0]), 3u);

    params.zoomRange = {4, 4};
    jamsView = mapData->asViewQueriable().queryView(ctx);
    ASSERT_EQ(jamsView.size(), 1u);
    EXPECT_EQ(countFeatures(jamsView[0]), 1u);


    params = data_set::ViewQueryParams({0, 0, 100, 100}, {0, 23});
    params.auxData = MapDataQueryParams();
    params.locale = locale::to<locale::Locale>("en_RU");
    params.layers.emplace();
    params.layers->insert(EVENTS_SOURCE_LAYER);
    auto eventsView = mapData->asViewQueriable().queryView(ctx);
    ASSERT_EQ(eventsView.size(), 1u);
    EXPECT_EQ(eventsView[0].info().id(), EVENTS_SOURCE_LAYER);
    EXPECT_EQ(eventsView[0].info().featureType(), feature::FeatureType::Point);
    EXPECT_THAT(
        collectFeatureTypes(eventsView[0]),
        ElementsAre(feature::FeatureType::Point));
    EXPECT_EQ(countFeatures(eventsView[0]), 2u);

    params.bbox = {0, 0, 50, 50};
    eventsView = mapData->asViewQueriable().queryView(ctx);
    ASSERT_EQ(eventsView.size(), 1u);
    ASSERT_EQ(countFeatures(eventsView[0]), 1u);
    feature = eventsView[0].iterator()->next().clone();
    EXPECT_EQ(feature->detailRange().min(), 7u);
    EXPECT_EQ(feature->detailRange().max(), 21u);
    geom = feature->geom().shapes();
    ASSERT_EQ(geom.size(), 2u);
    EXPECT_THAT(geom[0].x, DoubleNear(15, 0.1));
    EXPECT_THAT(geom[0].y, DoubleNear(25, 0.1));
    EXPECT_THAT(geom[1].x, DoubleNear(15, 0.1));
    EXPECT_THAT(geom[1].y, DoubleNear(25, 0.1));
    attrs = feature->attr.get();
    ASSERT_TRUE(attrs->IsObject());
    ASSERT_TRUE(attrs->HasMember("category"));
    ASSERT_TRUE(attrs->HasMember("eventTags"));
    ASSERT_TRUE(attrs->HasMember("moderated"));
    ASSERT_TRUE(attrs->HasMember("description"));
    ASSERT_TRUE(attrs->HasMember("text"));
    EXPECT_THAT((*attrs)["category"].GetInt(), Eq(1));
    EXPECT_THAT(readArray((*attrs)["eventTags"]), ElementsAre("reconstruction"));
    EXPECT_THAT(readArray((*attrs)["icons"]), ElementsAre("reconstruction"));
    EXPECT_THAT((*attrs)["future"].GetString(), StrEq("0"));
    EXPECT_THAT((*attrs)["moderated"].GetString(), StrEq("0"));
    EXPECT_THAT((*attrs)["description"].GetString(), StrEq("Road works"));
    EXPECT_THAT((*attrs)["text"].GetString(), StrEq("In the left lane"));

    params.bbox = {50, 0, 100, 50};
    eventsView = mapData->asViewQueriable().queryView(ctx);
    ASSERT_EQ(eventsView.size(), 1u);
    ASSERT_EQ(countFeatures(eventsView[0]), 1u);
    feature = eventsView[0].iterator()->next().clone();
    attrs = feature->attr.get();
    ASSERT_TRUE(attrs->IsObject());
    EXPECT_THAT((*attrs)["category"].GetInt(), Eq(3));
    EXPECT_THAT(readArray((*attrs)["eventTags"]), ElementsAre("closed"));
    EXPECT_THAT(readArray((*attrs)["icons"]), ElementsAre("closed"));
    EXPECT_THAT((*attrs)["future"].GetString(), StrEq("0"));
    EXPECT_THAT((*attrs)["moderated"].GetString(), StrEq("1"));
    EXPECT_THAT((*attrs)["description"].GetString(), StrEq("Parade"));
    EXPECT_THAT((*attrs)["text"].GetString(), StrEq("Until 02.07.90"));
}

Y_UNIT_TEST(fill_event_icons)
{
    setupi18n();
    const time_t now = std::time(nullptr);
    const time_t past = now - 300;
    const time_t future = now + 300;

    std::vector<TestJam> jams;
    std::vector<TestEvent> events;

    auto* event = &events.emplace_back();
    event->tags = {"closed"};
    event->startTime = past;
    event->description = "icons = closed";

    event = &events.emplace_back();
    event->tags = {"closed"};
    event->startTime = future;
    event->description = "icons = closed_future, closed";

    event = &events.emplace_back();
    event->tags = {"drawbridge"};
    event->startTime = past;
    event->description = "icons = drawbridge";

    event = &events.emplace_back();
    event->tags = {"drawbridge"};
    event->startTime = future;
    event->description = "icons = drawbridge_future, drawbridge";

    event = &events.emplace_back();
    event->tags = {"reconstruction"};
    event->startTime = past;
    event->description = "icons = reconstruction";

    event = &events.emplace_back();
    event->tags = {"reconstruction"};
    event->startTime = future;
    event->description = "icons = reconstruction_future, reconstruction";

    event = &events.emplace_back();
    event->tags = {"other"};
    event->startTime = past;
    event->description = "icons = other";

    event = &events.emplace_back();
    event->tags = {"other"};
    event->startTime = future;
    event->description = "icons = other";

    event = &events.emplace_back();
    event->tags = {"chat"};
    event->startTime = past;
    event->description = "icons = chat";

    event = &events.emplace_back();
    event->tags = {"chat"};
    event->startTime = future;
    event->description = "icons = chat";

    event = &events.emplace_back();
    event->tags = {"cross_road_control", "lane_control", "mobile_control", "no_stopping_control", "police", "road_marking_control", "speed_control"};
    event->description = "icons = speed_control, lane_control, cross_road_control, road_marking_control, no_stopping_control, mobile_control, police";

    event = &events.emplace_back();
    event->tags = {"police", "speed_control"};
    event->description = "icons = speed_control, police";

    event = &events.emplace_back();
    event->tags = {"cross_road_danger", "danger"};
    event->description = "icons = cross_road_danger, danger";

    event = &events.emplace_back();
    event->tags = {"danger", "overtaking_danger"};
    event->description = "icons = overtaking_danger, danger";

    event = &events.emplace_back();
    event->tags = {"other", "traffic_alert"};
    event->description = "icons = traffic_alert, other";

    event = &events.emplace_back();
    event->tags = {"a_new_tag", "police", "speed_control"};
    event->description = "icons = speed_control, a_new_tag, police";

    event = &events.emplace_back();
    event->tags = {"unknown1", "unknown2"};
    event->description = "icons = unknown1, unknown2";

    for (auto& e: events) {
        e.geometry = {10, 10};
    }

    maps::common::TemporaryDir tempDir;
    MapDataPaths paths(tempDir.path().string());
    buildMapData(jams, events, paths, now);

    auto mapData = createMapData(paths)[TRF_DATA_NAME];
    ASSERT_TRUE(mapData);
    data_set::ViewQueryParams params({0, 0, 20, 20}, {0, 23});
    params.auxData = MapDataQueryParams();
    data_set::ViewQueryContext ctx(&params);
    auto view = mapData->asViewQueriable().queryView(ctx);
    ASSERT_EQ(view.size(), 1u);
    EXPECT_EQ(countFeatures(view[0]), events.size());

    auto it = view[0].iterator();
    while (it->hasNext()) {
        const auto& ft = it->next();
        const auto attrs = ft.attr.get();
        const auto& icons = (*attrs)["icons"];
        std::string iconsStr = "icons = ";
        for (size_t i = 0; i < icons.Size(); ++i) {
            if (i > 0) {
                iconsStr += ", ";
            }
            iconsStr += icons[i].GetString();
        }
        EXPECT_EQ(iconsStr, (*attrs)["description"].GetString());
    }
}

Y_UNIT_TEST(filter_events)
{
    setupi18n();
    const time_t now = std::time(nullptr);

    std::vector<TestJam> jams;
    std::vector<TestEvent> events;

    auto* event = &events.emplace_back();
    event->tags = {"closed"};
    event->description = "1";

    event = &events.emplace_back();
    event->tags = {"closed"};
    event->moderated = true;
    event->description = "2";

    event = &events.emplace_back();
    event->tags = {"chat"};
    event->moderated = true;
    event->description = "3";

    event = &events.emplace_back();
    event->tags = {"speed_control", "police"};
    event->moderated = true;
    event->description = "4";

    event = &events.emplace_back();
    event->tags = {"mobile_control", "police"};
    event->moderated = true;
    event->description = "5";

    for (auto& e: events) {
        e.geometry = {10, 10};
    }

    maps::common::TemporaryDir tempDir;
    MapDataPaths paths(tempDir.path().string());
    buildMapData(jams, events, paths, now);

    auto mapData = createMapData(paths)[TRF_DATA_NAME];
    ASSERT_TRUE(mapData);
    data_set::ViewQueryParams params({0, 0, 20, 20}, {0, 23});
    params.auxData = MapDataQueryParams();
    data_set::ViewQueryContext ctx(&params);
    auto view = mapData->asViewQueriable().queryView(ctx);
    ASSERT_EQ(view.size(), 1u);
    EXPECT_THAT(
        readEventDescriptions(view[0].iterator()),
        UnorderedElementsAre("1", "2", "3", "4", "5"));

    MapDataQueryParams data;
    data.moderateFilter = ModerateFilter::ModeratedEvents;
    data.eventAllowedTags = {"closed", "speed_control"};
    params.auxData = data;
    view = mapData->asViewQueriable().queryView(ctx);
    ASSERT_EQ(view.size(), 1u);
    EXPECT_THAT(
        readEventDescriptions(view[0].iterator()),
        UnorderedElementsAre("2", "4"));
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::jams::renderer::tests
