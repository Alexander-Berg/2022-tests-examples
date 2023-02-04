from ads.quality.adv_machine.solo.registry.alert import get_all_alerts
from ads.quality.adv_machine.solo.registry.channel import get_all_channels
from ads.quality.adv_machine.solo.registry.cluster import get_all_clusters
from ads.quality.adv_machine.solo.registry.dashboard import get_all_dashboards
from ads.quality.adv_machine.solo.registry.graph import get_all_graphs
from ads.quality.adv_machine.solo.registry.menu import get_all_menus
from ads.quality.adv_machine.solo.registry.service import get_all_services
from ads.quality.adv_machine.solo.registry.shard import get_all_shards
from ads.quality.adv_machine.solo.tests.common import test_duplicate_ids


def test_alert_duplicate_ids():
    test_duplicate_ids('alert', get_all_alerts())


def test_channel_duplicate_ids():
    test_duplicate_ids('channel', get_all_channels())


def test_cluster_duplicate_ids():
    test_duplicate_ids('cluster', get_all_clusters())


def test_dashboard_duplicate_ids():
    test_duplicate_ids('dashboard', get_all_dashboards())


def test_graph_duplicate_ids():
    test_duplicate_ids('graph', get_all_graphs())


def test_menu_duplicate_ids():
    test_duplicate_ids('menu', get_all_menus())


def test_service_duplicate_ids():
    test_duplicate_ids('service', get_all_services())


def test_shard_duplicate_ids():
    test_duplicate_ids('shard', get_all_shards())
