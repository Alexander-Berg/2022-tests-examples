"""Tests synchronization with inventory database."""

import pytest

from infra.walle.server.tests.lib.util import TestCase, AUDIT_LOG_ID, monkeypatch_locks
from walle import authorization
from walle.application import app
from walle.clients import bot
from walle.db_sync import inventory
from walle.hosts import Host, HostState, HostStatus
from walle.models import monkeypatch_timestamp, timestamp

SKIP_STATUSES = [HostStatus.INVALID] + HostStatus.ALL_RENAMING
_UNDEFINED = object()


@pytest.fixture
def test(request):
    return TestCase.create(request)


def monkeypatch_bot(
    mp, current_hosts, host_info=_UNDEFINED, known_hosts=_UNDEFINED, ipmi_macs=_UNDEFINED, iter_hosts_info=_UNDEFINED
):
    inv_name = {host.inv: host.name for host in current_hosts}
    name_inv = {host.name: host.inv for host in current_hosts}

    monkeypatch_locks(mp)
    monkeypatch_timestamp(mp)

    mp.function(bot.get_host_info, return_value=(None if host_info == _UNDEFINED else host_info))
    mp.function(
        bot.get_known_hosts,
        return_value=((inv_name, name_inv, timestamp()) if known_hosts == _UNDEFINED else known_hosts),
    )
    mp.function(
        bot.get_ipmi_macs,
        return_value=({host.inv: host.ipmi_mac for host in current_hosts} if ipmi_macs == _UNDEFINED else ipmi_macs),
    )
    mp.function(
        bot.iter_hosts_info,
        side_effect=(
            lambda: (
                {
                    "inv": host.inv,
                    "macs": host.macs or [],
                }
                for host in current_hosts
            )
        )
        if iter_hosts_info == _UNDEFINED
        else iter_hosts_info,
    )


