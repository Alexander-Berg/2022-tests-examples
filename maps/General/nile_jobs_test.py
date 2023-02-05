import yt.wrapper as yt

from nile.api.v1 import clusters, local, Record
from maps.poi.personalized_poi.builder.lib.nile_jobs import (
    get_ymapsdf_positions_for_regions,
    calculate_statistics,
)
from maps.poi.personalized_poi.builder.lib.ut.nile_jobs_rows import (
    get_ymapsdf_positions_for_regions_rows,
    calculate_statistics_rows,
)


def _to_records(iterable):
    return [Record.from_dict(d) for d in iterable]


def job_test(job, rows):
    sources = {}
    for source_name, data in rows['input'].items():
        sources[source_name] = local.StreamSource(_to_records(data))
    sinks_storage = {}
    sinks_proxy = {}
    references = {}
    for output in rows['output']:
        sinks_storage[output] = []
        sinks_proxy[output] = local.ListSink(sinks_storage[output])
        references[output] = _to_records(rows['output'][output])

    job.local_run(sources=sources, sinks=sinks_proxy)

    for output in references:
        assert sinks_storage[output] == references[output]


def test_calculate_statistics():
    cluster = clusters.MockCluster()
    job = cluster.job()
    job = calculate_statistics(
        job, today='2020-01-01', ecstatic_daily_table='', statistics_table='')
    job_test(job, calculate_statistics_rows)


def test_get_ymapsdf_positions_for_regions():
    cluster = clusters.MockCluster()
    job = cluster.job()

    regions_tables = {
        region: {
            'ft_source': yt.ypath_join(region, 'ft_source'),
            'ft_center': yt.ypath_join(region, 'ft_center'),
            'node': yt.ypath_join(region, 'node'),
        }
        for region in ('cis1', 'tr')
    }
    job = get_ymapsdf_positions_for_regions(
        job, regions_tables, output_positions='')
    job_test(job, get_ymapsdf_positions_for_regions_rows)
