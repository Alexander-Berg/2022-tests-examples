from unittest import skipIf

from django.conf import settings
from django.contrib.auth import get_user_model

from wiki.pages.models import Page, PageLink
from wiki.pages.utils.resurrect import resurrect_page
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get


class RestorePages(BaseTestCase):
    def setUp(self):
        super(RestorePages, self).setUp()

        self.setUsers()

    def create_test_page(self):

        # create test page w/ redirects
        self.client.login(self.user_thasonic.username)
        self.gorilla_page = self.create_page(tag='Килла/Горилла')

        self.create_redirects()
        self.create_links()

        # mark created page, redirects and links removed.
        self.client.delete('/_api/frontend/%s' % self.gorilla_page.supertag)

    def create_redirects(self):
        badman = get(get_user_model(), username='badman')
        self.redirect_page1 = self.create_page(tag='redirectahr1', authors_to_add=[badman])
        self.redirect_page1.redirects_to = self.gorilla_page
        self.redirect_page1.save()

        self.redirect_page2 = self.create_page(tag='redirectah2')
        self.redirect_page2.redirects_to = self.gorilla_page

        self.redirect_page3 = self.create_page(tag='redirectah3')
        self.redirect_page3.body = 'text {{redirect page="/killa/gorilla"}} text'
        self.redirect_page3.redirects_to = self.gorilla_page
        self.redirect_page3.save()

    def create_links(self):
        PageLink(from_page=self.redirect_page2, to_page=self.gorilla_page).save()

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_resurrect(self):
        self.create_test_page()
        # get removed page
        page = Page.objects.get(pk=self.gorilla_page.id)

        # TODO: rewrite this so it'll use actual handler when there will be one
        resurrect_page(page)

        page = Page.objects.get(pk=self.gorilla_page.id)

        # page attributes restored
        self.assertEqual(page.tag, 'Килла/Горилла')
        self.assertEqual(page.supertag, 'killa/gorilla')
        self.assertTrue(page.is_active)

        # redirects are active too
        for redirect in Page.objects.filter(tag__in=('redirectah1', 'redirectah2', 'redirectah3')):
            self.assertTrue(redirect.is_active)

        # and links are present
        self.assertEqual(PageLink.objects.filter(to_page=page).count(), 1)
