from nose.tools import assert_equal, assert_raises
from yatest.common import source_path
import maps.analyzer.services.jams_analyzer.tools.jams_uploader.lib.common as common


def get_hosts_config():
    return source_path("maps/analyzer/services/jams_analyzer/tools/jams_uploader/tests/data/usershandler-hosts.conf")


def get_hosts_with_aliases_config():
    return source_path("maps/analyzer/services/jams_analyzer/tools/jams_uploader/tests/data/usershandler-hosts-alias.conf")


def test_host_group_index_and_count_for_first_group_first_host():
    assert_equal(
        (0, 3, ["alz02h.tst.maps.yandex.ru"]),
        common.get_host_group_index_and_count(
            get_hosts_config(),
            "alz01h.tst.maps.yandex.ru"))


def test_host_group_index_and_count_with_aliases():
    assert_equal(
        (0, 1, ["alz01h.tst.maps.yandex.ru"]),
        common.get_host_group_index_and_count(
            get_hosts_with_aliases_config(),
            "aliased.alz2.yandex.net"))


def test_host_group_index_and_count_for_first_group_last_host():
    assert_equal(
        (0, 3, ["alz01h.tst.maps.yandex.ru"]),
        common.get_host_group_index_and_count(
            get_hosts_config(),
            "alz02h.tst.maps.yandex.ru"))


def test_host_group_index_and_count_for_last_group_host():
    assert_equal(
        (2, 3, []),
        common.get_host_group_index_and_count(
            get_hosts_config(),
            "alz05h.tst.maps.yandex.ru"))


def test_host_group_index_and_count_for_unknown_host_raises():
    assert_raises(
        RuntimeError,
        common.get_host_group_index_and_count,
        get_hosts_config(),
        "alz06h.tst.maps.yandex.ru")


def test_usershandler_tests_config():
    path = source_path('maps/analyzer/services/jams_analyzer/modules/usershandler/tests/data/usershandler-hosts.conf.default')
    common.get_host_group_index_and_count(path, 'mira.maps.yandex.ru')
