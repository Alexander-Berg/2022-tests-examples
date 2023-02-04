from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from wiki.utils.tracker import IssueProcessor


class ProcessDurationTest(BaseApiTestCase):

    GIVEN_EXPECTED = (
        ('P3W', '3w'),
        ('P4D', '4d'),
        ('PT5H', '5h'),
        ('PT42M', '42m'),
        ('P3W4D', '3w 4d'),
        ('P3W4DT5H1M', '3w 4d 5h 1m'),
        ('BRABUBRABU', None),
        ('P3X', None),
    )

    def test_process_duration(self):
        for given, expected in self.GIVEN_EXPECTED:
            processed = IssueProcessor.process_duration(given)
            self.assertEqual(processed, expected)
