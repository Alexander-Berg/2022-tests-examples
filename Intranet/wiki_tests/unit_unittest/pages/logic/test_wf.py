from pretend import stub

from wiki.pages.logic.wf import format_comment
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class WikiFormatterLogicTestCase(BaseApiTestCase):
    def test_format_comment(self):
        page = stub(url='/SomeCommentedСтраница')
        comment = stub(body=None, page=page)
        comment.body = '**комент**\nлови момент'
        json = format_comment(comment)
        expected = {
            'block': 'wiki-doc',
            'wiki-attrs': {},
            'content': [
                {
                    'block': 'wiki-txt',
                    'wiki-attrs': {
                        'txt': '**комент**',
                        'pos_start': 0,
                        'pos_end': 10,
                    },
                }, {
                    'block': 'wiki-br',
                    'wiki-attrs': {
                        'pos_start': 10,
                        'pos_end': 11,
                    },
                }, {
                    'block': 'wiki-txt',
                    'wiki-attrs': {
                        'txt': 'лови момент',
                        'pos_start': 11,
                        'pos_end': 22,
                    },
                }],
        }
        self.assertEqual(json, expected)
