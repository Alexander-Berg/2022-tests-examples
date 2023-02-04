from collections import defaultdict
import datetime
from source.config import *
from source.monitoring.monitoring import SLACollector
from source.utils import (
    StatConnectorMoreDimensions,
    exc_thread_wrapper)

logger = logging.getLogger(__name__)

class UnitedDashboardBacklogCollector():
    def _process_upload_data_to_stat(self, stat_name, stat_title, data):
        stat = StatConnectorMoreDimensions(stat_name, title=stat_title)
        stat.upload_data(dict(data))

    def _fetch_data(self, issues):
        result = []
        processed_data = defaultdict(lambda: defaultdict(int))

        for issue in issues:
            fix_vers = [x.name for x in issue.fixVersions]
            abc_services = [x["id"] for x in issue.abcService]
            location = fix_vers[0].lower().replace(' ','_') if fix_vers else 'empty'
            abc_service = abc_services[0] if abc_services else 'empty'
            processed_data[location]['backlog_{}'.format(abc_service)] += 1
            processed_data[location]['backlog_0'] += 1

        for location in processed_data:
            prefetch_data = processed_data[location]
            prefetch_data["location"] = location
            result.append(dict(prefetch_data))

        return result

    def _fetch_sla_count(self, issues, sla_type, sla_metric):
        sla_collector = SLACollector()
        result = []
        processed_data = defaultdict(lambda: defaultdict(int))

        for issue in issues:
            fix_vers = [x.name for x in issue.fixVersions]
            abc_services = [x["id"] for x in issue.abcService]
            location = fix_vers[0].lower().replace(' ','_') if fix_vers else 'empty'
            abc_service = abc_services[0] if abc_services else 'empty'

            sla_info = sla_collector._get_sla_time_by_issue(issue)
            if sla_info and sla_metric == "count":
                processed_data[location]['sla_{status}_{abc_service}'.format(
                    status=sla_info[sla_type]['status'],
                    abc_service=abc_service)] += 1
                processed_data[location]['sla_{status}_0'.format(
                    status=sla_info[sla_type]['status'])] += 1

            if sla_info and sla_metric == "time":
                processed_data[location]['sla_spent_{abc_service}'.format(
                    abc_service=abc_service)] += sla_info[sla_type]["spent"]
                processed_data[location]['sla_spent_0'] += sla_info[sla_type]["spent"]

                processed_data[location]['sla_count_{abc_service}'.format(
                    abc_service=abc_service)] += 1
                processed_data[location]['sla_count_0'] += 1

        for location in processed_data:
            prefetch_data = processed_data[location]
            prefetch_data["location"] = location
            result.append(dict(prefetch_data))

        return result

    def _issue_date_to_datetime(self, issue_date):
        strip_date = issue_date[:19]
        return datetime.datetime.strptime(strip_date, '%Y-%m-%dT%H:%M:%S')

    def _fetch_astronomycal(self, issues):
        processed_data = defaultdict(lambda: defaultdict(int))
        result = []

        for issue in issues:
            fix_vers = [x.name for x in issue.fixVersions]
            abc_services = [x["id"] for x in issue.abcService]
            location = fix_vers[0].lower().replace(' ','_') if fix_vers else 'empty'
            abc_service = abc_services[0] if abc_services else 'empty'
            resolved_at = self._issue_date_to_datetime(issue.resolvedAt)
            created_at = self._issue_date_to_datetime(issue.createdAt)
            diff = (resolved_at - created_at).seconds / 60
            processed_data[location]['astronomycal_{abc_service}'.format(
                abc_service=abc_service)] += diff
            processed_data[location]['astronomycal_0'.format(
                abc_service=abc_service)] += diff
            processed_data[location]['count_{abc_service}'.format(
                abc_service=abc_service)] += 1
            processed_data[location]['count_0'.format(
                abc_service=abc_service)] += 1

        for location in processed_data:
            prefetch_data = processed_data[location]
            prefetch_data["location"] = location
            result.append(dict(prefetch_data))

        return result

    def _process_by_issues_and_table_count(self, issues, table, title, date=None):
        analyze_data = self._fetch_data(issues)
        for one_location_data in analyze_data:
            if date:
                one_location_data['fielddate'] = date.strftime('%Y-%m-%d')
            self._process_upload_data_to_stat(table, title, one_location_data)

    def _process_sla_by_issues(self, issues, table, title, sla_type, sla_metric):
        analyze_data = self._fetch_sla_count(issues, sla_type, sla_metric)
        for one_location_data in analyze_data:
            self._process_upload_data_to_stat(table, title, one_location_data)

    def process_backlog(self):
        issues = st_client.issues.find('Queue: HDRFS AND Resolution: empty() AND Status: !Решен')
        table = 'Helpdesk/RFS/Monitoring/uniteddashboard/backlog'
        title = 'UnitedashboardBacklog'
        self._process_by_issues_and_table_count(issues, table, title)

    def process_created(self):
        issues = st_client.issues.find('Queue: HDRFS AND Created: today()')
        table = 'Helpdesk/RFS/Monitoring/uniteddashboard/created'
        title = 'UnitedashboardCreated'
        self._process_by_issues_and_table_count(issues, table, title)

    def process_created_old(self):
        issues = st_client.issues.find('Queue: HDRFS AND Created: today()-3d')
        table = 'Helpdesk/RFS/Monitoring/uniteddashboard/created'
        title = 'UnitedashboardCreated'
        date = datetime.datetime.now() - datetime.timedelta(days=3)
        self._process_by_issues_and_table_count(issues, table, title, date=date)

    def process_resolved(self):
        issues = st_client.issues.find('Queue: HDRFS AND Resolved: today()')
        table = 'Helpdesk/RFS/Monitoring/uniteddashboard/resolved'
        title = 'UnitedashboardCreated'
        self._process_by_issues_and_table_count(issues, table, title)

    def process_sla_count(self):
        issues = st_client.issues.find('Queue: HDRFS AND Resolved: today()')
        table = 'Helpdesk/RFS/Monitoring/uniteddashboard/sla_reaction_count'
        title = 'UnitedashboardSLAReactionCount'
        self._process_sla_by_issues(issues, table, title, "reaction", "count")
        table = 'Helpdesk/RFS/Monitoring/uniteddashboard/sla_solve_count'
        title = 'UnitedashboardSLASolveCount'
        self._process_sla_by_issues(issues, table, title, "solve", "count")

        table = 'Helpdesk/RFS/Monitoring/uniteddashboard/sla_reaction_time'
        title = 'UnitedashboardSLAReactionTime'
        self._process_sla_by_issues(issues, table, title, "reaction", "time")
        table = 'Helpdesk/RFS/Monitoring/uniteddashboard/sla_solve_time'
        title = 'UnitedashboardSLASolveTime'
        self._process_sla_by_issues(issues, table, title, "solve", "time")

        issues_incidents = st_client.issues.find('Queue: HDRFS AND Resolved: today() AND Type: Инцидент')
        table = 'Helpdesk/RFS/Monitoring/uniteddashboard/sla_solve_time_inc'
        title = 'UnitedashboardSLASolveTimeInc'
        self._process_sla_by_issues(issues_incidents, table, title, "solve", "time")

    def process_astronomical_count(self):
        issues = st_client.issues.find('Queue: HDRFS AND Resolved: today()')
        table = 'Helpdesk/RFS/Monitoring/uniteddashboard/astronomycal'
        title = 'UnitedashboardAstronomycalResolve'
        fetched_data = self._fetch_astronomycal(issues)
        for one_location_data in fetched_data:
            self._process_upload_data_to_stat(table, title, one_location_data)

@exc_thread_wrapper
def main_unitedashboard_collector():
    logger.info('Start UnitedDashboard backlog collector')
    united_collector = UnitedDashboardBacklogCollector()
    united_collector.process_backlog()
    united_collector.process_created()
    united_collector.process_resolved()
    united_collector.process_sla_count()
    united_collector.process_astronomical_count()