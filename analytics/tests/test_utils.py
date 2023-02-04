#! coding: utf-8
"""tests for ta_report.utils"""
from unittest import TestCase


class TestCurrency(TestCase):

    def test_extract_currency(self):
        from ta_report.utils import extract_currency as fn

        self.assertEqual(fn("data_30.tsv"), 30.0, msg="#1")
        self.assertEqual(fn("data.tsv"), 1.0, msg="#2")
        self.assertEqual(fn("data_32.tsv"), 32.0, msg="#3")
        self.assertEqual(fn("data32.tsv"), 1.0, msg="#4")

    def test_extract_str_currency_from_tar(self):
        from ta_report.utils import extract_str_currency_from_tar as fn

        self.assertEqual(fn("issue_Omg-tst.me_RUB.tar.gz"), "RUB", msg="#1")
        with self.assertRaises(ValueError, msg="#3"):
            fn("issue_Omg-tst.me_2,3.tar.gz")
            fn("issue_Omg-tst.me_.tar.gz")

    def test_extract_currency_from_tar(self):
        from ta_report.utils import extract_currency_from_tar as fn

        self.assertEqual(fn("issue_Omg-tst.me_30.tar.gz"), 30, msg="#1")
        self.assertEqual(fn("issue_Omg-tst.me_43.5.tar.gz"), 43.5, msg="#2")
        self.assertEqual(fn("issue_Omg-tst.me_0.5.tar.gz"), 0.5, msg="#3")


class TestPeriod(TestCase):

    def test_extract_period_from_tar(self):
        from datetime import datetime as dt
        from ta_report.utils import extract_period_from_tar as fn

        self.assertEqual(fn("login_cur_2017-10-01-2017-10-15.tar.gz"),
                         [dt.strptime("2017-10-01", "%Y-%m-%d"), dt.strptime("2017-10-15", "%Y-%m-%d")], msg="#1")
        self.assertEqual(fn("login_cur_cur_2017-10-01-2017-10-15.tar.gz"),
                         [dt.strptime("2017-10-01", "%Y-%m-%d"), dt.strptime("2017-10-15", "%Y-%m-%d")], msg="#2")


class TestStrategy(TestCase):
    def test_get_strategy_type(self):
        from ta_report.utils import get_strategy_type as fn

        self.assertEqual(fn({"ShowsRSYA": 100, "ShowsSearch": 10}), "both", msg="#1")
        self.assertEqual(fn({"ShowsRSYA": 100, "ShowsSearch": 0}), "only_rsya", msg="#2")
        self.assertEqual(fn({"ShowsRSYA": 0, "ShowsSearch": 10}), "only_search", msg="#3")
        self.assertEqual(fn({"ShowsRSYA": 0, "ShowsSearch": 0}), "None", msg="#4")

    def test_extract_timetarget(self):
        pass


class TestImageProcess(TestCase):

    def test_classify_imgsize(self):
        from ta_report.utils import classify_imgsize as fn

        self.assertEqual(fn((150, 650)), 'bad_img (<= 450px)', msg="#1")
        self.assertEqual(fn((650, 150)), 'bad_img (<= 450px)', msg="#2")
        self.assertEqual(fn((350, 150)), 'bad_img (<= 450px)', msg="#3")
        self.assertEqual(fn((1650, 550)), 'good_img (>= 1000)', msg="#4")
        self.assertEqual(fn((650, 1550)), 'good_img (>= 1000)', msg="#5")
        self.assertEqual(fn((1350, 1550)), 'good_img (>= 1000)', msg="#6")
        self.assertEqual(fn((650, 550)), 'norm_img (others)', msg="#7")


