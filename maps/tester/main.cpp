#include <maps/infopoint/tie-events/lib/boost_compat.h>
#include "events.h"

#include <yandex/maps/program_options/option_description.h>
#include <maps/libs/graph/po_helpers/include/graph_objects.h>
#include <maps/libs/log8/include/log8.h>

namespace mg = maps::graph2;
namespace po = maps::program_options;

int main(int argc, char* argv[])
{
    po::options_description optionsDescr("Allowed options", 110);

    po::OptionDescription<std::string> routerEventsPath(
            &optionsDescr,
            "router-events",
            "Input file with router events"
                    " in yandex.maps.jams.router.proto.Event format");

    po::OptionDescription<std::string> easyViewEventsPath(
            &optionsDescr,
            "easyview-events",
            "Output file with events"
                    " in EasyView format");

    po::OptionDescription<size_t> recordsCount(
            &optionsDescr,
            "records-count",
            "Events count to be written to output file",
            WRITE_ALL_EVENTS);

    mg::GraphObjects graphObjects(
            &optionsDescr, mg::po::GraphEntity::COMPACT_PERSISTENT_INDEX);

    po::variables_map vm = po::parseCommandLine(argc, argv, optionsDescr, true);

    try {
        // read events from input file
        if (routerEventsPath->empty()) {
            ERROR() << "Input file with router events is not provided";
            return 1;
        }
        // read events from input file and write the file for easy view
        if (!easyViewEventsPath->empty()) {
            createFileForEasyView(
                    *routerEventsPath,
                    *easyViewEventsPath,
                    graphObjects.get<mg::po::GraphEntity::COMPACT_PERSISTENT_INDEX>(),
                    *recordsCount);
        } else {
            ERROR() << "Input file with events is not provided";
            return 1;
        }
    } catch (const maps::Exception& exception) {
        ERROR() << exception.what();
        return 1;
    }

    return 0;
}
