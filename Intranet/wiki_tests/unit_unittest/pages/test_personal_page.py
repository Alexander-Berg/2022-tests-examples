from unittest import skipIf

from django.conf import settings

from wiki.pages.models import Revision
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase


class PersonalPageTest(BaseTestCase):

    create_user_clusters = True

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_revision_is_created_on_cluster_creation(self):
        alice = self.get_or_create_user('alice')

        revisions = Revision.objects.filter(author=alice)
        self.assertEqual(revisions.count(), 2)

    def test_create_personal_cluster__last_author__with_redirect(self):
        """EBI-1048"""
        user = self.get_or_create_user('user-login@yandex-team.ru')

        for page in user.page_set.all():
            self.assertEqual(page.last_author, user, msg=page.slug)