class TestTextProcess(TestCase):

    def test_phrase_process(self):
        from ta_report.utils import phrase_process as fn

        _assert = fn("  Слова -купить  ") == u"слова"
        self.assertTrue(_assert, msg="#1")
        _assert = fn("  рJзы -купить ~0  ") == u"рjзы"
        self.assertTrue(_assert, msg="#2")
        _assert = fn("  словА-купить  ") == u"слова-купить"
        self.assertTrue(_assert, msg="#3")
        _assert = fn("  слОва   купить  ") == u"купить слова"
        self.assertTrue(_assert, msg="#4")
        _assert = fn("  Слова -купить  ") == u"слова"
        self.assertTrue(_assert, msg="#5")
        _assert = fn(u"  рJзы -купить ~0  ") == u"рjзы"
        self.assertTrue(_assert, msg="#6")
        _assert = fn(u"\"словА-купить\"") == u"слова-купить"
        self.assertTrue(_assert, msg="#7")
        _assert = fn(u"\"  слОва   купить  \"") == u"купить слова"
        self.assertTrue(_assert, msg="#8")

    def test_drop_antiwords(self):
        from ta_report.utils import drop_antiwords as fn

        self.assertEqual(fn(u"слово -минус"), u"слово", msg="#1")
        self.assertEqual(fn(u"слово ~0"), u"слово", msg="#2")
        self.assertEqual(fn(u"  слово  "), u"слово", msg="#3")

    def test_text_to_words(self):
        from ta_report.utils import text_to_words as fn

        self.assertSequenceEqual(fn(u"  продать  - москва  "), [u"продать", "-", u"москва"], msg="#1")
        self.assertSequenceEqual(fn(u"розы"), [u"розы"], msg="#2")
        self.assertSequenceEqual(fn(u"  "), [u""], msg="#3")

    def test_is_uppercase_word(self):
        from ta_report.utils import is_uppercase_word as fn

        with self.assertRaises(Exception) as cm:
            fn("ROSE")
        self.assertIsInstance(cm.exception, TypeError, msg="#1")

        self.assertTrue(fn(u"РОЗЫ"), msg="#2")
        self.assertFalse(fn(u"ROSfE"), msg="#3")
        self.assertFalse(fn(u"розfы"), msg="#4")
        self.assertFalse(fn(u"ро fзы"), msg="#5")
        self.assertFalse(fn(u"РоfЗЫ"), msg="#6")

    def test_has_uppercase_word(self):
        from ta_report.utils import has_uppercase_word as fn

        with self.assertRaises(Exception) as cm:
            fn("ROSE rose")
        self.assertIsInstance(cm.exception, TypeError, msg="#1")

        self.assertTrue(fn(u"РОЗЫ"), msg="#2")
        self.assertTrue(fn(u"ROSE"), msg="#2")
        self.assertTrue(fn(u"buy ROSE in Moscow"), msg="#2")
        self.assertTrue(fn(u"купи розы в МСК"), msg="#2")
        self.assertTrue(fn(u" МСК "), msg="#2")
        self.assertFalse(fn(u" Мск "), msg="#2")
        self.assertFalse(fn(u" Мск  Djq"), msg="#2")

    def test_get_phrase_info(self):
        from ta_report.utils import get_phrase_info as fn

        phrase_info = fn(u'"[купить !слова +в Москве]"')
        self.assertTrue(phrase_info.fixed_phrase, msg="#1")
        self.assertTrue(phrase_info.fixed_stopwords, msg="#1")
        self.assertTrue(phrase_info.fixed_order, msg="#1")
        self.assertTrue(phrase_info.fixed_form, msg="#1")

        phrase_info = fn(u'купить слова в Москве')
        self.assertFalse(phrase_info.fixed_phrase, msg="#2")
        self.assertFalse(phrase_info.fixed_stopwords, msg="#2")
        self.assertFalse(phrase_info.fixed_order, msg="#2")
        self.assertFalse(phrase_info.fixed_form, msg="#2")

        phrase_info = fn(u'купить слова в Москве -!в области -даром')
        self.assertFalse(phrase_info.fixed_phrase, msg="#3")
        self.assertFalse(phrase_info.fixed_stopwords, msg="#3")
        self.assertFalse(phrase_info.fixed_order, msg="#3")
        self.assertFalse(phrase_info.fixed_form, msg="#3")

        phrase_info = fn(u'[купить слова] +в !Москве -область -даром')
        self.assertFalse(phrase_info.fixed_phrase, msg="#4")
        self.assertTrue(phrase_info.fixed_stopwords, msg="#4")
        self.assertTrue(phrase_info.fixed_order, msg="#4")
        self.assertTrue(phrase_info.fixed_form, msg="#4")


class TestTemplateCorrectness(TestCase):

    def test_is_correct_template_usage(self):
        from ta_report.utils import is_correct_template_usage as fn

        self.assertTrue(fn(u"Купить ##", u"добра -слова", 100), msg="#1")
        self.assertTrue(fn(u"##", u"Купить в Москве-Питере всякого добра -слова -много", 100), msg="#2")
        self.assertTrue(fn(u"купить #####", u"df", 100), msg="#3")
        self.assertTrue(fn(u"купить ##!!!", u"+слова", 12), msg="#4")
        self.assertTrue(fn(u"купить #test#!!!", u"!слова", 100), msg="#5")
        self.assertIsNone(fn(u"купить -слова", None, 100), msg="#6")
        self.assertIsNone(fn(u"купить", "", 100), msg="#7")
        self.assertIsNone(fn(u"купить", u"+слова", 100), msg="#8")
        self.assertFalse(fn(u"купить ##", u"слова", 1), msg="#9")
        self.assertFalse(fn(u"купить ##!!!", u"слова", 11), msg="#10")
        self.assertFalse(fn(u"купить #Test#", u"слова продать", 19), msg="#11")
        self.assertTrue(fn(u"купить #Test#", u"слова продать", 20), msg="#12")


# todo(n-bar):
# - test_extract_timetarget
# - test_overlap_type
