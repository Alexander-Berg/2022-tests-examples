from maps.garden.sdk.core import TaskGraphBuilder
from maps.garden.libs.basemap_icons.lib.graph import fill_render_icons_graph
from maps.garden.libs.stylerepo.bundle_info import BundleInfo, TESTING_STAGE


def test_fill_graph():
    builder = TaskGraphBuilder()
    fill_render_icons_graph(builder, BundleInfo('test_service', ['test1', 'test2'], True, TESTING_STAGE), 'rendered_icons')
