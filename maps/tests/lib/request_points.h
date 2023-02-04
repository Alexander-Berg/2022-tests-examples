#pragma once

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/common/include/exception.h>

#include <contrib/libs/rapidjson/include/rapidjson/document.h>
#include <contrib/libs/rapidjson/include/rapidjson/istreamwrapper.h>

#include <library/cpp/testing/unittest/env.h>

#include <fstream>

namespace maps::bicycle::tests {

class RequestPointsReader {
public:
    RequestPointsReader():
        inputFile_(BuildRoot() + "/maps/bicycle/router/tests/data/points.json"),
        wrapper_(inputFile_)
        
    {
        document_.ParseStream(wrapper_);
        ASSERT(document_.GetArray().Size() > 0);
    }

    struct Request {
        maps::geolib3::Point2 from;
        maps::geolib3::Point2 to;
    };

    std::optional<Request> nextRequest()
    {
        const auto array = document_.GetArray();
        if (elementIndex_ == array.Size()) {
            return std::nullopt;
        }
        const auto& requestJson = array[elementIndex_];
        ++elementIndex_;
        const auto from =  maps::geolib3::Point2(requestJson["start"]["lon"].GetDouble(),
                requestJson["start"]["lat"].GetDouble());
        const auto to =  maps::geolib3::Point2(requestJson["end"]["lon"].GetDouble(),
                requestJson["end"]["lat"].GetDouble());
        return Request{from, to};
    }

private:
    std::ifstream inputFile_;
    rapidjson::IStreamWrapper wrapper_;
    std::size_t elementIndex_{0};
    rapidjson::Document document_;

};

inline std::ostream& operator<<(std::ostream& stream, const RequestPointsReader::Request& r) {
    return stream << "from: " << r.from.x() << " " << r.from.y()
        << " to: " << r.to.x() << " " << r.to.y();
}

} // namespace maps::bicycle::tests

