# coding: utf-8
from __future__ import unicode_literals

import pytest

from infra.rtc.walle_validator.lib.filters import And, Or, HasTag, HasNoTag, HasLabel
from infra.rtc.walle_validator.lib.coverage import create_coverage
from infra.rtc.walle_validator.lib.constants import (
    YT_TAG, YP_TAG, MARKET_TAG, QLOUD_TAG,
    YP_MASTERS_TAG, YP_MASTER_TESTING_TAG, YP_MASTER_PRESTABLE_TAG,
)

deploy_coverage = create_coverage()


@pytest.mark.project_filter(And([
    HasLabel("scheduler", "gencfg"),
    HasNoTag(MARKET_TAG),
    HasNoTag(YT_TAG)
]), deploy_coverage)
def test_runtime_deploy_config(project):
    if project.id == 'rtc-mtn-conductordb':
        return
    elif project.id == 'rtc-mtn-myt':
        assert project.deploy_config == 'web'
        assert project.deploy_config_policy == 'shared'
        return

    assert project.deploy_config == 'web'
    assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'


@pytest.mark.project_filter(HasTag(MARKET_TAG), deploy_coverage)
def test_market_deploy_config(project):
    assert project.deploy_config == 'market-web-ubuntu-16.04'
    assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'


@pytest.mark.project_filter(And([
    HasTag(YT_TAG),
    HasNoTag(YP_TAG)
]), deploy_coverage)
def test_yt_deploy_config(project):
    if project.id in ("rtc-yt-mtn", "rtc-yt-mtn-amd"):
        # search cohabitation
        assert project.deploy_config == 'rtc-yt-nodes-common-ubuntu-16.04'
        assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'
    elif project.id == "rtc-yt-inbox":
        assert project.deploy_config == 'rtc-yt-5.4-nodes-dedicated-rtc-ubuntu-20.04'
        assert project.deploy_config_policy == 'yt_dedicated'
    else:
        assert project.deploy_config == 'rtc-yt-nodes-dedicated-ubuntu-16.04'
        assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'


@pytest.mark.project_filter(Or([
    HasTag(YP_MASTERS_TAG),
    HasTag(YP_MASTER_TESTING_TAG),
    HasTag(YP_MASTER_PRESTABLE_TAG)
]), deploy_coverage)
def test_yp_masters_deploy_config(project):
    assert project.deploy_config == 'rtc-yt-nodes-dedicated-ubuntu-16.04'
    if project.id == "yp-core":
        assert project.deploy_config_policy == 'yt_dedicated'
    else:
        assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'


