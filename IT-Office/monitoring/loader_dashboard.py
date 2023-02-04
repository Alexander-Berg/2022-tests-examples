from source.utils import StaffUtils, StatConnectorMoreDimensions
from source.config import st_client
from collections import defaultdict
import datetime

LOADER_STAFF_URL = "ext_searchportal_2109_4148"

class LoaderCollector():
    def __init__(self):
        self.report_title = "Loader"
        self.report_path = "Helpdesk/RFS/Monitoring/Loader"

    def _fetch_data_from_st(self, date=None):
        loader_list = StaffUtils().groupmembership_by_group_url(LOADER_STAFF_URL)
        processed_data = defaultdict(lambda: defaultdict(int))
        result = []

        if not date:
            date = datetime.datetime.now().strftime('%d.%m.%Y')

        for loader in loader_list:
            issues = st_client.issues.find('Queue: "Helpdesk Requests" and Components: changed( from: Loader by: {loader}@ date: {date})'.format(
                loader = loader,
                date=date
            ))
            for issue in issues:
                fix_vers = [x.name for x in issue.fixVersions]
                location = fix_vers[0].lower().replace(' ',
                                                       '_') if fix_vers else 'empty'
                processed_data[location]['count_{}'.format(loader)] += 1
                processed_data[location]['count_all'] += 1

        for location in processed_data:
            prefetch_data = processed_data[location]
            prefetch_data["location"] = location
            prefetch_data["fielddate"] = date
            result.append(dict(prefetch_data))

        return result

    def process(self):
        data = self._fetch_data_from_st()
        stat = StatConnectorMoreDimensions(self.report_path, title=self.report_title)
        for item in data:
            stat.upload_data(item)

    def process_historical(self):
        result = []
        start = datetime.datetime(2019,1,1)
        today = datetime.datetime.now()
        while (today-start).days > 0:
            start = start + datetime.timedelta(days=1)
            date = start.strftime('%d.%m.%Y')
            result.extend(self._fetch_data_from_st(date))
        for item in result:
            stat = StatConnectorMoreDimensions(self.report_path, title=self.report_title)
            stat.upload_data(item)


def loader_dashboard():
    LoaderCollector().process()