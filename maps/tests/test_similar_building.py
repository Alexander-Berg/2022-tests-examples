from nile.api.v1 import (
    clusters,
    local,
)

from maps.geoq.hypotheses.flats.lib import similar_building

from data_similar_building import (
    AD_NM,
    ADDR,
    ADDR_NM,
    BLD,
    BLD_ADDR,
    BLD_GEOM,
    ENTRANCE_FLAT_RANGE,
    FT_ADDR,
    FT_NM,
    HYPOTHESES_DATA,
    NODE,
    RD_AD,
    RD_NM,
)
from utils import (
    prepare_source,
    to_records,
)


def test_prepare_similar_building_hypothesis():
    cluster = clusters.MockCluster()
    job = cluster.job()
    similar_building.prepare(job, '')

    hypotheses = []
    job.local_run(
        sources={
            'ad_nm': prepare_source(AD_NM),
            'addr': prepare_source(ADDR),
            'addr_nm': prepare_source(ADDR_NM),
            'bld': prepare_source(BLD),
            'bld_addr': prepare_source(BLD_ADDR),
            'bld_geom': prepare_source(BLD_GEOM),
            'entrance_flat_range': prepare_source(ENTRANCE_FLAT_RANGE),
            'ft_addr': prepare_source(FT_ADDR),
            'ft_nm': prepare_source(FT_NM),
            'node': prepare_source(NODE),
            'rd_ad': prepare_source(RD_AD),
            'rd_nm': prepare_source(RD_NM),
        },
        sinks={
            'output_hypotheses': local.ListSink(hypotheses),
        }
    )

    assert hypotheses == to_records(HYPOTHESES_DATA)
