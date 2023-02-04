import os
import mock
import pytest

import infra.rtc.nodeinfo.lib.modules.os_info as osi

LSB_RELEASE = """
DISTRIB_ID=Ubuntu
DISTRIB_RELEASE=16.04
DISTRIB_CODENAME=xenial
DISTRIB_DESCRIPTION="Ubuntu 16.04.5 LTS"

"""


@pytest.fixture
def open_mock():
    return mock.mock_open(read_data=LSB_RELEASE)


def test_get_os_version(open_mock):
    version, codename = osi.get_os_version(open_mock)
    assert version == "Ubuntu 16.04.5 LTS"
    assert codename == "xenial"


def test_get_kernel_version():
    uname = os.uname()
    assert osi.get_kernel_version() == uname[2]


def test_get_os_type():
    assert osi.get_os_type() == "Linux"


def test_get_os_info(open_mock):
    info = osi.get_os_info(open_mock)
    uname = os.uname()
    assert info.version == "Ubuntu 16.04.5 LTS"
    assert info.type == "Linux"
    assert info.kernel == uname[2]
    assert info.codename == "xenial"
