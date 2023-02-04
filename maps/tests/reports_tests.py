import datetime
import re
import urllib.parse
import urllib
import pytest

from service_test_base import ServiceTestBase
from maps.pylibs.nginx_testlib.test_helpers import MethodIs, UrlPathIs, HasArg, \
    And, Not, ContainsKey, ContainsKeyValue, \
    HasDummyServiceTicket, HasDummyUserTicket
from lib.request_params import RequestParams


class SummaryMatches(object):
    @classmethod
    def get_summary_pattern(cls, now=None):
        now = now or datetime.datetime.utcnow()
        return now.strftime('Head-Unit Report: (%Y-%m-%dT\\d\\d:\\d\\d:\\d\\dZ)')

    def __eq__(self, rhs):
        (_, url) = rhs.split(' ', 1)
        parsed_url = urllib.parse.urlsplit(url)
        params = urllib.parse.parse_qs(parsed_url.query)
        now = datetime.datetime.utcnow()
        match = re.match(SummaryMatches.get_summary_pattern(now), params['summary'][0])
        if not match:
            return False
        parsed_time = datetime.datetime.strptime(match.group(1), '%Y-%m-%dT%H:%M:%SZ')
        return abs(now - parsed_time) < datetime.timedelta(0, 60)

    def __repr__(self):
        query_string = urllib.urlencode(
            {'summary': SummaryMatches.get_summary_pattern()})
        return '...&{}&...'.format(query_string)


class UploadReportRequest(object):
    AUTHORIZATION = 'OAuth DUMMY_AUTH'

    def __init__(self, data):
        self.params = RequestParams(
            request_method='POST',
            uri=(
                '/reports/1.x/upload_report?' +
                'head_id={head_id}&device_id={device_id}'
            ).format(
                head_id=RequestParams.HEAD_ID,
                device_id=RequestParams.DEVICE_ID,
            ),
            body=data,
        )
        self.params.headers['Authorization'] = self.AUTHORIZATION

    def await_request(self, upstream):
        expected_description = 'Head Id: {}'.format(RequestParams.HEAD_ID)
        upstream.request(
            query_matcher=And(
                MethodIs('POST'),
                UrlPathIs('/add'),
                HasArg('service', 'autoreport'),
                SummaryMatches(),
                HasArg('description', expected_description),
                HasArg('deviceid', RequestParams.DEVICE_ID)
            ),
            data_matcher=self.params.body,
            headers_matcher=And(
                ContainsKeyValue('accept-encoding', 'gzip, deflate'),
                HasDummyServiceTicket('maps-core-startrek-proxy'),
                HasDummyUserTicket(self.AUTHORIZATION),
                Not(ContainsKey('Authorization'))
            )
        ).AndReturn(200, 'Info')

    def perform_request(self, nginx):
        return nginx.post(self.params.uri, self.params.body, self.params.headers)


class UploadFileRequest(object):
    def __init__(self, data):
        self.params = RequestParams(
            request_method='POST',
            uri=(
                '/reports/1.x/upload_file?' +
                'device_id={device_id}'
            ).format(
                device_id=RequestParams.DEVICE_ID,
            ),
            body=data,
        )

    def await_request(self, upstream):
        upstream.request(
            query_matcher=And(
                MethodIs('POST'),
                UrlPathIs('/collect'),
                HasArg('service', 'autoreport'),
                HasArg('deviceid', RequestParams.DEVICE_ID)
            ),
            data_matcher=self.params.body,
            headers_matcher=And(
                ContainsKeyValue('accept-encoding', 'gzip, deflate'),
                HasDummyServiceTicket('maps-core-startrek-proxy')
            )
        ).AndReturn(200, 'Info')

    def perform_request(self, nginx):
        return nginx.post(self.params.uri, self.params.body, self.params.headers)


REPORT_DATA = 'Report contents'


def reports_requests():
    return [
        UploadReportRequest(REPORT_DATA),
        UploadFileRequest(REPORT_DATA)
    ]


class TestReports(ServiceTestBase):
    def setup(self):
        super(TestReports, self).setup()
        self.add_locations_from_file('reports.service.conf')
        self.upstream = self.add_upstream(
            'startrek_proxy_upstream',
            'core-startrek-proxy.maps.yandex.net'
        )
        self.set_env_variable(
            'AUTOMOTIVE_HTTP_SIGNATURE_KEY',
            RequestParams.AUTOMOTIVE_HTTP_SIGNATURE_KEY)
        self.set_env_variable(
            'AUTOMOTIVE_PERFORM_OUTDATED_TIMESTAMP_CHECK',
            'disabled')

    @pytest.mark.parametrize("reports_request", reports_requests())
    def test_report_endpoint_redirects(self, reports_request):
        with self.bring_up_nginx() as nginx:
            reports_request.await_request(self.upstream)
            self.assertEqual((200, 'Info'), reports_request.perform_request(nginx))

    @pytest.mark.parametrize("reports_request", reports_requests())
    def test_report_endpoint_checks_incorrect_signature(self, reports_request):
        with self.bring_up_nginx() as nginx:
            reports_request.params.invalidate_signature()
            self.assertEqual((403, 'Forbidden'), reports_request.perform_request(nginx))

    @pytest.mark.parametrize("reports_request", reports_requests())
    def test_report_endpoint_checks_outdated_timestamp(self, reports_request):
        self.set_env_variable(
            'AUTOMOTIVE_PERFORM_OUTDATED_TIMESTAMP_CHECK',
            None)
        with self.bring_up_nginx() as nginx:
            self.assertEqual(
                (403, 'Forbidden: Timestamp difference is too big'),
                reports_request.perform_request(nginx))

    @pytest.mark.parametrize("reports_request", reports_requests())
    def test_report_endpoint_checks_timestamp_exists(self, reports_request):
        with self.bring_up_nginx() as nginx:
            reports_request.params.pop_timestamp()
            self.assertEqual((403, 'Forbidden'), reports_request.perform_request(nginx))

    def test_duplicate_head_id_is_forbidden(self):
        with self.bring_up_nginx() as nginx:
            request = UploadReportRequest(REPORT_DATA)
            request.params.uri += "&head_id=123456ABCDEF"
            self.assertEqual((400, 'Invalid head_id parameter'), request.perform_request(nginx))
