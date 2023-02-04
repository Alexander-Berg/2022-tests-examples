import json

import attr
import pytest

from walle.clients import bot, deploy
from walle.util import deploy_config

ORIG_CONFIG_NAME = "config-mock"
HDD_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.HDD, instance_number=10001, serial_number="HDDSN1", capacity_gb=8000, from_node_storage=False
)
SSD_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.SSD, instance_number=10002, serial_number="SSDSN2", capacity_gb=1600, from_node_storage=False
)
NVME_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.NVME, instance_number=10003, serial_number="NVMESN3", capacity_gb=3200, from_node_storage=False
)
SMALL_SSD_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.SSD, instance_number=10004, serial_number="SSDSN4", capacity_gb=600, from_node_storage=False
)
SMALL_SSD2_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.SSD, instance_number=10005, serial_number="SSDSN5", capacity_gb=600, from_node_storage=False
)
HDD2_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.HDD, instance_number=10006, serial_number="HDDSN6", capacity_gb=8000, from_node_storage=False
)
NODE_STORAGE_HDD_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.HDD,
    instance_number=10007,
    serial_number="NODESTORAGEHDDSN7",
    capacity_gb=8000,
    from_node_storage=True,
)
SMALL_HDD_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.HDD, instance_number=10008, serial_number="HDDSN8", capacity_gb=1200, from_node_storage=False
)
SMALL_SSD3_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.SSD, instance_number=10009, serial_number="SSDSN9", capacity_gb=600, from_node_storage=False
)
NVME2_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.NVME, instance_number=10010, serial_number="NVMESN10", capacity_gb=3200, from_node_storage=False
)
HDD3_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.HDD, instance_number=100011, serial_number="HDDSN11", capacity_gb=8000, from_node_storage=False
)
HDD4_DISK_INFO = bot.DiskInfo(
    kind=bot.DiskKind.HDD, instance_number=100012, serial_number="HDDSN12", capacity_gb=8000, from_node_storage=False
)


def mock_disk_configuration(mp, disk_conf):
    mp.function(bot.get_host_disk_configuration, return_value=disk_conf)


@pytest.fixture()
def host(walle_test):
    return walle_test.mock_host({"inv": 111})


def get_config_content(stage_params):
    config_content = stage_params.get("config_content_json")
    assert config_content
    return json.loads(config_content)


def gen_partition(serial_number, partition=2):
    return "disk_{}_{}".format(serial_number, partition)


def mock_lui_config(mp, content):
    mp.function(deploy.get_deploy_config, return_value=content)


@attr.s
class PartedSpec:
    name = attr.ib(default=None)
    serial_number = attr.ib(default=None)
    partitions = attr.ib(default=None)


@attr.s
class VolumeGroupSpec:
    idx = attr.ib()
    name = attr.ib()
    partitions = attr.ib()
    has_diskman_tag = attr.ib(default=True)


@attr.s
class FsSpec:
    partition = attr.ib()
    mountpoint = attr.ib()
    fs = attr.ib(default="ext4")
    fs_opts = attr.ib(default="-b 4096 -J size=4096")
    mount_options = attr.ib(default="nodiscard,barrier=1,noatime,lazytime,nosuid,nodev")


@attr.s
class YTRotationalFsSpec:
    partition = attr.ib()
    mountpoint = attr.ib()
    fs = attr.ib(default="ext4")
    fs_opts = attr.ib(default="-b 4096 -J size=32768")
    mount_options = attr.ib(default="nodiscard,barrier=1,noatime,lazytime,nosuid,nodev")


@attr.s
class YTNonRotationalFsSpec:
    partition = attr.ib()
    mountpoint = attr.ib()
    fs = attr.ib(default="ext4")
    fs_opts = attr.ib(default="-b 4096 -J size=4096")
    mount_options = attr.ib(default="nodiscard,barrier=1,noatime,lazytime,nosuid,nodev")


@attr.s
class MDSRotationalFsSpec:
    partition = attr.ib()
    mountpoint = attr.ib()
    fs = attr.ib(default="ext4")
    fs_opts = attr.ib(default="-b 4096 -J size=4096 -m 0")
    mount_options = attr.ib(default="nodiscard,noatime,lazytime,nosuid,nodev,errors=remount-ro")


@attr.s
class ConfigSpec:
    parted = attr.ib()
    volume_groups = attr.ib(default=None)
    ssd_partition = attr.ib(default=None)
    ssd_volume_group = attr.ib(default=None)
    place_size = attr.ib(default="*")
    ssd_size = attr.ib(default="*")
    stripes = attr.ib(default=0)
    fs = attr.ib(default=None)


DM_HDD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_hdd_10001", partitions=gen_partition(HDD_DISK_INFO.serial_number)),
    ],
)

DM_HDD_SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_hdd_10001", partitions=gen_partition(HDD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=1, name="vg_ssd_10002", partitions=gen_partition(SSD_DISK_INFO.serial_number)),
    ],
)

DM_HDD_SSD_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_hdd_10001", partitions=gen_partition(HDD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=1, name="vg_ssd_10002", partitions=gen_partition(SSD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=2, name="vg_nvme_10003", partitions=gen_partition(NVME_DISK_INFO.serial_number)),
    ],
)

DM_SSD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_ssd_10002", partitions=gen_partition(SSD_DISK_INFO.serial_number)),
    ],
)

DM_SSD_AND_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_ssd_10002", partitions=gen_partition(SSD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=1, name="vg_nvme_10003", partitions=gen_partition(NVME_DISK_INFO.serial_number)),
    ],
)

DM_NVME_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_nvme_10003", partitions=gen_partition(NVME_DISK_INFO.serial_number)),
    ],
)


