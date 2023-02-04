from maps.garden.libs.mapcompiler.mapcompiler import Style2MapCompiler, UploadParams
from maps.garden.sdk import ecstatic
from maps.garden.modules.renderer_dem_source.lib import resource_names as terrain_rn
from maps.garden.modules.renderer_denormalization_dump.defs import resource_names as dump_rn
from maps.garden.modules.renderer_design_testing.defs import resource_names as rn
from maps.garden.modules.renderer_publication.lib.locales import LOCALES
from maps.garden.modules.stylerepo_map_design_src.lib import bundle_info as map_bi
from maps.garden.modules.stylerepo_navi_design_src.lib import bundle_info as navi_bi


def fill_graph(graph_builder, regions):
    bundles = [map_bi.get_map_design_bundle_info(), navi_bi.get_navi_design_bundle_info()]
    upload_params = UploadParams(
        dataset_name='yandex-maps-renderer-compiled-designtesting',
        dataset_res_name=rn.COMPILED_MAP_ECSTATIC_DATASET,
        branch=ecstatic.STABLE_BRANCH,
        hold=True
    )
    compiler = Style2MapCompiler(module_name='renderer_design_testing',
                                 bundle_info_list=bundles,
                                 store_maps_in_garden=False,
                                 upload_params=upload_params,
                                 locales=LOCALES)

    compiler.run_from_dump(graph_builder, dump_rn.DUMP_FILE, dump_rn.SCHEMA_FILE, dump_rn.INDOOR_FILE, terrain_rn.src_dem_resource())
