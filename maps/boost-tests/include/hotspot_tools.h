#pragma once

#include <yandex/maps/renderer5/hotspots/HotspotsTools.h>
#include <yandex/maps/renderer/feature/attributes.h>
#include <yandex/maps/hotspots-base5/i_generator.h>
#include <yandex/maps/rapidjsonhelper/rapidjsonhelper.h>

namespace maps { namespace renderer5 { namespace test {

class GeneratorMock : public maps::hotspots::base5::IGenerator
{
public:
    GeneratorMock(std::string mark) :
        settings_("{\"enableRenderedGeometry\": true, \"geometryTypes\": [\"Point\", \"Line\", \"TextPoint\"]}"),
        counter_(0),
        mark_(std::move(mark))
    {
    }

    rapidjson::Value generate(
        const rapidjson::Value& source,
        const std::string& /*locale*/,
        rapidjson::Allocator* alloc) const override
    {
        maps::rjhelper::ValueRef ref(source);
        std::string layerKey = ref.GetMember<std::string>("layer_key_for_test");
        std::string id = ref.GetMember<std::string>(renderer::feature::SOURCE_ID_ATTR_NAME);
        std::string uid = hotspots::FeatureKeyGenerator(layerKey).generate(std::stol(id));

        rapidjson::Value data;
        rjhelper::ObjectBuilder o(&data, alloc);
        if (!mark_.empty() && std::string::npos == uid.find(mark_))
            return data;

        int h = hash(uid) + counter_++;
        o("HotspotMetaData", [&](rjhelper::ObjectBuilder o) {
            o("id", uid);
            o("layer", h % 2 ? "events" : "jams");
        });
        o("motto", motto(h % 3));

        return data;
    }

    std::string motto(int num) const
    {
        switch (num) {
            case 0: return "Veritas";
            case 1: return "Mens et Manus";
        }

        return "Dominus Illuminatio Mea";
    }

    int hash(const std::string& s) const
    {
        int r = 0;
        for (char c : s)
            r += static_cast<int>(c);

        return r;
    }

    const std::string& settings(const std::string&) const override
    {
        return settings_;
    }

private:
    mutable int counter_;
    const std::string settings_;
    const std::string mark_;
};


} } } // maps::renderer5::test
