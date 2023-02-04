# coding: utf-8

import unittest

from at.common import utils
from at.aux_.models import Person
from at.aux_ import Profile


class CommunityTests(unittest.TestCase):
    def setUp(self):
        pass

    def testUpdateModel(self):
        ai = utils.getAuthInfo(1, 'aaa')
        model = Person()
        model.person_id=ai.uid
        model.login=ai.login
        model.title='Old title ru'
        model.mood='old mood'
        assert Profile.update_model(model, {'mood': 'new mood'})
        assert model.mood == 'new mood'
        assert not Profile.update_model(model, {'mood': 'new mood'})

