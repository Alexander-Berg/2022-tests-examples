#include "events.h"

#include <yandex/maps/jams/router/jams.h>
#include <maps/libs/log8/include/log8.h>

#include <ctime>
#include <fstream>
#include <unordered_map>

using maps::jams::router::Jams;
using maps::jams::router::Event;

void createFileForEasyView(const std::string& inputEventsFileName,
                           const std::string& outputEasyViewFileName,
                           const maps::road_graph::PersistentIndex& persistentIndex,
                           size_t recordsCount)
{
    // Reading Jams by short egde ids (old style, TODO!)
    Jams jams(persistentIndex, inputEventsFileName);
    auto events = jams.events();
    INFO() << events.size() << " events were read";

    std::unordered_map<std::string, Event> eventMap;

    std::ofstream file;
    file.open(outputEasyViewFileName);
    file.precision(10);
    size_t writtenRecordsCount = 0;
    for (auto& event: events) {
        if (writtenRecordsCount == recordsCount) {
            break;
        }
        eventMap.insert(std::make_pair(event.second.id(), event.second));
    }
    for (auto&& eventElem: eventMap) {
        auto&& event = eventElem.second;
        file << event.lon() << " " << event.lat() << " "
             << event.type() << ":" << event.id() << "\n";
        ++ writtenRecordsCount;
    }
    INFO() << writtenRecordsCount << " events were written";
    file.close();
}
