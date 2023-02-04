from infra.rtc.docker_registry.docker_torrents.clients.rbtorrent_helper import ObsoleteTorrentHelper
from infra.rtc.docker_registry.docker_torrents.clients.skyboned_helper import get_torrent_helper_from_config
from infra.rtc.docker_registry.docker_torrents.tests.util.mock_database import TorrentDatabase
from infra.rtc.docker_registry.docker_torrents.tests.util.mock_blackbox import BlackboxClient
from infra.rtc.docker_registry.docker_torrents.tests.util.mock_registry import RegistryClient
from infra.rtc.docker_registry.docker_torrents.tests.util.mock_config import TorrentsConfig
from infra.rtc.docker_registry.docker_torrents.tests.util.mock_mds import MdsClient
from infra.rtc.docker_registry.docker_torrents.torrent_brewer import TorrentBrewer
import pytest
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import full_digest_mds_map
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import full_digest_rbtorrent_map
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import nonfull_digest_rbtorrent_map
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import xenial_torrented
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import jyggalag_torrented
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import xenial_undertorrented
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import jyggalag_undertorrented
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import full_mds_rbtorrent_map
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import jyggalag_untorrented
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import xenial_untorrented
from infra.rtc.docker_registry.docker_torrents.tests.util.test_data import scope_tag_events
from copy import deepcopy
from time import sleep


@pytest.mark.parametrize("scope,tag,expected", [
    ("ubuntu", "xenial", xenial_torrented),
    ("jyggalag", "latest", jyggalag_torrented)
])
def test_get_filled_manifest(scope, tag, expected):
    brewer = TorrentBrewer(
        TorrentsConfig(),
        RegistryClient(),
        TorrentDatabase(full_digest_rbtorrent_map, dict(), full_digest_mds_map),
        BlackboxClient(),
        MdsClient(dict(), set())
    )
    try:
        assert brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'}) == expected
    finally:
        brewer.kill()


@pytest.mark.parametrize("scope,tag,expected", [
    ("ubuntu", "xenial", xenial_undertorrented),
    ("jyggalag", "latest", jyggalag_undertorrented)
])
def test_get_not_fully_filled_manifest(scope, tag, expected):
    brewer = TorrentBrewer(
        TorrentsConfig(),
        RegistryClient(),
        TorrentDatabase(nonfull_digest_rbtorrent_map, dict(), full_digest_mds_map),
        BlackboxClient(),
        MdsClient(dict(), set())
    )
    try:
        assert brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'}) == expected
    finally:
        brewer.kill()


@pytest.mark.parametrize("scope,tag,expected", [
    ("ubuntu", "xenial", xenial_torrented),
    ("jyggalag", "latest", jyggalag_torrented)
])
def test_autocopy_from_old(scope, tag, expected):
    brewer = TorrentBrewer(
        TorrentsConfig(),
        RegistryClient(),
        TorrentDatabase(nonfull_digest_rbtorrent_map, full_mds_rbtorrent_map, full_digest_mds_map),
        BlackboxClient(),
        MdsClient(dict(), set())
    )
    try:
        assert brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'}) == expected
    finally:
        brewer.kill()


@pytest.mark.parametrize("scope,tag,expected", [
    ("ubuntu", "xenial", xenial_torrented),
    ("jyggalag", "latest", jyggalag_torrented),
    ("secret_jyggalag", "latest", jyggalag_untorrented),
    ("secret_ubuntu", "xenial", xenial_untorrented)
])
def test_brewing(scope, tag, expected):
    brewer = TorrentBrewer(
        TorrentsConfig(),
        RegistryClient(),
        TorrentDatabase(dict(), dict(), full_digest_mds_map),
        BlackboxClient(),
        MdsClient(full_mds_rbtorrent_map, set())
    )
    try:
        brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'})
        sleep(1)
        second = brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'})
        assert second == expected
    finally:
        brewer.kill()


