"""Tests Bot client."""

import io
import json
from random import shuffle

import pytest
from requests import Response

from infra.walle.server.tests.lib.util import monkeypatch_config, mock_response, load_mock_data
from sepelib.core.exceptions import LogicalError
from walle.clients import bot
from walle.juggler import _Location, _validate_location

_SERIAL_NUMBER = 10**4 + 1
_INSTANCE_NUMBER = 10**3 + 1


@pytest.fixture(autouse=True)
def bot_config(monkeypatch, disable_caches):
    monkeypatch_config(monkeypatch, "bot.host", "bot.yandex-team.ru")


def _mock_bot_resp(mp, mock_file_name):
    response = Response()
    response.raw = io.BytesIO(load_mock_data(mock_file_name, to_str=False))
    mp.function(bot.raw_request, return_value=response)


def _mock_bot_json_resp(monkeypatch, mock_file_name):
    mock_resp = json.loads(load_mock_data(mock_file_name))
    monkeypatch.setattr(bot, "json_request", lambda *args, **kwargs: mock_resp)


def test_get_known_hosts(mp):
    _mock_bot_resp(mp, "mocks/bot_get_known_hosts.txt")
    inv_name, name_inv, timestamp = bot.get_known_hosts()
    assert len(inv_name) and len(name_inv)
    assert len(inv_name) >= len(name_inv)
    assert int(timestamp) > 0


def test_get_host_platform(monkeypatch):
    _mock_bot_json_resp(monkeypatch, "mocks/bot_consist_of_response.json")
    platform = bot._get_host_platform("does.not.matter.y.net")
    assert platform.system is not None


def test_iter_hosts_info(mp):
    _mock_bot_resp(mp, "mocks/bot_get_host_location_info.txt")
    hosts_info = bot.iter_hosts_info()
    assert list(hosts_info)
    for host_info in hosts_info:
        assert host_info["location"].unit.isdigit()
        assert not _validate_location(_Location("mock-queue", hosts_info["location"].rack, "mock-rack"))


def test_get_locations(mp):
    _mock_bot_resp(mp, "mocks/bot_get_locations.txt")
    assert len(list(bot.get_locations()))


def test_get_host_location_info(mp):
    _mock_bot_resp(mp, "mocks/bot_get_host_location_info.txt")
    assert len(list(bot.get_host_location_info()))


def test_get_oebs_projects(monkeypatch):
    _mock_bot_json_resp(monkeypatch, "mocks/bot_get_oebs_projects.json")
    result = bot.get_oebs_projects()
    assert result


def test_get_oebs_projects_tree(monkeypatch):
    _mock_bot_json_resp(monkeypatch, "mocks/bot_get_oebs_projects.json")
    projects = bot.get_oebs_projects()
    result = bot.get_oebs_projects_tree()
    assert result
    assert len(result) < len(projects)

    def check_subprojects_validity(project):
        if "subprojects" not in project:
            return

        for subproject in project["subprojects"]:
            assert subproject["parent_project_id"] == project["project_id"]
            check_subprojects_validity(subproject)

    for project in result:
        assert project["parent_project_id"] is None
        check_subprojects_validity(project)


def test_get_preorder_missing_hosts(monkeypatch):
    _mock_bot_json_resp(monkeypatch, "mocks/bot_missed_preordered_hosts.json")
    result = bot.missed_preordered_hosts()
    assert isinstance(result, dict)  # do not check for non-zero, this may become empty eventually


def test_get_mac_addresses_with_not_empty_data():
    TMP_MAC = "00:00:00:00:00:0"
    test_data = {
        "XXCSI_MACADDRESS1": "{}{}".format(TMP_MAC, 1),
        "XXCSI_MACADDRESS2": "{}{}".format(TMP_MAC, 2),
        "XXCSI_MACADDRESS3": "{}{}".format(TMP_MAC, 3),
        "XXCSI_MACADDRESS4": "{}{}".format(TMP_MAC, 4),
        "XXCSI_MACADDRESS5": "{}{}".format(TMP_MAC, 5),
        "some outside value": "{}{}".format(TMP_MAC, 6),
    }

    result = bot._get_mac_addresses(test_data)

    assert result == {"{}{}".format(TMP_MAC, x) for x in range(1, 5)}


