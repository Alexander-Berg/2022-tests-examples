#include <maps/libs/cmdline/include/cmdline.h>
#include <maps/libs/geolib/include/segment.h>
#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/distance.h>

#include <maps/libs/road_graph/serialization/include/serialization.h>
#include <maps/libs/edge_persistent_index/packer/lib/include/persistent_index_builder.h>
#include <maps/libs/road_graph_import_yt/include/persistent_ids.h>

#include <vector>
#include <iostream>
#include <fstream>
#include <sstream>

namespace geo = maps::geolib3;
using maps::road_graph::LongEdgeId;
using maps::road_graph::EdgeId;
using maps::road_graph::VertexId;


inline std::uint32_t edgeIdToCategory(EdgeId edgeId) {
    return (edgeId.value() % 7) + 1;
}

class CoverageBuilder {
public:
    explicit CoverageBuilder(std::ostream& os)
        : os_(os)
    {
        writeHeader();
    }

    void addRegion(size_t region, const geo::Polyline2& border) {
        addRegion(region, border.points());
    }

    void addRegion(size_t region, const geo::BoundingBox& border) {
        const auto corners = border.corners();
        addRegion(region, geo::PointsVector{corners.begin(), corners.end()});
    }

    void addRegion(size_t region, const geo::PointsVector& border) {
        os_ <<
            "<GeoObject>\n"
            "<gml:metaDataProperty>\n"
            "<cvr:CoverageMemberMetaData>\n"
            "<cvr:id>" << region << "</cvr:id>\n"
            "</cvr:CoverageMemberMetaData>\n"
            "</gml:metaDataProperty>\n"
            "<gml:Polygon>\n"
            "<gml:exterior>\n"
            "<gml:LinearRing>\n"
            "<gml:posList>";
        for (const geo::Point2& point: border) {
            os_ << point.x() << " " << point.y() << "  ";
        }
        os_ << "</gml:posList>\n"
            "</gml:LinearRing>\n"
            "</gml:exterior>\n"
            "</gml:Polygon>\n"
            "</GeoObject>\n"
            ;
    }

    void finish() {
        writeFooter();
    }

private:
    void writeHeader() {
        os_ <<
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            "<ymaps xmlns=\"http://maps.yandex.ru/ymaps/1.x\" "
            "xmlns:gml=\"http://www.opengis.net/gml\" "
            "xmlns:cvr=\"http://maps.yandex.ru/coverage/2.x\""
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            "xsi:schemaLocation=\""
            "http://www.opengis.net/gml ../../../ymaps/1.x/gml.xsd "
            "http://maps.yandex.ru/ymaps/1.x ../../../ymaps/1.x/ymaps.xsd "
            "http://maps.yandex.ru/coverage/2.x ../../../coverage/2.x/coverage.xsd"
            "\">\n"
            "<GeoObjectCollection>\n"
            "<gml:metaDataProperty>\n"
            "<cvr:CoverageMetaData>\n"
            "<cvr:id>trf</cvr:id>\n"
            "</cvr:CoverageMetaData>\n"
            "</gml:metaDataProperty>\n"
            "<gml:featureMembers>\n"
            ;
    }

    void writeFooter() {
        os_ <<
            "</gml:featureMembers>\n"
            "</GeoObjectCollection>\n"
            "</ymaps>\n"
            ;
    }

    std::ostream& os_;
};

// Shortcut for avoiding excessively long lines.
geo::BoundingBox makeBBox(double x1, double y1, double x2, double y2) {
    return geo::BoundingBox(geo::Point2(x1, y1), geo::Point2(x2, y2));
}

int main(int argc, char* argv[]) {
    using namespace std::string_literals;

    maps::cmdline::Parser p;
    auto roadGraphFile = p.file("road-graph").help("output road graph file");
    auto persistentIndexFile = p.file("persistent-index").help("output file with edge persistent index");
    auto coverageFile = p.file("coverage-file").help("output file with coverage");

    p.parse(argc, argv);

    std::vector<double> x{0.0, 0.1, 1.0};
    std::vector<double> y{x};

    maps::road_graph::MutableGraph graph{
        "1"s, // version
        x.size() * y.size(), // vertices
        x.size() * (y.size() - 1) * 2 + 1, // edges within grid, +1 for one extra edge
        0
    };
    maps::road_graph::PersistentIndexBuilder persistentIndex{"1"s};

    const auto vertexId = [&](std::size_t i, std::size_t j) {
        return VertexId(i * y.size() + j);
    };

    const auto vertexPoint = [&](std::size_t i, std::size_t j) {
        return geo::Point2{x[i], y[j]};
    };

    // fill grid vertices
    for (std::size_t i = 0; i < x.size(); ++i) {
        for (std::size_t j = 0; j < y.size(); ++j) {
            graph.setVertexGeometry(vertexId(i, j), vertexPoint(i, j));
        }
    }

    EdgeId currentEdgeId{0};
    const auto addEdge = [&](VertexId from, VertexId to, const geo::Polyline2& pline) {
        auto edgeId = currentEdgeId++;

        graph.setEdge({edgeId, from, to}, true);
        graph.setEdgeData(edgeId, {
            .category=edgeIdToCategory(edgeId),
            .length=geo::geoLength(pline),
            .geometry=pline,
        });

        persistentIndex.setEdgePersistentId(
            edgeId,
            LongEdgeId{maps::graph_import::generateLongId(
                pline.pointAt(0),
                pline.pointAt(pline.pointsNumber() - 1),
                pline,
                true, // bidirectional
                false, // reverse
                0, 0 // z-levels
            )}
        );

        return edgeId;
    };

    // fill edges
    {
        using Coords = std::tuple<std::size_t, std::size_t>;
        std::vector<std::tuple<Coords, Coords>> edges;
        for (std::size_t i = 0; i < x.size(); ++i) {
            for (std::size_t j = 0; j < y.size(); ++j) {
                if (i > 0) {
                    edges.push_back({{i - 1, j}, {i, j}});
                }
                if (j > 0) {
                    edges.push_back({{i, j - 1}, {i, j}});
                }
            }
        }
        std::sort(edges.begin(), edges.end());
        for (const auto& [src, dst]: edges) {
            addEdge(
                std::apply(vertexId, src),
                std::apply(vertexId, dst),
                geo::Polyline2{geo::Segment2{
                    std::apply(vertexPoint, src),
                    std::apply(vertexPoint, dst)
                }}
            );
        }
    }

    // add one extra edge
    geo::Polyline2 polyline;
    polyline.add(vertexPoint(2, 2));
    polyline.add(geo::Point2(-1.0, 2.0));
    polyline.add(vertexPoint(0, 0));

    addEdge(
        vertexId(2, 2),
        vertexId(0, 0),
        polyline
    );

    maps::road_graph::serialize(graph, roadGraphFile);
    persistentIndex.save(persistentIndexFile);

    std::ofstream coverageStream(coverageFile.c_str());
    CoverageBuilder coverageBuilder(coverageStream);
    for (std::size_t region = 1; region <= 2; ++region) {
        coverageBuilder.addRegion(
            region,
            makeBBox(-0.5 + (region - 1), -0.5, -0.5 + region, 3.0)
        );
    }
    for (std::size_t region = 3; region <= 4; ++region) {
        coverageBuilder.addRegion(
            region,
            makeBBox(-0.5, -0.5 + (region - 3), 3.0, -0.5 + (region - 2))
        );
    }
    coverageBuilder.addRegion(5, makeBBox(-0.5, -0.5, 5.5, 5.5));
    coverageBuilder.finish();
    coverageStream.close();
}
