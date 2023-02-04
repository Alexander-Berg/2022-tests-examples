# coding: utf-8

import unittest

from at.common import Context
from at.aux_ import models


class PersonsTests(unittest.TestCase):

    def testGetTitle(self):
        """get_title should return current language title with fallback to login"""
        m = models.Person()
        m.login='my-login'
        self.assertEqual(m.get_title(), m.login)
        m.title = 'rus-title'
        m.title_eng = 'eng-title'
        with Context.ContextBinder(language='ru'):
            self.assertEqual(m.title, m.get_title())
        with Context.ContextBinder(language='en'):
            self.assertEqual(m.title_eng, m.get_title())

    def fakeInflect(self, results):
        def inner(title, sex):
            results.append((title, sex))
            return {'rod': 'anythinh would be ok here'}
        return inner

    def testShouldCallInflectorForPersons(self):
        results = []
        models.Inflector.inflect = self.fakeInflect(results)
        m = models.Person()
        m.title = 'Сахнов Михаил'
        with Context.ContextBinder(language='ru'):
            m.get_title(inflection='rod')
        self.assertEqual(results[-1], (m.title, m.sex))

    def testShouldNotCallInflectorForClubs(self):
        results = []
        models.Inflector.inflect = self.fakeInflect(results)
        m = models.Person()
        m.community_type='OPENED_COMMUNITY'
        m.person_id = 4611686018427387905
        m.title = 'Яндекс Музыка'
        m.get_title(inflection='dat')
        self.assertFalse(results)
