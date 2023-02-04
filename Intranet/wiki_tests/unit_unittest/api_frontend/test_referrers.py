
from wiki.pages.models import PageLink, Referer
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class ReferrersViewTest(BaseApiTestCase):
    def setUp(self):
        super(ReferrersViewTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    def _set_referrers(self, to_page, referer, ref_count):
        for i in range(ref_count):
            Referer(page=to_page, referer=referer).save()

    def test(self):
        to_page = self.create_page(tag='to_page')
        from_page_1 = self.create_page(tag='ОтСтраницы1')
        from_page_2 = self.create_page(tag='ОтСтраницы2')

        PageLink(from_page=from_page_1, to_page=to_page).save()
        PageLink(from_page=from_page_2, to_page=to_page).save()

        self._set_referrers(to_page, 'http://search.yandex-team.ru/search?text=blabla', 3)
        self._set_referrers(to_page, 'http://search.yandex-team.ru/search?text=foo', 2)

        url = '{api_url}/to_page/.referrers'.format(api_url=self.api_url)

        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)

        data = response.data['data']

        self.assertEqual(
            {
                'wiki_pages': [
                    {
                        'url': '/otstranicy1',
                        'tag': 'ОтСтраницы1',
                    },
                    {
                        'url': '/otstranicy2',
                        'tag': 'ОтСтраницы2',
                    },
                ],
                'external_pages': [
                    {
                        'url': 'http://search.yandex-team.ru/search?text=blabla',
                        'num': 3,
                    },
                    {
                        'url': 'http://search.yandex-team.ru/search?text=foo',
                        'num': 2,
                    },
                ],
            },
            data,
        )
