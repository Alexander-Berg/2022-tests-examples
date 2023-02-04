import unittest
import mock

from six.moves import http_client as httplib

from yandex.maps.wiki.tasks import grinder

FAKE_GRINDER_HOST = 'fake.grinder.host'

CRON_TASK_NAME = 'tasks_validation_123_1'

TYPE = 'submit-editor-task'
SOME_VALUE = 'some_value'
CRON_EXPR = '45 12 * * 1,2'


class MockResponse:
    def __init__(self, status, reason=None, body=None):
        self.status = status
        self.reason = reason
        self.body = body

    def read(self):
        return self.body


@mock.patch('six.moves.http_client.HTTPConnection', autospec=True)
class TestGrinderCrontab(unittest.TestCase):
    '''Test Grinder crontab api'''

    def setUp(self):
        self.gateway = grinder.GrinderGateway(FAKE_GRINDER_HOST)

    def test_get_crontabs(self, mock_conn_class):
        response_body = """["tasks_validation_123_1", "tasks_validation_123_2"]"""

        conn_instance = mock_conn_class.return_value
        conn_instance.getresponse.return_value = MockResponse(httplib.OK, body=response_body)

        self.assertEqual(self.gateway.crontabs(),
                         ["tasks_validation_123_1", "tasks_validation_123_2"])

    def test_get_crontabs_empty(self, mock_conn_class):
        conn_instance = mock_conn_class.return_value
        conn_instance.getresponse.return_value = MockResponse(httplib.OK, body="[]")

        self.assertEqual(self.gateway.crontabs(), [])

    def test_get_crontabs_error(self, mock_conn_class):
        conn_instance = mock_conn_class.return_value
        conn_instance.getresponse.return_value = MockResponse(httplib.INTERNAL_SERVER_ERROR,
                                                              reason='Some reason')

        with self.assertRaises(grinder.GrinderError):
            self.gateway.crontabs()

    def test_get_crontab(self, mock_conn_class):
        response_body = """{"args":{"type":"submit-editor-task","some_param":"some_value"},
                            "cron_expr":"45 12 * * 1,2"}"""

        conn_instance = mock_conn_class.return_value
        conn_instance.getresponse.return_value = MockResponse(httplib.OK, body=response_body)

        self.assertEqual(self.gateway.crontab(CRON_TASK_NAME),
                         {'args': {'type': TYPE, 'some_param': SOME_VALUE},
                          'cron_expr': CRON_EXPR})

    def test_get_crontab_not_found(self, mock_conn_class):
        conn_instance = mock_conn_class.return_value
        conn_instance.getresponse.return_value = MockResponse(httplib.NOT_FOUND)

        with self.assertRaises(grinder.CrontabNotFound):
            self.gateway.crontab(CRON_TASK_NAME)

    def test_get_crontab_error(self, mock_conn_class):
        conn_instance = mock_conn_class.return_value
        conn_instance.getresponse.return_value = MockResponse(httplib.INTERNAL_SERVER_ERROR,
                                                              reason='Some reason')

        with self.assertRaises(grinder.GrinderError):
            self.gateway.crontab(CRON_TASK_NAME)

    def test_put_crontab(self, mock_conn_class):
        conn_instance = mock_conn_class.return_value
        conn_instance.getresponse.return_value = MockResponse(httplib.OK)

        args = {'type': TYPE, 'some_param': SOME_VALUE}

        result = self.gateway.put_crontab(CRON_TASK_NAME, args, CRON_EXPR)

        self.assertEqual(result, {'args': args, 'cron_expr': CRON_EXPR})

    def test_put_crontab_error(self, mock_conn_class):
        conn_instance = mock_conn_class.return_value
        conn_instance.getresponse.return_value = MockResponse(httplib.INTERNAL_SERVER_ERROR,
                                                              reason='Some reason')

        args = {'type': TYPE, 'some_param': SOME_VALUE}

        with self.assertRaises(grinder.GrinderError):
            self.gateway.put_crontab(CRON_TASK_NAME, args, CRON_EXPR)

    def test_delete_crontab_not_found(self, mock_conn_class):
        conn_instance = mock_conn_class.return_value
        conn_instance.getresponse.return_value = MockResponse(httplib.NOT_FOUND)

        with self.assertRaises(grinder.CrontabNotFound):
            self.gateway.delete_crontab(CRON_TASK_NAME)

    def test_delete_crontab_error(self, mock_conn_class):
        conn_instance = mock_conn_class.return_value
        conn_instance.getresponse.return_value = MockResponse(httplib.INTERNAL_SERVER_ERROR,
                                                              reason='Some reason')

        with self.assertRaises(grinder.GrinderError):
            self.gateway.delete_crontab(CRON_TASK_NAME)

if __name__ == '__main__':
    unittest.main()
