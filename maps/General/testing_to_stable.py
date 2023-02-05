from maps.garden.sdk.core import MutagenGraphBuilder
from maps.garden.sdk.resources import FileResource
from maps.garden.sdk.extensions.copy_task import CopyTask
from maps.garden.sdk.extensions.property_propagators import EnsureEqualProperties

from maps.garden.sdk.extensions import easy_builder

from .resource_names import ResourceNames


def move_bundle_to_stable_fill_graph(graph_builder,
                                     testing_bundle_info,
                                     stable_res_suffix):
    testing_rn = ResourceNames(testing_bundle_info.service, testing_bundle_info.res_suffix)
    stable_rn = ResourceNames(testing_bundle_info.service, stable_res_suffix)

    graph_builder = MutagenGraphBuilder(
        graph_builder,
        property_propagator=EnsureEqualProperties(
            ['bundle_revision', 'styleset', testing_bundle_info.service, 'cartograph_url'])
    )
    graph_builder = easy_builder.EasyBuilder(graph_builder)

    graph_builder.task(
        CopyTask()
    ).creates(
        to_resource=FileResource(
            stable_rn.icons_tar(), stable_rn.icons_tar() + '{_hash}.tar')
    ).demands(
        from_resource=testing_rn.icons_tar()
    )

    for s in testing_bundle_info.stylesets:
        graph_builder.task(
            CopyTask()
        ).creates(
            to_resource=FileResource(stable_rn.stylesheet(s),
                                     'stylesheet_{styleset}_{bundle_revision}_' + stable_res_suffix + '.json')
        ).demands(
            from_resource=testing_rn.stylesheet(s)
        )
