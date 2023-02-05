#include "../setting_impl.h"
#include "../uri_query_parser.h"

#include <yandex/maps/navikit/mocks/mock_datasync_record.h>

#include <yandex/maps/runtime/async/dispatcher.h>

#include <boost/optional/optional_io.hpp>
#include <boost/test/unit_test.hpp>

namespace yandex::maps::navi::settings::tests {

using mapkit::road_events::EventTag;
using RoadEvents = std::map<EventTag, RoadEventNotificationMode>;

namespace {

template <typename T, datasync::ValueType valueType>
class MyRecord : public datasync::MockRecord {
public:
    boost::optional<T> getValue() const { return value_; }

    bool hasField(const std::string& fieldName) const override
    {
        ASSERT(fieldName == "value");
        return bool(value_);
    }

    datasync::ValueType type(const std::string& fieldName) const override {
        ASSERT(fieldName == "value");
        return valueType;
    }

protected:
    boost::optional<T> value_;
};

class MyBoolRecord : public MyRecord<bool, datasync::ValueType::Bool> {
public:
    virtual MyBoolRecord* setField(const std::string& fieldName, bool boolValue) override
    {
        ASSERT(fieldName == "value");
        value_ = boolValue;
        return this;
    }
};

class MyStringRecord : public MyRecord<std::string, datasync::ValueType::String> {
public:
    virtual MyStringRecord* setField(
        const std::string& fieldName, const std::string& value) override
    {
        ASSERT(fieldName == "value");
        value_ = value;
        return this;
    }

    std::string fieldAsString(const std::string& fieldName) const override {
        ASSERT(fieldName == "value");
        return *value_;
    }
};

class MyRoadEvents : public RoadEvents {
public:
    MyRoadEvents(const RoadEvents& events) : RoadEvents(events) {}

    MyRoadEvents set(EventTag event, RoadEventNotificationMode mode) const
    {
        MyRoadEvents result = *this;
        result[event] = mode;
        return result;
    }

    MyRoadEvents off() const
    {
        MyRoadEvents result = *this;
        for (auto& it : result)
            it.second = RoadEventNotificationMode::Disabled;
        return result;
    }

    std::string toString() const { return enumMapToString(*this); }
};

}  // namespace

BOOST_AUTO_TEST_CASE(SetBoolByUri)
{
    const struct {
        const char* strValue;
        bool boolValue;
    } tests[] = {
        {"true", true},
        {"false", false},
    };

    for (const auto& test : tests) {
        MyBoolRecord record;

        runtime::async::ui()
            ->spawn([&] {
                auto setting = createSetting("mySetting", /* pushDBChanges= */ [] {});
                setting->setSetterByUri(&UriQueryParser<bool>::setSetting);
                setting->setRecord(&record);
                const std::map<std::string, std::string> params = {{"value", test.strValue}};
                setting->setByUriQuery(params);
            })
            .wait();

        BOOST_CHECK_EQUAL(record.getValue(), test.boolValue);
    }
}

BOOST_AUTO_TEST_CASE(SetRoadEventsByUri)
{
    const MyRoadEvents defaultEvents = getDefaultRoadEventModes();
    const MyRoadEvents myEvents =
        defaultEvents.set(EventTag::Closed, RoadEventNotificationMode::Disabled)
            .set(EventTag::CrossRoadControl, RoadEventNotificationMode::VisualAndVoice);
    const MyRoadEvents offEvents = myEvents.off();

    const struct {
        std::map<std::string, std::string> params;
        RoadEvents events;
    } tests[] = {
        {{}, myEvents},
        {{{"value", ""}}, offEvents},
        {{{"value", "XXX"}}, offEvents},
        {{{"value", "Other"}},
         offEvents.set(EventTag::Other, RoadEventNotificationMode::VisualOnly)},
        {{{"value", "Other,School"}},
         offEvents.set(EventTag::Other, RoadEventNotificationMode::VisualOnly)
             .set(EventTag::School, RoadEventNotificationMode::VisualAndVoice)},
        {{{"value", "Other,School,CrossRoadControl"}},
         offEvents.set(EventTag::Other, RoadEventNotificationMode::VisualOnly)
             .set(EventTag::School, RoadEventNotificationMode::VisualAndVoice)
             .set(EventTag::CrossRoadControl, RoadEventNotificationMode::VisualAndVoice)},
        {{{"value", ""}, {"on", "Feedback"}, {"off", "Police"}}, offEvents},
        {{{"on", "Feedback"}, {"off", "Police"}},
         myEvents.set(EventTag::Feedback, RoadEventNotificationMode::VisualOnly)
             .set(EventTag::Police, RoadEventNotificationMode::Disabled)},
        {{{"on", "Feedback,XXX"}, {"off", "YYY,Police"}},
         myEvents.set(EventTag::Feedback, RoadEventNotificationMode::VisualOnly)
             .set(EventTag::Police, RoadEventNotificationMode::Disabled)},
        {{{"on", "Feedback,SpeedControl"}, {"off", "Police,CrossRoadControl"}},
         myEvents.set(EventTag::Feedback, RoadEventNotificationMode::VisualOnly)
             .set(EventTag::Police, RoadEventNotificationMode::Disabled)
             .set(EventTag::CrossRoadControl, RoadEventNotificationMode::Disabled)},
    };

    for (const auto& test : tests) {
        MyStringRecord record;
        record.setField("value", myEvents.toString());

        runtime::async::ui()
            ->spawn([&] {
                auto setting = createSetting("myRoadEvents", /* pushDBChanges= */ [] {});
                setting->setRecord(&record);
                setting->setDefault(defaultEvents.toString());
                setting->setSetterByUri(&UriQueryParser<RoadEvents>::setSetting);
                setting->setByUriQuery(test.params);
            })
            .wait();

        const std::string expectedValue = enumMapToString(test.events);
        BOOST_CHECK_EQUAL(record.getValue(), expectedValue);
    }
}

}  // namespace yandex