class TestDiskManagerConfigStrategy:
    def _create_strategy(self):
        return deploy_config.DiskManagerConfigStrategy()

    @pytest.fixture()
    def lui_config_mock(self, mp):
        mock_lui_config(mp, {})

    def _mock_config_content(self, spec):
        parted = {"disk_{}".format(parted_spec.serial_number): ["grub", "*"] for parted_spec in spec.parted}
        lvm = {
            "lv0": ["root", "40G", "vg0", "--addtag diskman.sys=true"],
            "lv1": ["home", "6G", "vg0", "--addtag diskman.sys=true"],
            "lv2": ["place", "500G", "vg0", "--addtag diskman.sys=true"],
        }
        lvm.update(
            {
                "vg{}".format(volume_group_spec.idx): [
                    volume_group_spec.name,
                    volume_group_spec.partitions,
                    "--addtag diskman=true",
                ]
                for volume_group_spec in spec.volume_groups
            }
        )
        config = {
            "md": {},
            "parted": parted,
            "lvm": lvm,
            "fs": {
                "lv0": ["ext4", "-b 4096", "/", "barrier=1,noatime,lazytime"],
                "lv1": ["ext4", "-b 4096", "/home", "barrier=1,noatime,lazytime,nosuid,nodev"],
                "lv2": ["ext4", "-b 4096", "/place", "barrier=1,noatime,lazytime,nosuid,nodev"],
            },
        }
        assert not spec.ssd_volume_group
        return config

    @pytest.mark.parametrize(
        "disk_conf, expected_disk",
        [
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[], nvmes=[]), DM_HDD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[]), DM_HDD_SSD_CONFIG),
            (
                bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[NVME_DISK_INFO]),
                DM_HDD_SSD_NVME_CONFIG,
            ),
            (bot.HostDiskConf(hdds=[], ssds=[SSD_DISK_INFO], nvmes=[]), DM_SSD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[], ssds=[SSD_DISK_INFO], nvmes=[NVME_DISK_INFO]), DM_SSD_AND_NVME_CONFIG),
            (bot.HostDiskConf(hdds=[], ssds=[], nvmes=[NVME_DISK_INFO]), DM_NVME_ONLY_CONFIG),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_normal_execution(self, mp, host, disk_conf, expected_disk):
        mock_disk_configuration(mp, disk_conf)
        stage_params = self._create_strategy().generate(host, ORIG_CONFIG_NAME)
        assert get_config_content(stage_params) == self._mock_config_content(expected_disk)

    @pytest.mark.parametrize(
        "disk_conf",
        [
            bot.HostDiskConf(hdds=[], ssds=[], nvmes=[]),
            bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO], ssds=[], nvmes=[]),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_no_disk_system_disk_error(self, mp, host, disk_conf):
        mock_disk_configuration(mp, disk_conf)
        with pytest.raises(deploy_config.NoSystemDiskError):
            self._create_strategy().generate(host, ORIG_CONFIG_NAME)

    def test_removes_deny_sections(self, mp, host):
        mock_disk_configuration(mp, bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[], nvmes=[]))
        mock_lui_config(mp, {section: {"bla": "blabla"} for section in ["ipxe", "pxe", "infrapackages", "base"]})
        stage_params = self._create_strategy().generate(host, ORIG_CONFIG_NAME)
        assert get_config_content(stage_params) == self._mock_config_content(DM_HDD_ONLY_CONFIG)

    def test_moves_repos_to_extra_repos(self, mp, host):
        mock_disk_configuration(mp, bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[], nvmes=[]))
        repos = ["such-a-secret-ya-repo"]
        mock_lui_config(mp, {"repositories": repos})

        stage_params = self._create_strategy().generate(host, ORIG_CONFIG_NAME)

        expected_config = self._mock_config_content(DM_HDD_ONLY_CONFIG)
        expected_config.update({"repositories_extras": repos})

        assert get_config_content(stage_params) == expected_config


class TestPassthroughDeployPolicy:
    def test_doesnt_mutate_stage_params(self, host):
        assert deploy_config.PassthroughConfigStrategy().generate(host, ORIG_CONFIG_NAME) == {}


HDD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_hdd_10001", partitions=gen_partition(HDD_DISK_INFO.serial_number)),
    ],
    place_size="*",
)

SMALL_HDD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=SMALL_HDD_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_hdd_10008", partitions=gen_partition(SMALL_HDD_DISK_INFO.serial_number)),
    ],
    place_size="*",
)

HDD_SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_hdd_10001", partitions=gen_partition(HDD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=1, name="vg_ssd_10002", partitions=gen_partition(SSD_DISK_INFO.serial_number)),
    ],
    place_size="*",
)

HDD_SSD_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_hdd_10001", partitions=gen_partition(HDD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=1, name="vg_ssd_10002", partitions=gen_partition(SSD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=2, name="vg_nvme_10003", partitions=gen_partition(NVME_DISK_INFO.serial_number)),
    ],
    place_size="*",
)

SSD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_ssd_10002", partitions=gen_partition(SSD_DISK_INFO.serial_number)),
    ],
    place_size="*",
)

SSD_AND_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_ssd_10002", partitions=gen_partition(SSD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=1, name="vg_nvme_10003", partitions=gen_partition(NVME_DISK_INFO.serial_number)),
    ],
    place_size="*",
)

TWO_SMALL_SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=SMALL_SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SMALL_SSD2_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0,
            name="ssd",
            partitions="{},{}".format(
                gen_partition(SMALL_SSD_DISK_INFO.serial_number), gen_partition(SMALL_SSD2_DISK_INFO.serial_number)
            ),
            has_diskman_tag=False,
        ),
    ],
    stripes=2,
)

TWO_HDD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
        PartedSpec(serial_number=HDD2_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_hdd_10001", partitions=gen_partition(HDD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=1, name="vg_hdd_10006", partitions=gen_partition(HDD2_DISK_INFO.serial_number)),
    ],
    place_size="*",
)

HDD_TWO_SMALL_SSD_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SMALL_SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SMALL_SSD2_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_hdd_10001", partitions=gen_partition(HDD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=1, name="vg_ssd_10004", partitions=gen_partition(SMALL_SSD_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=2, name="vg_ssd_10005", partitions=gen_partition(SMALL_SSD2_DISK_INFO.serial_number)),
        VolumeGroupSpec(idx=3, name="vg_nvme_10003", partitions=gen_partition(NVME_DISK_INFO.serial_number)),
    ],
    place_size="*",
)

NVME_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="vg_nvme_10003", partitions=gen_partition(NVME_DISK_INFO.serial_number)),
    ],
    place_size="*",
)

THREE_SMALL_SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=SMALL_SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SMALL_SSD2_DISK_INFO.serial_number),
        PartedSpec(serial_number=SMALL_SSD3_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0,
            name="ssd",
            partitions="{},{}".format(
                gen_partition(SMALL_SSD_DISK_INFO.serial_number), gen_partition(SMALL_SSD2_DISK_INFO.serial_number)
            ),
            has_diskman_tag=False,
        ),
        VolumeGroupSpec(idx=1, name="vg_ssd_10009", partitions=gen_partition(SMALL_SSD3_DISK_INFO.serial_number)),
    ],
    place_size="*",
    stripes=2,
)


