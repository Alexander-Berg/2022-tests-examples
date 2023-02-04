import unittest

import os
import pytest
import yenv

from at.aux_.models import Person
from at.common import ServiceAccess

pytestmark = pytest.mark.django_db


class TestAuth(unittest.TestCase):
    AFFILIATION_UID1 = 11111
    AFFILIATION_UID2 = 11112

    FORCED_DEP_UID = 11113
    FORCED_GROUP_UID = 11115

    BLOCK_UID = 11114

    persons = {
        AFFILIATION_UID1: {
            'department_group': {
                'ancestors': [
                    {'url': 'yandex'},
                    {'url': 'yandex_money'},
                    {'url': 'yandex_money_dev'}
                ],
                'url': 'yandex_money_dev_web'
            },
            'login': 'sergeev',
            'official': {
                'affiliation': 'yamoney'
            },
            'groups': [],
        },
        AFFILIATION_UID2: {
            'department_group': {
                'ancestors': [
                    {'url': 'yandex'},
                    {'url': 'yandex_rkub'},
                    {'url': 'yandex_infra'},
                    {'url': 'yandex_infra_tech'},
                    {'url': 'yandex_infra_tech_interface'}
                ],
                'url': 'yandex_infra_int_internal_dev'
            },
            'login': 'msahnov',
            'official': {
                'affiliation': 'yandex'
            },
            'groups': [],
        },
        FORCED_DEP_UID: {
            'department_group': {
                'ancestors': [
                    {'url': 'as'}
                ],
                'url': 'as_office'
            },
            'login': 'nesterov-a',
            'official': {
                'affiliation': 'external'
            },
            'groups': [],
        },
        FORCED_GROUP_UID: {
            'department_group': {
                'ancestors': [
                    {'url': 'as'}
                ],
                'url': 'bla'
            },
            'login': 'nesterov-a',
            'official': {
                'affiliation': 'external'
            },
            'groups': [{'group': {'url': 'atushkadostup'}}],
        },
        BLOCK_UID: {
            'department_group': {
                'ancestors': [
                    {'url': 'ext'}
                ],
                'url': 'ext_open'
            },
            'login': 'a-chibrikin',
            'official': {'affiliation': 'external'},
            'groups': [],
        }
    }

    FORCED_GROUPS_CONTENT = [{"person": {"uid": 123455}}]

    def setUp(self):
        yenv.type = 'notdevelopment'
        for uid, data in list(self.persons.items()):
            Person.objects.update_or_create(person_id=uid,
                                            login=data['login'],
                                            has_access=uid != self.BLOCK_UID
                                            )

    def tearDown(self):
        yenv.type = 'development'

    def testAffiliationYamoney(self):
        self.assertTrue(ServiceAccess.check_accessibility(self.AFFILIATION_UID1))

    def testAffiliationYandex(self):
        self.assertTrue(ServiceAccess.check_accessibility(self.AFFILIATION_UID2))

    def testForcedDeps(self):
        self.assertTrue(ServiceAccess.check_accessibility(self.FORCED_DEP_UID))

    def testForcedGroup(self):
        self.assertTrue(ServiceAccess.check_accessibility(self.FORCED_GROUP_UID))

    def testBlock(self):
        self.assertFalse(ServiceAccess.check_accessibility(self.BLOCK_UID))

    def testUnknown(self):
        self.assertFalse(ServiceAccess.check_accessibility(0o0012345000))