def test_get_mac_addresses_with_empty_data():
    test_data = {}
    result = bot._get_mac_addresses(test_data)
    assert result == set()


def test_get_mac_addresses_from_wrong_mac():
    test_data = {"XXCSI_MACADDRESS1": 'test'}
    with pytest.raises(bot.BotInternalError):
        bot._get_mac_addresses(test_data)


class TestDiskKindConvert:
    @pytest.mark.parametrize(
        "disk_interface, disk_type, expected_disk_kind",
        [
            (bot.BotDiskInterface.SATA, bot.BotDiskType.HDD, bot.DiskKind.HDD),
            (bot.BotDiskInterface.SAS, bot.BotDiskType.HDD, bot.DiskKind.HDD),
            (bot.BotDiskInterface.SATA, bot.BotDiskType.SSD, bot.DiskKind.SSD),
            (bot.BotDiskInterface.U2, bot.BotDiskType.SSD, bot.DiskKind.NVME),
        ],
    )
    def test_convert_from_bot(self, disk_interface, disk_type, expected_disk_kind):
        assert bot.DiskKind.convert_from_bot(disk_interface, disk_type) == expected_disk_kind

    @pytest.mark.parametrize(
        "disk_interface, disk_type",
        [
            (bot.BotDiskInterface.SAS, bot.BotDiskType.SSD),
            (bot.BotDiskInterface.U2, bot.BotDiskType.HDD),
            ("blabus", "urlulur"),
        ],
    )
    def test_get_disk_type_raises_on_unknown_combinations(self, disk_interface, disk_type):
        with pytest.raises(LogicalError):
            bot.DiskKind.convert_from_bot(disk_interface, disk_type)


def _gen_components(num_hdd, num_ssd, num_nvme, num_hdd_sata=0, num_gimmick=0):
    def gen_component(comp_type, disk_iface, disk_type, capacity_gb):
        global _SERIAL_NUMBER, _INSTANCE_NUMBER
        _SERIAL_NUMBER += 1
        _INSTANCE_NUMBER += 1

        return {
            "instance_number": str(_INSTANCE_NUMBER),
            "XXCSI_SERIAL_NUMBER": "SN{}".format(_SERIAL_NUMBER),
            "item_segment3": comp_type,
            "attribute14": capacity_gb,
            "attribute15": disk_iface,
            "attribute16": disk_type,
        }

    def gen_hdd():
        return gen_component("DISKDRIVES", "SAS", "HDD", "2000")

    def gen_hdd_sata():
        return gen_component("DISKDRIVES", "SATA", "HDD", "2001")

    def gen_ssd():
        return gen_component("DISKDRIVES", "SATA", "SSD", "500")

    def gen_nvme():
        return gen_component("DISKDRIVES", "U.2", "SSD", "3000")

    def gen_not_disk():
        return gen_component("GIMMICK", "USB", "FLOPPY3.5", "NOTACAPACITY")

    comp_gen_seq = (
        [gen_hdd] * num_hdd
        + [gen_hdd_sata] * num_hdd_sata
        + [gen_ssd] * num_ssd
        + [gen_nvme] * num_nvme
        + [gen_not_disk] * num_gimmick
    )
    shuffle(comp_gen_seq)
    return [fn() for fn in comp_gen_seq]


def _mock_bot_disk_resp(mp, num_hdd, num_ssd, num_nvme, num_gimmick=0):
    resp = {
        "res": 1,
        "data": {
            "Components": _gen_components(num_hdd, num_ssd, num_nvme, num_hdd_sata=num_hdd, num_gimmick=num_gimmick)
        },
    }
    mp.request(mock_response(resp))


