from django.test.utils import override_settings

from wiki.files.logic import should_download
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class FileLogicTestCase(BaseApiTestCase):
    @override_settings(IS_BUSINESS=True)
    def test_should_download(self):
        """
        Тестируем только для biz, потому что планируется,
        что везде будет поведение как в biz.
        """
        true_test_cases = [
            ('file.html', '0'),
            ('file.html', '1'),
            ('file.shtml', '0'),
            ('file.shtml', '1'),
            ('file.htm', '1'),
            ('file.htm', '0'),
            ('file.htm', '0'),
            ('file.php', '0'),
            ('file.php', '1'),
            ('file.shit', '1'),
            ('file.shit_', '0'),
            ('file.shit_', '1'),
            ('file', '0'),
            ('file', '1'),
            ('file.png', '1'),
            ('file.swf', '1'),
            ('file.jpeg', '1'),
            ('file.gif', '1'),
        ]

        for case, request_get in true_test_cases:
            self.assertTrue(should_download(case) or request_get == '1', msg='Failed {0} {1}'.format(*case))

        false_test_cases = [
            ('file.png', '0'),
            ('file.jpeg', '0'),
            ('file.gif', '0'),
        ]

        for case, request_get in false_test_cases:
            self.assertFalse(should_download(case) or request_get == '1', msg='Failed {0} {1}'.format(*case))
