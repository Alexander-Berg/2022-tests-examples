#include <maps/masstransit/libs/binding_to_graph_fb/include/build_bindings.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/cmdline/include/cmdline.h>

#include <fstream>

namespace geo = maps::geolib3;
using GraphData = maps::masstransit::graph_data::GraphData;

int main(int argc, char** argv)
{
    maps::cmdline::Parser parser;
    auto graphPath = parser
        .file('g', "graph")
        .defaultValue("tests/data/road_graph.fb");
    auto rtreePath = parser
        .file('r', "rtree")
        .defaultValue("tests/data/rtree.fb");
    auto barriersPath = parser
        .file('b', "barriers")
        .defaultValue("tests/data/routing_barriers_rtree.mms.1");
    auto outPath = parser
        .file('o', "output")
        .defaultValue("tests/data/test_bindings.fb");
    parser.parse(argc, argv);

    std::vector<std::pair<std::string, geo::Point2>> stops = {
        {"point1", geo::Point2{27.89759583, 53.87500895}},
        {"point2", geo::Point2{27.89245804, 53.87248450}},
    };

    const GraphData::Config graphDataConfig(graphPath, rtreePath, &barriersPath);
    const auto graphData = GraphData(graphDataConfig);

    maps::masstransit::binding_to_graph_fb::BindingConfig bindingConf{};
    std::ofstream out(outPath);
    maps::masstransit::binding_to_graph_fb::buildBindings(
        out,
        graphData,
        bindingConf,
        stops
    );
}
