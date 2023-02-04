# coding: utf-8

import pytest

from infra.rtc.walle_validator.lib.filters import Not, HasLabel
from infra.rtc.walle_validator.lib.constants import YASM_MONITORED_TAG, YASM_QLOUD_MONITORED_TAG
from infra.rtc.walle_validator.lib.coverage import create_coverage

yasm_coverage = create_coverage()


def expect_tag(tag, project):
    assert tag in project.tags, "{!r} hasn't tag {}".format(project, tag)


@pytest.mark.project_filter(HasLabel("scheduler", "qloud"), yasm_coverage)
def test_yasm_qloud_tag_presence(project):
    expect_tag(YASM_QLOUD_MONITORED_TAG, project)


@pytest.mark.project_filter(Not(HasLabel("scheduler", "qloud")), yasm_coverage)
def test_yasm_other_tag_presence(project):
    if project.id in ("search-delete", "rtc-preorders-lake", "qloud-dns"):
        return
    expect_tag(YASM_MONITORED_TAG, project)


def test_yasm_coverage(all_projects):
    yasm_coverage.check(all_projects)