@pytest.mark.usefixtures("monkeypatch_audit_log")
@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.parametrize("status", sorted(list(set(HostStatus.ALL) - set(SKIP_STATUSES))))
def test_synchronization(test, mp, state, status):
    assert state in HostState.MAC_SYNC_STATES

    current_hosts = [
        Host(inv=0, ipmi_mac="00:00:00:00:00:00", macs=test.macs, name="zero"),
        Host(inv=1, ipmi_mac="00:00:00:00:00:01", macs=test.macs),
        Host(inv=2, ipmi_mac="00:00:00:00:00:02", macs=["01:02:03:04:05:06", "10:11:12:13:14:15"], name="two"),
        Host(inv=3, ipmi_mac="00:00:00:00:00:03", macs=test.macs, name="thirty"),
        Host(inv=4, ipmi_mac="00:00:00:00:00:04", macs=test.macs),
        Host(inv=5, ipmi_mac="00:00:00:00:00:05", macs=test.macs, name="five"),
        Host(inv=6, ipmi_mac="00:00:00:00:00:06", macs=test.macs),
    ]

    for id, ignored_status in enumerate(HostStatus.ALL_RENAMING):
        inv = 100 + id

        host = test.mock_host(dict(inv=inv, name=ignored_status, status=ignored_status, ipmi_mac=test.ipmi_mac))
        if host.status != HostStatus.INVALID:
            del host.ipmi_mac
            del host.macs

        current_hosts.append(Host(inv=inv))

    monkeypatch_bot(mp, current_hosts)

    def mock_host(overrides, drop_name=False):
        host = test.mock_host(dict(state=state, status=status, **overrides))

        # At this time name is a required field and we can't create a host without name
        if drop_name:
            del host.name
            host.update(unset__name=True)

        return host

    invalid_hosts = []
    invalid_reasons = {}
    mock_host(dict(inv=0, ipmi_mac="00:00:00:00:00:00", name="zero"))

    host = mock_host(dict(inv=2, name="two"))
    host.ipmi_mac = "00:00:00:00:00:02"
    host.macs = ["01:02:03:04:05:06", "10:11:12:13:14:15"]

    # host have different name in bot (renamed outside of wall-e)
    invalid_hosts.append(mock_host(dict(inv=3, ipmi_mac="00:00:00:00:00:03", name="three")))
    invalid_reasons[3] = "#3 is thirty now (not three)."

    if state != HostState.FREE:
        # host have no name in bot (for whatever weird reason)
        invalid_hosts.append(mock_host(dict(inv=4, ipmi_mac="00:00:00:00:00:04", name="four")))
        invalid_reasons[4] = "There is no host name associated with #4 inventory number."

    if state == HostState.FREE:
        host = mock_host(dict(inv=5, ipmi_mac="00:00:00:00:00:00"), drop_name=True)
        host.ipmi_mac = "00:00:00:00:00:05"

        mock_host(dict(inv=6, ipmi_mac="00:00:00:00:00:06"), drop_name=True)

    assert inventory._Syncer().sync() == 0

    for host in invalid_hosts:
        if host.task:
            continue
        host.set_status(HostStatus.INVALID, authorization.ISSUER_WALLE, AUDIT_LOG_ID, reason=invalid_reasons[host.inv])
        del host.task

    test.hosts.assert_equal()


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_invalid_hosts_limit(test, mp):
    mp.setattr(inventory, "_MAX_INVALID_HOSTS_DEFAULT", 1)

    monkeypatch_bot(
        mp,
        [
            Host(inv=3, ipmi_mac="00:00:00:00:00:03", macs=test.macs, name="thirty"),
            Host(inv=4, ipmi_mac="00:00:00:00:00:04", macs=test.macs, name="forty"),
        ],
    )

    host3 = test.mock_host(dict(inv=3, ipmi_mac="00:00:00:00:00:03", name="three"))
    host4 = test.mock_host(dict(inv=4, ipmi_mac="00:00:00:00:00:04", name="four"))

    # hit _MAX_INVALID_HOSTS limit
    assert inventory._Syncer().sync() == 1
    test.hosts.assert_equal()

    settings = app.settings()
    settings.inventory_invalid_hosts_limit = 2
    settings.save()

    # limit is bumped, we don't hit it _MAX_INVALID_HOSTS limit
    assert inventory._Syncer().sync() == 0

    host3.set_status(
        HostStatus.INVALID, authorization.ISSUER_WALLE, AUDIT_LOG_ID, reason="#3 is thirty now (not three)."
    )
    host4.set_status(HostStatus.INVALID, authorization.ISSUER_WALLE, AUDIT_LOG_ID, reason="#4 is forty now (not four).")

    test.hosts.assert_equal()

    # reload settings. limit bump must be reset, next sync must run with _MAX_INVALID_HOSTS limit.
    settings = app.settings()
    assert not settings.inventory_invalid_hosts_limit


def test_error_reporting(mp, test):
    current_hosts = [test.mock_host(dict(ipmi_mac="00:00:00:00:00:00", macs=test.macs, name="zero"))]

    name_inv = {host.name: host.inv for host in current_hosts}

    class mock_inv_name:
        def __getitem__(self, item):
            raise inventory.BotDatabaseBroken("Error mock")

    mp.function(bot.get_known_hosts, return_value=(mock_inv_name(), name_inv, timestamp()))

    mp.function(bot.get_ipmi_macs, return_value={host.inv: host.ipmi_mac for host in current_hosts})
    mp.function(
        bot.iter_hosts_info,
        side_effect=lambda: (
            {
                "inv": host.inv,
                "macs": host.macs or [],
            }
            for host in current_hosts
        ),
    )

    assert inventory._Syncer().sync() == 1


def test_success_reporting(mp, test):
    current_hosts = [test.mock_host(dict(ipmi_mac="00:00:00:00:00:00", macs=test.macs, name="zero"))]

    inv_name = {host.inv: host.name for host in current_hosts}
    name_inv = {host.name: host.inv for host in current_hosts}

    mp.function(bot.get_known_hosts, return_value=(inv_name, name_inv, timestamp()))
    mp.function(bot.get_ipmi_macs, return_value={host.inv: host.ipmi_mac for host in current_hosts})
    mp.function(
        bot.iter_hosts_info,
        side_effect=lambda: (
            {
                "inv": host.inv,
                "macs": host.macs or [],
            }
            for host in current_hosts
        ),
    )

    assert inventory._Syncer().sync() == 0


