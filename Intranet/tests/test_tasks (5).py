# coding: utf-8
from __future__ import unicode_literals

from .utils import get_entity_data, get_meta
from mock import patch
from unittest import TestCase

import jsonschema
import dpath.util

from static_api import storage, tasks
from static_api.schema import SCHEMAS


class Finder(object):
    def __init__(self, obj):
        self.obj = obj

    def find(self, path):
        def _gen():
            for _, value in dpath.util.search(self.obj, path, yielded=True, separator='.'):
                yield value

        return list(_gen())

    def find_one(self, path):
        try:
            return self.find(path)[0]
        except IndexError:
            raise ValueError('Value for path does not exist: %s' % path)

    def validate(self, schema_name):
        try:
            return SCHEMAS[schema_name].validate(self.obj)
        except jsonschema.ValidationError as e:
            raise AssertionError(e)


class TasksTest(TestCase):
    @classmethod
    @patch('static_api.helpers.get_meta', new=get_meta)
    @patch('static_api.helpers.get_entity_data', new=get_entity_data)
    @patch('static_api.lock.lock_manager')
    def setUpClass(cls, *args):
        manager = storage.manager
        manager.get_state_manager().set_state('init')
        manager.reset(include_meta=True)

        tasks.init()

        cls.person = Finder(manager.db['person'].find_one({'login': 'alexkoshelev'}, projection={'_id': False}))
        cls.person_with_deleted_department = Finder(manager.db['person'].find_one({'login': 'sizeof'},
                                                                                  projection={'_id': False}))
        cls.person_with_deleted_group = Finder(manager.db['person'].find_one({'login': 'aleshin'},
                                                                             projection={'_id': False}))
        cls.person_trainee = Finder(manager.db['person'].find_one({'login': 'ifrag'}, projection={'_id': False}))
        cls.group = Finder(manager.db['group'].find_one({'id': 49798}, projection={'_id': False}))
        cls.group_with_responsibles = Finder(manager.db['group'].find_one({'id': 745}, projection={'_id': False}))
        cls.group_service_role = Finder(manager.db['group'].find_one({'id': 39446}, projection={'_id': False}))
        cls.office = Finder(manager.db['office'].find_one({'id': 1}, projection={'_id': False}))
        cls.organization = Finder(manager.db['organization'].find_one({'id': 1}, projection={'_id': False}))
        cls.floor = Finder(manager.db['floor'].find_one({'id': 6}, projection={'_id': False}))
        cls.table = Finder(manager.db['table'].find_one({'id': 17078}, projection={'_id': False}))
        cls.table_with_deleted_floor = Finder(manager.db['table'].find_one({'id': 4753}, projection={'_id': False}))
        cls.equipment = Finder(manager.db['equipment'].find_one({'id': 258}, projection={'_id': False}))
        cls.room = Finder(manager.db['room'].find_one({'id': 47}, projection={'_id': False}))
        cls.departmentstaff = Finder(manager.db['departmentstaff'].find_one({'id': 3735}, projection={'_id': False}))
        cls.departmentchain = Finder(manager.db['departmentchain'].find_one({'id': 839}, projection={'_id': False}))

    def test_person_schema(self):
        self.person.validate('person')

    def test_person(self):
        self.assertEqual(self.person.find_one('is_deleted'), False)
        self.assertEqual(self.person.find_one('login'), 'alexkoshelev')
        self.assertEqual(self.person.find_one('name.first.en'), 'Alexander')

        self.assertEqual(self.person.find_one('official.is_homeworker'), False)
        self.assertEqual(self.person.find_one('official.is_robot'), False)

    def test_person_modified_at(self):
        self.assertEqual(self.person.find_one('_meta.modified_at'), '2019-07-13T21:10:48.000000+00:00')

    def test_person_arrays(self):
        self.assertEqual(sorted(self.person.find('phones.*.type')), ['mobile'])
        self.assertEqual(sorted(self.person.find('phones.*.kind')), ['common'])
        self.assertEqual(sorted(self.person.find('phones.*.protocol')), ['all'])
        self.assertEqual(sorted(self.person.find('cars.*.plate')), ['Р574СР199'])
        self.assertEqual(sorted(self.person.find('cars.*.plate')), ['Р574СР199'])
        self.assertEqual(sorted(self.person.find('keys.*.description')), ['macbook'])
        self.assertEqual(sorted(self.person.find('keys.*.fingerprint')), ['A:B:C:D:E'])
        self.assertEqual(sorted(self.person.find('gpg_keys.*.description')),
                         ['pub  2048R/87EDD04E 2015-03-17 Vasily Pupkin <test@gmail.com>'])
        self.assertEqual(sorted(self.person.find('gpg_keys.*.fingerprint')),
                         ['EE2C95AB58DC2B0138D16B4FEFC4571D7C90E5AF'])

    def test_person_personal(self):
        self.assertEqual(self.person.find_one('personal.tshirt_size'), 'XL')
        self.assertEqual(self.person.find_one('personal.children'), 0)

    def test_person_mapping(self):
        self.assertEqual(self.person.find_one('personal.gender'), 'male')
        self.assertEqual(self.person.find_one('personal.family_status'), 'single')
        self.assertEqual(self.person.find_one('education.status'), 'specialist')
        self.assertEqual(self.person.find_one('official.employment'), 'full')
        self.assertEqual(self.person.find_one('official.affiliation'), 'yandex')

    def test_person_department_group(self):
        self.assertEqual(self.person.find_one('department_group.url'), 'yandex_infra_tech_tools_access_dev')
        self.assertEqual(self.person.find_one('department_group.department.level'), 5)
        self.assertEqual(self.person.find('department_group.department.heads.*.role'), ['chief'])
        self.assertEqual(self.person.find('department_group.department.heads.*.person.login'), ['alexkoshelev'])
        self.assertEqual(len(self.person.find('department_group.ancestors.*')), 5)

    def test_person_department_group_ancestors(self):
        # проверяем порядок
        self.assertEqual(self.person.find('department_group.ancestors.*.department.level'), [0, 1, 2, 3, 4])

        self.assertEqual(sorted(self.person.find('department_group.ancestors.*.url')),
                         ['yandex', 'yandex_infra_data', 'yandex_infra_tech_tools', 'yandex_main_searchadv',
                          'yandex_search_tech_sq_interfaceandtools'])

        self.assertEqual(sorted(self.person.find('department_group.ancestors.*.department.heads.*.person.login')),
                         ['abash', 'andrebel', 'bunina', 'chudosveta', 'elena-bo', 'evgpopkova', 'evgpopkova',
                          'ichaadaeva', 'katerinakam', 'katerinakam', 'kmdruzhinina', 'kotpy', 'kristinapogosyan',
                          'lili-na', 'styskin', 'tigran', 'titovaee', 'veged', 'volozh', 'yushurduk'])

    def test_person_deleted_department(self):
        self.assertEqual(self.person_with_deleted_department.find_one('department_group.department.id'), 488)
        self.assertEqual(self.person_with_deleted_department.find_one('department_group.department.is_deleted'), True)
        self.assertEqual(self.person_with_deleted_department.find_one('department_group.department.level'), 8)

        self.assertEqual(self.person_with_deleted_department.find_one('department_group.id'), 898)
        self.assertEqual(self.person_with_deleted_department.find_one('department_group.is_deleted'), True)
        self.assertEqual(self.person_with_deleted_department.find_one('department_group.type'), 'department')

        self.assertEqual(sorted(self.person_with_deleted_group.find('groups.*.group.url')),
                         ['del_03e70515', 'del_465587a8', 'fired', 'ml-adv-tech', 'mlrussia', 'priorityoverview',
                          'svc_mobmarket', 'yandex_monetize_market'])
        self.assertEqual(sorted(self.person_with_deleted_group.find('groups.*.group.is_deleted')),
                         [False, ] * 5 + [True, ] * 3)

    def test_person_groups(self):
        self.assertEqual(sorted(self.person.find('groups.*.group.id')),
                         [745, 26870, 39446, 49798])

        self.assertIn('2010-05-24T12:03:08+00:00', self.person.find('groups.*.joined_at'))

    def test_person_groups_ancestors(self):
        self.assertEqual(sorted(self.person.find('groups.0.group.ancestors.*.url')),
                         ['yandex', 'yandex_infra_data', 'yandex_infra_tech_tools',
                          'yandex_main_searchadv', 'yandex_search_tech_sq_interfaceandtools'])

    def test_person_office(self):
        self.assertEqual(self.person.find_one('location.office.code'), 'redrose')
        self.assertEqual(self.person.find_one('location.office.name.en'), 'Moscow, BC Morozov')
        self.assertEqual(self.person.find_one('location.office.city.name.en'), 'Moscow')
        self.assertEqual(self.person.find_one('location.office.city.country.name.en'), 'Russia')

    def test_person_room(self):
        self.assertEqual(self.person.find_one('location.room.number'), 631)
        self.assertEqual(self.person.find_one('location.table.room.number'), 631)

    def test_person_organization(self):
        self.assertEqual(self.person.find_one('official.organization.name'), 'Яндекс.Технологии')

    def test_person_table(self):
        self.assertEqual(self.person.find_one('location.table.number'), 17078)

    def test_group_schema(self):
        self.group.validate('group')
        self.group_with_responsibles.validate('group')

    def test_department_staff(self):
        self.departmentstaff.validate('departmentstaff')
        self.assertEqual(self.departmentstaff.find_one('department_group.department.id'), 3583)
        self.assertEqual(self.departmentstaff.find_one('department_group.type'), "department")
        self.assertEqual(self.departmentstaff.find_one('person.login'), 'alexkoshelev')

    def test_group(self):
        self.assertEqual(self.group.find_one('department.id'), 3583)
        self.assertEqual(self.group.find_one('department.level'), 5)

        # нет лишних полей
        self.assertEqual(self.group.find('department.tree_id'), [])

    def test_group_department_heads(self):
        self.assertEqual(self.group.find('department.heads.*.role'), ['chief'])
        self.assertEqual(self.group.find('department.heads.*.person.login'), ['alexkoshelev'])

        self.assertEqual(sorted(self.group.find('ancestors.*.department.heads.*.person.login')),
                         ['abash', 'andrebel', 'bunina', 'chudosveta', 'elena-bo', 'evgpopkova', 'evgpopkova',
                          'ichaadaeva', 'katerinakam', 'katerinakam', 'kmdruzhinina', 'kotpy', 'kristinapogosyan',
                          'lili-na', 'styskin', 'tigran', 'titovaee', 'veged', 'volozh', 'yushurduk'])

    def test_group_department_kind(self):
        self.assertEqual(self.group.find('ancestors.*.department.kind.slug'),
                         ['direction'])

    def test_group_contacts(self):
        self.failUnless(self.group.find_one('department.contacts'))
        self.failUnless(self.group.find('ancestors.*.department.contacts'))

    def test_group_affiliation(self):
        self.assertEqual(self.group.find_one('affiliation_counters.external'), 0)
        self.assertEqual(self.group.find_one('affiliation_counters.yandex'), 31)
        self.assertEqual(self.group.find_one('affiliation_counters.yamoney'), 0)

    def test_group_responsibles(self):
        self.assertEqual(sorted(self.group_with_responsibles.find('responsibles.*.person.login')),
                         ['abash', 'styskin', 'zagrebin'])

    def test_group_service_role(self):
        self.assertEqual(self.group_service_role.find_one('role_scope'),
                         'development')

    def test_group_parent(self):
        self.assertEqual(self.group.find_one('parent.url'), 'yandex_infra_tech_tools')

    def test_office_schema(self):
        self.office.validate('office')

    def test_office(self):
        self.assertEqual(self.office.find_one('name.en'), 'Moscow, BC Morozov')
        self.assertEqual(self.office.find_one('city.name.en'), 'Moscow')
        self.assertEqual(self.office.find_one('city.country.name.en'), 'Russia')

    def test_organization_schema(self):
        self.organization.validate('organization')

    def test_organization(self):
        self.assertEqual(self.organization.find_one('name'), 'Яндекс')

    def test_floor(self):
        self.assertEqual(self.floor.find_one('name.ru'), 'Шестой этаж')
        self.assertEqual(self.floor.find_one('office.name.en'), 'Moscow, BC Morozov')
        self.assertEqual(self.floor.find_one('office.city.name.en'), 'Moscow')
        self.assertEqual(self.floor.find_one('office.city.country.name.en'), 'Russia')

    def test_table_schema(self):
        self.table.validate('table')

    def test_table(self):
        self.assertEqual(self.table.find_one('number'), 17078)

    def test_table_deleted_floor(self):
        self.assertEqual(self.table_with_deleted_floor.find_one('floor.id'), 43)
        self.assertEqual(self.table_with_deleted_floor.find_one('floor.is_deleted'), True)
        self.failUnless(self.table_with_deleted_floor.find_one('floor.name'))

    def test_equipment_schema(self):
        self.equipment.validate('equipment')

    def test_equipment(self):
        self.assertFalse(self.equipment.find_one('is_deleted'))
        self.assertEqual(self.equipment.find_one('description'), 'Принтер HP P2015dn')
        self.assertEqual(self.equipment.find_one('floor.name.ru'), 'Шестой этаж')
        self.assertEqual(self.equipment.find_one('floor.office.name.en'), 'Moscow, BC Morozov')
        self.assertEqual(self.equipment.find_one('floor.office.city.name.en'), 'Moscow')
        self.assertEqual(self.equipment.find_one('floor.office.city.country.name.en'), 'Russia')

    def test_room_schema(self):
        self.room.validate('room')

    def test_room(self):
        self.assertEqual(self.room.find_one('name.display'), '6.Серафим')
        self.assertEqual(self.room.find_one('floor.name.ru'), 'Шестой этаж')
        self.assertEqual(self.room.find_one('floor.office.name.en'), 'Moscow, BC Morozov')
        self.assertEqual(self.room.find_one('floor.office.city.name.en'), 'Moscow')
        self.assertEqual(self.room.find_one('floor.office.city.country.name.en'), 'Russia')

    def test_field_intern(self):
        self.assertEqual(self.person_trainee.find_one('official.is_trainee'), True)
        self.assertEqual(self.person.find_one('official.is_trainee'), False)

    def test_department_chains(self):
        self.assertEqual(self.departmentchain.find('chiefs.*.login'), ['veged', 'abash', 'volozh'])
        self.assertEqual(self.departmentchain.find('hr_partners.*.login'), ['bunina'])

        self.assertEqual(self.person.find_one('chief.login'), 'veged')
        self.assertEqual(self.person.find('chiefs.*.login'), ['veged', 'abash', 'volozh'])
        self.assertEqual(self.person.find('hr_partners.*.login'), ['bunina'])
