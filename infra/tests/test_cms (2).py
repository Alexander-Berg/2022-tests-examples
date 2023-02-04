# coding: utf-8
# coding: utf-8
from __future__ import unicode_literals

import re
from collections import namedtuple
import pytest

from infra.rtc.walle_validator.lib.filters import And, Or, HasTag, HasNoTag, HasLabel
from infra.rtc.walle_validator.lib.constants import YT_TAG, YP_TAG, YP_MASTERS_TAG, YP_MASTER_TESTING_TAG, \
    YP_MASTER_PRESTABLE_TAG, QLOUD_TAG
from infra.rtc.walle_validator.lib.coverage import create_coverage

cms_coverage = create_coverage()

ClusterInfo = namedtuple('ClusterInfo', ['location', 'name'])

YpDatacenters = ("sas", "man", "vla", "iva", "myt", "man-pre", "sas-test", "xdc")


def is_project_ignored_for_1_4(project):
    return (
        project.id == "yp-testing-2"
        or "yp-hfsm" in project.cms_settings.get('cms') and "test" in project.cms_settings.get('cms')
        or "cloud-adapter-yp" in project.cms_settings.get('cms')
    )


def get_yp_cluster_info(project):
    parts = project.id.split("-")
    if "reserve" in parts:
        return ClusterInfo("default", project.id)
    elif "testing" in parts:
        return ClusterInfo("sas-test", project.id)
    elif "prestable" in parts and project.id not in ["yp-gpu-sas-dev-prestable", "yp-iss-sas-base-search-prestable"]:
        return ClusterInfo("man-pre", project.id)
    m = re.match(r"^yp-(iss|gpu)-([a-z]+)(-[a-z\-]+)?$", project.id)
    assert m is not None, "wrong project name"
    return ClusterInfo(m.group(2), project.id)


def get_yt_over_yp_cluster_info(project):
    m = re.match(r"^yp-iss-([a-z]+)-yt-([a-z]+)(-[0-9a-z\-]+)?$", project.id)
    assert m is not None, "wrong project name"
    return ClusterInfo(m.group(1), m.group(2))


def get_yt_over_runtime_cluster_name(project):
    if project.id in ("rtc-yt-mtn-amd", "rtc-yt-mtn"):
        return "arnold"
    elif project.id == "rtc-yt-hahn-gpu" or project.id in ("rtc-yt-hahn-gpu-prestable", "rtc-bert-mtn", "rtc-bert-infiniband-mtn"):
        return "hahn"
    else:
        m = re.match("^rtc-yt-([a-z]+)(-sas|-man|-vla)?(-masters|-lvm|-gpu)?$", project.id)
        assert m is not None, "wrong project name"
        return m.group(1)


@pytest.mark.project_filter(HasLabel("scheduler", "gencfg"), cms_coverage)
def test_rtc_cms_not_default(project):
    if project.id in ("rtc-mtn-orphaned", "rtc-mtn-poweroff-2019", "search-testing", "rtc-bert-infiniband-mtn", "yp-prestable", "rtc-market-report-api", "rtc-mtn-nanny"):
        return
    assert project.cms_settings.get('cms') != "default", "{} can't have default CMS".format(project)


@pytest.mark.project_filter(And([HasTag(YP_TAG), HasNoTag(YT_TAG)]), cms_coverage)
def test_yp_cms_not_default(project, expected_version="v1.4"):
    if project.id == "yp-testing-2":
        assert project.cms_settings.get('cms') == "default"
        return

    cluster_info = get_yp_cluster_info(project)
    assert cluster_info.location in project.cms_settings.get('cms'), "{} has wrong name in CMS url".format(project)

    if is_project_ignored_for_1_4(project):
        return

    if "reserve" in project.id:
        return
    assert project.cms_settings.get('cms_api_version', None) == expected_version, "{} project must have CMS {} version \"{}\"".format(
        project.id,
        project.cms_settings.get('cms'),
        expected_version
    )


@pytest.mark.project_filter(Or([
    HasTag(YP_MASTERS_TAG),
    HasTag(YP_MASTER_TESTING_TAG),
    HasTag(YP_MASTER_PRESTABLE_TAG)
]), cms_coverage)
def test_yp_masters_cms(project, expected_version="v1.4"):
    if project.id in ('yp-prestable'):
        return
    cms_mapping = {
        "yp-msk": "https://walle-cms.yt.yandex-team.ru/yp-myt",
        "yp-sas-test": "https://walle-cms-testing.yt.yandex-team.ru/yp-sas-test"
    }
    cms = cms_mapping.get(project.id, "https://walle-cms.yt.yandex-team.ru/{}".format(project.id))
    assert project.cms_settings.get('cms') == cms, "{} must have CMS {}".format(project, cms)
    assert project.cms_settings.get('cms_api_version') == expected_version, "{} must have CMS {} version \"{}\"".format(
        project,
        project.cms_settings.get('cms'),
        expected_version
    )