class TestSharedLvmConfigStrategy:
    def _create_strategy(self):
        return deploy_config.SharedLvmConfigStrategy()

    def _mock_config_content_shared_lvm(self, spec):
        parted = {"disk_{}".format(parted_spec.serial_number): ["grub", "*"] for parted_spec in spec.parted}
        options = "--addtag diskman.sys=true"
        if spec.stripes and spec.stripes >= 2:
            options = "{} --stripes {}".format(options, spec.stripes)
        lvm = {
            "lv0": ["root", "40G", "vg0", options],
            "lv1": ["home", "6G", "vg0", options],
            "lv2": ["place", spec.place_size, "vg0", options],
        }
        lvm.update(
            {
                "vg{}".format(volume_group_spec.idx): [
                    volume_group_spec.name,
                    volume_group_spec.partitions,
                    "--addtag diskman=true",
                ]
                for volume_group_spec in spec.volume_groups
                if volume_group_spec.has_diskman_tag
            }
        )
        lvm.update(
            {
                "vg{}".format(volume_group_spec.idx): [
                    volume_group_spec.name,
                    volume_group_spec.partitions,
                ]
                for volume_group_spec in spec.volume_groups
                if not volume_group_spec.has_diskman_tag
            }
        )
        config = {
            "md": {},
            "parted": parted,
            "lvm": lvm,
            "fs": {
                "lv0": ["ext4", "-b 4096", "/", "barrier=1,noatime,lazytime"],
                "lv1": ["ext4", "-b 4096", "/home", "barrier=1,noatime,lazytime,nosuid,nodev"],
                "lv2": ["ext4", "-b 4096", "/place", "barrier=1,noatime,lazytime,nosuid,nodev"],
            },
        }
        if spec.ssd_volume_group:
            config["lvm"]["lv3"] = ["ssd", spec.ssd_size, spec.ssd_volume_group, "--addtag diskman.sys=true"]
            config["fs"]["lv3"] = ["ext4", "-b 4096", "/ssd", "barrier=1,noatime,lazytime,nosuid,nodev"]
        return config

    @pytest.fixture()
    def lui_config_mock(self, mp):
        mock_lui_config(mp, {})

    @pytest.mark.parametrize(
        "disk_conf, expected_config",
        [
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[], nvmes=[]), HDD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[SMALL_HDD_DISK_INFO], ssds=[], nvmes=[]), SMALL_HDD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[]), HDD_SSD_CONFIG),
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[NVME_DISK_INFO]), HDD_SSD_NVME_CONFIG),
            (bot.HostDiskConf(hdds=[], ssds=[SSD_DISK_INFO], nvmes=[]), SSD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[], ssds=[SSD_DISK_INFO], nvmes=[NVME_DISK_INFO]), SSD_AND_NVME_CONFIG),
            (
                bot.HostDiskConf(hdds=[], ssds=[SMALL_SSD_DISK_INFO, SMALL_SSD2_DISK_INFO], nvmes=[]),
                TWO_SMALL_SSD_CONFIG,
            ),
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO, HDD2_DISK_INFO], ssds=[], nvmes=[]), TWO_HDD_CONFIG),
            (
                bot.HostDiskConf(
                    hdds=[HDD_DISK_INFO], ssds=[SMALL_SSD_DISK_INFO, SMALL_SSD2_DISK_INFO], nvmes=[NVME_DISK_INFO]
                ),
                HDD_TWO_SMALL_SSD_NVME_CONFIG,
            ),
            (bot.HostDiskConf(hdds=[], ssds=[], nvmes=[NVME_DISK_INFO]), NVME_ONLY_CONFIG),
            (
                bot.HostDiskConf(
                    hdds=[], ssds=[SMALL_SSD_DISK_INFO, SMALL_SSD2_DISK_INFO, SMALL_SSD3_DISK_INFO], nvmes=[]
                ),
                THREE_SMALL_SSD_CONFIG,
            ),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_normal_execution(self, mp, host, disk_conf, expected_config):
        mock_disk_configuration(mp, disk_conf)
        stage_params = self._create_strategy().generate(host, ORIG_CONFIG_NAME)
        assert get_config_content(stage_params) == self._mock_config_content_shared_lvm(expected_config)

    @pytest.mark.parametrize(
        "disk_conf",
        [
            bot.HostDiskConf(hdds=[], ssds=[], nvmes=[]),
            bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO], ssds=[], nvmes=[]),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_no_disk_system_disk_error(self, mp, host, disk_conf):
        mock_disk_configuration(mp, disk_conf)
        with pytest.raises(deploy_config.NoSystemDiskError):
            self._create_strategy().generate(host, ORIG_CONFIG_NAME)


SHARED_HDD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0, name="hdd", partitions=gen_partition(HDD_DISK_INFO.serial_number), has_diskman_tag=False
        ),
    ],
)

SHARED_HDD_SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0, name="hdd", partitions=gen_partition(HDD_DISK_INFO.serial_number), has_diskman_tag=False
        ),
        VolumeGroupSpec(
            idx=1, name="ssd", partitions=gen_partition(SSD_DISK_INFO.serial_number), has_diskman_tag=False
        ),
    ],
    ssd_volume_group="vg1",
)

SHARED_HDD_SSD_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0, name="hdd", partitions=gen_partition(HDD_DISK_INFO.serial_number), has_diskman_tag=False
        ),
        VolumeGroupSpec(idx=1, name="vg_ssd_10002", partitions=gen_partition(SSD_DISK_INFO.serial_number)),
        VolumeGroupSpec(
            idx=2, name="nvme", partitions=gen_partition(NVME_DISK_INFO.serial_number), has_diskman_tag=False
        ),
    ],
    ssd_volume_group="vg2",
)

SHARED_SSD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0, name="ssd", partitions=gen_partition(SSD_DISK_INFO.serial_number), has_diskman_tag=False
        ),
    ],
)

SHARED_SSD_AND_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0, name="ssd", partitions=gen_partition(SSD_DISK_INFO.serial_number), has_diskman_tag=False
        ),
        VolumeGroupSpec(idx=1, name="vg_nvme_10003", partitions=gen_partition(NVME_DISK_INFO.serial_number)),
    ],
)

SHARED_TWO_SMALL_SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=SMALL_SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SMALL_SSD2_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0,
            name="ssd",
            partitions="{},{}".format(
                gen_partition(SMALL_SSD_DISK_INFO.serial_number), gen_partition(SMALL_SSD2_DISK_INFO.serial_number)
            ),
            has_diskman_tag=False,
        ),
    ],
    stripes=2,
)

