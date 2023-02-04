#include "tests/boost-tests/include/tools/map_tools.h"
#include "contexts.hpp"

#include <yandex/maps/renderer/depot/label/label.h>
#include <yandex/maps/renderer/depot/topo/fd.h>
#include <yandex/maps/renderer/depot/rtree/rtree.h>
#include <yandex/maps/renderer/depot/storage_options.h>
#include "mapcompiler/consts.h"
#include "core/feature.h"
#include "core/ISearchableLayer.h"
#include "labeler/parse_labeling_rules.h"
#include "labeler/convert_label_backward.h"
#include "labeler/convert_label_forward.h"
#include <yandex/maps/renderer5/labeler/labeling_zone.h>
#include <maps/renderer/libs/base/include/hash.h>
#include <yandex/maps/renderer5/mapcompiler/mapcompiler.h>
#include <yandex/maps/renderer/io/io.h>
#include <yandex/maps/renderer/depot/topo/context.h>
#include <yandex/maps/renderer5/core/FeatureCapabilities.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/StyleHolders.h>
#include <maps/renderer/libs/base/include/sha1.h>
#include <maps/renderer/libs/base/include/array_ref.h>
#include <maps/renderer/libs/base/include/string_convert.h>
#include <yandex/maps/renderer/proj/mercator.h>
#include <yandex/maps/renderer/proj/tile.h>

#include <boost/format.hpp>
#include <boost/test/unit_test.hpp>
#include <iostream>
#include <string>
#include <time.h>

namespace {

using namespace maps;
using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::core;
using namespace maps::renderer5::labeler;

std::string mapNameOriginal = "tests/boost-tests/maps/TextAndIconMap.xml";
std::string mapName         = "tmp/staticMap.xml";
std::string tempPath        = "tmp/";

const std::string extDump  = ".dump";
const std::string extIndex = ".index";
const std::string extStore = ".store";

Feature ft(FeatureType::Text, FeatureCapabilities{CapabilityFeaturePlacedLabel2});
std::vector<base::Vec2D> tempV2;

void save(io::OStream& file, const PlacedLabel2& src)
{
    file << src.bbox << src.textMain << src.textAlt << src.priority;
    file << uint8_t(src.isCurved() ? 1 : 0);
    if (src.isStraight()) {
        auto& stra = src.straight();
        file << stra.posCenter << stra.hasIcon << bool(stra.textBox);
        if (stra.textBox)
            file << stra.textBox->pos << uint8_t(stra.textBox->align);
    } else {
        auto& curv = src.curved();
        file << uint32_t(curv.path.size());
        tempV2.assign(curv.path.begin(), curv.path.end());
        file.writeAll(tempV2);
    }
}

void load(io::IStream& file, PlacedLabel2& dst)
{
    file >> dst.bbox >> dst.textMain >> dst.textAlt >> dst.priority;
    uint8_t type; file >> type;
    if (0 == type) {
        auto& stra = dst.initStraight();
        file >> stra.posCenter >> stra.hasIcon;
        bool hasTBox; file >> hasTBox;
        if (hasTBox) {
            stra.textBox = PlacedLabel2::TextBox();
            file >> stra.textBox->pos;
            uint8_t align; file >> align;
            stra.textBox->align = PlacedLabel2::Align(align);
        } else {
            stra.textBox.reset();
        }
    } else {
        auto& curv = dst.initCurved();
        uint32_t size; file >> size;
        tempV2.resize(size);
        file.readArray(tempV2.data(), tempV2.size());
        curv.path.resize(size);
        for (size_t i = 0; i < size; ++i)
            curv.path[i] = Vertex(tempV2[i],
                i ? agg::path_cmd_line_to : agg::path_cmd_move_to);
    }
}

struct LegacyAdapter
{
    virtual ~LegacyAdapter() {}
    virtual bool objectFound(uint64_t id, const BoxD& box) = 0;
    operator depot::Receiver()
    {
        return [this](const depot::Entry& entry)->bool {
            return objectFound(entry.id, entry.box);
        };
    }
};

struct EqualChecker : public LegacyAdapter
{
    io::Reader dump;
    depot::ReaderPtr store;
    size_t total;
    FeatureUPtr ftEtalon;
    double sdist;

    bool objectFound(uint64_t id, const base::BoxD& bbox) override
    {
        ++total;
        store->read({id, bbox}, ft);
        io::IStream is(dump, ft.sourceId());
        load(is, ftEtalon->placedLabel2());
        auto& a = ft.placedLabel2();
        auto& b = ftEtalon->placedLabel2();

        BOOST_CHECK_EQUAL(a.textMain, b.textMain);
        BOOST_CHECK_EQUAL(a.textAlt, b.textAlt);
        BOOST_CHECK_EQUAL(a.priority, b.priority);
        BOOST_CHECK_EQUAL(a.isCurved(), b.isCurved());
        if (a.isCurved() == b.isCurved()) {
            if (a.isStraight()) {
                auto& as = a.straight();
                auto& bs = b.straight();
                BOOST_CHECK_EQUAL(as.hasIcon, bs.hasIcon);
                BOOST_CHECK_LE(normSq(as.posCenter - bs.posCenter), sdist);
                BOOST_CHECK_EQUAL(!!as.textBox, !!bs.textBox);
                if (!!as.textBox == !!bs.textBox && as.textBox) {
                    BOOST_CHECK(as.textBox->align == bs.textBox->align);
                    BOOST_CHECK_LE(normSq(as.textBox->pos - bs.textBox->pos), sdist);
                }
            } else {
                auto& ap = a.curved().path;
                auto& bp = b.curved().path;
                BOOST_CHECK_EQUAL(ap.size(), bp.size());
                for (size_t i = 0; i < ap.size() && i < bp.size(); ++i)
                    BOOST_CHECK_LE(normSq(ap[i] - bp[i]), sdist);
            }
        }

        return false;
    }
};

struct Context
{
    MapEnvironment*const env;

    uint32_t zoom;
    rules::Labeling ruls;
    std::string name;
    std::string keyMain;
    std::string keyAlt;

    size_t dump(ILayerPtr layer)
    {
        size_t total = 0;
        auto sl = layer->cast<ISearchableLayer>();
        BOOST_CHECK(!!sl);

        auto out = io::file::create(name + extDump);
        auto fi = sl->findFeatures(proj::EARTH_BOX,
                                   FeatureCapabilities{CapabilityFeaturePlacedLabel});
        fi->reset();
        while (fi->hasNext()) {
            ++total;
            save(*out, fi->next().placedLabel2());
        }
        out.reset();
        if (0 == total)
            io::file::remove(name + extDump);
        return total;
    }

    size_t check()
    {
        size_t total = 0;
        ConvertLabelBackward convBack(ruls, env->fontLoader);
        io::Reader rdr = io::file::open(name + extDump);

        io::IStream file(rdr);
        PlacedLabel pl;
        while (file.size() != file.offset()) {
            total++;
            auto id = file.offset();
            load(file, ft.placedLabel2());
            convBack(ft.placedLabel2(), pl);
        }
        return total;
    }

    size_t write()
    {
        depot::StorageOptions opt;
        opt.type = FeatureType::Text;
        opt.maxZoom = zoom;
        opt.priority = float(ruls.priority);
        opt.curvedLabels = isCurved(ruls);
        opt.keyMain = base::sha1(base::asByteArrayRef(keyMain));
        opt.keyAlt = base::sha1(base::asByteArrayRef(keyAlt));

        auto index = depot::rtree::createBuilder(zoom);
        auto store = depot::label::createWriter(name + extStore, opt, env->topo);

        size_t total = 0;
        auto reader = io::file::open(name + extDump);
        for (io::IStream fileDump(reader);
             fileDump.size() != fileDump.offset(); ++total) {
            auto id = fileDump.offset();
            load(fileDump, ft.placedLabel2());
            ft.placedLabel2().bbox = index->add(store->nextId(),
                                                ft.placedLabel2().bbox);
            ft.setSourceId(id);
            if (!store->tryAdd(ft, ft.placedLabel2().bbox))
                index->popLast();
        }

        auto keyPath = mapName + ".data/" + mapcompiler::PREFIX_TOPO;
        env->topo.setPath(keyPath);

        index->build(name + extIndex, store.get());

        return total;
    }

    size_t read()
    {
        auto idx = depot::rtree::openSearcher(name + extIndex);
        EqualChecker checker;
        checker.dump     = io::file::open(name + extDump);
        checker.store    = depot::label::openReader(name + extStore, env->topo);
        checker.total    = 0;
        checker.ftEtalon = ft.clone();
        checker.sdist    = 8 * base::math::square(proj::HALF_WORLD / (1u << (zoom + 8)));
        idx->enumerate(checker);
        return checker.total;
    }

    void process(ILayerPtr layer)
    {
        if (auto group = layer->cast<IGroupLayer>()) {
            for (auto child : group->children())
                process(child);
            return;
        }

        auto lsh = layer->cast<ILabelStyleHolder>();
        if (!lsh)
            return;
        auto rsh = layer->cast<IRenderStyleHolder>();
        BOOST_CHECK(!!rsh);
        auto rs = rsh->renderStyle();

        zoom = rs->visibilityScaling().max();
        ruls = rules::parse(*rs, *lsh->labelStyle(), proj::pixelPerUnit(zoom));

        auto sid = std::to_string(layer->id());
        name     = tempPath + sid;
        keyMain  = sid + "main";
        keyAlt   = sid + "alt";

        size_t total;

        clock_t ct = clock();
        total = dump(layer);
        if (!io::exists(name + extDump))
            return;
        total = check();
        total = write();
        total = read();
        ct = clock() - ct;

        return; // COMMENT ME

        uint64_t storeSize = io::file::open(name + extStore).size();
        uint64_t indexSize = io::file::open(name + extIndex).size();
        std::cout << boost::format("%s %2u %10u %8.1f %8.1f %8.1f N%04u %8u %45s\n")
            %(rules::isCurved(ruls) ? "CURV" : "STRA")
            %zoom %total
            %(storeSize * 8.0 / total)
            %(indexSize * 8.0 / total)
            %((storeSize + indexSize) * 8.0 / total)
            %layer->id() %ct
            %base::ws2s(layer->name());
    }
};

} // namespace


BOOST_AUTO_TEST_SUITE(depot_label)

BOOST_FIXTURE_TEST_CASE(main, CleanContext<>)
{
    auto mapGuiSrc = test::map::openMap(mapNameOriginal, false);
    core::OperationProgressPtr progress(test::map::createProgressStub());
    mapcompiler::compile(mapGuiSrc->map(), mapName,
        mapcompiler::Options(), mapGuiSrc->zoomIndexes(), mapGuiSrc->locales(), progress);
    auto mapGui = test::map::openMap(mapName, false, MapOpenFlags::DISABLE_VIRTUAL_ICON_LAYERS);

    Context ctx {&mapGui->map().env()};
    ctx.process(mapGui->rootLayer());
}

//BOOST_AUTO_TEST_CASE(big_map)
void foo() // COMMENT ME
{
    mapName = "C:/Maps/rus.3/map_russia_c994c45f188210bc7c5baa9d92b3d2744c673313f37704347ab4d77e511d5796.xml";
    tempPath = "C:/Maps/rus.3/dump.6/";

    auto mapGui = test::map::openMap(mapName, false, MapOpenFlags::DISABLE_VIRTUAL_ICON_LAYERS);
    std::cout << "READY..."; std::string str; std::cin >> str;
    Context ctx {&mapGui->map().env()};
    ctx.process(mapGui->rootLayer());
}

BOOST_AUTO_TEST_SUITE_END()
