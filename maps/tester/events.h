#pragma once

#include <maps/libs/edge_persistent_index/include/persistent_index.h>

#include <string>
#include <limits>

const size_t WRITE_ALL_EVENTS = std::numeric_limits<size_t>::max();

void createFileForEasyView(const std::string& inputEventsFileName,
                           const std::string& outputEasyViewFileName,
                           const maps::road_graph::PersistentIndex& persistentIndex,
                           size_t recordsCount = WRITE_ALL_EVENTS);
