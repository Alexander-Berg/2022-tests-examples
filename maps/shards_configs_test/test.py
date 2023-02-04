from maps.analyzer.services.jams_analyzer.modules.dispatcher.tests.shards_configs_test.lib.common import (
    get_dict_from_xml,
    check_targets,
    Staging,
)

STABLE = 'outputbuilder-hosts.conf.stable'
TESTING = 'outputbuilder-hosts.conf.testing'


def test():
    check(STABLE, Staging.STABLE.shards, Staging.STABLE.pods_in_shard)
    check(TESTING, Staging.TESTING.shards, Staging.TESTING.pods_in_shard)


def check(xml_file, shards, pods_in_shard):
    d = get_dict_from_xml(xml_file)
    check_targets(d['outputbuilder-hosts']['targets']['target'], shards, pods_in_shard, 0, 0)  # 0 pods in osm ob, as osm has no ob
