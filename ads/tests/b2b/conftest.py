import pytest

from ads.bsyeti.big_rt.py_test_lib import make_namedtuple
from ads.bsyeti.caesar.tests.lib.b2b import helpers, schema, utils


@pytest.fixture()
def stand(request, config_test_default_enabled, port_manager, yt_cluster):
    shards_count = 1
    yt_client = yt_cluster.primary_cluster.get_yt_client()
    yt_client.set("//sys/accounts/tmp/@resource_limits/tablet_count", 100500)
    queue_names = []
    queues = {}
    source2queue = {}

    for key, source in utils.get_sources().items():
        qname = source["ResharderInstance"]["Suppliers"][0]["YtSupplier"]["QueuePath"]
        queue_names.append(qname)
        source2queue[key] = qname
    caesar_workers = []

    for dest in utils.get_destinations():
        if "Writer" in dest:
            name = utils.get_profile_type(dest["DestinationName"])
            worker = utils.make_caesar_worker(name, dest)
            caesar_workers.append(worker)
            queue_names.append(worker.input_queue)

    helpers.create_profile_tables(yt_cluster, caesar_workers)

    queues = {}
    with utils.threadpool() as pool:
        for qname in queue_names:
            queues[qname] = pool.submit(utils.make_queue, yt_client, qname, shards_count)

    for qname in queue_names:
        queues[qname] = queues[qname].result()

    for path, attributes in schema.TABLES.items():
        yt_client.create("table", path, attributes=attributes)
        yt_client.mount_table(path)
    return make_namedtuple(
        "Stand",
        yt_cluster=yt_cluster,
        yt_client=yt_client,
        port_manager=port_manager,
        source2queue=source2queue,
        queues=queues,
        caesar_workers=caesar_workers,
        now=utils.get_test_now_dt(),
        resharder_port=port_manager.get_port(),
        caesar_port=port_manager.get_port(),
    )
