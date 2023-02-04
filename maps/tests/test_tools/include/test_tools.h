#pragma once

#include <library/cpp/testing/unittest/env.h>
#include <util/string/cast.h>

#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/task.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/segment_travel_time_consumer.h>

#include <maps/analyzer/libs/data/include/gpssignal.h>
#include <maps/libs/log8/include/log8.h>

#include <maps/libs/xml/include/xml.h>

#include <boost/date_time/posix_time/posix_time.hpp>

#include <fstream>
#include <string>
#include <vector>

namespace mad = maps::analyzer::data;

const std::string TEST_DATA_ROOT =
    ArcadiaSourceRoot() + "/maps/analyzer/services/jams_analyzer/modules/usershandler/tests/data";
const std::string GRAPH_DATA_PATH = BinaryPath("maps/data/test/analyzer/graph3/road_graph.fb");
const std::string EDGES_PERSISTENT_INDEX_PATH = BinaryPath("maps/data/test/analyzer/graph3/edges_persistent_index.fb");
const std::string EDGES_RTREE_PATH = BinaryPath("maps/data/test/analyzer/graph3/rtree.fb");
const std::string MANOEUVRES_PATH = BinaryPath("maps/analyzer/services/jams_analyzer/modules/usershandler/tests/generated_data/small_manoeuvres.mms");
const std::string NONEXISTENT_PATH = "nonexistent_path";

class PseudoRandom
{
public:
    PseudoRandom(size_t seed):seed_(seed) {}
    size_t next()
    {
        return seed_ = (seed_ * seed_ * 723 + seed_ * 517 + 5413) / 100;
    }
    size_t next(size_t max)
    {
        return next() % max;
    }

private:
    size_t seed_;
};


class ToStream : public maps::log8::Backend {
public:
    ToStream(std::ostream& out):
        out_(out)
    {}

    virtual void put(const maps::log8::Message& message)
    {
        out_ << message.text() << "\n";
    }

private:
    std::ostream& out_;
};


inline bool operator== (const Task& lhs, const Task& rhs)
{
    return lhs.time() == rhs.time() && lhs.type() == rhs.type()
        && lhs.id() == rhs.id()
        && lhs.signals() == rhs.signals();
}

inline std::ostream& operator << (std::ostream& out, const Task& task)
{
    out << ToString(task.type()) << " " << task.id() << " " << task.time();
    for (const maps::analyzer::data::GpsSignal& signal : task.signals()) {
        out << std::endl << signal.debugString();
    }
    return out;
}

inline std::string makeUsershandlerConfig(const std::string& configFilename)
{
    const auto configPath = TEST_DATA_ROOT + "/" + configFilename;
    maps::xml3::Doc config(configPath, maps::xml3::Doc::File);
    auto root = config.root();

    auto rg_root = root.addChild("road_graph");
    rg_root.addChild("path_to_graph", GRAPH_DATA_PATH);
    rg_root.addChild("path_to_rtree", EDGES_RTREE_PATH);
    rg_root.addChild("path_to_edges_persistent_index", EDGES_PERSISTENT_INDEX_PATH);

    root.addChild("path_to_manoeuvres", MANOEUVRES_PATH);

    const auto configPathTmp = configPath + ".tmp";
    std::ofstream out(configPathTmp);
    root.save(out, "utf-8");
    out.close();
    return configPathTmp;
}

template <typename T>
void updateConfig(
    const std::string& configPath, std::vector<std::string> pathToNode, const T& newValue
) {
    maps::xml3::Doc newDoc(configPath, maps::xml3::Doc::File);
    auto root = newDoc.root();
    auto toTrash = root;
    for (const auto& nodeName : pathToNode) {
        toTrash = toTrash.node(nodeName);
    }
    toTrash.remove();
    const std::string nodeNameToUpdate = pathToNode.back();
    pathToNode.pop_back();
    auto nodeToUpdate = root;
    for (const auto& nodeName : pathToNode) {
        nodeToUpdate = nodeToUpdate.node(nodeName);
    }
    nodeToUpdate.addChild<T>(nodeNameToUpdate, newValue);
    std::ofstream out(configPath);
    root.save(out, "utf-8");
    out.close();
}

// Accumulates incoming SegmentTravelTimes in vector
class TestConsumer : public SegmentTravelTimeConsumer
{
public:
    virtual void pushSegment(const mad::SegmentTravelTime& item)
    {
        segmentVector.push_back(item);
    }

    std::vector<mad::SegmentTravelTime> segmentVector;
};

namespace maps {
namespace analyzer {
namespace data {
inline bool operator == (const GpsSignal& lhs, const GpsSignal& rhs)
{
    return lhs.debugString() == rhs.debugString();
}
}}}//namespace maps::analyzer::data
