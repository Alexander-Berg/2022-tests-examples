import ujson as json

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class AuthorsViewTest(BaseApiTestCase):
    """
    Тесты для AuthorsView.
    """

    def setUp(self):
        super(AuthorsViewTest, self).setUp()
        self.setUsers()
        self.page = self.create_page(authors_to_add=[self.user_thasonic])
        self.page.authors.add(self.user_kolomeetz)
        self.client.login(self.user_thasonic.username)

    def get_url(self, page=None, placeholders=None, query_params=None):
        return '/'.join(
            [
                self.api_url,
                page.supertag,
                '.authors',
            ]
        )

    def test_get_authors(self):
        url = self.get_url(self.page)
        response = self.client.get(url)

        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.content)
        authors = response_data['data']['authors']
        self.assertEqual(len(authors), 2)
        self.assertTrue(self.user_thasonic.username in [author['login'] for author in authors])
        self.assertTrue(self.user_kolomeetz.username in [author['login'] for author in authors])

    def test_empty_authors(self):
        url = self.get_url(self.page)
        response = self.client.post(url, {'authors': []})

        self.assertEqual(response.status_code, 409)

    def test_change_authors(self):
        url = self.get_url(self.page)
        response = self.client.post(url, {'authors': [self.user_chapson.staff.uid, self.user_volozh.staff.uid]})

        self.assertEqual(response.status_code, 200)
        page = self.refresh_objects(self.page)
        authors = page.authors.all()
        self.assertEqual(len(authors), 2)
        self.assertTrue(self.user_chapson.username in [author.username for author in authors])
        self.assertTrue(self.user_volozh.username in [author.username for author in authors])

    def test_change_authors_with_subpages(self):
        self.subpage1 = self.create_page(
            tag='{}/subpage1'.format(self.page.tag), authors_to_add=[self.user_thasonic, self.user_kolomeetz]
        )
        self.subpage2 = self.create_page(
            tag='{}/subpage2'.format(self.page.tag),
            authors_to_add=[self.user_thasonic, self.user_kolomeetz, self.user_chapson],
        )
        self.subpage3 = self.create_page(
            tag='{}/subpage3'.format(self.page.tag), authors_to_add=[self.user_thasonic, self.user_chapson]
        )

        # authors:
        # page      [thasonic, kolomeetz]           -> [chapson, volozh]
        # subpage1  [thasonic, kolomeetz]           -> [chapson, volozh]
        # subpage2  [thasonic, kolomeetz, chapson]  -> [chapson, volozh]
        # subpage3  [thasonic, chapson]             -> [thasonic, chapson]
        url = self.get_url(self.page)
        response = self.client.post(
            url, {'authors': [self.user_chapson.staff.uid, self.user_volozh.staff.uid], 'with_subpages': True}
        )

        self.assertEqual(response.status_code, 200)

        page = self.refresh_objects(self.page)
        authors = page.authors.all()
        self.assertEqual(len(authors), 2)
        self.assertTrue(self.user_chapson.username in [author.username for author in authors])
        self.assertTrue(self.user_volozh.username in [author.username for author in authors])

        subpage1 = self.refresh_objects(self.subpage1)
        authors = subpage1.authors.all()
        self.assertEqual(len(authors), 2)
        self.assertTrue(self.user_chapson.username in [author.username for author in authors])
        self.assertTrue(self.user_volozh.username in [author.username for author in authors])

        subpage2 = self.refresh_objects(self.subpage2)
        authors = subpage2.authors.all()
        self.assertEqual(len(authors), 2)
        self.assertTrue(self.user_chapson.username in [author.username for author in authors])
        self.assertTrue(self.user_volozh.username in [author.username for author in authors])

        subpage3 = self.refresh_objects(self.subpage3)
        authors = subpage3.authors.all()
        self.assertEqual(len(authors), 2)
        self.assertTrue(self.user_chapson.username in [author.username for author in authors])
        self.assertTrue(self.user_thasonic.username in [author.username for author in authors])

    def test_change_authors_for_page_readonly(self):
        params = {'is_readonly': True, 'for_cluster': False}
        response = self.client.post(f'{self.api_url}/{self.page.supertag}/.readonly', data=params)
        self.assertEqual(200, response.status_code)

        self.client.login('chapson')
        url = self.get_url(self.page)
        response = self.client.post(url, {'authors': [self.user_chapson.staff.uid, self.user_volozh.staff.uid]})
        self.assertEqual(response.status_code, 403)

    def test_change_authors__remove_owner__sub_pages(self):

        self.page.authors.set([self.user_thasonic, self.user_kolomeetz])

        self.subpage1 = self.create_page(tag=f'{self.page.tag}/subpage1', owner=self.user_kolomeetz)
        self.subpage1.authors.set([self.user_thasonic, self.user_kolomeetz, self.user_chapson, self.user_asm])

        self.subpage2 = self.create_page(tag=f'{self.page.tag}/subpage2', owner=self.user_asm)
        self.subpage2.authors.set([self.user_thasonic, self.user_kolomeetz, self.user_asm])

        # change [thasonic, kolomeetz] to [volozh]
        url = self.get_url(self.page)
        response = self.client.post(url, {'authors': [self.user_volozh.staff.uid], 'with_subpages': True})
        self.assertEqual(response.status_code, 200)

        page, subpage1, subpage2 = self.refresh_objects(self.page, self.subpage1, self.subpage2)

        # change to single
        self.assertEqual(set(page.authors.all()), {self.user_volozh})
        self.assertEqual(page.owner, self.user_volozh)

        # change to first
        self.assertEqual(set(subpage1.authors.all()), {self.user_chapson, self.user_volozh, self.user_asm})
        self.assertEqual(subpage1.owner, subpage1.authors.first())

        # no change
        self.assertEqual(set(subpage2.authors.all()), {self.user_volozh, self.user_asm})
        self.assertEqual(subpage2.owner, self.user_asm)

    def test_change_authors__remove_owner__order(self):
        url = self.get_url(self.page)

        authors = [self.user_volozh, self.user_kolomeetz, self.user_asm]
        authors.sort(key=lambda x: x.id)

        # first author
        response = self.client.post(url, {'authors': [x.staff.uid for x in authors]})
        self.assertEqual(response.status_code, 200)

        page = self.refresh_objects(self.page)
        self.assertEqual(page.owner, authors[0])

        # second, because first dismissed
        authors[0].staff.is_dismissed = True
        authors[0].staff.save()
        self.page.owner = self.user_thasonic
        self.page.save()
        self.page.authors.set([self.user_thasonic])

        response = self.client.post(url, {'authors': [x.staff.uid for x in authors]})
        self.assertEqual(response.status_code, 200)

        page = self.refresh_objects(self.page)
        self.assertEqual(page.owner, authors[1])

        # first, because all dismissed
        authors[1].staff.is_dismissed, authors[2].staff.is_dismissed = True, True
        authors[1].staff.save()
        authors[2].staff.save()
        self.page.owner = self.user_thasonic
        self.page.save()
        self.page.authors.set([self.user_thasonic])

        response = self.client.post(url, {'authors': [x.staff.uid for x in authors]})
        self.assertEqual(response.status_code, 200)

        page = self.refresh_objects(self.page)
        self.assertEqual(page.owner, authors[0])
