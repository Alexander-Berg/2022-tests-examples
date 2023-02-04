
from wiki.notifications.generators.base import EventTypes
from wiki.notifications.models import PageEvent
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class RequestOwnershipViewTest(BaseApiTestCase):
    def setUp(self):
        super(RequestOwnershipViewTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic
        self.page = self.create_page(supertag='test')

    def _make_and_check_response(self, data, expected_status_code=200):
        url = '{api_url}/test/.request_ownership'.format(api_url=self.api_url)
        response = self.client.put(url, data)
        self.assertEqual(expected_status_code, response.status_code)

    def _check_event(self, expected_meta=None):
        events = list(
            PageEvent.objects.filter(page=self.page, event_type=EventTypes.request_ownership, author=self.user)
        )

        if expected_meta is not None:
            self.assertEqual(1, len(events))
            self.assertEqual(expected_meta, events[0].meta)
        else:
            self.assertEqual(0, len(events))

    def test(self):
        self._make_and_check_response({'reason': 'blabla'})
        self._check_event({'root_page_only': False, 'reason': 'blabla'})

    def test_root_page_only(self):
        self._make_and_check_response({'reason': 'blabla', 'root_page_only': True})
        self._check_event({'root_page_only': True, 'reason': 'blabla'})

    def test_no_reason(self):
        self._make_and_check_response({}, expected_status_code=409)
        self._check_event()