SHARED_TWO_HDD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number),
        PartedSpec(serial_number=HDD2_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0,
            name="hdd",
            partitions="{},{}".format(
                gen_partition(HDD_DISK_INFO.serial_number), gen_partition(HDD2_DISK_INFO.serial_number)
            ),
            has_diskman_tag=False,
        ),
    ],
    stripes=2,
)

SHARED_NVME_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0, name="nvme", partitions=gen_partition(NVME_DISK_INFO.serial_number), has_diskman_tag=False
        ),
    ],
)

SHARED_TWO_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME2_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0,
            name="nvme",
            partitions="{},{}".format(
                gen_partition(NVME2_DISK_INFO.serial_number), gen_partition(NVME_DISK_INFO.serial_number)
            ),
            has_diskman_tag=False,
        ),
    ],
)


class TestSharedConfigStrategy:
    def _create_strategy(self):
        return deploy_config.SharedConfigStrategy()

    def _mock_config_content_shared(self, spec):
        parted = {"disk_{}".format(parted_spec.serial_number): ["grub", "*"] for parted_spec in spec.parted}
        options = "--addtag diskman.sys=true"
        if spec.stripes and spec.stripes >= 2:
            options = "{} --stripes {}".format(options, spec.stripes)
        lvm = {
            "lv0": ["root", "40G", "vg0", options],
            "lv1": ["home", "6G", "vg0", options],
            "lv2": ["place", spec.place_size, "vg0", options],
        }
        lvm.update(
            {
                "vg{}".format(volume_group_spec.idx): [
                    volume_group_spec.name,
                    volume_group_spec.partitions,
                    "--addtag diskman=true",
                ]
                for volume_group_spec in spec.volume_groups
                if volume_group_spec.has_diskman_tag
            }
        )
        lvm.update(
            {
                "vg{}".format(volume_group_spec.idx): [
                    volume_group_spec.name,
                    volume_group_spec.partitions,
                ]
                for volume_group_spec in spec.volume_groups
                if not volume_group_spec.has_diskman_tag
            }
        )
        config = {
            "md": {},
            "parted": parted,
            "lvm": lvm,
            "fs": {
                "lv0": ["ext4", "-b 4096", "/", "barrier=1,noatime,lazytime"],
                "lv1": ["ext4", "-b 4096", "/home", "barrier=1,noatime,lazytime,nosuid,nodev"],
                "lv2": ["ext4", "-b 4096", "/place", "barrier=1,noatime,lazytime,nosuid,nodev"],
            },
        }
        if spec.ssd_volume_group:
            config["lvm"]["lv3"] = ["ssd", spec.ssd_size, spec.ssd_volume_group, "--addtag diskman.sys=true"]
            config["fs"]["lv3"] = ["ext4", "-b 4096", "/ssd", "barrier=1,noatime,lazytime,nosuid,nodev"]
        return config

    @pytest.fixture()
    def lui_config_mock(self, mp):
        mock_lui_config(mp, {})

    @pytest.mark.parametrize(
        "disk_conf, expected_config",
        [
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[], nvmes=[]), SHARED_HDD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[]), SHARED_HDD_SSD_CONFIG),
            (
                bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[NVME_DISK_INFO]),
                SHARED_HDD_SSD_NVME_CONFIG,
            ),
            (bot.HostDiskConf(hdds=[], ssds=[SSD_DISK_INFO], nvmes=[]), SHARED_SSD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[], ssds=[SSD_DISK_INFO], nvmes=[NVME_DISK_INFO]), SHARED_SSD_AND_NVME_CONFIG),
            (
                bot.HostDiskConf(hdds=[], ssds=[SMALL_SSD_DISK_INFO, SMALL_SSD2_DISK_INFO], nvmes=[]),
                SHARED_TWO_SMALL_SSD_CONFIG,
            ),
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO, HDD2_DISK_INFO], ssds=[], nvmes=[]), SHARED_TWO_HDD_CONFIG),
            (bot.HostDiskConf(hdds=[], ssds=[], nvmes=[NVME_DISK_INFO]), SHARED_NVME_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[], ssds=[], nvmes=[NVME_DISK_INFO, NVME2_DISK_INFO]), SHARED_TWO_NVME_CONFIG),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_normal_execution(self, mp, host, disk_conf, expected_config):
        mock_disk_configuration(mp, disk_conf)
        stage_params = self._create_strategy().generate(host, ORIG_CONFIG_NAME)
        assert get_config_content(stage_params) == self._mock_config_content_shared(expected_config)

    @pytest.mark.parametrize(
        "disk_conf",
        [
            bot.HostDiskConf(hdds=[], ssds=[], nvmes=[]),
            bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO], ssds=[], nvmes=[]),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_no_disk_system_disk_error(self, mp, host, disk_conf):
        mock_disk_configuration(mp, disk_conf)
        with pytest.raises(deploy_config.NoSystemDiskError):
            self._create_strategy().generate(host, ORIG_CONFIG_NAME)


YT_DEDICATED_HDD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(
            serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "500g", "*"]
        ),
    ],
    fs=[
        YTRotationalFsSpec(partition=gen_partition(HDD_DISK_INFO.serial_number, 8), mountpoint="/yt/disk1"),
    ],
)

YT_DEDICATED_HDD_SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(
            serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "500g", "*"]
        ),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
    ],
    fs=[
        YTRotationalFsSpec(partition=gen_partition(HDD_DISK_INFO.serial_number, 8), mountpoint="/yt/disk1"),
        YTNonRotationalFsSpec(partition=gen_partition(SSD_DISK_INFO.serial_number), mountpoint="/yt/ssd1"),
    ],
)

YT_DEDICATED_HDD_SSD_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(
            serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "500g", "*"]
        ),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
    ],
    fs=[
        YTRotationalFsSpec(partition=gen_partition(HDD_DISK_INFO.serial_number, 8), mountpoint="/yt/disk1"),
        YTNonRotationalFsSpec(partition=gen_partition(SSD_DISK_INFO.serial_number), mountpoint="/yt/ssd1"),
        YTNonRotationalFsSpec(partition=gen_partition(NVME_DISK_INFO.serial_number), mountpoint="/yt/nvme1"),
    ],
)