@pytest.mark.parametrize("scope,tag,headers,expected", [
    ("ubuntu", "xenial", {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'}, xenial_torrented),
    ("jyggalag", "latest", {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'}, jyggalag_torrented),
    ("ubuntu", "xenial", {'Authorization': 'GOODTOKEN'}, xenial_undertorrented),
    ("jyggalag", "latest", {'Authorization': 'GOODTOKEN'}, jyggalag_undertorrented)
])
def test_no_ip_header_brew(scope, tag, headers, expected):
    brewer = TorrentBrewer(
        TorrentsConfig(),
        RegistryClient(),
        TorrentDatabase(nonfull_digest_rbtorrent_map, dict(), full_digest_mds_map),
        BlackboxClient(),
        MdsClient(full_mds_rbtorrent_map)
    )
    try:
        request_headers = dict()
        request_headers.update(headers)
        brewer.get_manifest_with_torrents(
            scope,
            tag,
            request_headers)
        sleep(1)
        result = brewer.get_manifest_with_torrents(
            scope,
            tag,
            request_headers)
        assert result == expected
    finally:
        brewer.kill()


@pytest.mark.parametrize("pop_errors", [0, 2])
@pytest.mark.parametrize("scope,tag,expected", [
    ("ubuntu", "xenial", xenial_torrented),
    ("jyggalag", "latest", jyggalag_torrented)
])
def test_brewing_timeouts(scope, tag, expected, pop_errors):
    brewer = TorrentBrewer(
        TorrentsConfig(),
        RegistryClient(),
        TorrentDatabase(dict(), dict(), full_digest_mds_map),
        BlackboxClient(),
        MdsClient(full_mds_rbtorrent_map, set(), 2)
    )
    brewer.database.pop_errors = pop_errors
    try:
        first = brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'})
        sleep(2)
        second = brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'})
        assert second == expected and second != first
    finally:
        brewer.kill()


@pytest.mark.parametrize("scope,tag,expected1,expected2", [
    ("ubuntu", "xenial", xenial_undertorrented, xenial_torrented),
    ("jyggalag", "latest", jyggalag_undertorrented, jyggalag_torrented)
])
def test_brew_manifest_database_write_error(scope, tag, expected1, expected2):
    brewer = TorrentBrewer(
        TorrentsConfig(),
        RegistryClient(),
        TorrentDatabase(nonfull_digest_rbtorrent_map, dict(), full_digest_mds_map, Exception('I am an error')),
        BlackboxClient(),
        MdsClient(full_mds_rbtorrent_map, set())
    )
    try:
        first = brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'})
        sleep(1)
        second = brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'})
        brewer.database.write_error = None
        third = brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'})
        sleep(1)
        fourth = brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'})
        assert first == expected1 and second == expected1 and third == expected1 and fourth == expected2
    finally:
        brewer.kill()


@pytest.mark.parametrize("bad_keys,expected_drop", [
    ({"773746/e4a253b6-9129-4d0f-8c14-90ba6f37d16c"},
     {"0d7725bd7f92ca3ed3651247a8cca49605ba66ff7c7fc78ab15538efb4723976"}),
    ({"224184/1d4d8ed0-fef1-4819-9d50-db10c77e6bb6"},
     {"0b7e0cfe92dc66007af1095a60d7f1bbba48d642d97c2b796deab3edf50e9604"}),
    ({"773746/e4a253b6-9129-4d0f-8c14-90ba6f37d16c",
      "224184/1d4d8ed0-fef1-4819-9d50-db10c77e6bb6"},
     {"0d7725bd7f92ca3ed3651247a8cca49605ba66ff7c7fc78ab15538efb4723976",
      "0b7e0cfe92dc66007af1095a60d7f1bbba48d642d97c2b796deab3edf50e9604"}),
])
@pytest.mark.parametrize("scope,tag", [
    ("ubuntu", "xenial"),
    ("jyggalag", "latest")
])
def test_brewing_drop_on_error(scope, tag, bad_keys, expected_drop):
    brewer = TorrentBrewer(
        TorrentsConfig(),
        RegistryClient(),
        TorrentDatabase(dict(), dict(), full_digest_mds_map),
        BlackboxClient(),
        MdsClient(full_mds_rbtorrent_map, bad_keys)
    )
    try:
        brewer.get_manifest_with_torrents(
            scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'})
        sleep(1)
        assert brewer.database.dropped == expected_drop
    finally:
        brewer.kill()


@pytest.mark.parametrize('secret', [True, False])
@pytest.mark.parametrize('scope,tag,untorrented,torrented', [
    ('ubuntu', 'xenial', xenial_untorrented, xenial_torrented),
    ('jyggalag', 'latest', jyggalag_untorrented, jyggalag_torrented)
])
def test_event_brewing(scope, tag, secret, untorrented, torrented):
    brewer = TorrentBrewer(
        TorrentsConfig(),
        RegistryClient(),
        TorrentDatabase(dict(), dict(), full_digest_mds_map),
        BlackboxClient(),
        MdsClient(full_mds_rbtorrent_map, set())
    )
    try:
        mock_scope = 'secret_{}'.format(scope) if secret else scope
        for event in scope_tag_events['{}:{}'.format(scope, tag)]:
            mock_event = deepcopy(event)
            mock_event['target']['repository'] = mock_scope
            brewer.process_event(mock_event)
        sleep(1)
        second = brewer.get_manifest_with_torrents(
            mock_scope,
            tag,
            {'Authorization': 'GOODTOKEN', 'X-Real-Ip': '1.1.1.1'})
        if secret:
            assert second == untorrented
        else:
            assert second == torrented
    finally:
        brewer.kill()


def test_get_helper_from_config():
    obsolete_helper_config = {'kind': 'obsolete', 'params': {}}
    other_helper_config = {'kind': 'other', 'params': {}}
    empty_helper_config = {'kind': '', 'params': {}}
    assert isinstance(get_torrent_helper_from_config(obsolete_helper_config, None), ObsoleteTorrentHelper)
    with pytest.raises(KeyError):
        assert isinstance(isinstance(get_torrent_helper_from_config(other_helper_config, None), ObsoleteTorrentHelper), KeyError)
        assert isinstance(isinstance(get_torrent_helper_from_config(empty_helper_config, None), ObsoleteTorrentHelper), KeyError)
