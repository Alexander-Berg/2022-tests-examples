#!/usr/bin/env python3
# -*- coding: utf-8 -*-

__author__ = "Zasimov Alexey"
__email__ = "zasimov-a@yandex-team.ru"


import unittest
import sys
sys.path.append(".")


class DependsTest(unittest.TestCase):

    def setUp(self):
        pass

    def test_lxml(self):
        try:
            import stocks3.core.config
        except Exception:
            self.assertTrue(False, "Install lxml: sudo apt-get install python-lxml")


if __name__ == "__main__":
    unittest.main()
