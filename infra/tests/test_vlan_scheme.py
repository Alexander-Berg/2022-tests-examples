# coding: utf-8
from __future__ import unicode_literals

import pytest

from infra.rtc.walle_validator.lib.filters import HasLabel
from infra.rtc.walle_validator.lib.constants import YT_TAG
from infra.rtc.walle_validator.lib.coverage import create_coverage

vlan_schemas_coverage = create_coverage()


@pytest.mark.project_filter(HasLabel("scheduler", "yp"), vlan_schemas_coverage)
def test_vlan_scheme_for_yp_scheduler(project):
    if project.id in ("yp-iss-sas-yt-hahn-gpu-bert", "yp-iss-man-yt-hahn-gpu-bert",
                      "yp-iss-sas-yt-hahn-gpu-yati2", "yp-iss-vla-yt-hahn-gpu-yati3", "yp-iss-vla-yt-hahn-gpu-yati4", "yp-iss-vla-yt-hahn-gpu-yati-test"):
        assert project.hbf_project_id == "441d"
    else:
        assert project.vlan_scheme == "mtn"
        assert project.hbf_project_id == "604"


@pytest.mark.project_filter(HasLabel("scheduler", "qloud"), vlan_schemas_coverage)
def test_vlan_scheme_for_qloud_scheduler(project):
    assert project.vlan_scheme == "mtn"
    assert project.hbf_project_id == "604"


@pytest.mark.project_filter(HasLabel("scheduler", "gencfg"), vlan_schemas_coverage)
def test_vlan_scheme_for_gencfg_scheduler(project):
    if project.id == "search-testing":
        return
    elif project.id == "rtc-antimalware-mtn":
        assert project.vlan_scheme == "mtn"
        assert project.hbf_project_id == "8000409b"
    elif project.id in ("rtc-bert-mtn", "rtc-bert-infiniband-mtn"):
        assert project.vlan_scheme == "mtn"
        assert project.hbf_project_id == "441d"
    elif YT_TAG in project.tags:
        assert project.vlan_scheme == "mtn"
        if project.id.endswith("-masters"):
            assert project.hbf_project_id == "549"
        else:
            assert project.hbf_project_id == "604"
    elif project.id in (
            "rtc", "rtc-all-dynamic", "rtc-prestable", "rtc-testing", "rtc-without-ipxe", "rtc-yabs",
            "rtc-yabs-prestable", "rtc-iss-master-msk", 'rtc-iss-master-adm', 'rtc-iss-master-sas',
            "rtc-all-dynamic-sas", "rtc-all-dynamic-man", "rtc-all-dynamic-iva", "rtc-all-dynamic-myt",
            "rtc-man", "rtc-iva", "rtc-myt", "rtc-sas", "rtc-prestable-man", "rtc-prestable-sas"):
        assert project.vlan_scheme == "search"
        assert project.native_vlan == 604
    else:
        assert project.vlan_scheme == "mtn"
        assert project.hbf_project_id == "604"


@pytest.mark.project_filter(HasLabel("scheduler", "sandbox"), vlan_schemas_coverage)
def test_vlan_scheme_for_sandbox_scheduler(project):
    assert project.vlan_scheme == "mtn"
    assert project.hbf_project_id == "522"


@pytest.mark.project_filter(HasLabel("scheduler", "none"), vlan_schemas_coverage)
def test_vlan_scheme_for_none_scheduler(project):
    if project.id == "qloud-dns":
        return
    assert project.vlan_scheme == "mtn"
    assert project.hbf_project_id == "604"


def test_vlan_schemas_coverage(all_projects):
    vlan_schemas_coverage.check(all_projects)
