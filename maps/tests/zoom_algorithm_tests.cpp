#include <maps/factory/tools/eval_visible_mosaics_geometries/lib/util/util.h>
#include <maps/factory/tools/eval_visible_mosaics_geometries/lib/eval_with_zooms/zooms_evaluator.h>
#include "test_util.h"

namespace maps::factory::eval_mosaics_geometries::tests {

using namespace maps::factory::eval_mosaics_geometries;

TEST(sorting, scanline_tests)
{
    std::mt19937 rng(610);
    std::vector<EventPtr> events;
    for (size_t i = 0; i < 20; i++) {
        events.emplace_back(
            new ZoomScanlineEvent(rng() % 100, 0, EventType::AddGeometry));
    }

    ZoomScanline scanline(std::move(events));
    int time;
    while (scanline.hasNext()) {
        time = scanline.getCurrentTime();
        ASSERT(scanline.popNextEvent()->time() >= time);
    }
}

TEST(visual_part, zooms_evaluator_tests)
{
    auto geoms = getTest12();
    std::vector<GeometryWithZoom> zoomedGeoms;
    std::vector<Geometry> allGeoms;
    for (size_t i = 0; i < geoms.size(); i++) {
        zoomedGeoms.emplace_back(std::move(geoms[i]), i, 0, 0);
        allGeoms.push_back(zoomedGeoms.back().geometryCopy());
    }
    auto pieces = toPieces(zoomedGeoms);
    ZoomsEvaluator evaluator(std::move(zoomedGeoms), allGeoms, 10, 1e12);

    std::vector<TMap<size_t, Geometry>> solutions;
    zoomedGeoms = evaluator.getZoomedVisibleGeometries();
    auto zoomedSolution = toPieces(zoomedGeoms);

    solutions.push_back(uniteGeometryPieces(evalWithUnion(pieces)));
    solutions.push_back(uniteGeometryPieces(zoomedSolution));

    checkSameGeometries(solutions);
}

template <class T>
using ArrayPair = std::array<T, 2>;

TEST(two_zooms, zooms_evaluator_tests)
{
    // building two geometry sets
    ArrayPair<std::vector<Geometry>> geoms;
    for (auto& geomSet: geoms) {
        geomSet = getTest12();
        std::random_shuffle(geomSet.begin(), geomSet.end());
    }

    // uniting them as zoomed geometries
    std::vector<GeometryWithZoom> zoomedGeoms;
    std::vector<Geometry> allGeoms;
    size_t cnt = 0;
    for (size_t setNum = 0; setNum < 2; setNum++) {
        auto& geomSet = geoms[setNum];
        for (size_t i = 0; i < geomSet.size(); i++) {
            zoomedGeoms.emplace_back(geomSet[i].copy(), cnt++, setNum, setNum);
            allGeoms.push_back(geomSet[i].copy());
        }
    }

    // getting them as regular pieces
    ArrayPair<std::vector<GeometryPiece>> pieces;
    cnt = 0;
    for (size_t setNum = 0; setNum < 2; setNum++) {
        auto& geomSet = geoms[setNum];
        for (size_t i = 0; i < geomSet.size(); i++) {
            pieces[setNum].emplace_back(std::move(geomSet[i]), cnt++);
        }
    }

    // getting two ZoomsEvaluator soution
    ZoomsEvaluator evaluator(std::move(zoomedGeoms), allGeoms, 5, 1e12);
    zoomedGeoms = evaluator.getZoomedVisibleGeometries();

    // getting regular union solution
    std::vector<GeometryPiece> pieceSolution;
    for (size_t i = 0; i < 2; i++) {
        auto solution = evalWithGroupSplit(pieces[i]);
        for (auto& geom: solution) {
            pieceSolution.push_back(std::move(geom));
        }
    }

    // checking if they are the same
    std::vector<TMap<size_t, Geometry>> solutions;
    solutions.push_back(uniteGeometryPieces(pieceSolution));
    solutions.push_back(uniteGeometryPieces(toPieces(zoomedGeoms)));

    checkSameGeometries(solutions);
}

TEST(random_zooms, zooms_evaluator_tests)
{
    std::mt19937 rng(610);

    // get some random zoomed geometries
    std::vector<Geometry> allGeoms;
    std::vector<GeometryWithZoom> zoomedGeoms;
    auto geoms = addAndTwistGeometries(getTest12(), 500);

    for (size_t i = 0; i < geoms.size(); i++) {
        const auto& geom = geoms[i];
        int minZoom = rng() % 10;
        int maxZoom = rng() % 10;
        if (minZoom > maxZoom) {
            std::swap(minZoom, maxZoom);
        }

        zoomedGeoms.emplace_back(geom.copy(), i, minZoom, maxZoom);
        allGeoms.push_back(geom.copy());
    }

    // get geom sets by zoom
    TMap<int, std::vector<GeometryPiece>> geomByZoom;
    for (size_t i = 0; i < zoomedGeoms.size(); i++) {
        const auto& geom = zoomedGeoms[i];
        for (int zoom = geom.minZoom(); zoom <= geom.maxZoom(); zoom++) {
            geomByZoom[zoom].emplace_back(geom.geometryCopy(), geom.index());
        }
    }

    // calculate visible boundaries with zooms evaluator
    ZoomsEvaluator evaluator(std::move(zoomedGeoms), allGeoms, 10, 1e12);
    zoomedGeoms = evaluator.getZoomedVisibleGeometries();

    // calculate visible bouandaries with regular algorithm
    std::vector<GeometryPiece> allPieces;
    for (int zoom = 0; zoom < 10; zoom++) {
        geomByZoom[zoom] = evalWithGroupSplit(geomByZoom[zoom]);
        for (const auto& piece: geomByZoom[zoom]) {
            allPieces.emplace_back(piece.geometryCopy(), piece.index());
        }
    }

    // check if the results are the same
    std::vector<TMap<size_t, Geometry>> solutions;
    solutions.push_back(uniteGeometryPieces(allPieces));
    solutions.push_back(uniteGeometryPieces(toPieces(zoomedGeoms)));
    checkSameGeometries(solutions);
}

} // namespace maps::factory::eval_mosaics_geometries::tests
