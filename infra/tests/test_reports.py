import pytest

from infra.rtc.walle_validator.lib.constants import (
    MARKET_TAG, YP_MASTERS_TAG, YP_MASTER_PRESTABLE_TAG, YP_MASTER_TESTING_TAG)
from infra.rtc.walle_validator.lib.coverage import create_coverage
from infra.rtc.walle_validator.lib.filters import HasLabel, HasTag, HasNoTag, And, Or

reports_coverage = create_coverage()


@pytest.mark.project_filter(And([
    HasNoTag(MARKET_TAG),
    HasLabel("automation", "enabled")
]))
def test_reports_enabled(project):
    assert project.reports.get('enabled'), 'Report for project {} disabled.'.format(project.id)


class RtcReportsConfiguration(object):
    message = "Bad report parameters for project {}: {}".format

    @property
    def report_queue(self):
        return 'BURNE'

    @property
    def report_assignee(self):
        return 'robot-walle'

    @property
    def report_summary(self):
        raise NotImplementedError

    @property
    def report_tags(self):
        raise NotImplementedError

    @property
    def report_components(self):
        raise NotImplementedError

    def test_queue_set_properly(self, project):
        assert project.reports.get('queue') == self.report_queue, self.message(project.id, 'wrong queue')

    def test_summary_set_properly(self, project):
        assert project.reports.get('summary') == self.report_summary, self.message(project.id, 'wrong summary')

    def test_assignee_set_properly(self, project):
        assert project.reports.get('extra', {}).get('assignee') == self.report_assignee, \
            self.message(project.id, 'wrong assignee')

    def test_extra_params_set_properly(self, project):
        assert project.reports.get('extra'), self.message(project.id, 'extra params missing')

        assert project.reports['extra'].get('type') == 'serviceRequest', self.message(project.id, 'wrong issue type')
        assert project.reports['extra'].get('tags') == self.report_tags, self.message(project.id, 'wrong tags')
        assert project.reports['extra'].get('components') == self.report_components, \
            self.message(project.id, 'wrong components')


@pytest.mark.project_filter(And([
    # Market has reports in their own queue, see RUNTIMECLOUD-13882
    HasNoTag(MARKET_TAG),
    # YP_MASTERS have somewhat custom report settings, see WALLE-2758
    HasNoTag(YP_MASTERS_TAG),
    HasNoTag(YP_MASTER_PRESTABLE_TAG),
    HasNoTag(YP_MASTER_TESTING_TAG),
]), reports_coverage)
class TestRtcReportsConfigured(RtcReportsConfiguration):

    @property
    def report_summary(self):
        return 'RTC'

    @property
    def report_components(self):
        return 'rtc'

    @property
    def report_tags(self):
        return 'rtc'


@pytest.mark.project_filter(Or([
    HasTag(YP_MASTERS_TAG),
    HasTag(YP_MASTER_PRESTABLE_TAG),
    HasTag(YP_MASTER_TESTING_TAG),
]), reports_coverage)
class TestYpReportsConfigured(RtcReportsConfiguration):
    # YP_MASTERS have somewhat custom report settings, see WALLE-2758

    @property
    def report_summary(self):
        return 'YP masters'

    @property
    def report_components(self):
        return 'yp_masters,rtc'

    @property
    def report_tags(self):
        return 'rtc,yp'


@pytest.mark.project_filter(HasTag(MARKET_TAG), reports_coverage)
class TestMarketReportsConfigured(object):
    # Market has reports in their own queue
    # Read more RUNTIMECLOUD-13882

    message = "Bad report parameters for project {}: {}".format

    def test_reports_enabled(self, project):
        assert project.reports.get('enabled'), 'Report for project {} is disabled.'.format(project.id)

    def test_queue_set_properly(self, project):
        assert project.reports.get('queue') == 'CSADMIN', self.message(project.id, 'wrong queue')

    def test_extra_params_configured_properly(self, project):
        assert project.reports.get('extra'), self.message(project.id, 'extra params missing')
        assert project.reports['extra'].get('tags') == 'cs_duty', self.message(project.id, 'unexpected tags')


def test_reports_coverage(all_projects):
    reports_coverage.check(all_projects)
