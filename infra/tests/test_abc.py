# coding: utf-8
from __future__ import unicode_literals

import pytest

from infra.rtc.walle_validator.lib.filters import And, Or, HasTag, HasNoTag, HasLabel
from infra.rtc.walle_validator.lib.coverage import create_coverage
from infra.rtc.walle_validator.lib.constants import (
    YT_TAG, YP_TAG, MARKET_TAG, YABS_TAG,
    YP_MASTERS_TAG, YP_MASTER_TESTING_TAG, YP_MASTER_PRESTABLE_TAG,
    RTC_ENCLAVE_TAG, YP_DEV_TAG
)

abc_coverage = create_coverage()

# https://abc.yandex-team.ru/services/admintools/
CONDUCTOR_BOT_PROJECT_ID = 100000674

# https://abc.yandex-team.ru/services/runtimecloud/
RUNTIMECLOUD_BOT_PROJECT_ID = 100001955

# https://abc.yandex-team.ru/services/MARKETITO/
MARKETITO_BOT_PROJECT_ID = 100001060

# https://abc.yandex-team.ru/services/market-over-rtc/
MARKET_BOT_PROJECT_ID = 100019849

# https://abc.yandex-team.ru/services/yabs-over-rtc/
YABS_BOT_PROJECT_ID = 100013178

# https://abc.yandex-team.ru/services/yp/
YP_BOT_PROJECT_ID = 100005598

# https://abc.yandex-team.ru/services/yt-masters-over-rtc/
YT_MASTERS_OVER_RTC_BOT_PROJECT_ID = 100013584

# https://abc.yandex-team.ru/services/yt-over-rtc/
YT_OVER_RTC_BOT_PROJECT_ID = 100013177

# https://abc.yandex-team.ru/services/qloud-over-rtc/
QLOUD_OVER_RTC_BOT_PROJECT_ID = 100013179

# https://abc.yandex-team.ru/services/qyp-over-rtc/
QYP_OVER_RTC_BOT_PROJECT_ID = 100013180

# https://abc.yandex-team.ru/services/bert-over-rtc/
BERT_OVER_RTC_BOT_PROJECT_ID = 100014580

# https://abc.yandex-team.ru/services/distbuild-over-rtc/
DISTBUILD_OVER_RTC_BOT_PROJECT_ID = 100015819

# https://abc.yandex-team.ru/services/sandbox-over-rtc/
SANDBOX_OVER_RTC_BOT_PROJECT_ID = 100015820

# https://abc.yandex-team.ru/services/pumpkin-over-rtc/
PUMPKIN_OVER_RTC_BOT_PROJECT_ID = 100016385

# https://abc.yandex-team.ru/services/hostman-over-rtc/
HOSTMAN_OVER_RTC_BOT_PROJECT_ID = 100017473

# https://abc.yandex-team.ru/services/iss-over-rtc/
ISS_OVER_RTC_BOT_PROJECT_ID = 100017470

# https://abc.yandex-team.ru/services/nanny-over-rtc/
NANNY_OVER_RTC_BOT_PROJECT_ID = 100017554

# https://abc.yandex-team.ru/services/mdb-over-rtc/
MDB_OVER_RTC_BOT_PROJECT_ID = 100018583

# https://abc.yandex-team.ru/services/yp-dns-over-rtc/
YP_DNS_OVER_RTC_BOT_PROJECT_ID = 100019155

# https://abc.yandex-team.ru/services/RND/
RND_BOT_PROJECT_ID = 100000950

# https://abc.yandex-team.ru/services/gencfg/
GENCFG_BOT_PROJECT_ID = 100001962


@pytest.mark.project_filter(And([
    HasLabel("scheduler", "gencfg"),
    HasNoTag(YT_TAG),
    HasNoTag(MARKET_TAG),
    HasNoTag(YABS_TAG),
]), abc_coverage)
def test_rtc_default_bot_project_id(project):
    if project.id == "rtc-mtn-pumpkin":
        assert project.bot_project_id == PUMPKIN_OVER_RTC_BOT_PROJECT_ID
    elif project.id == "rtc-mtn-nanny":
        assert project.bot_project_id == NANNY_OVER_RTC_BOT_PROJECT_ID
    elif project.id == "rtc-mtn-conductordb":
        assert project.bot_project_id == CONDUCTOR_BOT_PROJECT_ID
    elif project.id in ("rtc-iss-master-adm", "rtc-iss-master-global",
                        "rtc-iss-master-man", "rtc-iss-master-msk",
                        "rtc-iss-master-sas", "rtc-iss-master-vla",
                        "rtc-iss-master-msk-mtn"):
        assert project.bot_project_id == ISS_OVER_RTC_BOT_PROJECT_ID
    elif project.id in ("rtc-yp-dns-iva", "rtc-yp-dns-myt",
                        "rtc-yp-dns-sas", "rtc-yp-dns-vla",
                        "rtc-yp-dns-man"):
        assert project.bot_project_id == YP_DNS_OVER_RTC_BOT_PROJECT_ID
    elif project.id in ('rtc-qloud-mongodb-prestable', 'rtc-qloud-mongodb'):
        assert project.bot_project_id == QLOUD_OVER_RTC_BOT_PROJECT_ID
    elif project.id == "rtc-gencfg-dev":
        assert project.bot_project_id == GENCFG_BOT_PROJECT_ID
    else:
        assert project.bot_project_id == RUNTIMECLOUD_BOT_PROJECT_ID


