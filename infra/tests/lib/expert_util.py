from walle.expert.rack_topology import RackTopology, normalize_hostname


MOCKED_AGGREGATE = "aggregate"
MOCKED_RACK_MODEL = "rack-01"
MOCKED_UNIT_RANGES = [(1, 21), (28, 48)]
MOCK_SYSTEM = "system"
MOCK_QUEUE = "queue"
MOCK_RACK = "rack"


def get_hostname_by_location(short_queue_name, rack, unit):
    return "host-{}-{}-{}.net".format(short_queue_name, rack, unit)


def mock_rack_hosts(
    walle_test, unit_ranges=None, system=MOCK_SYSTEM, queue=MOCK_QUEUE, rack=MOCK_RACK, start_inv_from=0, save=False
):
    if unit_ranges is None:
        unit_ranges = MOCKED_UNIT_RANGES
    hosts = []
    for unit_range in unit_ranges:
        for i in range(unit_range[0], unit_range[1] + 1):
            hostname = get_hostname_by_location(queue, rack, i)
            inv = start_inv_from
            start_inv_from += 1
            hosts.append(
                walle_test.mock_host(
                    overrides={
                        "inv": inv,
                        "name": hostname,
                        "platform": {"system": system},
                        "location": {"unit": str(i), "short_queue_name": queue, "rack": rack},
                    },
                    save=save,
                )
            )
    return hosts


def mock_rack_topology(
    unit_ranges=None, aggregate=MOCKED_AGGREGATE, rack_model=MOCKED_RACK_MODEL, queue=MOCK_QUEUE, rack=MOCK_RACK
):
    if unit_ranges is None:
        unit_ranges = MOCKED_UNIT_RANGES
    ranges = {}
    index = 0
    for unit_range in unit_ranges:
        for i in range(unit_range[0], unit_range[1] + 1):
            ranges[normalize_hostname(get_hostname_by_location(queue, rack, i))] = index
        index += 1
    return RackTopology(
        **{"aggregate": aggregate, "rack_model": rack_model, "hosts_ranges": ranges, "total_ranges": index}
    )