@pytest.mark.usefixtures("monkeypatch_audit_log")
def test_updated_macs_hosts_limit(test, mp):
    mp.setattr(inventory, "_MAX_UPDATED_MACS_HOSTS_DEFAULT", 1)
    current_hosts = [
        test.mock_host(
            dict(
                inv=0,
                ipmi_mac="00:00:00:00:00:00",
                macs=["01:02:03:04:05:00", "10:11:12:13:14:00"],
                name="zero",
                active_mac="01:02:03:04:05:00",
            )
        ),
        test.mock_host(
            dict(
                inv=1,
                ipmi_mac="00:00:00:00:00:01",
                macs=["01:02:03:04:05:01", "10:11:12:13:14:01"],
                name="one",
                active_mac="01:02:03:04:05:01",
            )
        ),
    ]
    broken_macs_list = [["01:02:03:04:05:22", "10:11:12:13:14:22"], ["01:02:03:04:05:11", "10:11:12:13:14:11"]]

    iter_hosts = ({"inv": host.inv, "macs": host.macs or []} for host in current_hosts)
    monkeypatch_bot(mp, current_hosts, iter_hosts_info=lambda: iter_hosts)

    assert inventory._Syncer().sync() == 0

    iter_hosts = ({"inv": host.inv, "macs": broken_macs} for host, broken_macs in zip(current_hosts, broken_macs_list))
    assert inventory._Syncer().sync() == 1


def test_sync_invalid_host_inv_and_ipmi_mac_changed(test, mp):
    current_hosts = [
        test.mock_host(dict(inv=0, status=HostStatus.INVALID, ipmi_mac="00:00:00:00:00:10")),
        test.mock_host(dict(inv=1, status=HostStatus.READY, ipmi_mac="00:00:00:00:00:20")),
    ]
    actual_macs = {2: "00:00:00:00:00:30", 3: "00:00:00:00:00:20"}

    monkeypatch_bot(
        mp,
        current_hosts,
        ipmi_macs=actual_macs,
        known_hosts=(
            {host.inv + 2: host.name for host in current_hosts},
            {host.name: host.inv + 2 for host in current_hosts},
            timestamp(),
        ),
    )

    assert inventory._Syncer().sync() == 0
    for host in current_hosts:
        host.inv += 2
        host.ipmi_mac = actual_macs[host.inv]
    test.hosts.assert_equal()


@pytest.mark.parametrize("state", HostState.ALL)
@pytest.mark.usefixtures("monkeypatch_locks", "monkeypatch_timestamp", "monkeypatch_audit_log")
def test_check_invalid_return_to_production(test, mp, state):
    host = test.mock_host({"state": state, "status": HostStatus.INVALID})
    monkeypatch_bot(mp, [host], host_info={"inv": host.inv, "name": host.name, "oebs_status": "OPERATION"})
    assert inventory._Syncer().sync() == 0
    host.set_status(
        HostStatus.default(host.state),
        authorization.ISSUER_WALLE,
        AUDIT_LOG_ID,
        reason="Host has been returned to production",
        confirmed=False,
    )

    test.hosts.assert_equal()


def test_check_invalid_in_production_but_renamed(test, mp):
    host = test.mock_host({"status": HostStatus.INVALID})
    monkeypatch_bot(mp, [host], host_info={"inv": host.inv, "name": "invalid-name", "oebs_status": "OPERATION"})
    assert inventory._Syncer().sync() == 0
    test.hosts.assert_equal()


def test_check_invalid_does_not_exist_in_bot(test, mp):
    host = test.mock_host({"status": HostStatus.INVALID})
    monkeypatch_bot(mp, [host], host_info=None)
    assert inventory._Syncer().sync() == 0
    test.hosts.assert_equal()


@pytest.mark.parametrize("oebs_status", ("DIAGNOSTICS", "NOT_USED"))
def test_check_invalid_excepted_status(test, mp, oebs_status):
    host = test.mock_host({"status": HostStatus.INVALID})
    monkeypatch_bot(mp, [host], host_info={"inv": host.inv, "name": host.name, "oebs_status": oebs_status})
    assert inventory._Syncer().sync() == 0
    test.hosts.assert_equal()
