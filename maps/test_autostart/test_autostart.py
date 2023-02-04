from maps.garden.sdk.test_utils.autostart import create_build
from maps.garden.sdk.module_autostart import module_autostart as common

from maps.garden.modules.renderer_denormalization_osm.lib import autostart


def test_start_build():
    geocoder_build = create_build("geocoder_osm_export")

    ymapsdf_build_cis1 = create_build("ymapsdf_osm", properties={"region": "cis1"})
    build_manager = common.BuildManager([ymapsdf_build_cis1, geocoder_build])
    autostart.start_build(ymapsdf_build_cis1, build_manager)
    assert build_manager.build_to_create.source_ids == [
        ymapsdf_build_cis1.full_id,
        geocoder_build.full_id,
    ]

    ymapsdf_build_planet = create_build("ymapsdf_osm", properties={"region": "planet"})
    build_manager = common.BuildManager([ymapsdf_build_planet, geocoder_build])
    autostart.start_build(ymapsdf_build_planet, build_manager)
    assert not build_manager.build_to_create
