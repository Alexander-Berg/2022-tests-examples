import mock
import ujson as json
from django.contrib.auth import get_user_model
from django.test import override_settings

from wiki.async_process.consts import AsyncTaskStatus
from wiki.async_process.tasks.async_request_processor import async_request_processor, delay_async_request
from wiki.pages.cluster import Cluster
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

User = get_user_model()


class APIAsyncMoveTest(BaseApiTestCase):
    """
    Тесты на перемещение отдельных страниц и кластеров в API
    """

    def setUp(self):
        super(APIAsyncMoveTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.cluster = Cluster('thasonic')
        self.user = User.objects.get(username='thasonic')
        self.dest_page = self.create_page(tag='dest', supertag='dest')
        self.src = self.create_page(tag='src', supertag='src')
        self.src_child = self.create_page(tag='src/child', supertag='src/child')

    def create_page(self, **kwargs):
        if 'body' not in kwargs:
            kwargs['body'] = 'example text'  # to create wom0, wom1, body
        return super(APIAsyncMoveTest, self).create_page(**kwargs)

    def check_task_status(self, task_id):
        response = self.client.post(
            '{api_url}/.async'.format(
                api_url=self.api_url,
            ),
            data={'task_id': task_id},
        )
        return response.status_code, response.json()

    @override_settings(CELERY_TASK_ALWAYS_EAGER=False, CELERY_ALWAYS_EAGER=False)
    def _move_async(self, args, crash_it=False):
        """
        К сожалению, селери настолько плох для тестирования, что тут не остается ничего кроме как мокать все подряд
         - если CELERY_TASK_ALWAYS_EAGER = True, то не будет работаь AsyncResult
         - если CELERY_TASK_ALWAYS_EAGER = False, то не будет воркер
        предлагается запускать в отдельном треде воркер селери и ждать в блокирующем режиме, но по мне это очень плохо
        """

        with mock.patch('wiki.api_frontend.views.move.delay_async_request', side_effect=delay_async_request) as m:
            request_url = '{api_url}/{supertag}/.move_async'.format(
                api_url=self.api_url,
                supertag=self.src.supertag,
            )
            response = self.client.post(request_url, data=args)
            self.assertEqual(response.status_code, 201)

            task_id = json.loads(response.content)['data']['task_id']

            s_c, content = self.check_task_status(task_id)

            self.assertEqual(s_c, 200)  # celery is off -- nothing worked
            self.assertEqual(
                content['data']['status'], AsyncTaskStatus.PROCESSING.value
            )  # celery is off -- nothing worked

            result = async_request_processor(**m.call_args.kwargs)
            mock_async_result = mock.MagicMock()
            mock_async_result.get = mock.MagicMock(return_value=result)
            mock_async_result.status = 'SUCCESS'

            with mock.patch('wiki.async_process.views.get_task_result', return_value=mock_async_result):
                return self.check_task_status(task_id)

    def test_move_async_success(self):
        status_code, response = self._move_async(
            {
                'destination': 'dest/src',
                'move_cluster': True,
            }
        )
        self.assertEqual(status_code, 200)
        self.assertEqual(response['data']['status'], AsyncTaskStatus.SUCCESS.value)
        self.src.refresh_from_db()
        self.src_child.refresh_from_db()
        self.assertEqual(self.src.supertag, 'dest/src')
        self.assertEqual(self.src_child.supertag, 'dest/src/child')

    def test_move_async_fail(self):
        self.client.login('kolomeetz')
        self._move_async(
            {
                'destination': 'dest/src',
                'move_cluster': True,
            },
            crash_it=True,
        )
