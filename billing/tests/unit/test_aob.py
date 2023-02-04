# -*- coding: utf-8 -*-

import unittest

import mock

from billing.dcs.dcs.compare import aob


class CalculateTotalsTestCase(unittest.TestCase):
    @staticmethod
    def analyze(issue=None):
        analyzer = aob.CalculateTotalsAutoAnalyzer(None, None, None)
        analyzer.analyze(issue)

    @staticmethod
    def patch(target, *args, **kwargs):
        path = 'billing.dcs.dcs.compare.aob.CalculateTotalsAutoAnalyzer'
        return mock.patch('{}.{}'.format(path, target), *args, **kwargs)

    @classmethod
    def patch_property(cls, target, *args, **kwargs):
        return cls.patch(target, new_callable=mock.PropertyMock,
                         *args, **kwargs)

    def perform_test(self, total_sum, min_threshold=None, max_threshold=None):
        self.assertIsNotNone(min_threshold or max_threshold)

        with self.patch_property('diffs_count', return_value=1), \
                self.patch('get_total_sum', return_value=total_sum), \
                self.patch_property('min_threshold',
                                    return_value=min_threshold), \
                self.patch_property('max_threshold',
                                    return_value=max_threshold):
            issue = mock.MagicMock()

            self.analyze(issue)

            comment_text = issue.comments.create.call_args[-1]['text']

            def assertIn(text):
                self.assertIn(text, comment_text)

            def assertNotIn(text):
                self.assertNotIn(text, comment_text)

            def s(sum_):
                return u' {} руб.'.format(sum_)

            assertIn(s(total_sum))

            if total_sum < min_threshold:
                assertIn(s(min_threshold))
                assertIn(u'не превышает')

                assertNotIn(s(max_threshold))
                assertNotIn(u'. превышает')
            elif total_sum >= max_threshold:
                assertIn(s(max_threshold))
                assertIn(u'. превышает')

                assertNotIn(s(min_threshold))
                assertNotIn(u'не превышает')
            else:
                assertIn(s(min_threshold))
                assertIn(s(max_threshold))

                assertIn(u'. превышает')
                assertIn(u'не превышает')

    def test_no_diffs(self):
        with self.patch('get_total_sum') as get_total_sum_mock:
            with self.patch_property('diffs_count', return_value=0):
                self.analyze()
            get_total_sum_mock.assert_not_called()

    def test_min_not_exceeded(self):
        self.perform_test(1, 10, 100)

    def test_min_exceeded_max_not_exceeded(self):
        self.perform_test(10, 10, 100)
        self.perform_test(50, 10, 100)

    def test_max_exceeded(self):
        self.perform_test(100, 10, 100)
        self.perform_test(200, 10, 100)

# vim:ts=4:sts=4:sw=4:tw=79:et:
