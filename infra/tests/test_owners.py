# coding: utf-8
from __future__ import unicode_literals

import pytest

import walletest

from infra.rtc.walle_validator.lib.constants import (
    RTC_BAD_OWNERS, RTCSRE_ABC_ADM_GROUP, RTCSRE_ABC_DEVOPS_GROUP,
    RTCSUPPORT_GROUP, ROBOT_WALLE, YT_ABC_DEV_GROUP, YT_TAG
)
from infra.rtc.walle_validator.lib.filters import HasTag


def test_rtc_owners(project):
    assert RTCSRE_ABC_ADM_GROUP in project.owners, \
        "Project {} not found group SRERTC administration".format(project.id)


def test_that_owners_has_no_bad_users(project):
    for user in RTC_BAD_OWNERS:
        assert user not in project.owners, \
            "Project {} exists bad user {}".format(project.id, user)


def test_that_robot_walle_is_not_an_owner(project):
    # robot-walle must not be in project owners anymore (now we have reboot_via_ssh)
    assert ROBOT_WALLE not in project.owners, "{} must not be in project {} owners".format(ROBOT_WALLE, project.id)


def test_that_devops_and_support_has_no_root(project):
    assert RTCSUPPORT_GROUP not in project.owners, "Project {} has group RTCSUPPORT".format(project.id)
    assert RTCSRE_ABC_DEVOPS_GROUP not in project.owners, "Project {} has group SRERTC devops".format(project.id)
    assert YT_ABC_DEV_GROUP not in project.owners, "Project {} has group YT devs".format(project.id)


@walletest.xfail
def test_rtc_notification(project):
    # Check bad recipients
    for level, recp in project.notifications['recipients'].items():
        for user in RTC_BAD_OWNERS:
            assert '{}@yandex-team.ru'.format(user) not in recp, \
                "Project {} notification level '{}' is bad recipients".format(project.id, level)
    # TODO: This email is correct ?
    ml_sre = 'wall-e-search-notifications@yandex-team.ru'
    # Warning
    assert ml_sre in project.notifications['recipients']['critical'], \
        "Project {} no email for SRE notify.".format(project.id)
    # Error
    assert ml_sre in project.notifications['recipients']['error'], \
        "Project {} no email for SRE notify.".format(project.id)


def test_owners_not_too_large(project):
    bad_groups = (
        "@svc_yt",  # It is recommended to use @svc_yt_administration
    )
    for group in bad_groups:
        assert group not in project.owners, \
            "Project {} must not have a {} group (it is too large)".format(project.id, group)


@pytest.mark.project_filter(HasTag(YT_TAG))
def test_yt_project_have_admins_group_in_owners(project):
    yt_required_groups = (
        "@svc_yt_administration",
    )
    for group in yt_required_groups:
        assert group in project.owners, "Project {} must have a {} group in its owners".format(project.id, group)
