from mock import patch

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from wiki.notifications.models import PageEvent


class PageReadonlyModeTest(BaseApiTestCase):
    handler = 'readonly'

    def setUp(self):
        super(PageReadonlyModeTest, self).setUp()
        self.setUsers()
        self.login('neofelis')

        self.page1 = self.create_page(tag='one', body='one body')
        self.page2 = self.create_page(tag='two', body='two body')
        self.page11 = self.create_page(tag='one/one', body='one/one body')
        self.page12 = self.create_page(tag='one/two', body='one/two body')
        self.default_page = self.page1

    def test_get_readonly_mode(self):
        data = self.get(status=200)
        self.assertFalse(data['is_readonly'])

    @patch('wiki.pages.access.is_admin', lambda x: True)
    def test_set_readonly_mode_for_page(self):
        params = {'is_readonly': True, 'for_cluster': False}
        self.post(status=200, data=params)

        data = self.get(status=200)
        self.assertTrue(data['is_readonly'])

        last_event = list(PageEvent.objects.filter(page_id=self.page1.id).order_by('created_at'))[-1]
        self.assertEqual(last_event.event_type, PageEvent.EVENT_TYPES.enable_page_readonly)

        data = self.get(page=self.page11, status=200)
        self.assertFalse(data['is_readonly'])

        data = self.get(page=self.page2, status=200)
        self.assertFalse(data['is_readonly'])

        params = {'is_readonly': False, 'for_cluster': False}
        self.post(status=200, data=params)

        data = self.get(status=200)
        self.assertFalse(data['is_readonly'])

        last_event = list(PageEvent.objects.filter(page_id=self.page1.id).order_by('created_at'))[-1]
        self.assertEqual(last_event.event_type, PageEvent.EVENT_TYPES.disable_page_readonly)

    @patch('wiki.pages.access.is_admin', lambda x: True)
    def test_set_readonly_mode_for_cluster(self):
        params = {'is_readonly': True, 'for_cluster': True}
        self.post(status=200, data=params)

        data = self.get(status=200)
        self.assertTrue(data['is_readonly'])

        last_event = list(PageEvent.objects.filter(page_id=self.page1.id).order_by('created_at'))[-1]
        self.assertEqual(last_event.event_type, PageEvent.EVENT_TYPES.enable_page_readonly)

        data = self.get(page=self.page11, status=200)
        self.assertTrue(data['is_readonly'])

        last_event = list(PageEvent.objects.filter(page_id=self.page11.id).order_by('created_at'))[-1]
        self.assertEqual(last_event.event_type, PageEvent.EVENT_TYPES.enable_page_readonly)

        data = self.get(page=self.page12, status=200)
        self.assertTrue(data['is_readonly'])

        last_event = list(PageEvent.objects.filter(page_id=self.page12.id).order_by('created_at'))[-1]
        self.assertEqual(last_event.event_type, PageEvent.EVENT_TYPES.enable_page_readonly)

        data = self.get(page=self.page2, status=200)
        self.assertFalse(data['is_readonly'])

    @patch('wiki.pages.access.is_admin', lambda x: False)
    def test_access_to_change_readonly_mode(self):
        """
        Не-админ не должен иметь прав менять
        :return:
        """
        params = {'is_readonly': True, 'for_cluster': False}
        self.post(status=403, data=params)

        params = {'is_readonly': False, 'for_cluster': True}
        self.post(status=403, data=params)
