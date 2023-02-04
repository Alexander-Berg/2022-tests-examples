#! coding: utf-8
"""tests for ta_report.cli"""
from subprocess import PIPE, Popen as popen
from unittest import TestCase
from ta_report.const import __version__


class TestCLIArgs(TestCase):

    STDOUT, STDERR = 0, 1

    def test_returns_usage_information(self):
        output = popen(['ta-report', '-h'], stdout=PIPE).communicate()[self.STDOUT]
        self.assertTrue('Usage:' in output, msg="#1")

        output = popen(['ta-report', '--help'], stdout=PIPE).communicate()[self.STDOUT]
        self.assertTrue('Usage:' in output, msg="#2")

    def test_returns_version_information(self):
        output = popen(['ta-report', '--version'], stdout=PIPE).communicate()[self.STDOUT]
        self.assertEqual(output.strip(), __version__)


class TestVersion(TestCase):

    def test_version_parse(self):
        from distutils.version import LooseVersion

        self.assertEqual(LooseVersion("0.0.0"), LooseVersion("0.0.0"), msg="#1")
        self.assertEqual(LooseVersion("02.10.3"), LooseVersion("02.10.3"), msg="#2")
        self.assertGreater(LooseVersion("02.10.3"), LooseVersion("02.9.3"), msg="#3")


class TestUsage(TestCase):
    def test_returns_succes_finish(self):
        from os import chdir, remove
        import shutil
        from glob import glob

        chdir("tests/mini")

        for x in ["*-src.*", "*.zip", "*.xlsx", "ta-report.log", "*.pptx"]:
            for f in glob(x):
                remove(f)
        for x in ["charts", "tmp_data", "images"]:
            shutil.rmtree(x, ignore_errors=True)

        output, _ = popen(['ta-report', '-czw', '.', '--dumps-src', '--pptx'], stdout=PIPE).communicate()
        sheets = [
            # report
            "00_total_campaigns_stat",
            "00_campaigns_overview",
            "00_logins_overview",
            "01_highlight_type",
            "01_highlight_hist",
            "01_campaign_highlight_type",
            "01_top_non-bolded_phrases",
            "01_operators_usage",
            "01_top_fixed_phrase",
            "01_top_fixed_form",
            "01_top_fixed_order",
            "01_top_fixed_stopwords",
            "02_sitelinks_cnt_S",
            "02_sitelinks_cnt_R",
            "02_bids_sl_count_lt",
            "02_description_sl_cnt",
            "02_description_eq_zero",
            "02_navig_incorrect",
            "03_callouts_cnt",
            "03_callouts_detail",
            "03_top_banners_zero_callouts",
            "03_top_banners_has_uppercase",
            "04_campaigns_type",
            "04_list_campaigns_type",
            "05_words_cnt",
            "06_templates_status",
            "06_templates_incorrect_usage",
            "06_display_url",
            "06_wo_display_url",
            "08_img_shapes",
            "09_rsya_banners_wo_img",
            "10_avg_time_target",
            "10_use_time_target_campaigns",
            "11_top_phrases_lost_clicks",
            "12_group_banners_count",
            "12_group_just_one_banner",
            "12_group_phrases_count",
            "12_group_just_one_phrase",
            "12_top_phrases_strange_pos",
            "13_top_cpc_phrases",
            "14_vcard_usage",
            "14_banners_wo_vcard",
            "15_banners_desk_w_mob_stat",
            "15_banners_mob_w_desk_stat",
            # search_queries
            "00_logins_overview",
            "01_highlight_hist_queries",
            "01_top_non-bolded_queries",
            "01_operators_usage",
            "01_top_fixed_phrase",
            "01_top_fixed_form",
            "01_top_fixed_order",
            "01_top_fixed_stopwords",
            "02_nested_queries",
            "02_top_nested_queries",
            "04_top_irrel_queries_0_25",
            "04_top_irrel_queries_25_50",
            "04_eshows_share_queries",
            "04_top_eshows_share_queries",
            "04_top_eshows_share_campaigns",
        ]
        assertContainSheet = lambda mbr: self.assertIn(mbr, container=output, msg="Missed sheet: %s" % mbr)
        map(assertContainSheet, sheets)
        self.assertTrue(output.endswith("\nFINISH APP\n"))
        chdir("../..")

    def test_ru_version(self):
        from os import chdir, remove
        import shutil
        from glob import glob

        chdir("tests/mini")

        for x in ["*-src.*", "*.zip", "*.xlsx", "ta-report.log", "*.pptx"]:
            for f in glob(x):
                remove(f)
        for x in ["charts", "tmp_data", "images"]:
            shutil.rmtree(x, ignore_errors=True)

        output, _ = popen(['ta-report', '-czw', '.', '--dumps-src', '--lang', 'ru', '--pptx'], stdout=PIPE).communicate()
        self.assertTrue(output.endswith("\nFINISH APP\n"))
        chdir("../..")