YT_DEDICATED_NVME_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(
            serial_number=NVME_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "500g", "*"]
        ),
    ],
    fs=[
        YTNonRotationalFsSpec(partition=gen_partition(NVME_DISK_INFO.serial_number, 8), mountpoint="/yt/nvme1"),
    ],
)


class TestYtDedicatedConfigStrategy:
    def _create_strategy(self):
        return deploy_config.YtDedicatedConfigStrategy()

    def _mock_config_content(self, spec):
        parted = {
            "disk_{}".format(parted_spec.serial_number): parted_spec.partitions or ["grub", "*"]
            for parted_spec in spec.parted
        }
        sys_partition = spec.parted[0].serial_number
        fs = {
            "disk_{}_2".format(sys_partition): ["vfat", "-S 512 -s 8", "/boot/efi", "noatime"],
            "disk_{}_3".format(sys_partition): ["ext4", "-b 4096", "/boot", "barrier=1,noatime,lazytime"],
            "disk_{}_4".format(sys_partition): ["ext4", "-b 4096", "/", "barrier=1,noatime,lazytime"],
            "disk_{}_5".format(sys_partition): ["ext4", "-b 4096", "/rootB", "barrier=1,noatime,lazytime"],
            "disk_{}_6".format(sys_partition): ["ext4", "-b 4096", "/home", "barrier=1,noatime,lazytime,nosuid,nodev"],
            "disk_{}_7".format(sys_partition): ["ext4", "-b 4096", "/place", "barrier=1,noatime,lazytime,nosuid,nodev"],
        }
        for fs_spec in spec.fs:
            fs[fs_spec.partition] = [fs_spec.fs, fs_spec.fs_opts, fs_spec.mountpoint, fs_spec.mount_options]
        config = {"md": {}, "parted": parted, "lvm": {}, "fs": fs}
        return config

    @pytest.fixture()
    def lui_config_mock(self, mp):
        mock_lui_config(mp, {})

    @pytest.mark.parametrize(
        "disk_conf, expected_config",
        [
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[], nvmes=[]), YT_DEDICATED_HDD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[]), YT_DEDICATED_HDD_SSD_CONFIG),
            (
                bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[NVME_DISK_INFO]),
                YT_DEDICATED_HDD_SSD_NVME_CONFIG,
            ),
            (bot.HostDiskConf(hdds=[], ssds=[], nvmes=[NVME_DISK_INFO]), YT_DEDICATED_NVME_ONLY_CONFIG),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_normal_execution(self, mp, host, disk_conf, expected_config):
        mock_disk_configuration(mp, disk_conf)
        stage_params = self._create_strategy().generate(host, ORIG_CONFIG_NAME)
        assert get_config_content(stage_params) == self._mock_config_content(expected_config)

    @pytest.mark.parametrize(
        "disk_conf",
        [
            bot.HostDiskConf(hdds=[], ssds=[], nvmes=[]),
            bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO], ssds=[], nvmes=[]),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_no_disk_system_disk_error(self, mp, host, disk_conf):
        mock_disk_configuration(mp, disk_conf)
        with pytest.raises(deploy_config.NoSystemDiskError):
            self._create_strategy().generate(host, ORIG_CONFIG_NAME)


YT_STORAGE_SYSTEM_HDD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=NODE_STORAGE_HDD_DISK_INFO.serial_number, partitions=["grub", "*"]),
        PartedSpec(
            serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "500g", "*"]
        ),
    ],
    fs=[
        YTRotationalFsSpec(partition=gen_partition(NODE_STORAGE_HDD_DISK_INFO.serial_number), mountpoint="/yt/disk1"),
        YTRotationalFsSpec(partition=gen_partition(HDD_DISK_INFO.serial_number, 8), mountpoint="/yt/disk2"),
    ],
)
YT_STORAGE_SYSTEM_SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=NODE_STORAGE_HDD_DISK_INFO.serial_number, partitions=["grub", "*"]),
        PartedSpec(
            serial_number=SSD_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "500g", "*"]
        ),
    ],
    fs=[
        YTRotationalFsSpec(partition=gen_partition(NODE_STORAGE_HDD_DISK_INFO.serial_number), mountpoint="/yt/disk1"),
        YTNonRotationalFsSpec(partition=gen_partition(SSD_DISK_INFO.serial_number, 8), mountpoint="/yt/ssd1"),
    ],
)
YT_STORAGE_SYSTEM_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=NODE_STORAGE_HDD_DISK_INFO.serial_number, partitions=["grub", "*"]),
        PartedSpec(
            serial_number=NVME_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "500g", "*"]
        ),
    ],
    fs=[
        YTRotationalFsSpec(partition=gen_partition(NODE_STORAGE_HDD_DISK_INFO.serial_number), mountpoint="/yt/disk1"),
        YTNonRotationalFsSpec(partition=gen_partition(NVME_DISK_INFO.serial_number, 8), mountpoint="/yt/nvme1"),
    ],
)


class TestYtStorageConfigStrategy:
    def _create_strategy(self):
        return deploy_config.YtStorageConfigStrategy()

    def _mock_config_content(self, spec):
        parted = {
            "disk_{}".format(parted_spec.serial_number): parted_spec.partitions or ["grub", "*"]
            for parted_spec in spec.parted
        }
        # dummy way to find out system disk
        sys_partition = None
        for i in spec.parted:
            if len(i.partitions) > 2:
                sys_partition = i.serial_number
                break
        if not sys_partition:
            raise Exception("misconfigured test: should have at least one system disk")

        fs = {
            "disk_{}_2".format(sys_partition): ["vfat", "-S 512 -s 8", "/boot/efi", "noatime"],
            "disk_{}_3".format(sys_partition): ["ext4", "-b 4096", "/boot", "barrier=1,noatime,lazytime"],
            "disk_{}_4".format(sys_partition): ["ext4", "-b 4096", "/", "barrier=1,noatime,lazytime"],
            "disk_{}_5".format(sys_partition): ["ext4", "-b 4096", "/rootB", "barrier=1,noatime,lazytime"],
            "disk_{}_6".format(sys_partition): ["ext4", "-b 4096", "/home", "barrier=1,noatime,lazytime,nosuid,nodev"],
            "disk_{}_7".format(sys_partition): ["ext4", "-b 4096", "/place", "barrier=1,noatime,lazytime,nosuid,nodev"],
        }
        for fs_spec in spec.fs:
            fs[fs_spec.partition] = [fs_spec.fs, fs_spec.fs_opts, fs_spec.mountpoint, fs_spec.mount_options]
        config = {"md": {}, "parted": parted, "lvm": {}, "fs": fs}
        return config

    @pytest.fixture()
    def lui_config_mock(self, mp):
        mock_lui_config(mp, {})

    @pytest.mark.parametrize(
        "disk_conf, expected_config",
        [
            (
                bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO, HDD_DISK_INFO], ssds=[], nvmes=[]),
                YT_STORAGE_SYSTEM_HDD_CONFIG,
            ),
            (
                bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[]),
                YT_STORAGE_SYSTEM_SSD_CONFIG,
            ),
            (
                bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO], ssds=[], nvmes=[NVME_DISK_INFO]),
                YT_STORAGE_SYSTEM_NVME_CONFIG,
            ),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_normal_execution(self, mp, host, disk_conf, expected_config):
        mock_disk_configuration(mp, disk_conf)
        stage_params = self._create_strategy().generate(host, ORIG_CONFIG_NAME)
        assert get_config_content(stage_params) == self._mock_config_content(expected_config)

    @pytest.mark.parametrize(
        "disk_conf",
        [
            bot.HostDiskConf(hdds=[], ssds=[], nvmes=[]),
            bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO], ssds=[], nvmes=[]),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_no_disk_system_disk_error(self, mp, host, disk_conf):
        mock_disk_configuration(mp, disk_conf)
        with pytest.raises(deploy_config.NoSystemDiskError):
            self._create_strategy().generate(host, ORIG_CONFIG_NAME)