@pytest.mark.parametrize("num_hdd", list(range(5)))
@pytest.mark.parametrize("num_ssd", list(range(5)))
@pytest.mark.parametrize("num_nvme", list(range(5)))
def test_get_host_disk_configuration(mp, num_hdd, num_ssd, num_nvme):
    def assert_spec_is_fine(disks):
        for disk in disks:
            assert disk.instance_number > 0
            assert disk.serial_number
            assert disk.capacity_gb > 0

    _mock_bot_disk_resp(mp, num_hdd, num_ssd, num_nvme)
    disk_conf = bot.get_host_disk_configuration(111111)

    assert len(disk_conf.hdds) == 2 * num_hdd
    if num_hdd > 0:
        assert_spec_is_fine(disk_conf.hdds)

    assert len(disk_conf.ssds) == num_ssd
    if num_ssd > 0:
        assert_spec_is_fine(disk_conf.ssds)

    assert len(disk_conf.nvmes) == num_nvme
    if num_nvme > 0:
        assert_spec_is_fine(disk_conf.nvmes)


def _mock_bot_disk_resp_with_node_storage(mp):
    global _INSTANCE_NUMBER
    _INSTANCE_NUMBER += 1
    resp = {
        "res": 1,
        "data": {
            "Components": _gen_components(1, 0, 2, 1, 5),
            "Shared": _gen_components(1, 0, 0),
            "Connected": [
                {
                    "instance_number": str(_INSTANCE_NUMBER + 1),
                    "item_segment3": "NODE-STORAGE",
                    "Components": _gen_components(1, 0, 0),
                },
                {
                    "instance_number": str(_INSTANCE_NUMBER + 2),
                    "item_segment3": "SOMETHING",
                    "Components": _gen_components(0, 2, 0),
                },
            ],
        },
    }
    _INSTANCE_NUMBER += 2
    mp.request(mock_response(resp))


def test_get_host_disk_configuration_with_node_storage(mp):
    _mock_bot_disk_resp_with_node_storage(mp)
    disk_conf = bot.get_host_disk_configuration(111111)

    assert len(disk_conf.hdds) == 4
    assert not disk_conf.hdds[0].from_node_storage
    assert not disk_conf.hdds[1].from_node_storage
    assert disk_conf.hdds[2].from_node_storage
    assert disk_conf.hdds[3].from_node_storage
    assert len(disk_conf.ssds) == 0
    assert len(disk_conf.nvmes) == 2
    assert not disk_conf.nvmes[0].from_node_storage
    assert not disk_conf.nvmes[1].from_node_storage


def mock_projects_to_planner_id(mp, bpi_to_planner_id):
    mp.function(
        bot.get_oebs_projects,
        return_value={bpi: {"planner_id": planner_id} for bpi, planner_id in bpi_to_planner_id.items()},
    )


def test_get_bot_project_id_by_planner_id(mp):
    mock_projects_to_planner_id(mp, {1111: 222222})

    assert bot.get_bot_project_id_by_planner_id(222222) == 1111

    with pytest.raises(bot.PlannerIdNotFound):
        bot.get_bot_project_id_by_planner_id(100500)


def _mock_bot_disk_resp_with_different_node_storage(mp):
    resp = {
        "res": 1,
        "data": {
            "Connected": [
                {
                    "instance_number": "111",
                    "item_segment3": "NODE-STORAGE",
                },
                {
                    "instance_number": "222",
                    "item_segment3": "STORAGES",
                },
                {
                    "instance_number": "333",
                    "item_segment3": "SOMETHING",
                },
                {
                    "instance_number": "444",
                    "item_segment3": "NODE-STORAGE",
                },
            ]
        },
    }
    mp.request(mock_response(resp))


def test_get_storages(mp):
    _mock_bot_disk_resp_with_different_node_storage(mp)
    storages = bot.get_storages(123)

    assert storages == [
        bot.StorageInfo(inv="111", type="NODE-STORAGE"),
        bot.StorageInfo(inv="222", type="STORAGES"),
        bot.StorageInfo(inv="444", type="NODE-STORAGE"),
    ]