@pytest.mark.project_filter(HasTag(RTC_ENCLAVE_TAG), abc_coverage)
def test_enclave_bot_project_id(project):
    assert project.bot_project_id == RUNTIMECLOUD_BOT_PROJECT_ID


@pytest.mark.project_filter(HasTag(MARKET_TAG), abc_coverage)
def test_market_bot_project_id(project):
    if project.id == "rtc-market-reserved":
        assert project.bot_project_id == MARKETITO_BOT_PROJECT_ID
    else:
        assert project.bot_project_id == MARKET_BOT_PROJECT_ID


@pytest.mark.project_filter(HasTag(YABS_TAG), abc_coverage)
def test_yabs_bot_project_id(project):
    assert project.bot_project_id == YABS_BOT_PROJECT_ID


@pytest.mark.project_filter(And([
    HasTag(YT_TAG),
    HasNoTag(YP_MASTERS_TAG),
    HasNoTag(YP_MASTER_TESTING_TAG),
    HasNoTag(YP_MASTER_PRESTABLE_TAG)
]), abc_coverage)
def test_yt_bot_project_id(project):
    if "masters" in project.id.split("-"):
        assert project.bot_project_id == YT_MASTERS_OVER_RTC_BOT_PROJECT_ID
    elif project.id in ("rtc-bert-mtn", "rtc-bert-infiniband-mtn", "yp-iss-sas-yt-hahn-gpu-bert",
                        "yp-iss-man-yt-hahn-gpu-bert", "yp-iss-sas-yt-hahn-gpu-yati2",
                        "yp-iss-vla-yt-hahn-gpu-yati3", "yp-iss-vla-yt-hahn-gpu-yati4", "yp-iss-vla-yt-hahn-gpu-yati-test"):
        assert project.bot_project_id == BERT_OVER_RTC_BOT_PROJECT_ID
    else:
        assert project.bot_project_id == YT_OVER_RTC_BOT_PROJECT_ID


@pytest.mark.project_filter(Or([
    HasTag(YP_MASTERS_TAG),
    HasTag(YP_MASTER_TESTING_TAG),
    HasTag(YP_MASTER_PRESTABLE_TAG)
]), abc_coverage)
def test_yp_masters_bot_project_id(project):
    assert project.bot_project_id == YP_BOT_PROJECT_ID


@pytest.mark.project_filter(And([HasLabel("scheduler", "yp"), HasNoTag(YT_TAG)]), abc_coverage)
def test_yp_bot_project_id(project):
    if YP_DEV_TAG in project.tags:
        assert project.bot_project_id == QYP_OVER_RTC_BOT_PROJECT_ID
    elif project.id.endswith("distbuild"):
        assert project.bot_project_id == DISTBUILD_OVER_RTC_BOT_PROJECT_ID
    elif project.id.endswith("sandbox"):
        assert project.bot_project_id == SANDBOX_OVER_RTC_BOT_PROJECT_ID
    elif project.id.endswith("dbaas"):
        assert project.bot_project_id == MDB_OVER_RTC_BOT_PROJECT_ID
    elif project.id.endswith("yabs"):
        assert project.bot_project_id == YABS_BOT_PROJECT_ID
    elif project.id in ("yp-arm64-prestable-mtn"):
        assert project.bot_project_id == RND_BOT_PROJECT_ID
    else:
        assert project.bot_project_id == RUNTIMECLOUD_BOT_PROJECT_ID


@pytest.mark.project_filter(HasLabel("scheduler", "qloud"), abc_coverage)
def test_qloud_bot_project_id(project):
    assert project.bot_project_id == QLOUD_OVER_RTC_BOT_PROJECT_ID


@pytest.mark.project_filter(HasLabel("scheduler", "none"), abc_coverage)
def test_other_bot_project_id(project):
    if YP_TAG in project.tags:
        assert project.bot_project_id == YP_BOT_PROJECT_ID
    elif YT_TAG in project.tags:
        if "masters" in project.id.split("-"):
            assert project.bot_project_id == YT_MASTERS_OVER_RTC_BOT_PROJECT_ID
        else:
            assert project.bot_project_id == YT_OVER_RTC_BOT_PROJECT_ID
    elif project.id == "rtc-mtn-hostman":
        assert project.bot_project_id == HOSTMAN_OVER_RTC_BOT_PROJECT_ID
    else:
        assert project.bot_project_id == RUNTIMECLOUD_BOT_PROJECT_ID


@pytest.mark.project_filter(HasLabel("scheduler", "sandbox"), abc_coverage)
def test_sandbox_bot_project_id(project):
    assert project.bot_project_id == SANDBOX_OVER_RTC_BOT_PROJECT_ID


def test_abc_coverage(all_projects):
    abc_coverage.check(all_projects)