YT_SHARED_HDD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "40g", "5g", "*"]),
        PartedSpec(serial_number=HDD2_DISK_INFO.serial_number),
    ],
    fs=[
        YTRotationalFsSpec(partition=gen_partition(HDD2_DISK_INFO.serial_number), mountpoint="/yt/disk1"),
    ],
)

YT_SHARED_HDD_SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "40g", "5g", "*"]),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
    ],
    fs=[
        YTNonRotationalFsSpec(partition=gen_partition(SSD_DISK_INFO.serial_number), mountpoint="/yt/ssd1"),
    ],
)

YT_SHARED_HDD_SSD_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "40g", "5g", "*"]),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME2_DISK_INFO.serial_number),
    ],
    fs=[
        YTNonRotationalFsSpec(partition=gen_partition(SSD_DISK_INFO.serial_number), mountpoint="/yt/ssd1"),
        YTNonRotationalFsSpec(partition=gen_partition(NVME2_DISK_INFO.serial_number), mountpoint="/yt/nvme1"),
    ],
    ssd_partition=NVME_DISK_INFO.serial_number,
)

YT_SHARED_NVME_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number, partitions=["grub", "40g", "5g", "*"]),
        PartedSpec(serial_number=NVME2_DISK_INFO.serial_number),
    ],
    fs=[
        YTNonRotationalFsSpec(partition=gen_partition(NVME2_DISK_INFO.serial_number), mountpoint="/yt/nvme1"),
    ],
)


class TestYtSharedConfigStrategy:
    def _create_strategy(self):
        return deploy_config.YtSharedConfigStrategy()

    def _mock_config_content(self, spec):
        parted = {
            "disk_{}".format(parted_spec.serial_number): parted_spec.partitions or ["grub", "*"]
            for parted_spec in spec.parted
        }
        sys_partition = spec.parted[0].serial_number
        fs = {
            "disk_{}_2".format(sys_partition): ["ext4", "-b 4096", "/", "barrier=1,noatime,lazytime"],
            "disk_{}_3".format(sys_partition): ["ext4", "-b 4096", "/home", "barrier=1,noatime,lazytime,nosuid,nodev"],
            "disk_{}_4".format(sys_partition): ["ext4", "-b 4096", "/place", "barrier=1,noatime,lazytime,nosuid,nodev"],
        }
        if spec.ssd_partition:
            fs["disk_{}_2".format(spec.ssd_partition)] = [
                "ext4",
                "-b 4096",
                "/ssd",
                "barrier=1,noatime,lazytime,nosuid,nodev",
            ]
        for fs_spec in spec.fs:
            fs[fs_spec.partition] = [fs_spec.fs, fs_spec.fs_opts, fs_spec.mountpoint, fs_spec.mount_options]
        config = {"md": {}, "parted": parted, "lvm": {}, "fs": fs}
        return config

    @pytest.fixture()
    def lui_config_mock(self, mp):
        mock_lui_config(mp, {})

    @pytest.mark.parametrize(
        "disk_conf, expected_config",
        [
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO, HDD2_DISK_INFO], ssds=[], nvmes=[]), YT_SHARED_HDD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[]), YT_SHARED_HDD_SSD_CONFIG),
            (
                bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[NVME_DISK_INFO, NVME2_DISK_INFO]),
                YT_SHARED_HDD_SSD_NVME_CONFIG,
            ),
            (bot.HostDiskConf(hdds=[], ssds=[], nvmes=[NVME_DISK_INFO, NVME2_DISK_INFO]), YT_SHARED_NVME_ONLY_CONFIG),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_normal_execution(self, mp, host, disk_conf, expected_config):
        mock_disk_configuration(mp, disk_conf)
        stage_params = self._create_strategy().generate(host, ORIG_CONFIG_NAME)
        assert get_config_content(stage_params) == self._mock_config_content(expected_config)

    @pytest.mark.parametrize(
        "disk_conf",
        [
            bot.HostDiskConf(hdds=[], ssds=[], nvmes=[]),
            bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO], ssds=[], nvmes=[]),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_no_disk_system_disk_error(self, mp, host, disk_conf):
        mock_disk_configuration(mp, disk_conf)
        with pytest.raises(deploy_config.NoSystemDiskError):
            self._create_strategy().generate(host, ORIG_CONFIG_NAME)


YT_MASTERS_HDD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "40g", "5g", "*"]),
        PartedSpec(serial_number=HDD2_DISK_INFO.serial_number),
    ],
    fs=[
        YTRotationalFsSpec(partition=gen_partition(HDD2_DISK_INFO.serial_number), mountpoint="/yt/disk1"),
    ],
)

YT_MASTERS_HDD_SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "40g", "5g", "*"]),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
    ],
    fs=[
        YTNonRotationalFsSpec(partition=gen_partition(SSD_DISK_INFO.serial_number), mountpoint="/yt/ssd1"),
    ],
)

