import pytest

from service_test_base import ServiceTestBase
from lib.request_params import RequestParams


class TestTasks(ServiceTestBase):
    def setup(self):
        super(TestTasks, self).setup()
        self.add_locations_from_file('tasks.service.conf')

    def test_tasks_endpoint_returns_ok(self, request):
        with self.bring_up_nginx() as nginx:
            uri = (
                '/tasks/1.x/get_available_task?' +
                'head_id={head_id}&device_id={device_id}&uuid={uuid}'
            ).format(
                head_id=RequestParams.HEAD_ID,
                device_id=RequestParams.DEVICE_ID,
                uuid=RequestParams.UUID,
            )
            self.assertEqual((200, ''), nginx.get(uri))

    @pytest.mark.parametrize("kind", ['result', 'error'])
    def test_upload_endpoint_returns_ok(self, kind):
        with self.bring_up_nginx() as nginx:
            uri = (
                '/tasks/1.x/upload_{kind}?' +
                'head_id={head_id}&device_id={device_id}&uuid={uuid}&task_id={task_id}'
            ).format(
                kind=kind,
                head_id=RequestParams.HEAD_ID,
                device_id=RequestParams.DEVICE_ID,
                uuid=RequestParams.UUID,
                task_id=RequestParams.TASK_ID,
            )
            body = 'Report contents'

            self.assertEqual((200, ''), nginx.post(uri, body))
