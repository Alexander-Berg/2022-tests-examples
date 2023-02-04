
from unittest import skipIf

from django.conf import settings
from django.contrib.auth import get_user_model

from wiki.intranet.models import Staff
from wiki.pages.access import get_bulk_raw_access, interpret_raw_access
from wiki.pages.models import Page
from wiki.personalisation.quick_notes import supertag_of_user_notes
from wiki.personalisation.user_cluster import is_in_user_cluster
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get

User = get_user_model()


class UserNotesPageTest(BaseTestCase):

    has_personal_cluster = False

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_user_note_page_created(self):
        # Личный кластер пользователя и кластер для заметок должен быть создан при первом обращении пользователя
        staff = get(Staff, login='thasonic', uid=10070)
        self.user = User.objects.create_user('thasonic', 'thasonic@yandex-team.ru')
        staff.user = self.user
        staff.save()
        self.client.login(staff.login)

        query_set = Page.objects.filter(supertag='users/%s' % staff.login)
        self.assertEqual(len(query_set), 0)

        request_url = '/_api/frontend/{page_supertag}'.format(page_supertag='users/%s' % staff.login)
        self.client.get(request_url)

        user_cluster = Page.objects.get(supertag='users/%s' % staff.login)
        pages = Page.objects.filter(supertag=supertag_of_user_notes(user_cluster))
        self.assertEqual(pages.count(), 1)
        self.assertEqual(pages[0].revision_set.all().count(), 1)
        access_status = interpret_raw_access(get_bulk_raw_access(pages)[pages[0]])

        self.assertEqual(access_status['is_owner'], True)


class UserClusterTest(BaseTestCase):

    create_user_clusters = True

    def test_user_cluster(self):
        self.setUsers()
        page_1 = Page.objects.get(supertag='users/thasonic')
        page_2 = self.create_page(tag='users/thasonic/killa', body='Thasonic\'s personal cluster')
        page_3 = self.create_page(tag='thasonic', body='Thasonic\'s personal cluster')
        page_4 = self.create_page(tag='thasonic/killa', body='Thasonic\'s personal cluster')
        page_5 = self.create_page(tag='AleksandrPokatilov', body='Thasonic\'s personal cluster')
        page_6 = self.create_page(tag='АлександрПокатилов/killa', body='Thasonic\'s personal cluster')
        page_7 = self.create_page(tag='AleksandrPokatilov/gorilla', body='Thasonic\'s personal cluster')
        foreign_page = self.create_page(tag='killa/gorilla', body='Not my page')

        self.assertTrue(is_in_user_cluster(page_1))
        self.assertTrue(is_in_user_cluster(page_2))
        self.assertTrue(is_in_user_cluster(page_3))
        self.assertTrue(is_in_user_cluster(page_4))
        self.assertTrue(is_in_user_cluster(page_5))
        self.assertTrue(is_in_user_cluster(page_6))
        self.assertTrue(is_in_user_cluster(page_7))
        self.assertFalse(is_in_user_cluster(foreign_page))