@pytest.mark.project_filter(And([
    HasTag(YT_TAG),
    HasTag(YP_TAG),
    HasNoTag(YP_MASTERS_TAG),
    HasNoTag(YP_MASTER_TESTING_TAG),
    HasNoTag(YP_MASTER_PRESTABLE_TAG)
]), cms_coverage)
def test_yt_over_yp_cms(project):
    cluster_info = get_yt_over_yp_cluster_info(project)
    if project.id in ("yp-iss-vla-yt-arnold-gpu", "yp-iss-vla-yt-arnold-gpu2",
                      "yp-iss-iva-yt-hahn-gpu", "yp-iss-sas-yt-hahn-gpu", "yp-iss-vla-yt-hahn-gpu",
                      "yp-iss-man-yt-hahn-gpu-bert", "yp-iss-sas-yt-hahn-gpu-bert",
                      "yp-iss-sas-yt-hahn-gpu-yati2", "yp-iss-vla-yt-hahn-gpu-yati3", "yp-iss-vla-yt-hahn-gpu-yati4", "yp-iss-vla-yt-hahn-gpu-yati-test"):
        assert cluster_info.location in project.cms_settings.get('cms'), "{} has wrong name in CMS url".format(project)
    elif project.id in ("yp-iss-prestable-yt-hahn-gpu"):
        assert "pre" in project.cms_settings.get('cms'), "{} has wrong name in CMS url".format(project)
    else:
        assert cluster_info.name in project.cms_settings.get('cms'), "{} has wrong name in CMS url".format(project)
    if cluster_info.name == "seneca":
        assert cluster_info.location in project.cms_settings.get('cms'), "{} has wrong location in CMS url".format(project)


@pytest.mark.project_filter(And([HasTag(YT_TAG), HasNoTag(YP_TAG)]), cms_coverage)
def test_yt_over_runtime_cms(project):
    if not project.id.startswith("rtc-") or project.id == "rtc-yt-inbox":
        return
    cluster_name = get_yt_over_runtime_cluster_name(project)
    assert cluster_name in project.cms_settings.get('cms'), "{} has wrong CMS".format(project)


@pytest.mark.project_filter(HasTag(QLOUD_TAG), cms_coverage)
def test_qloud_cms_not_default(project):
    assert project.cms_settings.get('cms') != "default", "{} can't have default CMS".format(project)


@pytest.mark.project_filter(HasLabel("scheduler", "sandbox"), cms_coverage)
def test_cms_for_sandbox_scheduler(project):
    assert project.cms_settings.get('cms') == "default"


@pytest.mark.project_filter(HasLabel("scheduler", "none"), cms_coverage)
def test_cms_for_none_scheduler(project):
    if project.id == "yp-core":
        return
    assert project.cms_settings.get('cms') == "default"


@pytest.mark.project_filter(HasLabel("cohabitation", "enabled"))
def test_that_cohabitation_use_suspension_api(project):
    if project.id in ("yp-iss-man-yt-socrates", "yp-iss-sas-yt-ada", "yp-iss-sas-yt-ada-masters",
                      "yp-iss-vla-yt-arnold-default"):
        cluster_info = get_yt_over_yp_cluster_info(project)
        assert cluster_info.name in project.cms_settings.get('cms'), "{} has wrong CMS".format(project)
    elif project.id == "rtc-yt-mtn":
        assert project.cms_settings.get('cms') == "https://walle-cms.yt.yandex-team.ru/arnold"
    else:
        assert project.cms_settings.get('cms') == "http://clusterstate.yandex-team.ru/api/v1", "{} with enabled cohabitation works only with gencfg".format(project)


def test_cms_coverage(all_projects):
    cms_coverage.check(all_projects)


@pytest.mark.project_filter(Or([HasTag(YT_TAG), HasTag(YP_TAG), HasLabel("cohabitation", "enabled")]), cms_coverage)
def test_temporary_unreachable(project):
    if "walle-cms.yt" in project.cms_settings.get("cms", ""):
        assert project.cms_settings.get("temporary_unreachable_enabled", None), True
    for dc in YpDatacenters:
        if "yp-hfsm-%s." % dc in project.cms_settings.get("cms", ""):
            assert project.cms_settings.get("temporary_unreachable_enabled", None), True