YT_MASTERS_HDD_SSD_NVME_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "40g", "5g", "*"]),
        PartedSpec(serial_number=SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number),
        PartedSpec(serial_number=NVME2_DISK_INFO.serial_number),
    ],
    fs=[
        YTNonRotationalFsSpec(partition=gen_partition(SSD_DISK_INFO.serial_number), mountpoint="/yt/ssd1"),
        YTNonRotationalFsSpec(partition=gen_partition(NVME_DISK_INFO.serial_number), mountpoint="/yt/nvme1"),
        YTNonRotationalFsSpec(partition=gen_partition(NVME2_DISK_INFO.serial_number), mountpoint="/yt/nvme2"),
    ],
)

YT_MASTERS_NVME_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=NVME_DISK_INFO.serial_number, partitions=["grub", "40g", "5g", "*"]),
        PartedSpec(serial_number=NVME2_DISK_INFO.serial_number),
    ],
    fs=[
        YTNonRotationalFsSpec(partition=gen_partition(NVME2_DISK_INFO.serial_number), mountpoint="/yt/nvme1"),
    ],
)


class TestYtMastersConfigStrategy:
    def _create_strategy(self):
        return deploy_config.YtMastersConfigStrategy()

    def _mock_config_content(self, spec):
        parted = {
            "disk_{}".format(parted_spec.serial_number): parted_spec.partitions or ["grub", "*"]
            for parted_spec in spec.parted
        }
        sys_partition = spec.parted[0].serial_number
        fs = {
            "disk_{}_2".format(sys_partition): ["ext4", "-b 4096", "/", "barrier=1,noatime,lazytime"],
            "disk_{}_3".format(sys_partition): ["ext4", "-b 4096", "/home", "barrier=1,noatime,lazytime,nosuid,nodev"],
            "disk_{}_4".format(sys_partition): ["ext4", "-b 4096", "/place", "barrier=1,noatime,lazytime,nosuid,nodev"],
        }
        for fs_spec in spec.fs:
            fs[fs_spec.partition] = [fs_spec.fs, fs_spec.fs_opts, fs_spec.mountpoint, fs_spec.mount_options]
        config = {"md": {}, "parted": parted, "lvm": {}, "fs": fs}
        return config

    @pytest.fixture()
    def lui_config_mock(self, mp):
        mock_lui_config(mp, {})

    @pytest.mark.parametrize(
        "disk_conf, expected_config",
        [
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO, HDD2_DISK_INFO], ssds=[], nvmes=[]), YT_MASTERS_HDD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[]), YT_MASTERS_HDD_SSD_CONFIG),
            (
                bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[SSD_DISK_INFO], nvmes=[NVME_DISK_INFO, NVME2_DISK_INFO]),
                YT_MASTERS_HDD_SSD_NVME_CONFIG,
            ),
            (bot.HostDiskConf(hdds=[], ssds=[], nvmes=[NVME_DISK_INFO, NVME2_DISK_INFO]), YT_MASTERS_NVME_ONLY_CONFIG),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_normal_execution(self, mp, host, disk_conf, expected_config):
        mock_disk_configuration(mp, disk_conf)
        stage_params = self._create_strategy().generate(host, ORIG_CONFIG_NAME)
        assert get_config_content(stage_params) == self._mock_config_content(expected_config)

    @pytest.mark.parametrize(
        "disk_conf",
        [
            bot.HostDiskConf(hdds=[], ssds=[], nvmes=[]),
            bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO], ssds=[], nvmes=[]),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_no_disk_system_disk_error(self, mp, host, disk_conf):
        mock_disk_configuration(mp, disk_conf)
        with pytest.raises(deploy_config.NoSystemDiskError):
            self._create_strategy().generate(host, ORIG_CONFIG_NAME)


MDS_DEDICATED_1HDD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "*"]),
    ],
    volume_groups=[
        VolumeGroupSpec(idx=0, name="hdd", partitions=gen_partition(HDD_DISK_INFO.serial_number, 7)),
    ],
)


MDS_DEDICATED_2HDD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "*"]),
        PartedSpec(serial_number=HDD2_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0,
            name="hdd",
            partitions="{},{}".format(
                gen_partition(HDD_DISK_INFO.serial_number, 7), gen_partition(HDD2_DISK_INFO.serial_number)
            ),
        ),
    ],
    stripes=2,
)


MDS_DEDICATED_4HDD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "*"]),
        PartedSpec(serial_number=HDD2_DISK_INFO.serial_number),
        PartedSpec(serial_number=HDD3_DISK_INFO.serial_number),
        PartedSpec(serial_number=HDD4_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0,
            name="hdd",
            partitions="{},{}".format(
                gen_partition(HDD_DISK_INFO.serial_number, 7), gen_partition(HDD2_DISK_INFO.serial_number)
            ),
        ),
    ],
    fs=[
        MDSRotationalFsSpec(partition=gen_partition(HDD3_DISK_INFO.serial_number), mountpoint='/srv/storage/1'),
        MDSRotationalFsSpec(partition=gen_partition(HDD4_DISK_INFO.serial_number), mountpoint='/srv/storage/2'),
    ],
    stripes=2,
)

MDS_DEDICATED_4HDD_2SSD_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(serial_number=HDD_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "*"]),
        PartedSpec(serial_number=HDD2_DISK_INFO.serial_number),
        PartedSpec(serial_number=HDD3_DISK_INFO.serial_number),
        PartedSpec(serial_number=HDD4_DISK_INFO.serial_number),
        PartedSpec(serial_number=SMALL_SSD_DISK_INFO.serial_number),
        PartedSpec(serial_number=SMALL_SSD2_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0,
            name="hdd",
            partitions="{},{}".format(
                gen_partition(HDD_DISK_INFO.serial_number, 7), gen_partition(HDD2_DISK_INFO.serial_number)
            ),
        ),
    ],
    ssd_volume_group=[
        VolumeGroupSpec(
            idx=1,
            name="ssd",
            partitions="{},{}".format(
                gen_partition(SMALL_SSD_DISK_INFO.serial_number),
                gen_partition(SMALL_SSD2_DISK_INFO.serial_number),
            ),
        ),
    ],
    fs=[
        MDSRotationalFsSpec(partition=gen_partition(HDD3_DISK_INFO.serial_number), mountpoint='/srv/storage/1'),
        MDSRotationalFsSpec(partition=gen_partition(HDD4_DISK_INFO.serial_number), mountpoint='/srv/storage/2'),
    ],
    stripes=2,
)

