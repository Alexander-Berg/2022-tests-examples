# coding: utf-8
from __future__ import unicode_literals

from static_api import storage

from .base import MongoTestCase


class OperationsTest(MongoTestCase):
    def setUp(self):
        super(OperationsTest, self).setUp()

        manager = storage.manager

        self.db = manager.db
        self.db.drop_collection('person')

        self.person = manager.get_write_namespace()['person']
        self.group = manager.get_write_namespace()['group']

    def test_put(self):
        self.person.put({'id': 1}, {'id': 1, 'attr': 'foo'})

        self.assertEqual(self.db['person'].count(), 1)

    def test_put_merge(self):
        self.person.put({'id': 1}, {'id': 1, 'attr': {'foo': 'bar'}})

        self.person.put({'id': 1}, {'attr': {'bar': 'foo'}}, merge=True)

        doc = self.db['person'].find_one({'id': 1})

        self.assertEqual(doc['attr']['foo'], 'bar')
        self.assertEqual(doc['attr']['bar'], 'foo')

    def test_delete(self):
        self.db['person'].insert({'id':1 , 'name': 'Sam'})

        self.person.delete({'id': 1})

        self.assertEqual(self.db['person'].count(), 0)

    def test_put_nested(self):
        for id_ in (1, 2):
            self.db['person'].insert({'id': id_, 'department': {'id': 7}})

        self.person.put_nested('department', {'department.id': 7}, {'id': 7, 'name': 'tools'})

        for doc in self.db['person'].find({'id': {'$in': [1, 2]}}):
            self.assertEqual(doc['department']['name'], 'tools')

    def test_put_nested_parent_lookup(self):
        for id_ in (1, 2):
            self.db['person'].insert({'id': id_, 'department': {'id': 7}})

        self.person.put_nested('department', {'department.id': 7},
                               {'id': 7, 'name': 'tools'}, {'id': 1})

        doc1 = self.db['person'].find_one({'id': 1})
        self.assertEqual(doc1['department']['name'], 'tools')

        doc2 = self.db['person'].find_one({'id': 2})
        self.assertIsNone(doc2['department'].get('name'))

    def test_delete_nested(self):
        for id_ in (1, 2):
            self.db['person'].insert({'id': id_, 'department': {'id': 7, 'name': 'tools'}})

        self.person.delete_nested('department', lookup={'department.id': 7})

        for doc in self.db['person'].find({'id': {'$in': [1, 2]}}):
            self.assertEqual(doc['department']['id'], 7)
            self.failUnless('name' not in doc['department'])

    def test_delete_nested_parent_lookup(self):
        for id_ in (1, 2):
            self.db['person'].insert({'id': id_, 'department': {'id': 7, 'name': 'tools'}})

        self.person.delete_nested('department', lookup={'department.id': 7}, parent_lookup={'id': 1})

        doc1 = self.db['person'].find_one({'id': 1})
        self.assertEqual(doc1['department']['id'], 7)
        self.failUnless('name' not in doc1['department'])

        doc2 = self.db['person'].find_one({'id': 2})
        self.assertEqual(doc2['department']['id'], 7)
        self.assertEqual(doc2['department']['name'], 'tools')

    def test_delete_nested_none(self):
        for id_ in (1, 2):
            self.db['person'].insert({'id': id_, 'department': {'id': 7, 'name': 'tools'}})

        self.person.delete_nested('department', lookup={'department.id': 7}, set_none=True)

        for doc in self.db['person'].find({'id': {'$in': [1, 2]}}):
            self.assertEqual(doc['department'], None)

    def test_delete_nested_parent_lookup_none(self):
        for id_ in (1, 2):
            self.db['person'].insert({'id': id_, 'department': {'id': 7, 'name': 'tools'}})

        self.person.delete_nested('department', lookup={'department.id': 7}, set_none=True, parent_lookup={'id': 1})

        doc1 = self.db['person'].find_one({'id': 1})
        self.assertEqual(doc1['department'], None)

        doc2 = self.db['person'].find_one({'id': 2})
        self.assertEqual(doc2['department']['id'], 7)
        self.assertEqual(doc2['department']['name'], 'tools')

    def test_put_nested_array(self):
        self.db['person'].insert({'id': 1, 'cars': []})
        self.db['person'].insert({'id': 2, 'cars': [{'id': 7}]})

        self.person.put_nested(
            attr='cars.$',
            lookup={'cars.$.id': 7},
            data={'id': 7, 'vendor': 'mitsubishi'},
            parent_lookup={'id': 1}
        )

        doc1 = self.db['person'].find_one({'id': 1})
        self.assertEqual(doc1['cars'][0]['vendor'], 'mitsubishi')

        doc2 = self.db['person'].find_one({'id': 2})
        self.failUnless('vendor' not in doc2['cars'][0])

    def test_delete_from_nested_array(self):
        self.db['person'].insert({'id': 1, 'cars': [{'id': 7, 'vendor': 'mitsubishi'},
                                               {'id': 8, 'vendor': 'opel'}]})
        self.db['person'].insert({'id': 2, 'cars': [{'id': 7, 'vendor': 'mitsubishi'},
                                               {'id': 9, 'vendor': 'toyota'}]})

        self.person.delete_nested('cars.$', {'cars.$.id': 7})

        doc1 = self.db['person'].find_one({'id': 1})
        self.assertEqual(len(doc1['cars']), 1)
        self.assertEqual(doc1['cars'][0]['vendor'], 'opel')

        doc2 = self.db['person'].find_one({'id': 2})
        self.assertEqual(len(doc2['cars']), 1)
        self.assertEqual(doc2['cars'][0]['vendor'], 'toyota')

    def test_delete_from_nested_array_parent_lookup(self):
        self.db['person'].insert({'id': 1, 'cars': [{'id': 7, 'vendor': 'mitsubishi'},
                                               {'id': 8, 'vendor': 'opel'}]})
        self.db['person'].insert({'id': 2, 'cars': [{'id': 7, 'vendor': 'mitsubishi'},
                                               {'id': 9, 'vendor': 'toyota'}]})

        self.person.delete_nested('cars.$', {'cars.$.id': 7}, {'id': 1})

        doc1 = self.db['person'].find_one({'id': 1})
        self.assertEqual(len(doc1['cars']), 1)
        self.assertEqual(doc1['cars'][0]['vendor'], 'opel')

        doc2 = self.db['person'].find_one({'id': 2})
        self.assertEqual(len(doc2['cars']), 2)

    def test_put_nested_array_exists(self):
        for id_ in (1, 2):
            self.db['person'].insert({'id': id_, 'cars': [{'id': 7, 'vendor': 'opel'}]})

        self.person.put_nested('cars.$', {'cars.$.id': 7}, {'id': 7, 'vendor': 'mitsubishi'},
                               parent_lookup={'id': 1})

        doc1 = self.db['person'].find_one({'id': 1})
        self.assertEqual(len(doc1['cars']), 1)
        self.assertEqual(doc1['cars'][0]['vendor'], 'mitsubishi')

        doc2 = self.db['person'].find_one({'id': 2})
        self.assertEqual(len(doc2['cars']), 1)
        self.assertEqual(doc2['cars'][0]['vendor'], 'opel')

    def test_put_nested_array_attr(self):
        self.db['person'].insert(
            {
                'id': 1,
                'cars': [
                    {'id': 6, 'vendor': {'id': 9, 'name': 'bmw'}},
                    {'id': 7, 'vendor': {'id': 10, 'name': 'opel'}},
                ]
            }
        )
        self.db['person'].insert(
            {
                'id': 2,
                'cars': [
                    {'id': 6, 'vendor': {'id': 9, 'name': 'bmw'}},
                    {'id': 7, 'vendor': {'id': 10, 'name': 'opel'}},
                ]
            }
        )

        self.person.put_nested(
            attr='cars.$.vendor',
            lookup={'cars.$.vendor.id': 10},
            data={'id': 10, 'name': 'mitsubishi'},
        )

        doc = self.db['person'].find_one({'id': 1})

        self.assertEqual(len(doc['cars']), 2)
        self.assertEqual(doc['cars'][0]['vendor']['name'], 'bmw')
        self.assertEqual(doc['cars'][1]['vendor']['name'], 'mitsubishi')

        doc = self.db['person'].find_one({'id': 2})

        self.assertEqual(len(doc['cars']), 2)
        self.assertEqual(doc['cars'][0]['vendor']['name'], 'bmw')
        self.assertEqual(doc['cars'][1]['vendor']['name'], 'mitsubishi')

    def test_delete_from_nested_array_attr(self):
        self.db['person'].insert({'id': 1, 'cars': [{'id': 7, 'vendor': {'id': 10, 'name': 'opel'}}]})

        self.person.delete_nested('cars.$.vendor', {'cars.$.vendor.id': 10})

        doc = self.db['person'].find_one({'id': 1})

        self.assertEqual(len(doc['cars']), 1)
        self.assertEqual(doc['cars'][0]['vendor']['id'], 10)

    def test_update_nested(self):
        group_col = self.db['group']
        group_col.insert({
            'id': 0,
        })

        group_col.insert({
            'id': 1,
            'parent': None,
        })

        group_col.insert({
            'id': 2,
            'parent': {},
        })

        group_col.insert({
            'id': 3,
            'parent': {
                'parent': {
                    'id': 10
                }
            },
        })

        group_col.insert({
            'id': 4,
            'parent': {
                'parent': {
                    'id': 100500,
                }
            },
        })

        update_op = self.group.update_nested(
            path='parent.parent',
            element_id=100500,
            data={'id': 100500, 'key': 'value'},
        )
        self.group.execute_bulk([update_op], ordered=False)

        doc = self.db['group'].find_one({'id': 0})
        self.assertTrue('parent' not in doc)

        doc = self.db['group'].find_one({'id': 1})
        self.assertEquals(doc['parent'], None)

        doc = self.db['group'].find_one({'id': 2})
        self.assertEquals(doc['parent'], {})

        doc = self.db['group'].find_one({'id': 3})
        self.assertEquals(doc['parent'], {'parent': {'id': 10}})

        doc = self.db['group'].find_one({'id': 4})
        self.assertEquals(doc['parent'], {'parent': {'id': 100500, 'key': 'value'}})

    def test_delete_array_element_deep(self):
        person_col = self.db['person']
        person_col.insert({
            'id': 0,
        })

        person_col.insert({
            'id': 1,
            'groups': []
        })

        person_col.insert({
            'id': 2,
            'groups': [{
                'id': 21,
                'group': {},
            }]
        })

        person_col.insert({
            'id': 3,
            'groups': [{
                'id': 31,
                'group': {'ancestors': []},
            }]
        })

        person_col.insert({
            'id': 4,
            'groups': [{
                'id': 41,
                'group': {
                    'ancestors': [
                        {
                            'id': 441,
                        }
                    ],
                },
            }]
        })

        person_col.insert({
            'id': 5,
            'groups': [{
                'id': 51,
                'group': {
                    'ancestors': [
                        {
                            'id': 441,
                        },
                        {
                            'id': 100500,
                        },
                    ],
                },
            }]
        })

        update_op = self.person.update_nested_array_element(
            path='groups.$.group.ancestors.$',
            element_id=100500,
            delete=True,
        )
        self.person.execute_bulk([update_op], ordered=False)

        doc = self.db['person'].find_one({'id': 0})
        self.assertTrue('groups' not in doc)

        doc = self.db['person'].find_one({'id': 1})
        self.assertEquals(doc['groups'], [])

        doc = self.db['person'].find_one({'id': 2})
        self.assertEquals(
            doc['groups'],
            [{
                'id': 21,
                'group': {},
            }]
        )

        doc = self.db['person'].find_one({'id': 3})
        self.assertEquals(
            doc['groups'],
            [{
                'id': 31,
                'group': {'ancestors': []},
            }]
        )

        doc = self.db['person'].find_one({'id': 4})
        self.assertEquals(
            doc['groups'],
            [{
                'id': 41,
                'group': {
                    'ancestors': [
                        {
                            'id': 441,
                        }
                    ],
                },
            }]
        )

        doc = self.db['person'].find_one({'id': 5})
        self.assertEquals(
            doc['groups'],
            [{
                'id': 51,
                'group': {
                    'ancestors': [
                        {
                            'id': 441,
                        },
                    ],
                },
            }]
        )

    def test_delete_array_element(self):
        group_col = self.db['group']
        group_col.insert({
            'id': 1,
            'ancestors': [],
        })
        group_col.insert({
            'id': 2,
        })
        group_col.insert({
            'id': 3,
            'ancestors': [{
                'id': 100500,
            }]
        })

        update_op = self.group.update_nested_array_element(
            path='ancestors.$',
            element_id=100500,
            delete=True,
        )
        self.group.execute_bulk([update_op], ordered=False)

        doc = self.db['group'].find_one({'id': 1})
        self.assertEquals(doc['ancestors'], [])

        doc = self.db['group'].find_one({'id': 2})
        self.assertTrue('ancestors' not in doc)

        doc = self.db['group'].find_one({'id': 3})
        self.assertEquals(doc['ancestors'], [])

    def test_update_array_element_deep(self):
        person_col = self.db['person']
        person_col.insert({
            'id': 0,
        })

        person_col.insert({
            'id': 1,
            'groups': []
        })

        person_col.insert({
            'id': 2,
            'groups': [{
                'id': 21,
                'group': {},
            }]
        })

        person_col.insert({
            'id': 3,
            'groups': [{
                'id': 31,
                'group': {'ancestors': []},
            }]
        })

        person_col.insert({
            'id': 4,
            'groups': [{
                'id': 41,
                'group': {
                    'ancestors': [
                        {
                           'id': 441,
                        }
                    ],
                },
            }]
        })

        person_col.insert({
            'id': 5,
            'groups': [{
                'id': 51,
                'group': {
                    'ancestors': [
                        {
                            'id': 441,
                        },
                        {
                            'id': 100500,
                        },
                    ],
                },
            }]
        })

        update_op = self.person.update_nested_array_element(
            path='groups.$.group.ancestors.$',
            element_id=100500,
            data={'id': 100500, 'key': 'value'},
        )
        self.person.execute_bulk([update_op], ordered=False)

        doc = self.db['person'].find_one({'id': 0})
        self.assertTrue('groups' not in doc)

        doc = self.db['person'].find_one({'id': 1})
        self.assertEquals(doc['groups'], [])

        doc = self.db['person'].find_one({'id': 2})
        self.assertEquals(
            doc['groups'],
            [{
                'id': 21,
                'group': {},
            }]
        )

        doc = self.db['person'].find_one({'id': 3})
        self.assertEquals(
            doc['groups'],
            [{
                'id': 31,
                'group': {'ancestors': []},
            }]
        )

        doc = self.db['person'].find_one({'id': 4})
        self.assertEquals(
            doc['groups'],
            [{
                'id': 41,
                'group': {
                    'ancestors': [
                        {
                            'id': 441,
                        }
                    ],
                },
            }]
        )

        doc = self.db['person'].find_one({'id': 5})
        self.assertEquals(
            doc['groups'],
            [{
                'id': 51,
                'group': {
                    'ancestors': [
                        {
                            'id': 441,
                        },
                        {
                            'id': 100500,
                            'key': 'value',
                        },
                    ],
                },
            }]
        )

    def test_update_array_element(self):
        group_col = self.db['group']
        group_col.insert({
            'id': 1,
            'ancestors': [],
        })
        group_col.insert({
            'id': 2,
        })
        group_col.insert({
            'id': 3,
            'ancestors': [{
                'id': 100500,
            }]
        })

        update_op = self.group.update_nested_array_element(
            path='ancestors.$',
            element_id=100500,
            data={'id': 100500, 'key': 'value'},
        )
        self.group.execute_bulk([update_op], ordered=False)

        doc = self.db['group'].find_one({'id': 1})
        self.assertEquals(doc['ancestors'], [])

        doc = self.db['group'].find_one({'id': 2})
        self.assertTrue('ancestors' not in doc)

        doc = self.db['group'].find_one({'id': 3})
        self.assertEquals(
            doc['ancestors'],
            [{
                'id': 100500,
                'key': 'value',
            }]
        )

    def test_update_in_array_element(self):
        person_col = self.db['person']
        person_col.insert({
            'id': 1,
            'department_group': {
                'id': 2,
                'heads': [
                    {
                        'id': 3,
                        'person': {
                            'id': 4,
                        }
                    },
                    {
                        'id': 4,
                        'person': {
                            'id': 5,
                        }
                    }
                ],
            },
        })

        person_col.insert({
            'id': 2,
            'department_group': {
                'id': 2,
                'heads': [],
            },
        })

        person_col.insert({
            'id': 3,
            'department_group': {
                'id': 2,
            },
        })

        person_col.insert({
            'id': 4,
            'department_group': {
                'id': 2,
                'heads': [
                    {
                        'id': 10,
                        'person': {
                            'id': 3,
                        }
                    },
                    {
                        'id': 3,
                        'person': {
                            'id': 4,
                        }
                    },
                    {
                        'id': 4,
                        'person': {
                            'id': 4,
                            'name': 'some2',
                        }
                    }
                ],
            },
        })

        update_op = self.person.update_in_nested_array_element(
            'department_group.heads.$.person',
            4,
            {
                'id': 4,
                'name': 'some'
            }
        )
        self.person.execute_bulk([update_op], ordered=False)

        doc = self.db['person'].find_one({'id': 1})
        self.assertEquals(
            doc['department_group']['heads'][0],
            {
                'id': 3,
                'person': {
                    'id': 4,
                    'name': 'some'
                }
            },
        )

        doc = self.db['person'].find_one({'id': 1})
        self.assertEquals(
            doc['department_group']['heads'][1],
            {
                'id': 4,
                'person': {
                    'id': 5,
                }
            },
        )

        doc = self.db['person'].find_one({'id': 2})
        self.assertEquals(
            doc['department_group']['heads'],
            [],
        )

        doc = self.db['person'].find_one({'id': 3})
        self.assertEquals(
            doc['department_group'],
            {'id': 2},
        )

        doc = self.db['person'].find_one({'id': 4})
        self.assertEquals(
            doc['department_group']['heads'][0],
            {
                'id': 10,
                'person': {
                    'id': 3,
                }
            },
        )

        self.assertEquals(
            doc['department_group']['heads'][1],
            {
                'id': 3,
                'person': {
                    'id': 4,
                    'name': 'some'
                }
            },
        )

        self.assertEquals(
            doc['department_group']['heads'][2],
            {
                'id': 4,
                'person': {
                    'id': 4,
                    'name': 'some'
                }
            },
        )

    def test_update_nested_deep(self):
        person_col = self.db['person']
        person_col.insert({
            'id': 1,
            'groups': [
                {
                    'parent': {
                        'heads': [
                            {
                                'without_person': True,
                            },
                            {
                                'person': {
                                    'id': 4,
                                }
                            },
                            {
                                'person': {
                                    'id': 5,
                                }
                            }
                        ]
                    }

                },
                {
                   'without_parent': True,
                },
            ],
        })

        update_op = self.person.update_in_nested_array_element(
            'groups.$.parent.heads.$.person',
            4,
            {
                'id': 4,
                'name': 'some'
            }
        )
        self.person.execute_bulk([update_op], ordered=False)

        doc = self.db['person'].find_one({'id': 1})
        self.assertEquals(
            doc['groups'][0]['parent']['heads'][0],
            {
                'without_person': True,
            },
        )

        self.assertEquals(
            doc['groups'][0]['parent']['heads'][1],
            {
                'person': {
                    'id': 4,
                    'name': 'some',
                }
            },
        )

        self.assertEquals(
            doc['groups'][0]['parent']['heads'][2],
            {
                'person': {
                    'id': 5,
                }
            },
        )

        self.assertEquals(
            doc['groups'][1],
            {
                'without_parent': True
            },
        )

    def test_put_nested_array_deep_add(self):
        self.db['person'].insert({
            'id': 1,
            'groups': [{
                'id': 7,
                'group': {'name': 'tools', 'heads': []}
            }]
        })
        self.db['person'].insert({
            'id': 2,
            'groups': [{
                'id': 7,
                'group': {'name': 'tools'}
            }]
        })

        self.person.put_nested(
            attr='groups.$.group.heads.$',
            lookup={'groups.$.group.heads.$.person.id': 17},
            data={'id': 10, 'person': {'id': 17, 'name': 'Samuel'}},
            parent_lookup={'groups.$.group.name': 'tools'}
        )

        doc = self.db['person'].find_one({'id': 1})

        self.assertEqual(len(doc['groups']), 1)
        self.assertEqual(len(doc['groups'][0]['group']['heads']), 1)
        self.assertEqual(doc['groups'][0]['group']['heads'][0]['person']['name'], 'Samuel')

        doc = self.db['person'].find_one({'id': 2})

        self.assertEqual(len(doc['groups']), 1)
        self.assertEqual(len(doc['groups'][0]['group']['heads']), 1)
        self.assertEqual(doc['groups'][0]['group']['heads'][0]['person']['name'], 'Samuel')

    def test_delete_from_nested_array_deep(self):
        self.db['person'].insert({
            'id': 1,
            'groups': [{
                'id': 7,
                'group': {
                    'name': 'tools',
                    'heads': [
                        {
                            'id': 8,
                            'person': {'id': 17, 'name': 'Samuel'}
                        },
                        {
                            'id': 10,
                            'person': {'id': 17, 'name': 'Samuel'}
                        }
                    ]
                }
            }]
        })

        self.person.delete_nested(
            attr='groups.$.group.heads.$',
            lookup={'groups.$.group.heads.$.id': 10}
        )

        doc = self.db['person'].find_one({'id': 1})

        self.assertEqual(len(doc['groups']), 1)
        self.assertEqual(len(doc['groups'][0]['group']['heads']), 1)
        self.assertEqual(doc['groups'][0]['group']['heads'][0]['id'], 8)

    def test_put_nested_array_deep_exists(self):
        self.db['person'].insert({
            'id': 1,
            'groups': [{
                'id': 7,
                'group': {
                    'name': 'tools',
                    'heads': [{
                        'id': 10,
                        'person': {'id': 17, 'name': 'Sam'}
                    }]
                }
            }]
        })

        self.person.put_nested(
            attr='groups.$.group.heads.$.person',
            lookup={'groups.$.group.heads.$.person.id': 17},
            data={'id': 17, 'name': 'Samuel'}
        )

        doc = self.db['person'].find_one({'id': 1})

        self.assertEqual(len(doc['groups']), 1)
        self.assertEqual(len(doc['groups'][0]['group']['heads']), 1)
        self.assertEqual(doc['groups'][0]['group']['heads'][0]['person']['name'], 'Samuel')

    def test_find_in_dict(self):
        d = {'groups': [{'department':{'heads': [{'person': {'name': {'first': 'alex'}}},
                                                 {'person': {'name': 'sam'}}]}},
                        {}]}

        self.assertEqual(storage.utils._find_in_dict(d, {'groups.$.department.heads.$.person.name': 'sam'}),
                         'groups.0.department.heads.1.person.name')
        self.assertEqual(storage.utils._find_in_dict(d, {'groups.$.department.heads.$.person.name.first': 'alex'}),
                         'groups.0.department.heads.0.person.name.first')

    def test_apply_resolved_path(self):
        self.assertEqual(storage.utils._apply_resolved_path('groups.$.department.heads.$.person',
                                                            'groups.1.department.heads.3.person.name'),
                         'groups.1.department.heads.3.person')

    def test_parse_sort(self):
        self.assertEqual(
            storage.utils._parse_sort('id'),
            [('id', 1)]
        )
        self.assertEqual(
            storage.utils._parse_sort(['id', u'-time', 'count']),
            [('id', 1), ('time', -1), ('count', 1)]
        )

    def test_flatten_dict(self):
        d = {'_meta': {'modified_at': 1, 'message_id': 2}}

        self.assertEqual(storage.utils.flatten_dict(d),
                         {'_meta.message_id': 2, '_meta.modified_at': 1})
