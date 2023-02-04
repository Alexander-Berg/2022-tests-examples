# -*- coding: utf-8

from unittest import TestCase, main
from query_normalizer import simple_normalize


class TestNormalizer(TestCase):
    def test_1(self):
        self.assertEqual('оценка машины', simple_normalize('оценка машины -наследства -нотариус'))

    def test_2(self):
        self.assertEqual('купить тест к нг 1 с', simple_normalize('купить тест +к нг 1с'))

    def test_3(self):
        self.assertEqual('11 б 18 бк', simple_normalize('11б18бк'))

    def test_4(self):
        self.assertEqual('11 x 11', simple_normalize('11x11'))

    def test_5(self):
        self.assertEqual('168 192 0 1 настройка роутера', simple_normalize('168.192.0.1 настройка роутера'))


if __name__ == '__main__':
    main()
