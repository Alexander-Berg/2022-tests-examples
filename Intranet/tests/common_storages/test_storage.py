# -*- coding: utf-8 -*-
from django.test import TestCase

from events.common_storages.storage import MdsStorage


class TestSaaSEllipticsStorage___crop_filename(TestCase):
    def setUp(self):
        self.storage = MdsStorage()

    def test_cropped_filename_max_length_without_extension(self):
        filename = 'filename' * 40
        cropped_filename = self.storage._crop_filename(filename)
        self.assertEqual(len(cropped_filename), 32)
        self.assertEqual(cropped_filename, filename[:32])

    def test_cropped_filename_max_length_with_extension(self):
        filename = 'filename' * 40
        ext = 'txt'
        cropped_filename = self.storage._crop_filename('%s.%s' % (filename, ext))
        exp_cropped_filename = filename[:32]
        exp_cropped_ext = ext[-7:]
        exp_len = len(exp_cropped_filename) + len(exp_cropped_ext) + 1
        self.assertEqual(len(cropped_filename), exp_len)
        exp_cropped = '%s.%s' % (exp_cropped_filename, exp_cropped_ext)
        self.assertEqual(cropped_filename, exp_cropped)

    def test_cropped_filename_max_length_with_long_extension(self):
        filename = 'myfile'
        ext = 'txt' * 100
        cropped_filename = self.storage._crop_filename('%s.%s' % (filename, ext))
        exp_cropped_ext = ext[-7:]
        exp_cropped_filename = filename[:32]
        exp_len = len(exp_cropped_filename) + len(exp_cropped_ext) + 1
        self.assertEqual(len(cropped_filename), exp_len)
        exp_cropped_filename = '%s.%s' % (exp_cropped_filename, exp_cropped_ext)
        self.assertEqual(cropped_filename, exp_cropped_filename)

    def test_cropped_filename_max_length_with_long_extension_and_long_name(self):
        filename = 'myfile' * 10
        ext = 'txt' * 100
        cropped_filename = self.storage._crop_filename('%s.%s' % (filename, ext))
        exp_cropped_filename = filename[:32]
        exp_cropped_ext = ext[-7:]
        exp_len = len(exp_cropped_filename) + len(exp_cropped_ext) + 1
        self.assertEqual(len(cropped_filename), exp_len)
        exp_cropped_filename = '%s.%s' % (exp_cropped_filename, exp_cropped_ext)
        self.assertEqual(cropped_filename, exp_cropped_filename)