@pytest.mark.project_filter(And([
    HasTag(YP_TAG),
    HasNoTag(YP_MASTERS_TAG),
    HasNoTag(YP_MASTER_TESTING_TAG),
    HasNoTag(YP_MASTER_PRESTABLE_TAG),
]), deploy_coverage)
def test_yp_deploy_config(project):
    # proijects without host deployment support
    if project.id in ("yp-testing-azuseast1"):
        return

    if project.id in ("yp-testing-rnd", "yp-prestable-rnd"):
        assert project.deploy_config == "rtc-yp-5.4-rtc-ubuntu-16.04"
        assert project.deploy_config_policy == 'shared'
    elif project.id in ("yp-iss-vla-yt-arnold-amd", "yp-iss-sas-yt-hahn-amd", "yp-iss-sas-yt-pythia",
                        "yp-iss-man-yt-pythia", "yp-iss-vla-yt-pythia"):
        # amd nodes
        assert project.deploy_config == 'rtc-yt-5.4-nodes-dedicated-rtc-ubuntu-20.04'
        assert project.deploy_config_policy == 'yt_dedicated'
    elif project.id in ("yp-testing", "yp-testing-mtn", "yp-testing-2",
                        "yp-iss-prestable", "yp-iss-prestable-slave", "yp-iss-prestable-gpu",
                        "yp-iss-prestable-base-search", "yp-iss-vla-noclab"):
        assert project.deploy_config == 'rtc-yp-ubuntu-16.04'
        assert project.deploy_config_policy == 'shared'
    elif project.id in ("yp-testing-lvm", "yp-iss-prestable-yard", "yp-prestable-lvm",
                        "yp-iss-sas-saas", "yp-iss-man-saas", "yp-iss-vla-saas"):
        assert project.deploy_config == 'web'
        assert project.deploy_config_policy == 'sharedlvm'
    elif project.id in ("yp-iss-sas-lvm", "yp-iss-man-lvm", "yp-iss-vla-lvm"):
        assert project.deploy_config == 'web-5.4-rtc-ubuntu-16.04'
        assert project.deploy_config_policy == 'sharedlvm'
    elif project.id == "yp-iss-man-yt-hume-lvm":
        assert project.deploy_config == 'rtc-yt-nodes-dedicated-ubuntu-16.04'
        assert project.deploy_config_policy == 'diskmanager'
    elif project.id in ("yp-iss-vla-yt-arnold-rnd", "yp-iss-sas-yt-hahn-gpu-bert",
                        "yp-iss-sas-yt-hahn-gpu-yati2", "yp-iss-vla-yt-hahn-gpu-yati3", "yp-iss-vla-yt-hahn-gpu-yati4", "yp-iss-vla-yt-hahn-gpu-yati-test"):
        assert project.deploy_config == 'rtc-yt-rnd-rtc-ubuntu-16.04'
        assert project.deploy_config_policy == 'yt_dedicated'
    elif project.id.endswith("-distbuild"):
        assert project.deploy_config == "rtc-yp-5.4-rtc-ubuntu-16.04"
        assert project.deploy_config_policy == 'shared'
    elif project.id.endswith(("-sandbox", "-yabs")):
        assert project.deploy_config == 'rtc-yp-ubuntu-16.04'
        assert project.deploy_config_policy == 'shared'
    elif project.id.endswith("-dbaas"):
        assert project.deploy_config == 'rtc-yp-ubuntu-16.04'
        assert project.deploy_config_policy == 'sharedlvm'
    elif project.id in ("yp-iss-iva", "yp-iss-man", "yp-iss-myt", "yp-iss-sas", "yp-iss-vla"):
        assert project.deploy_config == "rtc-yp-5.4-rtc-ubuntu-16.04"
        assert project.deploy_config_policy == 'shared'
    elif project.id in ("yp-iss-iva-dev", "yp-iss-man-dev", "yp-iss-myt-dev", "yp-iss-sas-dev", "yp-iss-vla-dev"):
        assert project.deploy_config == "rtc-yp-5.4-rtc-ubuntu-16.04"
        assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'
    elif project.id in ("yp-iss-sas-yt-hume"):
        assert project.deploy_config == 'rtc-yt-5.4-nodes-dedicated-rtc-ubuntu-16.04'
        assert project.deploy_config_policy == 'yt_dedicated'
    elif project.id == "yp-iss-vla-yt-arnold-default":
        # search cohabitation
        assert project.deploy_config == 'rtc-yt-5.4-nodes-common-rtc-ubuntu-16.04'
        assert project.deploy_config_policy == 'yt_shared'
    elif project.id in ("yp-iss-vla-yt-seneca-vla-masters", "yp-iss-man-yt-seneca-man-masters",
                        "yp-iss-sas-yt-bohr-masters", "yp-iss-vla-yt-landau-masters",
                        "yp-iss-iva-yt-markov-masters", "yp-iss-myt-yt-markov-masters",
                        "yp-iss-sas-yt-markov-masters", "yp-iss-man-yt-markov-masters", "yp-iss-vla-yt-markov-masters",
                        "yp-iss-man-yt-pythia-masters", "yp-iss-sas-yt-pythia-masters", "yp-iss-myt-yt-pythia-masters",
                        "yp-iss-sas-yt-nash-masters", "yp-iss-vla-yt-nash-masters", "yp-iss-myt-yt-nash-masters",
                        "yp-iss-vla-yt-pythia-masters"):
        assert project.deploy_config == 'rtc-yt-masters-rtc-ubuntu-16.04'
        assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'
    elif project.id == "yp-iss-man-yt-zeno-masters":
        # yt masters
        assert project.deploy_config == 'rtc-yt-5.4-nodes-dedicated-rtc-ubuntu-16.04'
        assert project.deploy_config_policy == 'yt_masters'
    elif project.id in ("yp-iss-man-yt-zeno", "yp-iss-sas-yt-hahn",
                        "yp-iss-man-yt-socrates", "yp-iss-sas-yt-ada", "yp-iss-man-yt-freud-masters", "yp-iss-man-yt-socrates-masters",
                        "yp-iss-sas-yt-hume-masters", "yp-iss-sas-yt-ada-masters"):
        assert project.deploy_config == 'rtc-yt-5.4-nodes-dedicated-rtc-ubuntu-16.04'
        assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'
    elif project.id == "yp-iss-man-yt-freud":
        assert project.deploy_config == 'rtc-yt-5.4-nodes-dedicated-rtc-ubuntu-20.04'
        assert project.deploy_config_policy == 'yt_dedicated'
    elif YT_TAG in project.tags:
        if project.id in ('yp-iss-vla-yt-arnold-gpu2',):
            assert project.deploy_config_policy == 'shared'
        else:
            assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'
        assert project.deploy_config == 'rtc-yt-nodes-dedicated-ubuntu-16.04'
    elif project.id in ("yp-arm64-prestable-mtn"):
        assert project.deploy_config == "web"
    elif project.id in ("yp-gpu-vla-dev", "yp-gpu-sas-dev-prestable"):
        assert project.deploy_config == "rtc-yp-5.4-rtc-ubuntu-16.04"
    elif "reserve" in project.id:
        assert project.deploy_config == 'web-5.4-rtc-ubuntu-20.04'
    else:
        assert project.deploy_config == 'rtc-yp-ubuntu-16.04'
        assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'


@pytest.mark.project_filter(HasTag(QLOUD_TAG), deploy_coverage)
def test_qloud_deploy_config(project):
    assert project.deploy_config == 'web'
    assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'


@pytest.mark.project_filter(HasLabel("scheduler", "none"), deploy_coverage)
def test_other_deploy_config(project):
    if YT_TAG in project.tags:
        return
    assert project.deploy_config == 'web'
    if project.id in ("rtc-mtn-hostman"):
        assert project.deploy_config_policy == 'shared'
        return
    assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'


@pytest.mark.project_filter(HasLabel("scheduler", "sandbox"), deploy_coverage)
def test_sandbox_deploy_config(project):
    assert project.deploy_config == 'web'
    assert not project.deploy_config_policy or project.deploy_config_policy == 'passthrough'


def test_deploy_coverage(all_projects):
    deploy_coverage.check(all_projects)
