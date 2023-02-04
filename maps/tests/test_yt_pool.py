import pytest
from maps.garden.libs_server.task.yt_pool import get_yt_pool


@pytest.mark.parametrize(
    "contour, module, pool", [
        ["stable", "offline_driving_cache", "garden_offline_cache"],
        ["datatesting", "offline_fonts_cache", "garden_offline_cache"],
        ["any_contour", "offline_search_cache", "garden_offline_cache"],
        ["any_contour", "offline_tile_cache", "garden_offline_cache"],
        ["stable", "any_module", "garden_stable"],
        ["datatesting", "any_module", "garden_datatesting"],
        ["study", "any_module", "garden_development"],
        ["testing", "any_module", "garden_development"],
        ["vasia_pupkin_contour", "any_module", "garden_development"],
    ]
)
def test_get_yt_pool(contour: str, module: str, pool: str):
    assert get_yt_pool(contour, module) == pool
