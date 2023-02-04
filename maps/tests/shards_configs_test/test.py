from maps.analyzer.services.jams_analyzer.modules.dispatcher.tests.shards_configs_test.lib.common import (
    get_dict_from_xml,
    check_targets,
    Staging,
)

STABLE = 'dispatcher-hosts.conf.stable'
TESTING = 'dispatcher-hosts.conf.testing'


def test():
    # check(STABLE, Staging.STABLE.shards, Staging.STABLE.pods_in_shard, Staging.STABLE.osm_shards, Staging.STABLE.osm_pods_in_shard)
    check(TESTING, Staging.TESTING.shards, Staging.TESTING.pods_in_shard, Staging.TESTING.osm_shards, Staging.TESTING.osm_pods_in_shard)


def check(xml_file, shards, pods_in_shard, osm_shards, osm_pods_in_shard):
    def is_mt_class(c):
        return c == 'masstransit'

    d = get_dict_from_xml(xml_file)
    for p in d['dispatcher-hosts']['parsers']['parser']:
        target = p['target']
        if isinstance(target, list) and not is_mt_class(target[0].get('@class')):
            check_targets(target, shards, pods_in_shard, osm_shards, osm_pods_in_shard, allowed_classes=['alz'])
