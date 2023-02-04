# coding: utf-8
from __future__ import unicode_literals

import pytest

from infra.rtc.walle_validator.lib.filters import HasTag, HasLabel
from infra.rtc.walle_validator.lib.constants import YT_TAG, QLOUD_TAG, YP_TAG, YP_DEV_TAG
from infra.rtc.walle_validator.lib.coverage import create_coverage

check_schemas_coverage = create_coverage()


@pytest.mark.project_filter(HasLabel("scheduler", "gencfg"), check_schemas_coverage)
def test_rtc_check_schemas(project):
    if project.labels["gpu"] == "nvidia":
        assert project.automation_plot_id == "rtc-gpu", "{!r} has wrong automation plot".format(project)
    elif project.id in ('rtc-qloud-mongodb-prestable', 'rtc-qloud-mongodb'):
        assert project.automation_plot_id == "qloud", "{!r} has wrong automation plot".format(project)
    elif project.id in ('rtc-gencfg-dev'):
        return
    else:
        assert project.automation_plot_id == "rtc", "{!r} has wrong automation plot".format(project)


@pytest.mark.project_filter(HasTag(YT_TAG), check_schemas_coverage)
def test_yt_check_schemas(project):
    if not project.id.startswith("rtc-") or project.id in ('rtc-yt-inbox'):
        return
    if project.labels["gpu"] == "nvidia":
        assert project.automation_plot_id == "rtc-gpu", "{!r} has wrong automation plot".format(project)
    else:
        assert project.automation_plot_id == "rtc", "{!r} has wrong automation plot".format(project)


@pytest.mark.project_filter(HasTag(YP_TAG), check_schemas_coverage)
def test_yp_check_schemas(project):
    if project.id in ('yp-iss-iva-yt-hahn-gpu', 'yp-iss-sas-yt-hahn-gpu', 'yp-iss-vla-yt-hahn-gpu',
                      'yp-iss-prestable-yt-hahn-gpu', 'yp-iss-vla-yt-arnold-gpu', 'yp-iss-vla-yt-arnold-gpu2', )\
    or 'yati' in project.id or 'bert' in project.id:
        assert project.automation_plot_id == "rtc-gpu-yati", "{!r} has wrong automation plot".format(project)
    elif project.labels["gpu"] == "nvidia" and YP_DEV_TAG not in project.tags:
        assert project.automation_plot_id == "rtc-gpu", "{!r} has wrong automation plot".format(project)

    elif project.id in (
            "yp-gpu-iva-dev", "yp-gpu-man-dev", "yp-gpu-myt-dev", "yp-gpu-sas-dev", "yp-gpu-sas-dev-prestable",
            "yp-gpu-vla-dev", "yp-iss-iva-dev", "yp-iss-man-dev", "yp-iss-myt-dev", "yp-iss-sas-dev",
            "yp-iss-vla-dev"):
        assert project.automation_plot_id == "rtc-qyp", "{!r} has wrong automation plot".format(project)
    else:
        assert project.automation_plot_id == "rtc", "{!r} has wrong automation plot".format(project)


@pytest.mark.project_filter(HasTag(QLOUD_TAG), check_schemas_coverage)
def test_qloud_check_schemas(project):
    assert project.automation_plot_id == "qloud", "{!r} has wrong automation plot".format(project)


@pytest.mark.project_filter(HasLabel("scheduler", "none"), check_schemas_coverage)
def test_other_check_schemas(project):
    if project.id == "rtc-yt-inbox":
        assert project.automation_plot_id == "rtc-incoming", "{!r} has wrong automation plot".format(project)
    elif project.id in ("rtc-mtn-orphaned", "rtc-mtn-enclave", "rtc-mtn-hostman"):
        assert project.automation_plot_id == "rtc", "{!r} has wrong automation plot".format(project)
    elif YT_TAG in project.tags or project.id in ("search-testing", "rtc-mtn-poweroff-2019"):
        return
    else:
        assert not project.automation_plot_id, "{!r} has wrong automation plot".format(project)


@pytest.mark.project_filter(HasLabel("scheduler", "sandbox"), check_schemas_coverage)
def test_sandbox_check_schemas(project):
    assert project.automation_plot_id == "rtc", "{!r} has wrong automation plot".format(project)


def test_check_schemas_coverage(all_projects):
    check_schemas_coverage.check(all_projects)


def test_check_schemas_inheritance(automation_plots):
    extract_checks = lambda plot: {x["name"] for x in plot.config["checks"]}
    base_checks = extract_checks(automation_plots["rtc"])
    assert extract_checks(automation_plots["rtc-gpu"]).issuperset(base_checks)
    assert extract_checks(automation_plots["rtc-gpu-yati"]).issuperset(base_checks)
    assert extract_checks(automation_plots["rtc-incoming"]).issuperset(base_checks)
    assert extract_checks(automation_plots["qloud"]).issuperset(base_checks)


def test_check_schemas_wait_option(automation_plots):
    checks_to_wait = ["hostman-ready", "check_skynet_procs", "check_iss_agent"]
    for plot_name, automation_plot in automation_plots.items():
        if plot_name != 'wall-e-hw-checks':
            check_map = {opt["name"]: opt for opt in automation_plot.config["checks"]}
            for check_name in checks_to_wait:
                assert check_map[check_name].get("wait"), "should wait check {} in plot {}".format(plot_name, check_name)
