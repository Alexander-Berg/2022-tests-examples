# coding: utf-8

import pytest

from infra.rtc.walle_validator.lib.constants import (
    RTCSRE_ABC_DEVOPS_GROUP,
    RTCSUPPORT_GROUP,
    YT_ABC_DEV_GROUP,
    DOOR_TO_RTC_GROUP,
    LOGBROKER_ABC_DEVOPS_GROUP,
    DUTYSEARCH_ABC_DEVOPS_GROUP,
    YA_AGENT_ABC_DEVOPS_GROUP,
    SOLOMON_ABC_DEVOPS_GROUP,
    YT_TAG
)
from infra.rtc.walle_validator.lib.filters import Or, HasTag, HasLabel


def test_default_project_roles(project):
    assert project.roles["noc_access"] == [DOOR_TO_RTC_GROUP]
    assert set(project.roles["user"]).issuperset({RTCSUPPORT_GROUP})
    assert set(project.roles["user"]).issuperset({SOLOMON_ABC_DEVOPS_GROUP})
    assert set(project.roles["superuser"]).issuperset({RTCSRE_ABC_DEVOPS_GROUP})


@pytest.mark.project_filter(HasTag(YT_TAG))
def test_that_yt_has_its_own_users(project):
    assert YT_ABC_DEV_GROUP in project.roles["user"]


@pytest.mark.project_filter(Or([HasLabel("scheduler", "gencfg"), HasLabel("scheduler", "yp")]))
def test_that_devops_has_access_to_containers(project):
    assert LOGBROKER_ABC_DEVOPS_GROUP in project.roles["user"]
    assert DUTYSEARCH_ABC_DEVOPS_GROUP in project.roles["user"]
    assert YA_AGENT_ABC_DEVOPS_GROUP in project.roles["user"]
