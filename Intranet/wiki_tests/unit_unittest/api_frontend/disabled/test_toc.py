# from ujson import dumps, loads
# from wiki_tests.common.unittest_base import BaseApiTestCase
#
#
# class PageTocTest(BaseApiTestCase):
#     def setUp(self):
#         super(PageTocTest, self).setUp()
#         self.setUsers()
#         self.client.login('thasonic')
#         self.user = self.user_thasonic
#
#     def test_error404(self):
#         """
#         404
#         """
#         response = self.client.get('{api_url}/NonExistentPage/.toc'.format(api_url=self.api_url))
#
#         json = loads(response.content)
#         self.assertTrue('error' in json)
#         status_code = response.status_code
#         self.assertEqual(status_code, 404)
#
#     def test_empty_toc(self):
#         page = self.create_page(tag='Страница', body='this is my first page')
#         request_url = '{api_url}/{page_supertag}/.toc'.format(api_url=self.api_url, page_supertag=page.supertag)
#         response = self.client.get(request_url)
#         self.assertEqual(200, response.status_code)
#
#         page_toc_data = loads(response.content)['data']
#         self.assertTrue('toc' in page_toc_data)
#         self.assertTrue('content' in page_toc_data['toc'])
#         self.assertTrue('content' not in page_toc_data['toc']['content'][0])
#         expected = '{"toc":{"content":[{"wiki-attrs":{"page":"/Stranica"},"block":"wiki-toc"}],' \
#                    '"wiki-attrs":{"lang":"en","path":"/Stranica","mode":"view"},"block":"wiki-doc"}}'
#         self.assertEqual(expected, dumps(page_toc_data))
#
#     def test_simple_toc(self):
#         page = self.create_page(
#             tag='Страница', body="""
# == item1
# sadfasdfasdfasdf asdfasdf
#
# === item21
# asdfasdfasdfasdfasd edasdf
#
# === item22
# asdedfgb rgsfg grt
# """
#         )
#         request_url = '{api_url}/{page_supertag}/.toc'.format(api_url=self.api_url, page_supertag=page.supertag)
#
#         response = self.client.get(request_url)
#
#         self.assertEqual(200, response.status_code)
#
#         page_toc_data = loads(response.content)['data']
#         self.assertTrue('toc' in page_toc_data)
#         self.assertTrue('content' in page_toc_data['toc'])
#         self.assertTrue('content' in page_toc_data['toc']['content'][0])
#         tocitems = page_toc_data['toc']['content'][0]['content']
#         self.assertTrue(3, len(tocitems))
#         self.assertTrue('block' in tocitems[0])
#         self.assertTrue('wiki-tocitem', tocitems[0]['block'])
#         expected = '{"toc":{"content":[{"content":[{"wiki-attrs":{"txt":"item1","anchor":"item1","level":1},' \
#                    '"block":"wiki-tocitem"},{"wiki-attrs":{"txt":"item21","anchor":"item21","level":2},' \
#                    '"block":"wiki-tocitem"},{"wiki-attrs":{"txt":"item22","anchor":"item22","level":2},' \
#                    '"block":"wiki-tocitem"}],"wiki-attrs":{"page":"/Stranica"},"block":"wiki-toc"}],' \
#                    '"wiki-attrs":{"lang":"en","path":"/Stranica","mode":"view"},"block":"wiki-doc"}}'
#         self.assertEqual(expected, dumps(page_toc_data))