MDS_DEDICATED_2SSD_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(
            serial_number=SMALL_SSD_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "*"]
        ),
        PartedSpec(serial_number=SMALL_SSD2_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0,
            name="ssd",
            partitions="{},{}".format(
                gen_partition(SMALL_SSD_DISK_INFO.serial_number, 7),
                gen_partition(SMALL_SSD2_DISK_INFO.serial_number),
            ),
        ),
    ],
    stripes=2,
)

MDS_DEDICATED_2NVME_ONLY_CONFIG = ConfigSpec(
    parted=[
        PartedSpec(
            serial_number=NVME_DISK_INFO.serial_number, partitions=["grub", "2g", "2g", "40g", "40g", "5g", "*"]
        ),
        PartedSpec(serial_number=NVME2_DISK_INFO.serial_number),
    ],
    volume_groups=[
        VolumeGroupSpec(
            idx=0,
            name="nvme",
            partitions="{},{}".format(
                gen_partition(NVME_DISK_INFO.serial_number, 7), gen_partition(NVME2_DISK_INFO.serial_number)
            ),
        ),
    ],
)


class TestMdsDedicatedConfigStrategy:
    def _create_strategy(self):
        return deploy_config.MdsDedicatedConfigStrategy()

    def _mock_config_content(self, spec):
        config = {"md": {}, "parted": {}, "lvm": {}, "fs": {}}
        # make parts
        config["parted"].update(
            {
                "disk_{}".format(parted_spec.serial_number): parted_spec.partitions or ["grub", "*"]
                for parted_spec in spec.parted
            }
        )
        # make fs for roots
        sys_partition = spec.parted[0].serial_number
        fs = {
            "disk_{}_2".format(sys_partition): ["vfat", "-S 512 -s 8", "/boot/efi", "noatime"],
            "disk_{}_3".format(sys_partition): ["ext4", "-b 4096", "/boot", "barrier=1,noatime,lazytime"],
            "disk_{}_4".format(sys_partition): ["ext4", "-b 4096", "/", "barrier=1,noatime,lazytime"],
            "disk_{}_5".format(sys_partition): ["ext4", "-b 4096", "/rootB", "barrier=1,noatime,lazytime"],
            "disk_{}_6".format(sys_partition): ["ext4", "-b 4096", "/home", "barrier=1,noatime,lazytime,nosuid,nodev"],
        }

        # make /place
        config["lvm"].update(
            {
                "vg{}".format(volume_group_spec.idx): [
                    volume_group_spec.name,
                    volume_group_spec.partitions,
                ]
                for volume_group_spec in spec.volume_groups
            }
        )
        options = "--addtag diskman.sys=true"
        if spec.stripes and spec.stripes > 1:
            options = "{} --stripes {}".format(options, spec.stripes)
        config["lvm"]["lv0"] = ["place", spec.place_size, "vg0", options]
        fs["lv0"] = ["ext4", "-b 4096", "/place", "barrier=1,noatime,lazytime,nosuid,nodev"]

        # make /ssd
        if spec.ssd_volume_group:
            config["lvm"]["lv1"] = ["ssd", spec.ssd_size, "vg1", options]
            config["fs"]["lv1"] = ["ext4", "-b 4096", "/ssd", "barrier=1,noatime,lazytime,nosuid,nodev"]
            fs["lv1"] = ["ext4", "-b 4096", "/ssd", "barrier=1,noatime,lazytime,nosuid,nodev"]
            config["lvm"].update(
                {
                    "vg{}".format(volume_group_spec.idx): [
                        volume_group_spec.name,
                        volume_group_spec.partitions,
                    ]
                    for volume_group_spec in spec.ssd_volume_group
                }
            )

        # mds disks
        if spec.fs:
            for fs_spec in spec.fs:
                fs[fs_spec.partition] = [fs_spec.fs, fs_spec.fs_opts, fs_spec.mountpoint, fs_spec.mount_options]
        config["fs"] = fs
        return config

    @pytest.fixture()
    def lui_config_mock(self, mp):
        mock_lui_config(mp, {})

    @pytest.mark.parametrize(
        "disk_conf, expected_config",
        [
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO], ssds=[], nvmes=[]), MDS_DEDICATED_1HDD_ONLY_CONFIG),
            (bot.HostDiskConf(hdds=[HDD_DISK_INFO, HDD2_DISK_INFO], ssds=[], nvmes=[]), MDS_DEDICATED_2HDD_ONLY_CONFIG),
            (
                bot.HostDiskConf(
                    hdds=[HDD_DISK_INFO, HDD2_DISK_INFO, HDD3_DISK_INFO, HDD4_DISK_INFO], ssds=[], nvmes=[]
                ),
                MDS_DEDICATED_4HDD_ONLY_CONFIG,
            ),
            (
                bot.HostDiskConf(
                    hdds=[HDD_DISK_INFO, HDD2_DISK_INFO, HDD3_DISK_INFO, HDD4_DISK_INFO],
                    ssds=[SMALL_SSD_DISK_INFO, SMALL_SSD2_DISK_INFO],
                    nvmes=[],
                ),
                MDS_DEDICATED_4HDD_2SSD_CONFIG,
            ),
            (
                bot.HostDiskConf(hdds=[], ssds=[SMALL_SSD_DISK_INFO, SMALL_SSD2_DISK_INFO], nvmes=[]),
                MDS_DEDICATED_2SSD_ONLY_CONFIG,
            ),
            (
                bot.HostDiskConf(hdds=[], ssds=[], nvmes=[NVME_DISK_INFO, NVME2_DISK_INFO]),
                MDS_DEDICATED_2NVME_ONLY_CONFIG,
            ),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_normal_execution(self, mp, host, disk_conf, expected_config):
        mock_disk_configuration(mp, disk_conf)
        stage_params = self._create_strategy().generate(host, ORIG_CONFIG_NAME)
        assert get_config_content(stage_params) == self._mock_config_content(expected_config)

    @pytest.mark.parametrize(
        "disk_conf",
        [
            bot.HostDiskConf(hdds=[], ssds=[], nvmes=[]),
            bot.HostDiskConf(hdds=[NODE_STORAGE_HDD_DISK_INFO], ssds=[], nvmes=[]),
        ],
    )
    @pytest.mark.usefixtures("lui_config_mock")
    def test_no_disk_system_disk_error(self, mp, host, disk_conf):
        mock_disk_configuration(mp, disk_conf)
        with pytest.raises(deploy_config.NoSystemDiskError):
            self._create_strategy().generate(host, ORIG_CONFIG_NAME)
