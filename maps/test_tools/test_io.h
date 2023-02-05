#pragma once

#include "../geom_tools/geom_io.h"
#include "../events_data.h"

#include <yandex/maps/wiki/topo/common.h>

#include <ostream>


namespace maps {
namespace wiki {
namespace topo {
namespace test {


std::ostream& operator<<(std::ostream& out, const std::pair<NodeID, NodeID>& id);

std::ostream& operator<<(std::ostream& out, const IncidentNodes&);

std::ostream& operator<<(std::ostream& out, const IncidentEdges&);

std::ostream& operator<<(std::ostream& out, const IncidencesByNodeMap&);

std::ostream& operator<<(std::ostream& out, const IncidencesByEdgeMap&);


} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps

