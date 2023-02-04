#include "tests/boost-tests/include/tools/compilation_tools.h"

#include "tests/boost-tests/include/tools/map_tools.h"

#include "mapcompiler/layers_compiler.h"
#include "mapcompiler/resource_existence_checker.h"
#include "mapcompiler/style_patcher.h"
#include "mapcompiler/tar_creator.h"
#include "mapcompiler/tools.h"

#include <yandex/maps/renderer5/core/ContextProvider.h>
#include <yandex/maps/renderer5/core/LayerProperties.h>
#include <maps/renderer/libs/base/include/logger.h>
#include <yandex/maps/renderer5/core/MapLoader.h>
#include <yandex/maps/renderer5/core/MapPathResolverFactory.h>
#include <yandex/maps/renderer5/core/MapTools.h>
#include <yandex/maps/renderer5/postgres/DefaultPostgresTransactionProvider.h>
#include <yandex/maps/renderer5/postgres/PostgresServices.h>
#include <yandex/maps/renderer5/mapcompiler/find_duplicated_locales.h>

namespace maps { namespace renderer5 { namespace test {

using namespace renderer;

void checkResources(core::Map& map, bool forNavi)
{
    mapcompiler::CheckerOptions options(core::MapMode::Dynamic, forNavi);

    mapcompiler::ResourceExistenceChecker checker(
        map.imageLoader(),
        options);

    checker.processLayer(*map.rootLayer());
    if (checker.valid())
        return;

    std::string message = "Can not compile map: some resource files are ";
    message += "missing. See details:\n";

    for (const auto& em : checker.errorMessages())
        message += em + "\n";

    REN_THROW() << message;
}

void normalizeSourceFileNames(core::Map& map)
{
    auto layers = map.rootGroupLayer()->getChildrenPtrRecursive();
    for (const auto& layer : layers)
        if (auto ptr = layer->cast<core::ILayerSourceFileName>())
            ptr->setSourceFileName(
                map.pathResolver().resolveMapPath(ptr->sourceFileName()));
}

void prepareSrcMap(core::Map& src, CompilationSetting& cmp)
{
    using namespace maps::renderer5::postgres;

    core::MapLoader().loadMap(src, cmp.srcFileName, true);

    postgres::PostgresTransactionProviderPtr postgres(
        new postgres::DefaultPostgresTransactionProvider(cmp.dboptions));

    normalizeSourceFileNames(src);

    src.setPostgresTransactionProvider(postgres);

    setFetchMode(src, true);

    boost::shared_ptr<core::Map> tmp(&src, [](void*){});
    src.setContextProvider(core::createContextProvider(
            core::ContextProviderTypes::ThreadLocal));

    if (!src.env().contextProvider)
        src.setContextProvider(core::createContextProvider());

    disableValidation(tmp);

    if (!cmp.repositoryPath.empty()) {
        boost::shared_ptr<core::MapPathResolverFactory>
            factory(new core::MapPathResolverFactory);
        factory->setRepositoryPath(cmp.repositoryPath);
        src.setMapPathResolverFactory(factory);
    }

    src.open();
    src.validate();

    try {
        src.checkCanModify();
        src.checkOpenedState();
        core::maptools::checkValidState(src, SOURCE_LOCATION);
        checkResources(src, cmp.forNavi);
    } catch (const std::exception& ex) {
        REN_THROW() << "Cann't compile map:\n" << ex.what() << "\n";
    }
}

void prepareDstMap(core::Map& dst, CompilationSetting& cmp, core::Map& src)
{
    auto factory = test::map::createPathResolverFactory(cmp.dstFileName);
    auto staticPathResolver = factory->create(
        cmp.dstFileName,
        core::MapMode::Static,
        true);
    dst.setMapPathResolverFactory(factory);
    dst.setContextProvider(src.env().contextProvider);

    std::set<core::ZoomType> dstZooms;
    for (auto zoom = cmp.zmin; zoom <= cmp.zmax; zoom++)
        if (src.zoomIndexes().count((core::ZoomType) zoom))
            dstZooms.insert(zoom);

    dst.env().zoomIndexes.init(dstZooms);
    dst.env().topo.setPath(mapcompiler::getTopoPathWrite(*dst.env().pathResolver));
    dst.setLocales(src.locales());
    dst.setBackgroundColor(src.backgroundColor());
}

mapcompiler::Options prepareOptions(CompilationSetting& cmp)
{
    using namespace maps::renderer5::mapcompiler;

    Options res(cmp.forNavi ? Options::ForNavigator : Options::Default);

    res.useFilePacking = true;
    res.useRequestGroups = cmp.useRG;
    res.updateExtent = cmp.updateExtent;
    res.isolateTransaction = cmp.isolateTransaction;

    return res;
}

void prepareSetting(
    mapcompiler::CompilationSetting& setting,
    const mapcompiler::Options& options,
    const base::BoxD& window,
    core::Map& src,
    core::Map& dst)
{
    using namespace maps::renderer5::mapcompiler;

    setting.setSrcEnv(src.env());
    setting.setDstEnv(dst.env());
    setting.setLocales(findDuplicatedLocales(dst.locales(), src));
    setting.options = options;
    setting.window = window;
    setting.servicesProvider = &src;
}

mapcompiler::Id2Checksum readChecksum(const std::string &fileName)
{
    using namespace std;

    mapcompiler::Id2Checksum result;
    std::string prefix = "tests/boost-tests/data/";
    std::string fullName = prefix + fileName;

    FILE * in = fopen(fullName.c_str(), "rt");

    if (!in)
        REN_THROW() << "Cann't read checksum from " << fullName;

    int key;
    int value;

    while (fscanf(in, "%d : %d\n", &key, &value) == 2)
        result[key] = value;

    fclose(in);
    return result;
}

std::string writeChecksum(const mapcompiler::Id2Checksum& sum, const std::string &name)
{
    using namespace std;

    std::string prefix = "tmp/";
    std::string fullName = prefix + name;

    FILE * out = fopen(fullName.c_str(), "wt");

    if (!out)
        REN_THROW() << "Cannot write checksum to " << fullName;

    for (auto& it : sum)
        fprintf(out, "%d : %d\n", it.first, static_cast<int>(it.second));

    fclose(out);
    return fullName;
}

CompilationResult compile(CompilationSetting& cmp)
{
    CompilationResult result;

    core::Map src(core::MapMode::Dynamic);
    prepareSrcMap(src, cmp);

    src.setContextProvider(core::createContextProvider());

    core::Map dst(core::MapMode::Static);
    prepareDstMap(dst, cmp, src);

    auto options = prepareOptions(cmp);

    mapcompiler::CompilationSetting setting;
    prepareSetting(setting, options, cmp.window, src, dst);

    base::ILoggerPtr consoleLogger(new base::ConsoleLogger());

    mapcompiler::LayersCompiler compiler(setting);
    result.rootLayer = compiler.compile(*src.rootLayer(),
        test::map::createProgressStub());

    if (cmp.packFiles) {
        result.rootLayer->close();

        mapcompiler::ResourceSaverForCompile saver(
            *src.env().pathResolver,
            *dst.env().pathResolver);
        mapcompiler::StylePatcher stylePatcher(&saver);

        stylePatcher.processLayer(*result.rootLayer);

        const auto& pathResolver = *dst.env().pathResolver;
        std::string dataPath = pathResolver.resolveDataPath("");

        auto patternsDest = cmp.forNavi
            ? mapcompiler::TarCreator::PatternsDestination::TarAndDir
            : mapcompiler::TarCreator::PatternsDestination::TarOnly;

        mapcompiler::TarCreator packer(pathResolver,
            dataPath + ".tar", patternsDest);
        packer.processGroupLayer(*result.rootLayer);
        packer.finalize();

        result.rootLayer->open();
    }

    result.checksum = setting.id2sum;

    return result;
}


} } } // namespace maps::renderer5::test
