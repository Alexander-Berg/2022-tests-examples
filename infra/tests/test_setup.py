# coding: utf-8
from __future__ import unicode_literals

import pytest
from packaging import version

from infra.rtc.walle_validator.lib.filters import HasTag, Not
from infra.rtc.walle_validator.lib.constants import YT_TAG

FL_READY_KERNEL = version.parse("4.19.119.30.1")


def test_rtc_base_config(setup_config):
    # FIXME: proper base image testing
    # didn't have any attribute for proper base image determination, so just added focal as an acceptable variant
    assert setup_config.config["base"]["base_image"] in ["http://dist.yandex.ru/images/rtc/ubuntu-16.04.tar.bz2", "http://dist.yandex.ru/images/rtc/ubuntu-20.04.tar.bz2"]


def test_system_info_source(setup_config):
    assert setup_config.config["general"].get("system_info_source", "") == "bot"


def test_package_set(setup_config):
    if setup_config.name in ("market-web-ubuntu-16.04", "yt-rtc-masters-ubuntu-16.04"):
        return
    valid_packages = {
        "linux-image-server",
        "yandex-cauth",
        "yandex-gosky",
        "yandex-netconfig",
        "yandex-internal-root-ca",
        "yandex-search-salt",
        "yandex-archive-keyring",
        "yandex-search-user-root"
    }
    if setup_config.name == "yt-rtc-dm-ubuntu-16.04":
        valid_packages.add("yandex-diskmanager-autoformat")
    assert set(setup_config.config["packages"]) == valid_packages


def test_no_project_id(setup_config):
    assert not setup_config.config.get("network", {}).get("project_id")


@pytest.mark.project_filter(HasTag(YT_TAG))
def test_yt_hpa(project, all_setup_config_map):
    setup_config = all_setup_config_map[project.deploy_config]
    assert setup_config.config["hpa"]["ssd"] == "10%"
#    assert setup_config.config["hpa"]["hdd"] == "0%"


@pytest.mark.project_filter(Not(HasTag(YT_TAG)))
def test_hpa(project, all_setup_config_map):
    setup_config = all_setup_config_map[project.deploy_config]
    assert setup_config.config["hpa"]["ssd"] == "0%"
#    assert setup_config.config["hpa"]["hdd"] == "0%"


def test_that_kernel_is_fl_ready(project, all_setup_config_map):
    setup_config = all_setup_config_map[project.deploy_config]
    assert "linux-image-server" in setup_config.config["packages"]
    kernel_version = setup_config.config["packages"]["linux-image-server"].replace('-', '.')
    assert version.parse(kernel_version) >= FL_READY_KERNEL, "wrong kernel for setup config {}".format(setup_config.name)


def test_cauth_version(setup_config):
    assert setup_config.config['packages']['yandex-cauth'] == '1.7.7'


def test_hdd_absence_action(setup_config):
    # Setup MUST fail when host actual disks set does not match it's BOT conf
    # (to avoid misconfigured hosts, https://st.yandex-team.ru/HOSTMAN-1001)
    assert setup_config.config['system']['hdd_absence_action'] == 'stop'
