# coding: utf-8
from __future__ import unicode_literals

import pytest

from infra.rtc.walle_validator.lib.filters import HasLabel
from infra.rtc.walle_validator.lib.coverage import create_coverage

automation_coverage = create_coverage()

PROJECTS_WITH_DISABLED_AUTOMATION = {
    'rtc-gencfg-dev',
    'rtc-iss-master-adm',
    'rtc-iss-master-global',
    'rtc-iss-master-man',
    'rtc-iss-master-msk',
    'rtc-iss-master-msk-mtn',
    'rtc-iss-master-sas',
    'rtc-iss-master-vla',
    'rtc-mtn-enclave',
    'rtc-mtn-conductordb',
    'rtc-qloud-mongodb',
    'rtc-qloud-mongodb-prestable',
    'search-delete',
    'search-testing',
    'test',
    'yp-testing-rnd',
    'yp-prestable-rnd',
    'yp-arm64-prestable-mtn'
}


@pytest.mark.project_filter(HasLabel("automation", "enabled"), automation_coverage)
def test_automation_enabled(project):
    assert project.id not in PROJECTS_WITH_DISABLED_AUTOMATION


@pytest.mark.project_filter(HasLabel("automation", "disabled"), automation_coverage)
def test_automation_disabled(project):
    assert project.id in PROJECTS_WITH_DISABLED_AUTOMATION
    if project.healing_automation:
        assert not project.healing_automation["enabled"]


def test_obsolete_automation_set(all_projects):
    actual_projects = {project.id for project in all_projects}
    assert not PROJECTS_WITH_DISABLED_AUTOMATION - actual_projects, PROJECTS_WITH_DISABLED_AUTOMATION - actual_projects


def test_automation_coverage(all_projects):
    automation_coverage.check(all_projects)
