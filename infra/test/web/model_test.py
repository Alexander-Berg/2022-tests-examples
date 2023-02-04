import unittest
import copy
import itertools
from datetime import datetime

import mock

from pymongo import MongoClient

from genisys.web import model, errors


class ModelTestCase(unittest.TestCase):
    maxDiff = None
    testdb_host = 'localhost'
    testdb_name = 'genisys_unittest'

    def clear_db(self):
        for collection in self.database.collection_names(False):
            self.database[collection].drop()

    def setUp(self):
        super(ModelTestCase, self).setUp()
        client = MongoClient(self.testdb_host)
        self.database = client[self.testdb_name]
        self.storage = model.MongoStorage(client, self.testdb_name)
        self.clear_db()
        self.setup_mocks()
        with mock.patch.object(self.storage, '_generate_oid') as gen_oid:
            gen_oid.return_value = '#root'
            self.storage.init_db(['user1', 'user2'])

    def setup_mocks(self):
        self.mock_ts = mock.patch.object(self.storage, '_get_ts')
        self.mock_ts_return_value = 1444000000
        def fake_get_ts():
            self.mock_ts_return_value += 1
            return self.mock_ts_return_value
        self.mock_ts.start().side_effect = fake_get_ts

        def fake_hash(val):
            return "hash(%r)" % (val, )
        self.mock_hash = mock.patch.object(model, 'volatile_key_hash')
        self.mock_hash.start().side_effect = fake_hash

        self.mock_gen_oid = mock.patch.object(self.storage, '_generate_oid')
        mock_gen_oid_return_value = itertools.count(1)
        def fake_gen_oid():
            return '#{}'.format(next(mock_gen_oid_return_value))
        self.mock_gen_oid.start().side_effect = fake_gen_oid
        self.mock_check_oid = mock.patch.object(self.storage, '_check_oid')
        self.mock_check_oid.start()

    def tearDown(self):
        super(ModelTestCase, self).tearDown()
        self.clear_db()
        self.mock_ts.stop()
        self.mock_gen_oid.stop()
        self.mock_hash.stop()
        self.mock_check_oid.stop()

    def assert_audit(self, path, expected, fields=None, extra_fields=None):
        audit = self.database.audit.find({'path': path}, {'_id': False})
        audit = list(audit.sort([('when', -1)]))
        if fields:
            fields = set(fields)
            if extra_fields:
                fields.add('extra')
            for record in audit:
                for f in set(record) - fields:
                    del record[f]
        try:
            if not fields or 'extra' in fields:
                for i, record in enumerate(audit):
                    record['extra'] = model._deserialize(record['extra'])
                    if len(expected) > i and 'traceback' in expected[i]:
                        self.assertRegexpMatches(record['traceback'],
                                                 expected[i]['traceback'])
                        del record['traceback'], expected[i]['traceback']
                    if extra_fields:
                        extra_fields = set(extra_fields)
                        for f in set(record['extra']) - extra_fields:
                            del record['extra'][f]
            self.assertEquals(audit, expected)
        except:
            import pprint
            pprint.pprint(audit)
            raise

    def reset_audit(self):
        self.database.audit.delete_many({})

    def assert_volatiles(self, expected, vtype=None, key=None, fields=None):
        filter_ = {}
        if vtype is not None:
            filter_['vtype'] = vtype
        if key is not None:
            filter_['key'] = key
        records = self.database.volatile.find(filter_, {'_id': False})
        records = list(records.sort([('vtype', 1), ('key', 1)]))
        for record in records:
            record['source'] = model._deserialize(record['source'])
            if record['value']:
                record['value'] = model._deserialize(record['value'])
        if fields:
            fields = set(fields)
            for record in records:
                for f in set(record) - fields:
                    del record[f]
        try:
            self.assertEquals(records, expected)
        except:
            import pprint
            pprint.pprint(records)
            raise

    def reset_volatiles(self):
        self.database.volatile.delete_many({
            '$nor': [{'vtype': 'section'}, {'key': model.volatile_key_hash('')}],
        })

    def save_rules(self, username, path, new_rules):
        rules = [{'name': rulename,
                  'selector': 'sel',
                  'subrules': [],
                  'editors': [],
                  'config': {'foo': 1}}
                 for rulename in new_rules]
        section = self.storage._find_section(path)
        usergroups = ()
        self.storage._save_rules(username, usergroups, section, new_rules=rules,
                                 action='save_rules', affected_rules=new_rules,
                                 extra={})

    def get_section_subtree(self, path, revision=None, max_depth=-1,
                            with_status=False):
        tree = self.storage.get_section_subtree(path, revision, max_depth)
        if with_status:
            return tree
        stack = [tree]
        while stack:
            sec = stack.pop()
            del sec['status']
            if isinstance(sec['subsections'], dict):
                stack.extend(sec['subsections'].values())
        return tree


class CreateSectionTestCase(ModelTestCase):
    def test_create_root(self):
        path = self.storage.create_section(
            username="user1", usergroups=['gg'],
            parent_path="", parent_rev=1, name="section1",
            desc="long winding description", owners=["user3", "user2"],
            stype='yaml', stype_options=None
        )
        self.assertEquals(path, "section1")
        [root, result] = list(self.database.section
                              .find({'revision': 2}).sort('path'))
        del result['_id']
        self.assertEquals(result, {
            'changed_by': 'user1',
            'name': 'section1',
            'marked_by': [],
            "all_editors": [],
            'path': 'section1',
            'revision': 2,
            'desc': 'long winding description',
            'mtime': 1444000002,
            'ctime': 1444000002,
            'owners': ['user2', 'user3'],
            'rules': model._serialize([]),
            'subsections': model._serialize({}),
            'stype': 'yaml',
            'stype_options': None,
        })
        self.assert_audit('section1', [
            {'extra': {'desc': u'long winding description',
                       'owners': ['user2', 'user3'],
                       'stype': 'yaml', 'stype_options': None},
             'affected_rules': [],
             'path': 'section1',
             'result': 'success',
             'revision': 2,
             'what': 'create_section',
             'when': 1444000002,
             'who': u'user1',
             'who_groups': ['gg']},
        ])

    def test_create_root_nonunique_name(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", name="section1", parent_rev=1,
            desc="desc1", owners=["user3", "user2"],
            stype='yaml', stype_options=None,
        )
        with self.assertRaises(errors.NotUnique):
            self.storage.create_section(
                username="user2", usergroups=[],
                parent_path="", parent_rev=2,
                name="section1", desc="desc2", owners=[],
                stype='yaml', stype_options=None
            )
        self.assert_audit('section1', [
            {'extra': {'desc': 'desc1',
                       'owners': ['user2', 'user3'],
                       'stype': 'yaml', 'stype_options': None},
             'affected_rules': [],
             'path': 'section1',
             'result': 'success',
             'revision': 2,
             'what': 'create_section',
             'when': 1444000002,
             'who': u'user1',
             'who_groups': []},
        ])

    def test_create_subsection(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", name="sec1", parent_rev=1,
            desc="desc1", owners=["user3", 'group:g33'],
            stype='yaml', stype_options=None
        )
        path = self.storage.create_section(
            username="user31", usergroups=['g12', 'g33'],
            parent_path="sec1", name="sec2", parent_rev=2,
            desc="subdesc", owners=["user4"],
            stype='sandbox_resource', stype_options={'resource_type': 'r1234'},
        )
        self.assertEquals(path, "sec1.sec2")
        sections = list(self.database.section.find({'revision': 3}).sort('path'))
        self.assertEquals(len(sections), 3)
        root = sections[0]
        del sections[0]
        subsection_id = sections[1]['_id']
        self.assertEquals(sections[1], {
            'name': 'sec2',
            'marked_by': [],
            "all_editors": [],
            'changed_by': 'user31',
            'path': 'sec1.sec2',
            'revision': 3,
            'desc': 'subdesc',
            'mtime': 1444000003,
            'ctime': 1444000003,
            'owners': ['user4'],
            'rules': model._serialize([]),
            '_id': sections[1]['_id'],
            'stype': 'sandbox_resource',
            'stype_options': {'resource_type': 'r1234'},
            'subsections': model._serialize({}),
        })
        self.assertEquals(sections[0], {
            'name': 'sec1',
            'marked_by': [],
            "all_editors": [],
            'changed_by': 'user1',
            'path': 'sec1',
            'revision': 3,
            'desc': 'desc1',
            'mtime': 1444000002,
            'ctime': 1444000003,
            'owners': ['group:g33', 'user3'],
            'rules': model._serialize([]),
            '_id': sections[0]['_id'],
            'stype': 'yaml',
            'stype_options': None,
            'subsections': model._serialize(
                {'sec2': {'_id': sections[1]['_id'], 'subsections': {}}}
            )
        })

    def test_create_nonunique_subsection(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="section1",
            desc="desc", owners=[],
            stype='yaml', stype_options=None
        )
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="section1", parent_rev=2,
            name="section1", desc="desc1", owners=[],
            stype='yaml', stype_options=None
        )
        with self.assertRaises(errors.NotUnique):
            self.storage.create_section(
                username="user1", usergroups=[],
                parent_path="section1", parent_rev=3,
                name="section1", desc="desc2", owners=[],
                stype='yaml', stype_options=None
            )

    def test_create_subsection_not_owner(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="section1",
            desc="desc1", owners=['user3'],
            stype='yaml', stype_options=None
        )
        with self.assertRaises(errors.Unauthorized):
            self.storage.create_section(
                username="user4", usergroups=[],
                parent_path="section1", parent_rev=2,
                name="subsection1", desc="desc2", owners=['user4'],
                stype='yaml', stype_options=None
            )
        self.assert_audit('section1.subsection1', [
            {'extra': {'desc': 'desc2', 'owners': ['user4'],
                       'stype': 'yaml', 'stype_options': None},
             'affected_rules': [],
             'path': 'section1.subsection1',
             'result': 'model_error',
             'exc_class': 'Unauthorized',
             'revision': 2,
             'what': 'create_section',
             'when': 1444000003,
             'who': u'user4',
             'who_groups': []},
        ])

    def test_create_subsection_parent_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.create_section(
                username="user2", usergroups=[],
                parent_path="section1", parent_rev=1,
                name="subsection1",
                desc="desc2", owners=[],
                stype='yaml', stype_options=None
            )

    def test_invalid_name(self):
        invalid_names = [
            None, 33, "", "sub.section", "$and", "some\0thing",
        ]
        for name in invalid_names:
            with self.assertRaises(ValueError):
                self.storage.create_section(
                    username="user2", usergroups=[],
                    parent_path="", parent_rev=1,
                    name=name, desc="desc2", owners=[],
                    stype='yaml', stype_options=None
                )


class DeleteEmptySectionTestCase(ModelTestCase):
    def test_success(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="root",
            desc="desc", owners=[],
            stype='yaml', stype_options=None
        )
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="root", parent_rev=2, name="child1",
            desc="desc", owners=['group:coolusers'],
            stype='yaml', stype_options=None
        )
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="root.child1", parent_rev=3,
            name="child1-1", desc="desc", owners=[],
            stype='yaml', stype_options=None
        )
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="root.child1", parent_rev=4,
            name="child1-2", desc="desc", owners=[],
            stype='yaml', stype_options=None
        )
        self.reset_audit()
        self.storage.delete_empty_section("user1", ['gr2', 'gr1'],
                                          "root.child1.child1-2", 5)
        self.assert_audit('root.child1.child1-2', [
            {'extra': {'ctime': 1444000005,
                       'changed_by': 'user1',
                       'desc': 'desc',
                       'mtime': 1444000005,
                       'name': 'child1-2',
                       'marked_by': [],
                       "all_editors": [],
                       'owners': [],
                       'path': 'root.child1.child1-2',
                       'stype': 'yaml',
                       'stype_options': None,
                       'revision': 5,
                       'rules': [],
                       'subsections': {}},
             'affected_rules': [],
             'path': 'root.child1.child1-2',
             'result': 'success',
             'revision': 6,
             'what': 'delete_empty_section',
             'when': 1444000006,
             'who': 'user1',
             'who_groups': ['gr1', 'gr2']},
        ])
        self.storage.delete_empty_section("user1", [],
                                          "root.child1.child1-1", 4)

        child1 = self.database.section.find_one({'path': "root.child1"})
        self.assertEquals(model._deserialize(child1['subsections']), {})
        self.assertEquals(child1['changed_by'], 'user1')

        self.storage.delete_empty_section("user8", ['coolusers'],
                                          "root.child1", 7)
        self.storage.delete_empty_section("user1", [], "root", 8)

        self.assertEquals(self.database.volatile.find({'vtype': 'section'}).count(), 1)
        self.assertEquals(
            self.storage.get_section_subtree('root.child1')['deleted'], True
        )

    def test_not_empty(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="root",
            desc="desc", owners=[],
            stype='yaml', stype_options=None
        )
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="root", parent_rev=2, name="child1",
            desc="desc", owners=[],
            stype='yaml', stype_options=None
        )
        self.reset_audit()
        with self.assertRaises(errors.Unauthorized):
            self.storage.delete_empty_section("user1", ['ggrroouupp'],
                                              "root", 3)
        self.assert_audit('root', [
            {'exc_class': u'Unauthorized',
             'extra': {'changed_by': 'user1',
                       'ctime': 1444000003,
                       'desc': u'desc',
                       'mtime': 1444000002,
                       'name': u'root',
                       'marked_by': [],
                       "all_editors": [],
                       'owners': [],
                       'path': u'root',
                       'revision': 3,
                       'rules': [],
                       'stype': 'yaml',
                       'stype_options': None,
                       'subsections': {u'child1': {'_id': '#3',
                                                   'subsections': {}}}},
             'affected_rules': [],
             'path': u'root',
             'result': u'model_error',
             'revision': 3,
             'what': u'delete_empty_section',
             'when': 1444000004,
             'who': u'user1',
             'who_groups': ['ggrroouupp']},
        ])

    def test_not_owner(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="root",
            desc="desc", owners=[],
            stype='yaml', stype_options=None
        )
        self.reset_audit()
        with self.assertRaises(errors.Unauthorized):
            self.storage.delete_empty_section("user4", ['gg'], "root", 2)
        self.assert_audit('root', [
            {'exc_class': u'Unauthorized',
             'extra': {'ctime': 1444000002,
                       'changed_by': 'user1',
                       'desc': u'desc',
                       'mtime': 1444000002,
                       'name': u'root',
                       'marked_by': [],
                       "all_editors": [],
                       'owners': [],
                       'path': u'root',
                       'revision': 2,
                       'stype': 'yaml',
                       'stype_options': None,
                       'rules': [],
                       'subsections': {}},
             'affected_rules': [],
             'path': u'root',
             'result': u'model_error',
             'revision': 2,
             'what': u'delete_empty_section',
             'when': 1444000003,
             'who': u'user4',
             'who_groups': ['gg']},
        ])

    def test_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.delete_empty_section("user1", [], "root", 1)


class SetSectionDescTestCase(ModelTestCase):
    def test_success(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="root",
            desc="desc1", owners=['group:beatles'],
            stype='yaml', stype_options=None
        )
        self.reset_audit()
        self.storage.set_section_desc("user1", ['gg'], "root", 2, "new desc")
        [root, section] = list(self.database.section
                               .find({'revision': 3}).sort('path'))
        self.assertEquals(section['desc'], "new desc")
        self.assert_audit('root', [
            {'extra': {u'new': u'new desc', u'prev': u'desc1'},
             'affected_rules': [],
             'path': u'root',
             'result': u'success',
             'revision': 3,
             'what': u'set_section_desc',
             'when': 1444000003,
             'who': u'user1',
             'who_groups': ['gg']},
        ])
        self.storage.set_section_desc("user7", ['beatles'],
                                      "root", 3, "new new desc")
        [root, section] = list(self.database.section
                               .find({'revision': 4}).sort('path'))
        self.assertEquals(section['desc'], "new new desc")

    def test_not_owner(self):
        self.storage.create_section(
            username="user1", usergroups=[],
             parent_path="", parent_rev=1, name="root",
            desc="desc1", owners=[],
            stype='yaml', stype_options=None
        )
        self.reset_audit()
        with self.assertRaises(errors.Unauthorized):
            self.storage.set_section_desc("user4", [], "root", 2, "new desc")
        self.assert_audit('root', [
            {'exc_class': u'Unauthorized',
             'extra': {u'new': u'new desc', u'prev': u'desc1'},
             'affected_rules': [],
             'path': u'root',
             'result': u'model_error',
             'revision': 2,
             'what': u'set_section_desc',
             'when': 1444000003,
             'who': u'user4',
             'who_groups': []},
        ])

    def test_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.set_section_desc("user2", [], "root", 1, "new desc")


class SetOwnersTestCase(ModelTestCase):
    def test_success(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="root",
            desc="desc1", owners=['user5', 'user6'],
            stype='yaml', stype_options=None
        )
        self.reset_audit()
        self.storage.set_section_owners("user1", ['ggg'],
                                        "root", 2,
                                        ['user3', 'user9', 'group:gro'])
        [root, section] = list(self.database.section
                               .find({'revision': 3}).sort('path'))
        self.assertEquals(section['owners'], ['group:gro', 'user3', 'user9'])
        self.assert_audit('root', [
            {'extra': {'new': ['group:gro', 'user3', 'user9'],
                       'prev': ['user5', 'user6']},
             'affected_rules': [],
             'path': u'root',
             'result': u'success',
             'revision': 3,
             'what': u'set_section_owners',
             'when': 1444000003,
             'who': u'user1',
             'who_groups': ['ggg']},
        ])
        self.storage.set_section_owners("user99", ['gr1', 'ggg', 'gro'],
                                        "root", 3,
                                        ['user5'])
        [root, section] = list(self.database.section
                               .find({'revision': 4}).sort('path'))
        self.assertEquals(section['owners'], ['user5'])

    def test_not_owner(self):
        self.storage.create_section(
            username="user1", usergroups=['gr5'],
            parent_path="", parent_rev=1, name="root",
            desc="desc1", owners=[],
            stype='yaml', stype_options=None
        )
        self.reset_audit()
        with self.assertRaises(errors.Unauthorized):
            self.storage.set_section_owners("user4", [], "root", 2, ['blah'])
        self.assert_audit('root', [
            {'exc_class': u'Unauthorized',
             'extra': {u'new': ['blah'], u'prev': []},
             'affected_rules': [],
             'path': u'root',
             'result': u'model_error',
             'revision': 2,
             'what': u'set_section_owners',
             'when': 1444000003,
             'who': u'user4',
             'who_groups': []},
        ])

    def test_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.set_section_owners("user2", [], "root", 1, [])


class _PrefilledTreeTestCase(ModelTestCase):
    def setUp(self):
        super(_PrefilledTreeTestCase, self).setUp()
        self.prefill_tree(self.SECTION_TREE)

    def prefill_tree(self, tree):
        stack = [(section, "") for section in tree]
        while stack:
            section, parent_path = stack.pop()
            parent_rev = self.storage._find_section(
                parent_path, projection={'revision': True}
            )['revision']
            self.storage.create_section(
                username=section['creator'],
                usergroups=section.get('creator_groups', []),
                parent_path=parent_path,
                parent_rev=parent_rev,
                name=section['name'], desc=section.get('desc', ''),
                owners=section['owners'],
                stype='yaml', stype_options=None
            )
            if parent_path == "":
                section_path = section['name']
            else:
                section_path = ".".join((parent_path, section['name']))
            stack.extend((subsection, section_path)
                         for subsection in section['subsections'])

    def assert_subtrees_equal(self, expected, actual):
        expected = copy.deepcopy(expected)
        actual = copy.deepcopy(actual)
        sections = [actual]
        while sections:
            section = sections.pop()
            del section['ctime']
            if isinstance(section['subsections'], dict):
                sections.extend(section['subsections'].values())
        sections = [expected]
        while sections:
            section = sections.pop()
            section.setdefault('rules', [])
            if isinstance(section['subsections'], dict):
                sections.extend(section['subsections'].values())
        try:
            self.assertEquals(expected, actual)
        except:
            import pprint
            pprint.pprint(actual)
            raise

    def assert_oids_for_path_revisions(self, expected_oids):
        for (path, revision), eoid in expected_oids.items():
            section = self.database.section.find_one({"path": path,
                                                      "revision": revision})
            assert section is not None, \
                "section path='{}' revision={} doesn't exist" \
                .format(path, revision)
            assert section['_id'] == eoid, \
                "section path='{}' revision={} has _id {}, expected {}" \
                .format(path, revision, section['_id'], eoid)


class GetSectionTreeTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = [
        {"creator": "user1",
         "name": "root",
         "owners": [],
         "desc": "rootdesc",
         "subsections": [
             {"creator": "user1",
              "name": "child1",
              "owners": ["user3"],
              "desc": "child1 desc",
              "subsections": [
                  {"creator": "user3",
                   "name": "child11",
                   "desc": "child11 desc",
                   "owners": ["user3"],
                   "subsections": []},
                  {"creator": "user1",
                   "name": "child12",
                   "desc": "child12 desc",
                   "owners": ["user3"],
                   "subsections": []},
              ]},
             {"creator": "user1",
              "name": "child2",
              "owners": [],
              "desc": "child2 desc",
              "subsections": [
                  {"creator": "user1",
                   "name": "child21",
                   "desc": "child21 desc",
                   "owners": [],
                   "subsections": []},
                  {"creator": "user1",
                   "name": "child22",
                   "desc": "child22 desc",
                   "owners": [],
                   "subsections": []},
              ]},
         ]},
    ]

    def test_structure_only(self):
        subtree = self.storage.get_section_subtree("", structure_only=True)
        expected_tree = {
            "name": "genisys",
            "path": "",
            "subsections": {
                "root": {
                    "name": "root",
                    "path": "root",
                    "subsections": {
                        "child1": {
                            "name": "child1",
                            "path": "root.child1",
                            "subsections": {
                                "child11": {
                                    "name": "child11",
                                    "path": "root.child1.child11",
                                    "subsections": {}},
                                "child12": {
                                    "name": "child12",
                                    "path": "root.child1.child12",
                                    "subsections": {}},
                            }},
                        "child2": {
                            "name": "child2",
                            "path": "root.child2",
                            "subsections": {
                                "child21": {
                                    "name": "child21",
                                    "path": "root.child2.child21",
                                    "subsections": {}},
                                "child22": {
                                    "name": "child22",
                                    "path": "root.child2.child22",
                                    "subsections": {}},
                            }},
                    }
                }
            }
        }
        self.assertEquals(subtree, expected_tree)

    def test_structure_only_depth(self):
        subtree = self.storage.get_section_subtree("root", structure_only=True,
                                                   max_depth=0)
        expected_tree = {"name": "root", "path": "root",
                         "subsections": ["child1", "child2"]}
        self.assertEquals(subtree, expected_tree)

        subtree = self.storage.get_section_subtree("root", structure_only=True,
                                                   max_depth=1)
        expected_tree = {
            "name": "root",
            "path": "root",
            "subsections": {
                "child1": {
                    "name": "child1",
                    "path": "root.child1",
                    "subsections": ['child11', 'child12'],
                    },
                "child2": {
                    "name": "child2",
                    "path": "root.child2",
                    "subsections": ['child21', 'child22'],
                    },
            }
        }
        self.assertEquals(subtree, expected_tree)

        subtree = self.storage.get_section_subtree(
            "root.child1", structure_only=True, max_depth=1
        )
        expected_tree = {
            "name": "child1",
            "path": "root.child1",
            "subsections": {
                "child11": {
                    "name": "child11",
                    "path": "root.child1.child11",
                    "subsections": []},
                "child12": {
                    "name": "child12",
                    "path": "root.child1.child12",
                    "subsections": []},
            }
        }
        self.assertEquals(subtree, expected_tree)

        subtree = self.storage.get_section_subtree(
            "root.child1.child12", structure_only=True, max_depth=1
        )
        expected_tree = {
            "name": "child12",
            "path": "root.child1.child12",
            "subsections": {}
        }
        self.assertEquals(subtree, expected_tree)

    def test_root(self):
        subtree = self.get_section_subtree("")['subsections']['root']
        expected_tree = {
            "name": "root",
            'marked_by': [],
            "all_editors": [],
            "path": "root",
            "revision": 8,
            'stype': 'yaml',
            'stype_options': None,
            "owners": [],
            "desc": "rootdesc",
            'mtime': 1444000002,
            'changed_by': 'user1',
            "subsections": {
                "child1": {
                    "name": "child1",
                    'marked_by': [],
                    "all_editors": [],
                    "owners": ["user3"],
                    "path": "root.child1",
                    "revision": 8,
                    'stype': 'yaml',
                    'stype_options': None,
                    "desc": "child1 desc",
                    'mtime': 1444000006,
                    'changed_by': 'user1',
                    "subsections": {
                        "child11": {
                            "name": "child11",
                            'marked_by': [],
                            "all_editors": [],
                            'changed_by': 'user3',
                            "path": "root.child1.child11",
                            "revision": 8,
                            'stype': 'yaml',
                            'stype_options': None,
                            "desc": "child11 desc",
                            'mtime': 1444000008,
                            "owners": ["user3"],
                            "subsections": {}},
                        "child12": {
                            "name": "child12",
                            'marked_by': [],
                            "all_editors": [],
                            'changed_by': 'user1',
                            "path": "root.child1.child12",
                            "revision": 7,
                            'stype': 'yaml',
                            'stype_options': None,
                            "desc": "child12 desc",
                            'mtime': 1444000007,
                            "owners": ["user3"],
                            "subsections": {}},
                    }},
                "child2": {
                    "name": "child2",
                    'marked_by': [],
                    "all_editors": [],
                    "owners": [],
                    "path": "root.child2",
                    "revision": 5,
                    'stype': 'yaml',
                    'stype_options': None,
                    "desc": "child2 desc",
                    'mtime': 1444000003,
                    'changed_by': 'user1',
                    "subsections": {
                        "child21": {
                            "name": "child21",
                            'marked_by': [],
                            "all_editors": [],
                            'changed_by': 'user1',
                            "path": "root.child2.child21",
                            "revision": 5,
                            'stype': 'yaml',
                            'stype_options': None,
                            "desc": "child21 desc",
                            'mtime': 1444000005,
                            "owners": [],
                            "subsections": {}},
                        "child22": {
                            "name": "child22",
                            'marked_by': [],
                            "all_editors": [],
                            'changed_by': 'user1',
                            "path": "root.child2.child22",
                            "revision": 4,
                            'stype': 'yaml',
                            'stype_options': None,
                            "desc": "child22 desc",
                            'mtime': 1444000004,
                            "owners": [],
                            "subsections": {}},
                    }},
            }
        }
        self.assert_subtrees_equal(expected_tree, subtree)
        expected_oids = {
            ("", 8): "#24",
            ("root", 8): "#23",
            ("root.child1", 8): "#22",
            ("root.child1.child11", 8): "#21",
            ("root.child1.child12", 7): "#17",
            ("root.child2", 5): "#11",
            ("root.child2.child21", 5): "#10",
            ("root.child2.child22", 4): "#6",
        }
        self.assert_oids_for_path_revisions(expected_oids)

    def test_subtree(self):
        child2 = self.get_section_subtree("root.child2")
        expected_tree = {
            "name": "child2",
            'marked_by': [],
            "all_editors": [],
            "owners": [],
            "path": "root.child2",
            "revision": 5,
            'stype': 'yaml',
            'stype_options': None,
            "desc": "child2 desc",
            'mtime': 1444000003,
            'changed_by': 'user1',
            "subsections": {
                "child21": {
                    "name": "child21",
                    'marked_by': [],
                    "all_editors": [],
                    'changed_by': 'user1',
                    "path": "root.child2.child21",
                    "revision": 5,
                    'stype': 'yaml',
                    'stype_options': None,
                    "desc": "child21 desc",
                    'mtime': 1444000005,
                    "owners": [],
                    "subsections": {}},
                "child22": {
                    "name": "child22",
                    'marked_by': [],
                    "all_editors": [],
                    'changed_by': 'user1',
                    "path": "root.child2.child22",
                    "revision": 4,
                    'stype': 'yaml',
                    'stype_options': None,
                    "desc": "child22 desc",
                    'mtime': 1444000004,
                    "owners": [],
                    "subsections": {}},
            }
        }
        self.assert_subtrees_equal(expected_tree, child2)

    def test_revisions(self):
        self.save_rules('user1', 'root.child1.child11',
                        new_rules=['rule1', 'rule2'])
        self.save_rules('user2', 'root.child2', new_rules=['rule3'])
        self.save_rules('user1', 'root.child1.child12', new_rules=['rule4'])
        self.save_rules('user3', 'root.child1.child12',
                        new_rules=['rule5', 'rule6'])

        child12 = self.get_section_subtree("root.child1.child12",
                                                   revision=9)
        expected_tree = {
            "name": "child12",
            'marked_by': [],
            "all_editors": [],
            "path": "root.child1.child12",
            'changed_by': 'user3',
            "revision": 9,
            'stype': 'yaml',
            'stype_options': None,
            "desc": "child12 desc",
            'mtime': 1444000012,
            "rules": [{'ctime': 1444000012,
                       'mtime': 1444000012,
                       'name': 'rule5',
                       'editors': [],
                       'config': {'foo': 1},
                       'selector': 'sel',
                       'subrules': []},
                      {'ctime': 1444000012,
                       'mtime': 1444000012,
                       'name': 'rule6',
                       'editors': [],
                       'config': {'foo': 1},
                       'selector': 'sel',
                       'subrules': []}],
            "owners": ["user3"],
            "subsections": {},
        }
        self.assert_subtrees_equal(expected_tree, child12)
        child12 = self.get_section_subtree("root.child1.child12")
        self.assert_subtrees_equal(expected_tree, child12)

        child12 = self.get_section_subtree("root.child1.child12", revision=8)
        expected_tree = {
            "name": "child12",
            'marked_by': [],
            "all_editors": [],
            "path": "root.child1.child12",
            'changed_by': 'user1',
            "revision": 8,
            'stype': 'yaml',
            'stype_options': None,
            "desc": "child12 desc",
            'mtime': 1444000011,
            "rules": [{'name': 'rule4',
                       'editors': [],
                       'ctime': 1444000011,
                       'mtime': 1444000011,
                       'config': {'foo': 1},
                       'selector': 'sel',
                       'subrules': []}],
            "owners": ["user3"],
            "subsections": {},
        }
        self.assert_subtrees_equal(expected_tree, child12)

        child2 = self.get_section_subtree("root.child2", revision=6)
        expected_tree = {
            "name": "child2",
            'marked_by': [],
            "all_editors": [],
            "path": "root.child2",
            'changed_by': 'user2',
            "revision": 6,
            'stype': 'yaml',
            'stype_options': None,
            "desc": "child2 desc",
            'mtime': 1444000010,
            "rules": [{'name': "rule3",
                       'editors': [],
                       'ctime': 1444000010,
                       'mtime': 1444000010,
                       'config': {'foo': 1},
                       'selector': 'sel',
                       'subrules': []}],
            "owners": [],
            "subsections": {
                'child21': {
                    'desc': u'child21 desc',
                    'mtime': 1444000005,
                    'name': u'child21',
                    'marked_by': [],
                    "all_editors": [],
                    'changed_by': 'user1',
                    'owners': [],
                    'path': u'root.child2.child21',
                    'revision': 5,
                    'stype': 'yaml',
                    'stype_options': None,
                    'rules': [],
                    'subsections': {}},
                'child22': {
                    'desc': u'child22 desc',
                    'mtime': 1444000004,
                    'name': u'child22',
                    'marked_by': [],
                    "all_editors": [],
                    'changed_by': 'user1',
                    'owners': [],
                    'path': u'root.child2.child22',
                    'revision': 4,
                    'stype': 'yaml',
                    'stype_options': None,
                    'rules': [],
                    'subsections': {}}
            }
        }
        self.assert_subtrees_equal(expected_tree, child2)
        child2 = self.get_section_subtree("root.child2")
        self.assert_subtrees_equal(expected_tree, child2)

        with self.assertRaises(errors.NotFound):
            self.get_section_subtree("root.child2", 19)


    def test_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.get_section_subtree("root.child222")

    def test_depth_zero(self):
        child2 = self.get_section_subtree("root.child2", max_depth=0)
        expected_tree = {
            "name": "child2",
            'marked_by': [],
            "all_editors": [],
            "path": "root.child2",
            'changed_by': 'user1',
            "revision": 5,
            'stype': 'yaml',
            'stype_options': None,
            "desc": "child2 desc",
            'mtime': 1444000003,
            "rules": [],
            "owners": [],
            "subsections": ['child21', 'child22'],
        }
        self.assert_subtrees_equal(expected_tree, child2)

    def test_depth_one(self):
        child2 = self.get_section_subtree("root.child2", max_depth=1)
        expected_tree = {
            "name": "child2",
            'marked_by': [],
            "all_editors": [],
            'changed_by': 'user1',
            "path": "root.child2",
            "revision": 5,
            'stype': 'yaml',
            'stype_options': None,
            "desc": "child2 desc",
            'mtime': 1444000003,
            "rules": [],
            "owners": [],
            "subsections": {
                'child21': {
                    'desc': u'child21 desc',
                    'mtime': 1444000005,
                    'name': u'child21',
                    'marked_by': [],
                    "all_editors": [],
                    'changed_by': 'user1',
                    'owners': [],
                    'path': u'root.child2.child21',
                    'revision': 5,
                    'stype': 'yaml',
                    'stype_options': None,
                    'rules': [],
                    'subsections': []},
                'child22': {
                    'desc': u'child22 desc',
                    'mtime': 1444000004,
                    'name': u'child22',
                    'marked_by': [],
                    "all_editors": [],
                    'changed_by': 'user1',
                    'owners': [],
                    'path': u'root.child2.child22',
                    'revision': 4,
                    'stype': 'yaml',
                    'stype_options': None,
                    'rules': [],
                    'subsections': []}
            },
        }
        self.assert_subtrees_equal(expected_tree, child2)


class SaveRulesTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = [
        {"creator": "user1",
         "name": "root1",
         "owners": [],
         "desc": "rootdesc",
         "subsections": [
             {"creator": "user1",
              "name": "child1",
              "owners": ['group:r1c1', "user3"],
              "desc": "child1 desc",
              "subsections": [
                  {"creator": "user3",
                   "name": "child11",
                   "desc": "child11 desc",
                   "owners": ["user3"],
                   "subsections": []},
                  {"creator": "user1",
                   "name": "child12",
                   "desc": "child12 desc",
                   "owners": ["user3"],
                   "subsections": []},
              ]},
             {"creator": "user1",
              "name": "child2",
              "owners": [],
              "desc": "child2 desc",
              "subsections": []},
         ]},
        {"creator": "user2",
         "name": "root2",
         "owners": [],
         "desc": "root2desc",
         "subsections": []},
    ]

    def test_success(self):
        self.reset_audit()
        self.reset_volatiles()
        self.save_rules('user1', 'root1.child1.child11',
                        new_rules=['rule1', 'rule2'])
        expected_tree = {
            "name": "genisys",
            'marked_by': [],
            "all_editors": [],
            "path": "",
            "revision": 8,
            'stype': 'yaml',
            'stype_options': None,
            "owners": ["user1", "user2"],
            "desc": "Root section",
            'mtime': 1444000001,
            'changed_by': 'user1',
            "subsections": {
                "root1": {
                    "name": "root1",
                    'marked_by': [],
                    "all_editors": [],
                    "owners": [],
                    "revision": 8,
                    'stype': 'yaml',
                    'stype_options': None,
                    "path": "root1",
                    "desc": "rootdesc",
                    'mtime': 1444000003,
                    'changed_by': 'user1',
                    "subsections": {
                        "child1": {
                            "name": "child1",
                            'marked_by': [],
                            "all_editors": [],
                            "owners": ['group:r1c1', "user3"],
                            "revision": 8,
                            'stype': 'yaml',
                            'stype_options': None,
                            "path": "root1.child1",
                            "desc": "child1 desc",
                            'mtime': 1444000005,
                            'changed_by': 'user1',
                            "subsections": {
                                "child11": {
                                    "name": "child11",
                                    'marked_by': [],
                                    "all_editors": [],
                                    "desc": "child11 desc",
                                    'mtime': 1444000008,
                                    "revision": 8,
                                    'stype': 'yaml',
                                    'stype_options': None,
                                    "path": "root1.child1.child11",
                                    "owners": ['user3'],
                                    "rules": [{'name': "rule1",
                                               'editors': [],
                                               'ctime': 1444000008,
                                               'mtime': 1444000008,
                                               'config': {'foo': 1},
                                               'selector': 'sel',
                                               'subrules': []},
                                              {'name': "rule2",
                                               'editors': [],
                                               'ctime': 1444000008,
                                               'mtime': 1444000008,
                                               'config': {'foo': 1},
                                               'selector': 'sel',
                                               'subrules': []}],
                                    'changed_by': 'user1',
                                    "subsections": {}},
                                "child12": {
                                    "name": "child12",
                                    'marked_by': [],
                                    "all_editors": [],
                                    "desc": "child12 desc",
                                    'mtime': 1444000006,
                                    "revision": 6,
                                    'stype': 'yaml',
                                    'stype_options': None,
                                    "path": "root1.child1.child12",
                                    "owners": ['user3'],
                                    'changed_by': 'user1',
                                    "subsections": {}},
                        }},
                        "child2": {
                            "name": "child2",
                            'marked_by': [],
                            "all_editors": [],
                            "owners": [],
                            "desc": "child2 desc",
                            'mtime': 1444000004,
                            "revision": 4,
                            'stype': 'yaml',
                            'stype_options': None,
                            "path": "root1.child2",
                            'changed_by': 'user1',
                            "subsections": {}
                        },
                    }},
                "root2": {
                    "name": "root2",
                    'marked_by': [],
                    "all_editors": [],
                    "owners": [],
                    "revision": 2,
                    'stype': 'yaml',
                    'stype_options': None,
                    "path": "root2",
                    "desc": "root2desc",
                    'mtime': 1444000002,
                    'changed_by': 'user2',
                    "subsections": {}},
            }
        }
        tree = self.get_section_subtree("", revision=8)
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_audit('root1', [])
        self.assert_audit('root1.child1', [])
        self.assert_audit('root1.child1.child11', [{
            'extra': {'prev_rules': []},
            'affected_rules': ['rule1', 'rule2'],
            'path': u'root1.child1.child11',
            'result': u'success',
            'revision': 8,
            'what': u'save_rules',
            'when': 1444000008,
            'who': u'user1',
            'who_groups': []}
        ])

        expected_oids = {
            ('', 1): '#root',
            ('root2', 2): '#1',
            ('', 2): '#2',
            ('root1', 3): '#3',
            ('', 3): '#4',
            ('root1.child2', 4): '#5',
            ('root1', 4): '#6',
            ('', 4): '#7',
            ('root1.child1', 5): '#8',
            ('root1', 5): '#9',
            ('', 5): '#10',
            ('root1.child1.child12', 6): '#11',
            ('root1.child1', 6): '#12',
            ('root1', 6): '#13',
            ('', 6): '#14',
            ('root1.child1.child11', 7): '#15',
            ('root1.child1', 7): '#16',
            ('root1', 7): '#17',
            ('', 7): '#18',
            ('root1.child1.child11', 8): '#19',
            ('root1.child1', 8): '#20',
            ('root1', 8): '#21',
            ('', 8): '#22',
        }
        self.assert_oids_for_path_revisions(expected_oids)

        self.assert_volatiles(vtype='selector', expected=[
            {'atime': 1444000008,
             'ctime': 1444000008,
             'etime': 1444000008,
             'key': "hash('sel')",
             'raw_key': 'sel',
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {},
             'mtime': None,
             'source': 'sel',
             'value': None,
             'vtype': 'selector'},
        ])
        self.assert_volatiles(vtype='section',
                              key="hash('root1.child1.child11')", expected=[
            {'atime': 1444000007,
             'ctime': 1444000008,
             'etime': 1444000008,
             'key': "hash('root1.child1.child11')",
             'raw_key': 'root1.child1.child11',
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {
                'changed_by': 'user1',
                'reresolve_selectors': True,
                'mtime': 1444000008,
                'revision': 8,
                'owners': ['user3']},
             'mtime': None,
             'source': {
                 'rules': [{'config': {'foo': 1},
                            'rule_name': 'rule1',
                            'selector_keys': ["hash('sel')"]},
                           {'config': {'foo': 1},
                            'rule_name': 'rule2',
                            'selector_keys': ["hash('sel')"]}],
                 'stype': 'yaml',
                 'stype_options': None},
             'value': None,
             'vtype': 'section'},
        ])

    def test_outdated(self):
        self.save_rules('user1', 'root1.child1', new_rules=['rule1', 'rule2'])
        self.reset_audit()
        section = self.storage._find_section('root1.child1', revision=5)
        with self.assertRaises(errors.Outdated):
            self.storage._save_rules('user1', [], section, new_rules=[],
                                     action='save_rules', affected_rules=[],
                                     extra={})
        self.assert_audit('root1.child1', [])

    def test_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.save_rules('user1', 'root4', new_rules=['rule3'])

    def test_not_owner(self):
        self.reset_audit()
        with self.assertRaises(errors.Unauthorized):
            self.save_rules('user3', 'root2', new_rules=['rule3'])
        self.assert_audit('root2', [
             {'exc_class': u'Unauthorized',
              'extra': {'prev_rules': []},
              'affected_rules': ['rule3'],
              'path': u'root2',
              'result': u'model_error',
              'revision': 2,
              'what': u'save_rules',
              'when': 1444000008,
              'who': u'user3',
              'who_groups': []}
        ])

    def test_concurent_edit(self):
        rev1_ancestors = self.storage._get_section_ancestors("root1.child2")
        self.save_rules('user1', 'root1.child1.child12', new_rules=['rule1'])
        self.reset_audit()
        with mock.patch.object(self.storage, '_get_section_ancestors') as ganc:
            ganc.return_value = rev1_ancestors
            with self.assertRaises(errors.Outdated):
                self.save_rules('user2', 'root1.child2', new_rules=['rule2'])
        self.assertEquals(
            self.database.section.find({'path': 'root1.child2'}).count(), 1
        )
        self.assert_audit('root1.child2', [
             {'exc_class': u'Outdated',
              'extra': {'prev_rules': []},
              'affected_rules': ['rule2'],
              'path': u'root1.child2',
              'result': u'model_error',
              'revision': 4,
              'what': u'save_rules',
              'when': 1444000009,
              'who': u'user2',
              'who_groups': []}
        ])


class RevertRulesTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = SaveRulesTestCase.SECTION_TREE

    def test_success(self):
        self.save_rules('user1', 'root1.child1', new_rules=['rule1', 'rule2'])
        self.save_rules('user1', 'root1.child1', new_rules=['rule3'])
        self.database.section.update_one(
            {'path': 'root1.child1', 'revision': 9},
            {'$set': {'stype': 'sandbox_resource',
                      'stype_options': {'resource_type': 'WHATEVER'}}}
        )
        self.reset_audit()
        self.storage.revert_rules('user2', ['g1'], 'root1.child1',
                                  current_rev=9, revert_to_rev=8)

        child1 = self.get_section_subtree("root1.child1")
        self.assertEquals(child1['revision'], 10)
        self.assertEquals(child1['rules'], [
            {'ctime': 1444000010,
             'mtime': 1444000010,
             'name': 'rule1',
             'editors': [],
             'config': {'foo': 1},
             'selector': 'sel',
             'subrules': []},
            {'ctime': 1444000010,
             'mtime': 1444000010,
             'name': 'rule2',
             'editors': [],
             'config': {'foo': 1},
             'selector': 'sel',
             'subrules': []}
        ])
        self.assert_audit('root1.child1', [
            {'affected_rules': ['rule1', 'rule2', 'rule3'],
             'extra': {'new': {'stype': 'yaml',
                               'stype_options': None},
                       'prev': {'stype': 'sandbox_resource',
                                'stype_options': {'resource_type': 'WHATEVER'}},
                       'prev_rules': [{'config': {'foo': 1},
                                       'editors': [],
                                       'ctime': 1444000009,
                                       'mtime': 1444000009,
                                       'name': 'rule3',
                                       'selector': 'sel',
                                       'subrules': []}],
                       'new_rules': [
                           {'config': {'foo': 1},
                            'ctime': 1444000010,
                            'mtime': 1444000010,
                            'name': 'rule1',
                            'editors': [],
                            'selector': 'sel',
                            'subrules': []},
                           {'config': {'foo': 1},
                            'ctime': 1444000010,
                            'mtime': 1444000010,
                            'name': 'rule2',
                            'editors': [],
                            'selector': 'sel',
                            'subrules': []},
                       ],
                       'revert_to_rev': 8,
                       'prev_rev': 9},
             'path': 'root1.child1',
             'result': 'success',
             'revision': 10,
             'what': 'revert_rules',
             'when': 1444000010,
             'who': 'user2',
             'who_groups': ['g1']}
        ])
        self.storage.revert_rules('user0', ['r1c1'], 'root1.child1',
                                  current_rev=10, revert_to_rev=8)

    def test_outdated(self):
        self.save_rules('user1', 'root1.child1', new_rules=['rule1', 'rule2'])
        self.save_rules('user1', 'root1.child1', new_rules=['rule3', 'rule4'])
        self.reset_audit()
        with self.assertRaises(errors.Outdated):
            self.storage.revert_rules('user1', [], 'root1.child1',
                                      current_rev=6, revert_to_rev=5)
        self.assert_audit('root1.child1', [])

    def test_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.revert_rules('user1', [], 'root4',
                                      current_rev=2, revert_to_rev=1)

    def test_not_owner(self):
        self.reset_audit()
        with self.assertRaises(errors.Unauthorized):
            self.storage.revert_rules('user3', [], 'root2',
                                      current_rev=2, revert_to_rev=2)
        self.assert_audit('root2', [
             {'exc_class': u'Unauthorized',
              'extra': {'new': {'stype': 'yaml', 'stype_options': None},
                        'prev': {'stype': 'yaml', 'stype_options': None},
                        'prev_rules': [],
                        'new_rules': [],
                        'revert_to_rev': 2,
                        'prev_rev': 2},
              'affected_rules': [],
              'path': u'root2',
              'result': u'model_error',
              'revision': 2,
              'what': u'revert_rules',
              'when': 1444000008,
              'who': u'user3',
              'who_groups': []}
        ])


class CreateChangeDeleteCreateAgainTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = []

    def test(self):
        self.storage.create_section(username="user1", usergroups=[],
                                    parent_path="", parent_rev=1,
                                    name="root", desc="desc",
                                    owners=["user4", 'group:sudoers'],
                                    stype='yaml', stype_options=None)
        self.storage.create_section(username="user4", usergroups=['somegroup'],
                                    parent_path="root", parent_rev=2,
                                    name="sec", desc="desc2", owners=[],
                                    stype='yaml', stype_options=None)
        self.storage.create_section(username="user90", usergroups=['sudoers'],
                                    parent_path="root.sec", parent_rev=3,
                                    name="subsec", desc="desc2", owners=[],
                                    stype='yaml', stype_options=None)

        self.save_rules('user4', 'root.sec.subsec', new_rules=['rules1'])
        self.save_rules('user4', 'root.sec', new_rules=['rules2'])

        self.save_rules('user4', 'root.sec.subsec', new_rules=[])
        self.save_rules('user4', 'root.sec', new_rules=[])

        self.storage.delete_empty_section('user4', [], 'root.sec.subsec', 6)
        self.storage.delete_empty_section('user4', [], 'root.sec', 9)

        tree = self.get_section_subtree("")
        expected_tree = {
            "name": "genisys",
            "path": "",
            "revision": 10,
            "desc": "Root section",
            'marked_by': [],
            'all_editors': [],
            'mtime': 1444000001,
            "rules": [],
            "owners": ["user1", "user2"],
            'changed_by': 'user1',
            'stype': 'yaml',
            'stype_options': None,
            "subsections": {
                'root': {
                    'desc': u'desc',
                    'mtime': 1444000002,
                    'name': u'root',
                    'marked_by': [],
                    'all_editors': [],
                    'owners': ['group:sudoers', 'user4'],
                    'path': u'root',
                    'changed_by': 'user1',
                    'revision': 10,
                    'rules': [],
                    'stype': 'yaml',
                    'stype_options': None,
                    'subsections': {}},
            }
        }
        self.assert_subtrees_equal(expected_tree, tree)

        self.storage.create_section(username="user4", usergroups=[],
                                    parent_path="root", parent_rev=10,
                                    name="sec", desc="desc3", owners=[],
                                    stype='yaml', stype_options=None)
        self.storage.create_section(username="user4", usergroups=[],
                                    parent_path="root.sec", parent_rev=11,
                                    name="subsec", desc="desc3", owners=[],
                                    stype='yaml', stype_options=None)
        self.save_rules('user4', 'root.sec.subsec', new_rules=['rules3'])
        self.save_rules('user4', 'root.sec', new_rules=['rules4'])

        tree = self.get_section_subtree("")
        expected_tree = {
            "name": "genisys",
            "path": "",
            "revision": 14,
            "desc": "Root section",
            'marked_by': [],
            'all_editors': [],
            'mtime': 1444000001,
            "rules": [],
            "owners": ["user1", "user2"],
            'changed_by': 'user1',
            'stype': 'yaml',
            'stype_options': None,
            "subsections": {
                'root': {
                    'desc': u'desc',
                    'mtime': 1444000002,
                    'name': u'root',
                    'owners': ['group:sudoers', 'user4'],
                    'path': u'root',
                    'marked_by': [],
                    'all_editors': [],
                    'changed_by': 'user1',
                    'revision': 14,
                    'rules': [],
                    'stype': 'yaml',
                    'stype_options': None,
                    'subsections': {
                        'sec': {
                            'desc': u'desc3',
                            'mtime': 1444000014,
                            'name': u'sec',
                            'marked_by': [],
                            'all_editors': [],
                            'owners': [],
                            'path': u'root.sec',
                            'changed_by': 'user4',
                            'revision': 14,
                            'rules': [{'name': 'rules4',
                                       'editors': [],
                                       'ctime': 1444000014,
                                       'mtime': 1444000014,
                                       'config': {'foo': 1},
                                       'selector': 'sel',
                                       'subrules': []}],
                            'stype': 'yaml',
                            'stype_options': None,
                            'subsections': {
                                'subsec': {
                                    'desc': u'desc3',
                                    'mtime': 1444000013,
                                    'name': u'subsec',
                                    'marked_by': [],
                                    'all_editors': [],
                                    'owners': [],
                                    'path': u'root.sec.subsec',
                                    'changed_by': 'user4',
                                    'revision': 13,
                                    'rules': [{'name': 'rules3',
                                               'editors': [],
                                               'ctime': 1444000013,
                                               'mtime': 1444000013,
                                               'config': {'foo': 1},
                                               'selector': 'sel',
                                               'subrules': []}],
                                    'stype': 'yaml',
                                    'stype_options': None,
                                    'subsections': {}},
                                }
                        }
                    }
                },
            }
        }
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_audit('root.sec.subsec', [
            {'extra': {'prev_rules': []},
             'affected_rules': ['rules3'],
             'path': 'root.sec.subsec',
             'result': 'success',
             'revision': 13,
             'what': 'save_rules',
             'when': 1444000013,
             'who': 'user4',
             'who_groups': []},
            {'extra': {'desc': 'desc3',
                       'owners': [],
                       'stype': 'yaml', 'stype_options': None},
             'affected_rules': [],
             'path': 'root.sec.subsec',
             'result': 'success',
             'revision': 12,
             'what': 'create_section',
             'when': 1444000012,
             'who': 'user4',
             'who_groups': []},
            {'extra': {'changed_by': 'user4',
                       'ctime': 1444000007,
                       'desc': 'desc2',
                       'mtime': 1444000007,
                       'name': 'subsec',
                       'marked_by': [],
                       "all_editors": [],
                       'owners': [],
                       'path': 'root.sec.subsec',
                       'revision': 6,
                       'rules': [],
                       'subsections': {},
                       'stype': 'yaml',
                       'stype_options': None},
             'affected_rules': [],
             'path': 'root.sec.subsec',
             'result': 'success',
             'revision': 7,
             'what': 'delete_empty_section',
             'when': 1444000009,
             'who': 'user4',
             'who_groups': []},
            {'extra': {'prev_rules': [{'ctime': 1444000005,
                                       'mtime': 1444000005,
                                       'name': 'rules1',
                                       'editors': [],
                                       'config': {'foo': 1},
                                       'selector': 'sel',
                                       'subrules': []}]},
             'affected_rules': [],
             'path': 'root.sec.subsec',
             'result': 'success',
             'revision': 6,
             'what': 'save_rules',
             'when': 1444000007,
             'who': 'user4',
             'who_groups': []},
            {'extra': {'prev_rules': []},
             'affected_rules': ['rules1'],
             'path': 'root.sec.subsec',
             'result': 'success',
             'revision': 5,
             'what': 'save_rules',
             'when': 1444000005,
             'who': 'user4',
             'who_groups': []},
            {'extra': {'desc': 'desc2',
                       'owners': [],
                       'stype': 'yaml', 'stype_options': None},
             'affected_rules': [],
             'path': 'root.sec.subsec',
             'result': 'success',
             'revision': 4,
             'what': 'create_section',
             'when': 1444000004,
             'who': 'user90',
             'who_groups': ['sudoers']}
        ])

        history = self.storage.get_section_history('root.sec.subsec')
        self.assertEquals(history, [
             {'extra': {'prev_rules': []},
              'affected_rules': ['rules3'],
              'path': 'root.sec.subsec',
              'result': 'success',
              'revision': 13,
              'what': 'save_rules',
              'when': 1444000013,
              'who': 'user4',
              'who_groups': []},
             {'extra': {'desc': 'desc3',
                        'owners': [],
                        'stype': 'yaml', 'stype_options': None},
              'affected_rules': [],
              'path': 'root.sec.subsec',
              'result': 'success',
              'revision': 12,
              'what': 'create_section',
              'when': 1444000012,
              'who': 'user4',
              'who_groups': []},
             {'extra': {'changed_by': 'user4',
                        'ctime': 1444000007,
                        'desc': 'desc2',
                        'mtime': 1444000007,
                        'name': 'subsec',
                        'marked_by': [],
                        "all_editors": [],
                        'owners': [],
                        'path': 'root.sec.subsec',
                        'revision': 6,
                        'rules': [],
                        'stype': 'yaml',
                        'stype_options': None,
                        'subsections': {}},
              'affected_rules': [],
              'path': 'root.sec.subsec',
              'result': 'success',
              'revision': 7,
              'what': 'delete_empty_section',
              'when': 1444000009,
              'who': 'user4',
              'who_groups': []},
             {'extra': {'prev_rules': [{'name': 'rules1',
                                        'ctime': 1444000005,
                                        'mtime': 1444000005,
                                        'editors': [],
                                        'config': {'foo': 1},
                                        'selector': 'sel',
                                        'subrules': []}]},
              'affected_rules': [],
              'path': 'root.sec.subsec',
              'result': 'success',
              'revision': 6,
              'what': 'save_rules',
              'when': 1444000007,
              'who': 'user4',
              'who_groups': []},
             {'extra': {'prev_rules': []},
              'affected_rules': ['rules1'],
              'path': 'root.sec.subsec',
              'result': 'success',
              'revision': 5,
              'what': 'save_rules',
              'when': 1444000005,
              'who': 'user4',
              'who_groups': []},
             {'extra': {'desc': 'desc2',
                        'owners': [],
                        'stype': 'yaml', 'stype_options': None},
              'affected_rules': [],
              'path': 'root.sec.subsec',
              'result': 'success',
              'revision': 4,
              'what': 'create_section',
              'when': 1444000004,
              'who': 'user90',
              'who_groups': ['sudoers']}
        ])

        history2 = self.storage.get_section_history('root.sec.subsec', limit=4)
        self.assertEquals(history2, history[:4])
        history2 = self.storage.get_section_history('root.sec.subsec',
                                                    offset=2)
        self.assertEquals(history2, history[2:])
        history2 = self.storage.get_section_history('root.sec.subsec',
                                                    offset=2, limit=2)
        self.assertEquals(history2, history[2:4])

        rhist = self.storage.get_section_history('root.sec', recursive=True)
        rhist = [dict(path=rec['path'], when=rec['when'], what=rec['what'])
                 for rec in rhist]
        self.assertEquals(rhist, [
            {'when': 1444000014, 'what': 'save_rules', 'path': 'root.sec'},
            {'when': 1444000013, 'what': 'save_rules', 'path': 'root.sec.subsec'},
            {'when': 1444000012, 'what': 'create_section', 'path': 'root.sec.subsec'},
            {'when': 1444000011, 'what': 'create_section', 'path': 'root.sec'},
            {'when': 1444000010, 'what': 'delete_empty_section', 'path': 'root.sec'},
            {'when': 1444000009, 'what': 'delete_empty_section', 'path': 'root.sec.subsec'},
            {'when': 1444000008, 'what': 'save_rules', 'path': 'root.sec'},
            {'when': 1444000007, 'what': 'save_rules', 'path': 'root.sec.subsec'},
            {'when': 1444000006, 'what': 'save_rules', 'path': 'root.sec'},
            {'when': 1444000005, 'what': 'save_rules', 'path': 'root.sec.subsec'},
            {'when': 1444000004, 'what': 'create_section', 'path': 'root.sec.subsec'},
            {'when': 1444000003, 'what': 'create_section', 'path': 'root.sec'},
        ])

        rhist2 = self.storage.get_section_history('', recursive=True)
        rhist2 = [dict(path=rec['path'], when=rec['when'], what=rec['what'])
                  for rec in rhist2]
        self.assertEquals(rhist2, rhist + [
            {'when': 1444000002, 'what': 'create_section', 'path': 'root'},
            {'when': 1444000001, 'what': 'create_section', 'path': ''}
        ])

        rulehist = self.storage.get_section_history('root.sec.subsec',
                                                    rule='rules1')
        rulehist = [dict(path=rec['path'], when=rec['when'], what=rec['what'])
                    for rec in rulehist]
        self.assertEquals(rulehist, [
            {'path': 'root.sec.subsec', 'what': 'save_rules', 'when': 1444000005}
        ])
        rulehist = self.storage.get_section_history('root.sec.subsec',
                                                    rule='rules100')
        self.assertEquals(rulehist, [])


class AuditTestCase(ModelTestCase):
    def test_unhandled_exception(self):
        with mock.patch.object(self.storage, '_generate_oid') as upd_sect:
            upd_sect.side_effect = ZeroDivisionError
            with self.assertRaises(ZeroDivisionError):
                self.storage.create_section(
                    username="user1", usergroups=['ug1', 'ug2'],
                    parent_path="", parent_rev=1,
                    name="section1", desc="descr", owners=["user5"],
                    stype='yaml', stype_options=None
                )
        self.assert_audit('section1', [
             {'extra': {'desc': u'descr', 'owners': ['user5'],
                        'stype': 'yaml', 'stype_options': None},
              'affected_rules': [],
              'path': 'section1',
              'result': 'unhandled_error',
              'exc_class': 'ZeroDivisionError',
              'revision': 1,
              'traceback': r'in _mock_call\n\s+raise effect\nZeroDivisionError\n$',
              'what': u'create_section',
              'when': 1444000002,
              'who': u'user1',
              'who_groups': ['ug1', 'ug2']}
        ])


class CreateRuleTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = []

    def test_yaml(self):
        self.storage.create_section(username="user1", usergroups=[],
                                    parent_path="", parent_rev=1,
                                    name="sec", desc="desc", owners=["user4"],
                                    stype='yaml', stype_options={})
        self.reset_audit()
        self.storage.create_rule(
            'user4', ['g1', 'g2'], 'sec', old_revision=2, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov expr', config={'resource_id': 44},
            config_source='cs',
        )
        expected_tree = {
            'desc': 'Root section',
            'marked_by': [],
            "all_editors": [],
            'mtime': 1444000001,
            'changed_by': 'user1',
            'name': 'genisys',
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 3,
            'rules': [],
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {'sec': {
                'desc': 'desc',
                'mtime': 1444000003,
                'changed_by': 'user4',
                'name': 'sec',
                'marked_by': [],
                "all_editors": ['user6', 'user7'],
                'owners': ['user4'],
                'path': 'sec',
                'revision': 3,
                'stype': 'yaml',
                'stype_options': {},
                'rules': [{'config': {'resource_id': 44},
                           'config_source': 'cs',
                           'ctime': 1444000003,
                           'mtime': 1444000003,
                           'desc': 'ruledesc',
                           'editors': ['user6', 'user7'],
                           'name': 'rule1',
                           'selector': 'blinov expr',
                           'subrules': []}],
                'subsections': {}}
            }
        }
        tree = self.get_section_subtree("")
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_volatiles([
            {'atime': 1444000001,
             'ctime': 1444000001,
             'etime': 1444000001,
             'key': "hash('')",
             'raw_key': '',
             'last_status': 'new',
             'lock_id': None,
             'locked': False,
             'mcount': 0,
             'meta': {
                 'changed_by': 'user1',
                 'reresolve_selectors': True,
                 'mtime': 1444000001,
                 'revision': 1,
                 'owners': ['user1', 'user2']},
             'mtime': None,
             'proclog': [],
             'source': {'rules': [], 'stype': 'yaml', 'stype_options': None},
             'tcount': 0,
             'ttime': None,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'utime': None,
             'value': None,
             'vtype': 'section'},
            {'atime': 1444000002,
             'ctime': 1444000003,
             'etime': 1444000003,
             'key': "hash('sec')",
             'raw_key': 'sec',
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {
                'changed_by': 'user4',
                'reresolve_selectors': True,
                'mtime': 1444000003,
                'revision': 3,
                'owners': ['user4']},
             'mtime': None,
             'source': {'rules': [{'config': {'resource_id': 44},
                                   'rule_name': 'rule1',
                                   'selector_keys': ["hash('blinov expr')"]}],
                        'stype': 'yaml',
                        'stype_options': {}},
             'value': None,
             'vtype': 'section'},
            {'atime': 1444000003,
             'ctime': 1444000003,
             'etime': 1444000003,
             'key': "hash('blinov expr')",
             'raw_key': 'blinov expr',
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {},
             'mtime': None,
             'source': 'blinov expr',
             'value': None,
             'vtype': 'selector'}
        ])

    def test_default_rule(self):
        self.storage.create_section(username="user1", usergroups=[],
                                    parent_path="", parent_rev=1,
                                    name="sec", desc="desc", owners=["user4"],
                                    stype='yaml', stype_options={})
        self.reset_audit()
        self.storage.create_rule(
            'user4', [], 'sec', old_revision=2, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
            selector=None, config={'resource_id': 44}, config_source='cs',
        )
        expected_tree = {
           'desc': 'desc',
           'mtime': 1444000003,
           'changed_by': 'user4',
           'name': 'sec',
           'marked_by': [],
           "all_editors": ['user6', 'user7'],
           'owners': ['user4'],
           'path': 'sec',
           'revision': 3,
           'stype': 'yaml',
           'stype_options': {},
           'rules': [{'config': {'resource_id': 44},
                      'config_source': 'cs',
                      'ctime': 1444000003,
                      'mtime': 1444000003,
                      'desc': 'ruledesc',
                      'editors': ['user6', 'user7'],
                      'name': 'rule1',
                      'selector': None,
                      'subrules': []}],
           'subsections': {}
        }
        tree = self.get_section_subtree("sec")
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_volatiles(vtype='selector', expected=[])
        self.assert_volatiles(vtype='section', expected=[
            {'atime': 1444000001,
             'ctime': 1444000001,
             'etime': 1444000001,
             'key': "hash('')",
             'raw_key': '',
             'last_status': 'new',
             'lock_id': None,
             'locked': False,
             'mcount': 0,
             'meta': {
                 'changed_by': 'user1',
                 'reresolve_selectors': True,
                 'mtime': 1444000001,
                 'revision': 1,
                 'owners': ['user1', 'user2']},
             'mtime': None,
             'proclog': [],
             'source': {'rules': [], 'stype': 'yaml', 'stype_options': None},
             'tcount': 0,
             'ttime': None,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'utime': None,
             'value': None,
             'vtype': 'section'},
            {'atime': 1444000002,
             'ctime': 1444000003,
             'etime': 1444000003,
             'key': "hash('sec')",
             'raw_key': 'sec',
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {
                'changed_by': 'user4',
                'reresolve_selectors': True,
                'mtime': 1444000003,
                'revision': 3,
                'owners': ['user4']},
             'mtime': None,
             'source': {'rules': [{'config': {'resource_id': 44},
                                   'rule_name': 'rule1',
                                   'selector_keys': []}],
                        'stype': 'yaml',
                        'stype_options': {}},
             'value': None,
             'vtype': 'section'},
        ])

    def test_subrules(self):
        self.reset_audit()
        self.storage.create_rule(
            'user1', [], '', old_revision=1, parent_rule=None,
            rulename='r1', desc='ruledesc1', editors=['user6', 'user7'],
            selector='expr1', config={'c': 1}, config_source='cs1',
        )
        self.storage.create_rule(
            'user1', [], '', old_revision=2, parent_rule='r1',
            rulename='r2', desc='ruledesc2', editors=['user8', 'user9'],
            selector='expr2', config={'c': 2}, config_source='cs2',
        )
        self.storage.create_rule(
            'user2', [], '', old_revision=3, parent_rule=None,
            rulename='r3', desc='ruledesc3', editors=['user6', 'user7'],
            selector='expr3', config={'c': 3}, config_source='cs3',
        )
        self.storage.create_rule(
            'user6', [], '', old_revision=4, parent_rule='r1',
            rulename='r4', desc='ruledesc4', editors=['user10'],
            selector='expr4', config={'c': 4}, config_source='cs4',
        )
        expected_tree = {
            'desc': 'Root section',
            'marked_by': [],
            "all_editors": ['user10', 'user6', 'user7', 'user8', 'user9'],
            'mtime': 1444000005,
            'changed_by': 'user6',
            'name': 'genisys',
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 5,
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {},
            'rules': [{'config': {'c': 1},
                       'config_source': 'cs1',
                       'ctime': 1444000002,
                       'desc': 'ruledesc1',
                       'editors': ['user6', 'user7'],
                       'mtime': 1444000005,
                       'name': 'r1',
                       'selector': 'expr1',
                       'subrules': [{'config': {'c': 2},
                                     'config_source': 'cs2',
                                     'ctime': 1444000003,
                                     'desc': 'ruledesc2',
                                     'editors': ['user8', 'user9'],
                                     'mtime': 1444000003,
                                     'name': 'r2',
                                     'selector': 'expr2'},
                                    {'config': {'c': 4},
                                     'config_source': 'cs4',
                                     'ctime': 1444000005,
                                     'desc': 'ruledesc4',
                                     'editors': ['user10'],
                                     'mtime': 1444000005,
                                     'name': 'r4',
                                     'selector': 'expr4'}]},
                      {'config': {'c': 3},
                       'config_source': 'cs3',
                       'ctime': 1444000004,
                       'desc': 'ruledesc3',
                       'editors': ['user6', 'user7'],
                       'mtime': 1444000004,
                       'name': 'r3',
                       'selector': 'expr3',
                       'subrules': []}]
        }
        tree = self.get_section_subtree("")
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_volatiles(vtype='section', fields=['key', 'meta', 'source'], expected=[
            {'key': "hash('')",
             'meta': {'changed_by': 'user6',
                      'reresolve_selectors': True,
                      'mtime': 1444000005,
                      'owners': ['user1', 'user2'],
                      'revision': 5},
             'source': {'rules': [{'config': {'c': 2},
                                   'rule_name': 'r2',
                                   'selector_keys': ["hash('expr1')", "hash('expr2')"]},
                                  {'config': {'c': 4},
                                   'rule_name': 'r4',
                                   'selector_keys': ["hash('expr1')", "hash('expr4')"]},
                                  {'config': {'c': 1},
                                   'rule_name': 'r1',
                                   'selector_keys': ["hash('expr1')"]},
                                  {'config': {'c': 3},
                                   'rule_name': 'r3',
                                   'selector_keys': ["hash('expr3')"]}],
                        'stype': 'yaml',
                        'stype_options': None}}
        ])
        self.assert_volatiles(vtype='selector', fields=['key', 'source'], expected=[
            {'key': "hash('expr1')", 'source': 'expr1'},
            {'key': "hash('expr2')", 'source': 'expr2'},
            {'key': "hash('expr3')", 'source': 'expr3'},
            {'key': "hash('expr4')", 'source': 'expr4'}
        ])
        self.assert_audit('', fields=['affected_rules', 'what', 'who'],
                          extra_fields=['parent_rule'], expected=[
            {'affected_rules': ['r4', 'r1'],
             'extra': {'parent_rule': 'r1'},
             'what': 'create_rule',
             'who': 'user6'},
            {'affected_rules': ['r3'],
             'extra': {'parent_rule': None},
             'what': 'create_rule',
             'who': 'user2'},
            {'affected_rules': ['r2', 'r1'],
             'extra': {'parent_rule': 'r1'},
             'what': 'create_rule',
             'who': 'user1'},
            {'affected_rules': ['r1'],
             'extra': {'parent_rule': None},
             'what': 'create_rule',
             'who': 'user1'}
        ])

    def test_subrule_group_auth(self):
        self.storage.create_section(username="user1", usergroups=[],
                                    parent_path="", parent_rev=1,
                                    name="sec", desc="desc", owners=[],
                                    stype='yaml', stype_options=None)
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=2, parent_rule=None,
            rulename='root-rule', desc='ruledesc', editors=['group:editors'],
            selector='blinov1', config={'foo': 1}, config_source='cs1',
        )
        self.storage.create_rule(
            'user6', ['editors'], 'sec', old_revision=3, parent_rule='root-rule',
            rulename='r4', desc='ruledesc4', editors=['user10'],
            selector='expr4', config={'c': 4}, config_source='cs4',
        )

    def test_parent_rule_not_found(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1, parent_rule=None,
            rulename='r1', desc='ruledesc1', editors=['user6', 'user7'],
            selector='expr1', config={'c': 1}, config_source='cs1',
        )
        self.storage.create_rule(
            'user1', [], '', old_revision=2, parent_rule='r1',
            rulename='r2', desc='ruledesc2', editors=['user8', 'user9'],
            selector='expr2', config={'c': 2}, config_source='cs2',
        )
        with self.assertRaises(errors.NotFound):
            self.storage.create_rule(
                'user1', [], '', old_revision=3, parent_rule='r2',
                rulename='r3', desc='ruledesc3', editors=['user8', 'user9'],
                selector='expr3', config={'c': 3}, config_source='cs3',
            )

    def test_sandbox_resource(self):
        self.storage.create_section(username="user1", usergroups=[],
                                    parent_path="", parent_rev=1,
                                    name="sec", desc="desc",
                                    owners=["user4", 'group:bloop'],
                                    stype='sandbox_resource',
                                    stype_options={'resource_type': 'r4567'})
        self.reset_audit()
        self.storage.create_rule(
            'user19', ['bloop'], 'sec', old_revision=2, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov expr', config={'resource_id': 44},
            config_source='cs',
        )
        expected_tree = {
            'desc': 'Root section',
            'marked_by': [],
            "all_editors": [],
            'mtime': 1444000001,
            'changed_by': 'user1',
            'name': 'genisys',
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 3,
            'rules': [],
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {'sec': {
                'desc': 'desc',
                'mtime': 1444000003,
                'changed_by': 'user19',
                'name': 'sec',
                'marked_by': [],
                "all_editors": ['user6', 'user7'],
                'owners': ['group:bloop', 'user4'],
                'path': 'sec',
                'revision': 3,
                'stype': 'sandbox_resource',
                'stype_options': {'resource_type': 'r4567'},
                'rules': [{'config': {'resource_id': 44},
                           'config_source': 'cs',
                           'ctime': 1444000003,
                           'mtime': 1444000003,
                           'desc': 'ruledesc',
                           'editors': ['user6', 'user7'],
                           'name': 'rule1',
                           'selector': 'blinov expr',
                           'subrules': []}],
                'subsections': {}}
            }
        }
        tree = self.get_section_subtree("")
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_volatiles([
            {'atime': 1444000002,
             'ctime': 1444000002,
             'etime': 1444000002,
             'key': "hash('r4567')",
             'raw_key': "r4567",
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'lock_id': None,
             'locked': False,
             'meta': {},
             'mtime': None,
             'proclog': [],
             'source': {'resource_type': 'r4567'},
             'ttime': None,
             'utime': None,
             'value': None,
             'vtype': 'sandbox_releases'},
            {'atime': 1444000003,
             'ctime': 1444000003,
             'etime': 1444000003,
             'key': 'hash(44)',
             'raw_key': 44,
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'lock_id': None,
             'locked': False,
             'meta': {},
             'mtime': None,
             'proclog': [],
             'source': {'resource_id': 44},
             'ttime': None,
             'utime': None,
             'value': None,
             'vtype': 'sandbox_resource'},
            {'atime': 1444000001,
             'ctime': 1444000001,
             'etime': 1444000001,
             'key': "hash('')",
             'raw_key': "",
             'last_status': 'new',
             'lock_id': None,
             'locked': False,
             'mcount': 0,
             'meta': {
                 'changed_by': 'user1',
                 'reresolve_selectors': True,
                 'mtime': 1444000001,
                 'revision': 1,
                 'owners': ['user1', 'user2']},
             'mtime': None,
             'proclog': [],
             'source': {'rules': [], 'stype': 'yaml', 'stype_options': None},
             'tcount': 0,
             'ttime': None,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'utime': None,
             'value': None,
             'vtype': 'section'},
            {'atime': 1444000002,
             'ctime': 1444000003,
             'etime': 1444000003,
             'key': "hash('sec')",
             'raw_key': "sec",
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {
                'changed_by': 'user19',
                'reresolve_selectors': True,
                'mtime': 1444000003,
                'revision': 3,
                'owners': ['group:bloop', 'user4']},
             'mtime': None,
             'source': {'rules': [{'resource_key': 'hash(44)',
                                   'rule_name': 'rule1',
                                   'selector_keys': ["hash('blinov expr')"]}],
                        'stype': 'sandbox_resource',
                        'stype_options': {'resource_type': 'r4567'},
                        'sandbox_releases_key': "hash('r4567')"},
             'value': None,
             'vtype': 'section'},
            {'atime': 1444000003,
             'ctime': 1444000003,
             'etime': 1444000003,
             'key': "hash('blinov expr')",
             'raw_key': "blinov expr",
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {},
             'mtime': None,
             'source': 'blinov expr',
             'value': None,
             'vtype': 'selector'}
        ])

        self.storage.create_rule(
            'user1', [], 'sec', old_revision=3, parent_rule=None,
            rulename='rule2', desc='rule2desc', editors=[],
            selector='blinov expr2', config={'resource_id': 12340},
            config_source='{"bar": "baz"}',
        )
        tree = self.get_section_subtree("")
        expected_rules = [
            {'config': {'resource_id': 44},
             'config_source': 'cs',
             'ctime': 1444000003,
             'mtime': 1444000003,
             'desc': 'ruledesc',
             'editors': ['user6', 'user7'],
             'name': 'rule1',
             'selector': 'blinov expr',
             'subrules': []},
            {'config': {'resource_id': 12340},
             'config_source': '{"bar": "baz"}',
             'ctime': 1444000004,
             'mtime': 1444000004,
             'desc': 'rule2desc',
             'editors': [],
             'name': 'rule2',
             'selector': 'blinov expr2',
             'subrules': []},
        ]
        self.assertEquals(expected_rules, tree['subsections']['sec']['rules'])
        self.assert_audit('sec', [
            {'extra': {'config': {'resource_id': 12340},
                       'config_source': '{"bar": "baz"}',
                       'desc': 'rule2desc',
                       'editors': [],
                       'name': 'rule2',
                       'parent_rule': None,
                       'prev_rules': [{'config': {'resource_id': 44},
                                       'config_source': 'cs',
                                       'ctime': 1444000003,
                                       'mtime': 1444000003,
                                       'desc': 'ruledesc',
                                       'editors': ['user6', 'user7'],
                                       'name': 'rule1',
                                       'selector': 'blinov expr',
                                       'subrules': []}],
                       'selector': 'blinov expr2',
                       'stype': 'sandbox_resource',
                       'stype_options': {'resource_type': 'r4567'},
                       'subrules': []},
             'affected_rules': ['rule2'],
             'path': 'sec',
             'result': 'success',
             'revision': 4,
             'what': 'create_rule',
             'when': 1444000004,
             'who': 'user1',
             'who_groups': []},
            {'extra': {'config': {'resource_id': 44},
                       'config_source': 'cs',
                       'desc': 'ruledesc',
                       'editors': ['user6', 'user7'],
                       'name': 'rule1',
                       'prev_rules': [],
                       'parent_rule': None,
                       'selector': 'blinov expr',
                       'stype': 'sandbox_resource',
                       'stype_options': {'resource_type': 'r4567'},
                       'subrules': []},
             'affected_rules': ['rule1'],
             'path': 'sec',
             'result': 'success',
             'revision': 3,
             'what': 'create_rule',
             'when': 1444000003,
             'who': 'user19',
             'who_groups': ['bloop']},
        ])
        self.assert_volatiles(vtype='section', expected=[
            {'atime': 1444000001,
             'ctime': 1444000001,
             'etime': 1444000001,
             'key': "hash('')",
             'raw_key': "",
             'last_status': 'new',
             'lock_id': None,
             'locked': False,
             'mcount': 0,
             'meta': {
                 'changed_by': 'user1',
                 'reresolve_selectors': True,
                 'mtime': 1444000001,
                 'revision': 1,
                 'owners': ['user1', 'user2']},
             'mtime': None,
             'proclog': [],
             'source': {'rules': [], 'stype': 'yaml', 'stype_options': None},
             'tcount': 0,
             'ttime': None,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'utime': None,
             'value': None,
             'vtype': 'section'},
            {'atime': 1444000002,
             'ctime': 1444000004,
             'etime': 1444000004,
             'key': "hash('sec')",
             'raw_key': "sec",
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {
                'changed_by': 'user1',
                'reresolve_selectors': True,
                'mtime': 1444000004,
                'revision': 4,
                'owners': ['group:bloop', 'user4']},
             'mtime': None,
             'source': {'rules': [{'resource_key': "hash(44)",
                                   'rule_name': 'rule1',
                                   'selector_keys': ["hash('blinov expr')"]},
                                  {'resource_key': "hash(12340)",
                                   'rule_name': 'rule2',
                                   'selector_keys': ["hash('blinov expr2')"]}],
                        'stype': 'sandbox_resource',
                        'stype_options': {'resource_type': 'r4567'},
                        'sandbox_releases_key': "hash('r4567')"},
             'value': None,
             'vtype': 'section'}
        ])

    def test_outdated(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov expr', config={'foo': 'bar'}, config_source='cs',
        )
        self.reset_audit()
        self.reset_volatiles()
        with self.assertRaises(errors.Outdated):
            self.storage.create_rule(
                'user1', [], '', old_revision=1, parent_rule=None,
                rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
                selector='blinov expr', config={'foo': 'bar'},
                config_source='cs2',
            )
        self.assert_audit('', [])

    def test_name_not_unique(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1,
            rulename='rule1', desc='ruledesc', editors=[], parent_rule=None,
            selector='blinov expr', config={'foo': 'bar'}, config_source='cs',
        )
        self.reset_audit()
        self.reset_volatiles()
        with self.assertRaises(errors.NotUnique):
            self.storage.create_rule(
                'user1', [], '', old_revision=2, parent_rule=None,
                rulename='rule1', desc='ruledesc', editors=[],
                selector='blinov expr', config={'foo': 'bar'},
                config_source='cs2',
            )
        self.assert_audit('', [])

    def test_subrule_name_not_unique(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1,
            rulename='rule1', desc='ruledesc', editors=[], parent_rule=None,
            selector='blinov expr', config={'foo': 'bar'}, config_source='cs',
        )
        self.storage.create_rule(
            'user1', [], '', old_revision=2,
            rulename='rule2', desc='ruledesc', editors=[], parent_rule='rule1',
            selector='blinov expr', config={'foo': 'bar'}, config_source='cs',
        )
        self.storage.create_rule(
            'user1', [], '', old_revision=3,
            rulename='rule3', desc='ruledesc', editors=[], parent_rule=None,
            selector='blinov expr', config={'foo': 'bar'}, config_source='cs',
        )
        with self.assertRaises(errors.NotUnique):
            self.storage.create_rule(
                'user1', [], '', old_revision=4, parent_rule='rule3',
                rulename='rule1', desc='ruledesc', editors=[],
                selector='blinov expr', config={'f': 'b'}, config_source='cs',
            )

    def test_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.create_rule(
                'user1', [], 'nonexistent.path', old_revision=1,
                parent_rule=None,
                rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
                selector='blinov expr', config={'foo': 'bar'},
                config_source='cs',
            )

    def test_not_owner(self):
        self.reset_audit()
        self.reset_volatiles()
        with self.assertRaises(errors.Unauthorized):
            self.storage.create_rule(
                'user5', ['rr'], '', old_revision=1, parent_rule=None,
                rulename='rule', desc='ruledesc', editors=['user6', 'user7'],
                selector='blinov', config={'foo': 0}, config_source='cfgsrc',
            )
        self.assert_audit('', [
            {'exc_class': 'Unauthorized',
             'extra': {'config': {'foo': 0},
                       'config_source': 'cfgsrc',
                       'desc': 'ruledesc',
                       'editors': ['user6', 'user7'],
                       'name': 'rule',
                       'prev_rules': [],
                       'selector': 'blinov',
                       'stype': 'yaml',
                       'stype_options': None,
                       'parent_rule': None,
                       'subrules': []},
             'affected_rules': ['rule'],
             'path': '',
             'result': 'model_error',
             'revision': 1,
             'what': 'create_rule',
             'when': 1444000002,
             'who': 'user5',
             'who_groups': ['rr']}
        ])


class EditRuleTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = []

    def test_success(self):
        self.storage.create_section(username="user1", usergroups=[],
                                    parent_path="", parent_rev=1,
                                    name="sec", desc="desc",
                                    owners=["user4", 'group:bzz'],
                                    stype='yaml', stype_options=None)
        self.storage.create_rule(
            'user4', [], 'sec', old_revision=2, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov expr', config={'foo': 'bar'},
            config_source='first config',
        )
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=3, parent_rule=None,
            rulename='rule2', desc='rule2desc', editors=['user1'],
            selector='blinov expr2', config={'foo': 'baz'},
            config_source='second config',
        )
        self.reset_audit()
        self.reset_volatiles()

        self.storage.edit_rule(
            'user2', ['g1'], 'sec', old_revision=4,
            rulename='rule1', desc='newdesc', editors=['user8', 'user2'],
            selector='newselector',
        )
        expected_tree = {
            'desc': 'Root section',
            'mtime': 1444000001,
            'changed_by': 'user1',
            'name': 'genisys',
            'marked_by': [],
            "all_editors": [],
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 5,
            'stype': 'yaml',
            'stype_options': None,
            'rules': [],
            'subsections': {'sec': {
                'desc': 'desc',
                'mtime': 1444000005,
                'changed_by': 'user2',
                'name': 'sec',
                'marked_by': [],
                "all_editors": ['user1', 'user2', 'user8'],
                'owners': ['group:bzz', 'user4'],
                'path': 'sec',
                'revision': 5,
                'stype': 'yaml',
                'stype_options': None,
                'rules': [{'config': {'foo': 'bar'},
                           'config_source': 'first config',
                           'ctime': 1444000003,
                           'mtime': 1444000005,
                           'desc': 'newdesc',
                           'editors': ['user2', 'user8'],
                           'name': 'rule1',
                           'selector': 'newselector',
                           'subrules': []},
                          {'config': {'foo': 'baz'},
                           'config_source': 'second config',
                           'ctime': 1444000004,
                           'mtime': 1444000004,
                           'desc': 'rule2desc',
                           'editors': ['user1'],
                           'name': 'rule2',
                           'selector': 'blinov expr2',
                           'subrules': []}],
                'subsections': {}}}
        }
        tree = self.get_section_subtree("")
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_audit('sec', [
            {'extra': {'new': {'desc': 'newdesc',
                               'editors': ['user2', 'user8'],
                               'name': 'rule1',
                               'selector': 'newselector'},
                       'prev': {'desc': 'ruledesc',
                                'editors': ['user6', 'user7'],
                                'name': 'rule1',
                                'selector': 'blinov expr'},
                       'prev_rules': [{'config': {'foo': 'bar'},
                                       'config_source': 'first config',
                                       'ctime': 1444000003,
                                       'mtime': 1444000003,
                                       'desc': 'ruledesc',
                                       'editors': ['user6', 'user7'],
                                       'name': 'rule1',
                                       'selector': 'blinov expr',
                                       'subrules': []},
                                      {'config': {'foo': 'baz'},
                                       'config_source': 'second config',
                                       'ctime': 1444000004,
                                       'mtime': 1444000004,
                                       'desc': 'rule2desc',
                                       'editors': ['user1'],
                                       'name': 'rule2',
                                       'selector': 'blinov expr2',
                                       'subrules': []}]},
             'affected_rules': ['rule1'],
             'path': 'sec',
             'result': 'success',
             'revision': 5,
             'what': 'edit_rule',
             'when': 1444000005,
             'who': 'user2',
             'who_groups': ['g1']}
        ])
        self.assert_volatiles(vtype='section', key="hash('sec')", expected=[
            {'atime': 1444000002,
             'ctime': 1444000005,
             'etime': 1444000005,
             'key': "hash('sec')",
             'raw_key': 'sec',
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {
                'changed_by': 'user2',
                'reresolve_selectors': True,
                'mtime': 1444000005,
                'revision': 5,
                'owners': ['group:bzz', 'user4']},
             'mtime': None,
             'source': {'rules': [{'config': {'foo': 'bar'},
                                   'rule_name': 'rule1',
                                   'selector_keys': ["hash('newselector')"]},
                                  {'config': {'foo': 'baz'},
                                   'rule_name': 'rule2',
                                   'selector_keys': ["hash('blinov expr2')"]}],
                        'stype': 'yaml',
                        'stype_options': None},
             'value': None,
             'vtype': 'section'},
        ])
        self.storage.edit_rule(
            'user5', ['bzz'], 'sec', old_revision=5,
            rulename='rule1', desc='newdesc2', editors=[],
            selector='newselector',
        )

    def test_subrule(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['e1'],
            selector='blinov expr', config={'foo': 'bar'},
            config_source='cs2',
        )
        self.storage.create_rule(
            'user1', [], '', old_revision=2, parent_rule='rule1',
            rulename='rule2', desc='subruledesc', editors=['e2'],
            selector='expr2', config={'c': 2}, config_source='cs2',
        )
        self.reset_audit()
        self.reset_volatiles()
        self.storage.edit_rule(
            'e1', [], '', old_revision=3,
            rulename='rule2', desc='newdesc', editors=['e5', 'e7'],
            selector='expr3'
        )
        self.assert_audit('', fields=['who', 'what', 'affected_rules'],
                          extra_fields=['new', 'prev'], expected=[
            {'affected_rules': ['rule2', 'rule1'],
             'extra': {'new': {'desc': 'newdesc',
                               'editors': ['e5', 'e7'],
                               'name': 'rule2',
                               'selector': 'expr3'},
                       'prev': {'desc': 'subruledesc',
                                'editors': ['e2'],
                                'name': 'rule2',
                                'selector': 'expr2'}},
             'what': 'edit_rule',
             'who': 'e1'}
        ])
        expected_tree = {
            'changed_by': 'e1',
            'desc': 'Root section',
            'mtime': 1444000004,
            'name': 'genisys',
            'marked_by': [],
            "all_editors": ['e1', 'e5', 'e7'],
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 4,
            'rules': [{'config': {'foo': 'bar'},
                       'config_source': 'cs2',
                       'ctime': 1444000002,
                       'desc': 'ruledesc',
                       'editors': ['e1'],
                       'mtime': 1444000004,
                       'name': 'rule1',
                       'selector': 'blinov expr',
                       'subrules': [{'config': {'c': 2},
                                     'config_source': 'cs2',
                                     'ctime': 1444000003,
                                     'desc': 'newdesc',
                                     'editors': ['e5', 'e7'],
                                     'mtime': 1444000004,
                                     'name': 'rule2',
                                     'selector': 'expr3'}]}],
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {}}
        tree = self.get_section_subtree("")
        self.assert_subtrees_equal(expected_tree, tree)
        self.assert_volatiles(vtype='selector', fields=['key'], expected=[
            {'key': "hash('blinov expr')"},
            {'key': "hash('expr3')"}
        ])

        with self.assertRaises(errors.Unauthorized):
            self.storage.edit_rule(
                'e5', [], '', old_revision=4,
                rulename='rule2', desc='newdesc', editors=['e5', 'e7'],
                selector='expr3'
            )

    def test_subrule_group_auth(self):
        self.storage.create_section(username="user1", usergroups=[],
                                    parent_path="", parent_rev=1,
                                    name="sec", desc="desc", owners=[],
                                    stype='yaml', stype_options=None)
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=2, parent_rule=None,
            rulename='root-rule', desc='ruledesc', editors=['group:editors'],
            selector='blinov1', config={'foo': 1}, config_source='cs1',
        )
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=3, parent_rule='root-rule',
            rulename='subrule', desc='ruledesc', editors=[],
            selector='blinov1', config={'foo': 1}, config_source='cs1',
        )
        self.storage.edit_rule(
            'user100', ['editors'], 'sec', old_revision=4,
            rulename='subrule', desc='ruledesc2', editors=['user67'],
            selector='blinov expr2'
        )

    def test_outdated(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov expr', config={'foo': 'bar'}, config_source='cs',
        )
        self.storage.create_rule(
            'user1', [], '', old_revision=2, parent_rule=None,
            rulename='rule2', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov expr', config={'foo': 'bar'}, config_source='cs',
        )
        self.reset_audit()
        self.reset_volatiles()
        with self.assertRaises(errors.Outdated):
            self.storage.edit_rule(
                'user1', [], '', old_revision=2,
                rulename='rule1', desc='ruledesc2', editors=['user67'],
                selector='blinov expr2'
            )
        self.assert_audit('', [])

    def test_section_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.edit_rule(
                'user1', [], 'nonexistent.path', old_revision=1,
                rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
                selector='blinov expr'
            )

    def test_rule_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.edit_rule(
                'user1', [], '', old_revision=1,
                rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
                selector='blinov expr'
            )

    def test_not_owner(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1, parent_rule=None,
            rulename='rule', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov', config={'foo': 0}, config_source='cfgsrc',
        )
        self.reset_audit()
        self.reset_volatiles()
        with self.assertRaises(errors.Unauthorized):
            self.storage.edit_rule(
                'user5', [], '', old_revision=2,
                rulename='rule', desc='ruledesc', editors=['user6', 'user7'],
                selector='blinov'
            )
        self.assert_audit('', [
            {'exc_class': 'Unauthorized',
             'extra': {'new': {'desc': 'ruledesc',
                               'editors': ['user6', 'user7'],
                               'name': 'rule',
                               'selector': 'blinov'},
                       'prev': {'desc': 'ruledesc',
                                'editors': ['user6', 'user7'],
                                'name': 'rule',
                                'selector': 'blinov'},
                       'prev_rules': [{'config': {'foo': 0},
                                       'config_source': 'cfgsrc',
                                       'ctime': 1444000002,
                                       'mtime': 1444000002,
                                       'desc': 'ruledesc',
                                       'editors': ['user6', 'user7'],
                                       'name': 'rule',
                                       'selector': 'blinov',
                                       'subrules': []}]},
             'affected_rules': ['rule'],
             'path': '',
             'result': 'model_error',
             'revision': 2,
             'what': 'edit_rule',
             'when': 1444000003,
             'who': 'user5',
             'who_groups': []}
        ])


class EditRuleConfigTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = []

    def test_success(self):
        self.storage.create_section(username="user1", usergroups=[],
                                    parent_path="", parent_rev=1,
                                    name="sec", desc="desc",
                                    owners=["user4", 'group:puorg'],
                                    stype='stype', stype_options={'f': 'oo'})
        self.storage.create_rule(
            'user4', [], 'sec', old_revision=2, parent_rule=None,
            rulename='rule1', desc='ruledesc',
            editors=['user6', 'user7', 'group:g4'],
            selector='blinov expr', config={'foo': 'bar'}, config_source='src',
        )
        self.reset_audit()
        self.reset_volatiles()

        self.storage.db.volatile.insert_one({
            'vtype': 'selector',
            'key': "hash('blinov expr')",
            'etime': 1444000144,
            'pcount': 20,
            'value': None,
            'source': model._serialize('blinov expr'),
        })

        self.storage.edit_rule_config('user7', ['gg'], 'sec', old_revision=3,
                                      rulename='rule1', config={'some': 'xx'},
                                      config_source='new src')
        expected_tree = {
            'desc': 'Root section',
            'mtime': 1444000001,
            'changed_by': 'user1',
            'name': 'genisys',
            'marked_by': [],
            "all_editors": [],
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 4,
            'stype': 'yaml',
            'stype_options': None,
            'rules': [],
            'subsections': {'sec': {
                'desc': 'desc',
                'mtime': 1444000004,
                'changed_by': 'user7',
                'name': 'sec',
                'marked_by': [],
                "all_editors": ['group:g4', 'user6', 'user7'],
                'owners': ['group:puorg', 'user4'],
                'path': 'sec',
                'revision': 4,
                'stype': 'stype',
                'stype_options': {'f': 'oo'},
                'rules': [{'config': {'some': 'xx'},
                           'config_source': 'new src',
                           'ctime': 1444000003,
                           'mtime': 1444000004,
                           'desc': 'ruledesc',
                           'editors': ['group:g4', 'user6', 'user7'],
                           'name': 'rule1',
                           'selector': 'blinov expr',
                           'subrules': []}],
                'subsections': {}}}
        }
        tree = self.get_section_subtree("")
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_audit('sec', [
            {'extra': {'new': {'some': 'xx'},
                       'new_source': 'new src',
                       'prev': {'foo': 'bar'},
                       'prev_source': 'src',
                       'prev_rules': [{'config': {'foo': 'bar'},
                                       'config_source': 'src',
                                       'ctime': 1444000003,
                                       'mtime': 1444000003,
                                       'desc': 'ruledesc',
                                       'editors': ['group:g4', 'user6', 'user7'],
                                       'name': 'rule1',
                                       'selector': 'blinov expr',
                                       'subrules': []}],
                       'stype': 'stype',
                       'stype_options': {'f': 'oo'}},
             'affected_rules': ['rule1'],
             'path': 'sec',
             'result': 'success',
             'revision': 4,
             'what': 'edit_rule_config',
             'when': 1444000004,
             'who': 'user7',
             'who_groups': ['gg']}
        ])

        self.assert_volatiles(vtype='section', key="hash('sec')", expected=[
            {'atime': 1444000002,
             'ctime': 1444000004,
             'etime': 1444000004,
             'key': "hash('sec')",
             'raw_key': 'sec',
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {
                'changed_by': 'user7',
                'reresolve_selectors': False,
                'mtime': 1444000004,
                'revision': 4,
                'owners': ['group:puorg', 'user4']},
             'mtime': None,
             'source': {'rules': [{'config': {'some': 'xx'},
                                   'rule_name': 'rule1',
                                   'selector_keys': ["hash('blinov expr')"]}],
                        'stype': 'stype',
                        'stype_options': {'f': 'oo'}},
             'value': None,
             'vtype': 'section'},
        ])
        self.assert_volatiles(vtype='selector', key="hash('blinov expr')", expected=[
            {'etime': 1444000144,
             'key': "hash('blinov expr')",
             'meta': {},
             'pcount': 20,
             'source': 'blinov expr',
             'value': None,
             'vtype': 'selector'}
        ])

        self.storage.edit_rule_config('user9', ['puorg'], 'sec', old_revision=4,
                                      rulename='rule1', config={'some': 'new'},
                                      config_source='newest')
        tree = self.get_section_subtree("")
        self.assertEquals({'some': 'new'},
                          tree['subsections']['sec']['rules'][0]['config'])
        self.assertEquals(
            'newest', tree['subsections']['sec']['rules'][0]['config_source']
        )

        self.storage.edit_rule_config('user8', ['g4'], 'sec', old_revision=5,
                                      rulename='rule1', config={'some': 'ne'},
                                      config_source='newest2')
        tree = self.get_section_subtree("")
        self.assertEquals({'some': 'ne'},
                          tree['subsections']['sec']['rules'][0]['config'])

    def test_subrule(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['e1'],
            selector='blinov expr', config={'foo': 'bar'},
            config_source='cs2',
        )
        self.storage.create_rule(
            'user1', [], '', old_revision=2, parent_rule='rule1',
            rulename='rule2', desc='subruledesc', editors=['e2'],
            selector='expr2', config={'c': 2}, config_source='cs2',
        )
        self.reset_audit()
        self.reset_volatiles()
        self.storage.edit_rule_config(
            'e2', [], '', old_revision=3, rulename='rule2',
            config={'c': 'new'}, config_source='csnew',
        )
        self.assert_audit('', fields=['who', 'what', 'affected_rules'],
                          extra_fields=['new', 'prev'], expected=[
            {'affected_rules': ['rule2', 'rule1'],
             'extra': {'new': {'c': 'new'}, 'prev': {'c': 2}},
             'what': 'edit_rule_config',
             'who': 'e2'}
        ])
        expected_tree = {
            'changed_by': 'e2',
            'desc': 'Root section',
            'mtime': 1444000004,
            'name': 'genisys',
            'marked_by': [],
            "all_editors": ['e1', 'e2'],
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 4,
            'rules': [{'config': {'foo': 'bar'},
                       'config_source': 'cs2',
                       'ctime': 1444000002,
                       'desc': 'ruledesc',
                       'editors': ['e1'],
                       'mtime': 1444000004,
                       'name': 'rule1',
                       'selector': 'blinov expr',
                       'subrules': [{'config': {'c': 'new'},
                                     'config_source': 'csnew',
                                     'ctime': 1444000003,
                                     'desc': 'subruledesc',
                                     'editors': ['e2'],
                                     'mtime': 1444000004,
                                     'name': 'rule2',
                                     'selector': 'expr2'}]}],
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {}
        }
        tree = self.get_section_subtree("")
        self.assert_subtrees_equal(expected_tree, tree)

        self.storage.edit_rule_config(
            'e1', [], '', old_revision=4, rulename='rule2',
            config={'c': 'new2'}, config_source='csnew2',
        )
        expected_tree = {
            'changed_by': 'e1',
            'desc': 'Root section',
            'mtime': 1444000005,
            'name': 'genisys',
            'marked_by': [],
            "all_editors": ['e1', 'e2'],
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 5,
            'rules': [{'config': {'foo': 'bar'},
                       'config_source': 'cs2',
                       'ctime': 1444000002,
                       'desc': 'ruledesc',
                       'editors': ['e1'],
                       'mtime': 1444000005,
                       'name': 'rule1',
                       'selector': 'blinov expr',
                       'subrules': [{'config': {'c': 'new2'},
                                     'config_source': 'csnew2',
                                     'ctime': 1444000003,
                                     'desc': 'subruledesc',
                                     'editors': ['e2'],
                                     'mtime': 1444000005,
                                     'name': 'rule2',
                                     'selector': 'expr2'}]}],
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {}
        }
        tree = self.get_section_subtree("")
        self.assert_subtrees_equal(expected_tree, tree)

        with self.assertRaises(errors.Unauthorized):
            self.storage.edit_rule_config(
                'e5', [], '', old_revision=5, rulename='rule2',
                config={'c': 'c'}, config_source='cs',
            )
        with self.assertRaises(errors.Unauthorized):
            self.storage.edit_rule_config(
                'e2', [],  '', old_revision=5, rulename='rule1',
                config={'c': 'c'}, config_source='cs',
            )

    def test_subrule_group_auth(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['group:e2'],
            selector='blinov expr', config={'foo': 'bar'},
            config_source='cs2',
        )
        self.storage.create_rule(
            'user1', [], '', old_revision=2, parent_rule='rule1',
            rulename='rule2', desc='subruledesc', editors=[],
            selector='expr2', config={'c': 2}, config_source='cs2',
        )
        self.storage.edit_rule_config(
            'user', ['e2'], '', old_revision=3, rulename='rule2',
            config={'c': 'new'}, config_source='csnew',
        )

    def test_outdated(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov expr', config={'foo': 'bar'}, config_source='cs',
        )
        self.storage.create_rule(
            'user1', [], '', old_revision=2, parent_rule=None,
            rulename='rule2', desc='ruledesc2', editors=[],
            selector='blinov expr', config={'foo': 'bar'}, config_source='cs',
        )
        self.reset_audit()
        with self.assertRaises(errors.Outdated):
            self.storage.edit_rule_config(
                'user1', [], '', old_revision=2,
                rulename='rule1', config={'foo': None}, config_source='wtvr',
            )
        self.assert_audit('', [])

    def test_section_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.edit_rule_config(
                'user1', [], 'nonexistent.path', old_revision=1,
                rulename='rule1', config={'foo': 'bar'}, config_source='cs'
            )

    def test_rule_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.edit_rule_config('user1', [], '', old_revision=1,
                                          rulename='rule1', config={'f': 16},
                                          config_source='zz')

    def test_not_owner_not_editor(self):
        self.storage.create_rule(
            'user1', [], '', old_revision=1, parent_rule=None,
            rulename='rule', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov', config={'foo': 0}, config_source='cs'
        )
        self.reset_audit()
        with self.assertRaises(errors.Unauthorized):
            self.storage.edit_rule_config(
                'user5', [], '', old_revision=2,
                rulename='rule', config={'foo': 1}, config_source='cs2'
            )
        self.assert_audit('', [
            {'exc_class': 'Unauthorized',
             'extra': {'new': {'foo': 1},
                       'new_source': 'cs2',
                       'prev': {'foo': 0},
                       'prev_source': 'cs',
                       'prev_rules': [{'config': {'foo': 0},
                                       'config_source': 'cs',
                                       'ctime': 1444000002,
                                       'mtime': 1444000002,
                                       'desc': 'ruledesc',
                                       'editors': ['user6', 'user7'],
                                       'name': 'rule',
                                       'selector': 'blinov',
                                       'subrules': []}],
                       'stype': 'yaml',
                       'stype_options': None},
             'affected_rules': ['rule'],
             'path': '',
             'result': 'model_error',
             'revision': 2,
             'what': 'edit_rule_config',
             'when': 1444000003,
             'who': 'user5',
             'who_groups': []}
        ])


class ReorderRulesTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = []

    def setUp(self):
        super(ReorderRulesTestCase, self).setUp()
        self.storage.create_section(username="user1", usergroups=[],
                                    parent_path="", parent_rev=1,
                                    name="sec", desc="desc",
                                    owners=["user4", 'group:froup'],
                                    stype='yaml', stype_options=None)
        self.storage.create_rule(
            'user4', [], 'sec', old_revision=2, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov1', config={'foo': 1}, config_source='cs1',
        )
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=3, parent_rule=None,
            rulename='rule2', desc='ruledesc2',
            editors=['user8', 'group:g8'],
            selector='blinov2', config={'foo': 2}, config_source='cs2',
        )
        self.storage.create_rule(
            'user2', [], 'sec', old_revision=4, parent_rule=None,
            rulename='rule3', desc='ruledesc3', editors=['user9'],
            selector='blinov3', config={'foo': 3}, config_source='cs3',
        )
        self.reset_audit()
        self.reset_volatiles()

    def test_success(self):
        self.storage.reorder_rules('user19', ['froup'], 'sec', old_revision=5,
                                   new_order=[0, 2, 1],  parent_rule=None)
        expected_tree = {
            'changed_by': 'user1',
            'desc': 'Root section',
            'mtime': 1444000001,
            'name': 'genisys',
            'marked_by': [],
            "all_editors": [],
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 6,
            'stype': 'yaml',
            'stype_options': None,
            'rules': [],
            'subsections': {'sec': {
                'changed_by': 'user19',
                'desc': 'desc',
                'mtime': 1444000006,
                'name': 'sec',
                'marked_by': [],
                "all_editors": ['group:g8', 'user6', 'user7', 'user8', 'user9'],
                'owners': ['group:froup', 'user4'],
                'path': 'sec',
                'revision': 6,
                'stype': 'yaml',
                'stype_options': None,
                'rules': [{'config': {'foo': 1},
                           'config_source': 'cs1',
                           'ctime': 1444000003,
                           'mtime': 1444000006,
                           'desc': 'ruledesc',
                           'editors': ['user6', 'user7'],
                           'name': 'rule1',
                           'selector': 'blinov1',
                           'subrules': []},
                          {'config': {'foo': 3},
                           'config_source': 'cs3',
                           'ctime': 1444000005,
                           'mtime': 1444000006,
                           'desc': 'ruledesc3',
                           'editors': ['user9'],
                           'name': 'rule3',
                           'selector': 'blinov3',
                           'subrules': []},
                          {'config': {'foo': 2},
                           'config_source': 'cs2',
                           'ctime': 1444000004,
                           'mtime': 1444000006,
                           'desc': 'ruledesc2',
                           'editors': ['group:g8', 'user8'],
                           'name': 'rule2',
                           'selector': 'blinov2',
                           'subrules': []}],
                'subsections': {}}}
        }
        tree = self.get_section_subtree("")
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_audit('sec', [
            {'extra': {'new': ['rule1', 'rule3', 'rule2'],
                       'prev': ['rule1', 'rule2', 'rule3'],
                       'prev_rules': [{'config': {'foo': 1},
                                       'config_source': 'cs1',
                                       'ctime': 1444000003,
                                       'mtime': 1444000003,
                                       'desc': 'ruledesc',
                                       'editors': ['user6', 'user7'],
                                       'name': 'rule1',
                                       'selector': 'blinov1',
                                       'subrules': []},
                                      {'config': {'foo': 2},
                                       'config_source': 'cs2',
                                       'ctime': 1444000004,
                                       'mtime': 1444000004,
                                       'desc': 'ruledesc2',
                                       'editors': ['group:g8', 'user8'],
                                       'name': 'rule2',
                                       'selector': 'blinov2',
                                       'subrules': []},
                                      {'config': {'foo': 3},
                                       'config_source': 'cs3',
                                       'ctime': 1444000005,
                                       'mtime': 1444000005,
                                       'desc': 'ruledesc3',
                                       'editors': ['user9'],
                                       'name': 'rule3',
                                       'selector': 'blinov3',
                                       'subrules': []}]},
             'affected_rules': ['rule1', 'rule3', 'rule2'],
             'path': 'sec',
             'result': 'success',
             'revision': 6,
             'what': 'reorder_rules',
             'when': 1444000006,
             'who': 'user19',
             'who_groups': ['froup']}
        ])

        self.assert_volatiles(vtype='section', key="hash('sec')", expected=[
            {'atime': 1444000002,
             'ctime': 1444000006,
             'etime': 1444000006,
             'key': "hash('sec')",
             'raw_key': 'sec',
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {
                'changed_by': 'user19',
                'reresolve_selectors': False,
                'mtime': 1444000006,
                'revision': 6,
                'owners': ['group:froup', 'user4']},
             'mtime': None,
             'value': None,
             'vtype': 'section',
             'source': {'rules': [{'config': {'foo': 1},
                                   'rule_name': 'rule1',
                                   'selector_keys': ["hash('blinov1')"]},
                                  {'config': {'foo': 3},
                                   'rule_name': 'rule3',
                                   'selector_keys': ["hash('blinov3')"]},
                                  {'config': {'foo': 2},
                                   'rule_name': 'rule2',
                                   'selector_keys': ["hash('blinov2')"]}],
                        'stype': 'yaml',
                        'stype_options': None}}
        ])

    def test_subrules(self):
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=5, parent_rule='rule2',
            rulename='sr1', desc='', editors=[],
            selector='s1', config={'c': 1}, config_source='cs2',
        )
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=6, parent_rule='rule2',
            rulename='sr2', desc='', editors=[],
            selector='s2', config={'c': 2}, config_source='cs2',
        )
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=7, parent_rule='rule2',
            rulename='sr3', desc='', editors=['subrule-editor'],
            selector='s3', config={'c': 3}, config_source='cs2',
        )
        self.reset_audit()
        self.storage.reorder_rules(
            'user8', [], 'sec', old_revision=8, parent_rule='rule2',
            new_order=[2, 0, 1]
        )
        self.assert_volatiles(vtype='section', key="hash('sec')",
                              fields=['source'], expected=[
            {'source': {'rules': [{'config': {'foo': 1},
                                   'rule_name': 'rule1',
                                   'selector_keys': ["hash('blinov1')"]},
                                  {'config': {'c': 3},
                                   'rule_name': 'sr3',
                                   'selector_keys': ["hash('blinov2')", "hash('s3')"]},
                                  {'config': {'c': 1},
                                   'rule_name': 'sr1',
                                   'selector_keys': ["hash('blinov2')", "hash('s1')"]},
                                  {'config': {'c': 2},
                                   'rule_name': 'sr2',
                                   'selector_keys': ["hash('blinov2')", "hash('s2')"]},
                                  {'config': {'foo': 2},
                                   'rule_name': 'rule2',
                                   'selector_keys': ["hash('blinov2')"]},
                                  {'config': {'foo': 3},
                                   'rule_name': 'rule3',
                                   'selector_keys': ["hash('blinov3')"]}],
                        'stype': 'yaml',
                        'stype_options': None}}
        ])
        expected_tree = {
            'changed_by': 'user8',
            'desc': 'desc',
            'mtime': 1444000009,
            'name': 'sec',
            'marked_by': [],
            "all_editors": ['group:g8', 'subrule-editor',
                            'user6', 'user7', 'user8', 'user9'],
            'owners': ['group:froup', 'user4'],
            'path': 'sec',
            'revision': 9,
            'rules': [{'config': {'foo': 1},
                       'config_source': 'cs1',
                       'ctime': 1444000003,
                       'desc': 'ruledesc',
                       'editors': ['user6', 'user7'],
                       'mtime': 1444000003,
                       'name': 'rule1',
                       'selector': 'blinov1',
                       'subrules': []},
                      {'config': {'foo': 2},
                       'config_source': 'cs2',
                       'ctime': 1444000004,
                       'desc': 'ruledesc2',
                       'editors': ['group:g8', 'user8'],
                       'mtime': 1444000009,
                       'name': 'rule2',
                       'selector': 'blinov2',
                       'subrules': [{'config': {'c': 3},
                                     'config_source': 'cs2',
                                     'ctime': 1444000008,
                                     'desc': '',
                                     'editors': ['subrule-editor'],
                                     'mtime': 1444000009,
                                     'name': 'sr3',
                                     'selector': 's3'},
                                    {'config': {'c': 1},
                                     'config_source': 'cs2',
                                     'ctime': 1444000006,
                                     'desc': '',
                                     'editors': [],
                                     'mtime': 1444000009,
                                     'name': 'sr1',
                                     'selector': 's1'},
                                    {'config': {'c': 2},
                                     'config_source': 'cs2',
                                     'ctime': 1444000007,
                                     'desc': '',
                                     'editors': [],
                                     'mtime': 1444000009,
                                     'name': 'sr2',
                                     'selector': 's2'}]},
                      {'config': {'foo': 3},
                       'config_source': 'cs3',
                       'ctime': 1444000005,
                       'desc': 'ruledesc3',
                       'editors': ['user9'],
                       'mtime': 1444000005,
                       'name': 'rule3',
                       'selector': 'blinov3',
                       'subrules': []}],
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {}
        }
        tree = self.get_section_subtree("sec")
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_audit('sec', fields=['affected_rules', 'who', 'what'],
                          extra_fields=['new', 'prev'], expected=[
            {'affected_rules': ['sr3', 'sr1', 'sr2', 'rule2'],
             'extra': {'new': ['sr3', 'sr1', 'sr2'],
                       'prev': ['sr1', 'sr2', 'sr3']},
             'what': 'reorder_rules',
             'who': 'user8'}
        ])

        self.storage.reorder_rules(
            'usernull', ['g8'], 'sec', old_revision=9, parent_rule='rule2',
            new_order=[0, 1, 2]
        )

    def test_outdated(self):
        with self.assertRaises(errors.Outdated):
            self.storage.reorder_rules('user1', [], 'sec', old_revision=4,
                                       new_order=[1, 0], parent_rule=None)
        self.assert_audit('sec', [])

    def test_section_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.reorder_rules(
                'user1', [], 'nonexistent.path', old_revision=1, parent_rule=None,
                new_order=[0, 1, 2, 3]
            )

    def test_missing_rulenum(self):
        with self.assertRaises(errors.Unauthorized):
            self.storage.reorder_rules(
                'user1', [], 'sec', old_revision=4, parent_rule=None,
                new_order=[1, 2]
            )

    def test_rule_not_found(self):
        with self.assertRaises(errors.Unauthorized):
            self.storage.reorder_rules(
                'user1', [], 'sec', old_revision=4, parent_rule=None,
                new_order=[0, 1, 2, 3, 4, 5]
            )

    def test_rule_num_dup(self):
        with self.assertRaises(errors.Unauthorized):
            self.storage.reorder_rules(
                'user1', [], 'sec', old_revision=5, parent_rule=None,
                new_order=[0, 1, 1]
            )

    def test_not_owner(self):
        with self.assertRaises(errors.Unauthorized):
            self.storage.reorder_rules(
                'user3', [], 'sec', old_revision=5, parent_rule=None,
                new_order=[1, 0]
            )
        self.assert_audit('sec', [])


class DeleteRuleTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = []

    def setUp(self):
        super(DeleteRuleTestCase, self).setUp()
        self.storage.create_section(username="user1", usergroups=[],
                                    parent_path="", parent_rev=1,
                                    name="sec", desc="desc",
                                    owners=['group:deleters', "user4"],
                                    stype='yaml', stype_options=None)
        self.storage.create_rule(
            'user4', [], 'sec', old_revision=2, parent_rule=None,
            rulename='rule1', desc='ruledesc', editors=['user6', 'user7'],
            selector='blinov1', config={'foo': 1}, config_source='cs1',
        )
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=3, parent_rule=None,
            rulename='rule2', desc='ruledesc2', editors=['group:g0', 'user8'],
            selector='blinov2', config={'foo': 2}, config_source='cs2',
        )
        self.storage.create_rule(
            'user2', [], 'sec', old_revision=4, parent_rule=None,
            rulename='rule3', desc='ruledesc3', editors=['user9'],
            selector='blinov3', config={'foo': 3}, config_source='cs3',
        )
        self.reset_audit()

    def test_success(self):
        self.storage.delete_rule('user1', ['g1'], 'sec', old_revision=5,
                                 rulename='rule1')
        expected_tree = {
            'desc': 'Root section',
            'mtime': 1444000001,
            'changed_by': 'user1',
            'name': 'genisys',
            'marked_by': [],
            "all_editors": [],
            'owners': ['user1', 'user2'],
            'path': '',
            'revision': 6,
            'stype': 'yaml',
            'stype_options': None,
            'rules': [],
            'subsections': {'sec': {
                'changed_by': 'user1',
                'desc': 'desc',
                'mtime': 1444000006,
                'name': 'sec',
                'marked_by': [],
                "all_editors": ['group:g0', 'user8', 'user9'],
                'owners': ['group:deleters', 'user4'],
                'path': 'sec',
                'revision': 6,
                'stype': 'yaml',
                'stype_options': None,
                'rules': [{'config': {'foo': 2},
                           'config_source': 'cs2',
                           'ctime': 1444000004,
                           'mtime': 1444000004,
                           'desc': 'ruledesc2',
                           'editors': ['group:g0', 'user8'],
                           'name': 'rule2',
                           'selector': 'blinov2',
                           'subrules': []},
                          {'config': {'foo': 3},
                           'config_source': 'cs3',
                           'ctime': 1444000005,
                           'mtime': 1444000005,
                           'desc': 'ruledesc3',
                           'editors': ['user9'],
                           'name': 'rule3',
                           'selector': 'blinov3',
                           'subrules': []}],
                'subsections': {}}}
        }
        tree = self.get_section_subtree("")
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_audit('sec', [
            {'extra': {'config': {'foo': 1},
                       'config_source': 'cs1',
                       'ctime': 1444000003,
                       'mtime': 1444000003,
                       'desc': 'ruledesc',
                       'editors': ['user6', 'user7'],
                       'name': 'rule1',
                       'selector': 'blinov1',
                       'prev_rules': [{'config': {'foo': 1},
                                       'config_source': 'cs1',
                                       'ctime': 1444000003,
                                       'mtime': 1444000003,
                                       'desc': 'ruledesc',
                                       'editors': ['user6', 'user7'],
                                       'name': 'rule1',
                                       'selector': 'blinov1',
                                       'subrules': []},
                                      {'config': {'foo': 2},
                                       'config_source': 'cs2',
                                       'ctime': 1444000004,
                                       'mtime': 1444000004,
                                       'desc': 'ruledesc2',
                                       'editors': ['group:g0', 'user8'],
                                       'name': 'rule2',
                                       'selector': 'blinov2',
                                       'subrules': []},
                                      {'config': {'foo': 3},
                                       'config_source': 'cs3',
                                       'ctime': 1444000005,
                                       'mtime': 1444000005,
                                       'desc': 'ruledesc3',
                                       'editors': ['user9'],
                                       'name': 'rule3',
                                       'selector': 'blinov3',
                                       'subrules': []}],
                     'stype': 'yaml',
                     'stype_options': None,
                     'subrules': []},
             'affected_rules': ['rule1'],
             'path': 'sec',
             'result': 'success',
             'revision': 6,
             'what': 'delete_rule',
             'when': 1444000006,
             'who': 'user1',
             'who_groups': ['g1']}
        ])
        self.assert_volatiles(vtype='section', expected=[
            {'atime': 1444000001,
             'ctime': 1444000001,
             'etime': 1444000001,
             'key': "hash('')",
             'raw_key': '',
             'last_status': 'new',
             'lock_id': None,
             'locked': False,
             'mcount': 0,
             'meta': {
                 'changed_by': 'user1',
                 'reresolve_selectors': True,
                 'mtime': 1444000001,
                 'revision': 1,
                 'owners': ['user1', 'user2']},
             'mtime': None,
             'proclog': [],
             'source': {'rules': [], 'stype': 'yaml', 'stype_options': None},
             'tcount': 0,
             'ttime': None,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'utime': None,
             'value': None,
             'vtype': 'section'},
            {'atime': 1444000002,
             'ctime': 1444000006,
             'etime': 1444000006,
             'key': "hash('sec')",
             'raw_key': 'sec',
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {
                'changed_by': 'user1',
                'reresolve_selectors': False,
                'mtime': 1444000006,
                'revision': 6,
                'owners': ['group:deleters', 'user4']},
             'mtime': None,
             'source': {'rules': [{'config': {'foo': 2},
                                   'rule_name': 'rule2',
                                   'selector_keys': ["hash('blinov2')"]},
                                  {'config': {'foo': 3},
                                   'rule_name': 'rule3',
                                   'selector_keys': ["hash('blinov3')"]}],
                        'stype': 'yaml',
                        'stype_options': None},
             'value': None,
             'vtype': 'section'}
        ])
        self.storage.delete_rule('user100', ['deleters'],
                                 'sec', old_revision=6, rulename='rule2')

    def test_subrules(self):
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=5, parent_rule='rule2',
            rulename='sr1', desc='', editors=[],
            selector='s1', config={'c': 1}, config_source='cs1',
        )
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=6, parent_rule='rule2',
            rulename='sr2', desc='', editors=['e1'],
            selector='s2', config={'c': 2}, config_source='cs2',
        )
        self.reset_audit()
        self.storage.delete_rule('user8', [],
                                 'sec', old_revision=7, rulename='sr1')

        self.assert_volatiles(vtype='section', key="hash('sec')",
                              fields=['source'], expected=[
            {'source': {'rules': [{'config': {'foo': 1},
                                   'rule_name': 'rule1',
                                   'selector_keys': ["hash('blinov1')"]},
                                  {'config': {'c': 2},
                                   'rule_name': 'sr2',
                                   'selector_keys': ["hash('blinov2')", "hash('s2')"]},
                                  {'config': {'foo': 2},
                                   'rule_name': 'rule2',
                                   'selector_keys': ["hash('blinov2')"]},
                                  {'config': {'foo': 3},
                                   'rule_name': 'rule3',
                                   'selector_keys': ["hash('blinov3')"]}],
                        'stype': 'yaml',
                        'stype_options': None}}
        ])

        expected_tree = {
            'changed_by': 'user8',
            'desc': 'desc',
            'mtime': 1444000008,
            'name': 'sec',
            'marked_by': [],
            "all_editors": ['group:g0', 'e1', 'user6', 'user7', 'user8', 'user9'],
            'owners': ['group:deleters', 'user4'],
            'path': 'sec',
            'revision': 8,
            'rules': [{'config': {'foo': 1},
                       'config_source': 'cs1',
                       'ctime': 1444000003,
                       'desc': 'ruledesc',
                       'editors': ['user6', 'user7'],
                       'mtime': 1444000003,
                       'name': 'rule1',
                       'selector': 'blinov1',
                       'subrules': []},
                      {'config': {'foo': 2},
                       'config_source': 'cs2',
                       'ctime': 1444000004,
                       'desc': 'ruledesc2',
                       'editors': ['group:g0', 'user8'],
                       'mtime': 1444000008,
                       'name': 'rule2',
                       'selector': 'blinov2',
                       'subrules': [{'config': {'c': 2},
                                     'config_source': 'cs2',
                                     'ctime': 1444000007,
                                     'desc': '',
                                     'editors': ['e1'],
                                     'mtime': 1444000007,
                                     'name': 'sr2',
                                     'selector': 's2'}]},
                      {'config': {'foo': 3},
                       'config_source': 'cs3',
                       'ctime': 1444000005,
                       'desc': 'ruledesc3',
                       'editors': ['user9'],
                       'mtime': 1444000005,
                       'name': 'rule3',
                       'selector': 'blinov3',
                       'subrules': []}],
            'stype': 'yaml',
            'stype_options': None,
            'subsections': {}
        }
        tree = self.get_section_subtree("sec")
        self.assert_subtrees_equal(expected_tree, tree)

        self.assert_audit('sec', fields=['who', 'what', 'affected_rules'],
                          extra_fields=['name'], expected=[
            {'affected_rules': ['sr1', 'rule2'],
             'extra': {'name': 'sr1'},
             'what': 'delete_rule',
             'who': 'user8'}
        ])

        with self.assertRaises(errors.Unauthorized):
            self.storage.delete_rule('e1', [], 'sec', old_revision=8,
                                     rulename='sr2')
        self.storage.delete_rule('user89', ['g0'],
                                 'sec', old_revision=8, rulename='sr2')

    def test_outdated(self):
        with self.assertRaises(errors.Outdated):
            self.storage.delete_rule('user1', [], 'sec', old_revision=4,
                                     rulename='rule2')
        self.assert_audit('sec', [])

    def test_section_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.delete_rule(
                'user1', [], 'nonexistent.path', old_revision=1,
                rulename='rule1'
            )

    def test_rule_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.delete_rule(
                'user1', [], 'sec', old_revision=4, rulename='rule5',
            )

    def test_not_owner(self):
        with self.assertRaises(errors.Unauthorized):
            self.storage.delete_rule(
                'user3', [], 'sec', old_revision=5, rulename='rule3',
            )
        self.assert_audit('sec', [
            {'exc_class': 'Unauthorized',
             'extra': {'config': {'foo': 3},
                       'config_source': 'cs3',
                       'ctime': 1444000005,
                       'mtime': 1444000005,
                       'desc': 'ruledesc3',
                       'editors': ['user9'],
                       'name': 'rule3',
                       'selector': 'blinov3',
                       'prev_rules': [{'config': {'foo': 1},
                                       'config_source': 'cs1',
                                       'ctime': 1444000003,
                                       'mtime': 1444000003,
                                       'desc': 'ruledesc',
                                       'editors': ['user6', 'user7'],
                                       'name': 'rule1',
                                       'selector': 'blinov1',
                                       'subrules': []},
                                      {'config': {'foo': 2},
                                       'config_source': 'cs2',
                                       'ctime': 1444000004,
                                       'mtime': 1444000004,
                                       'desc': 'ruledesc2',
                                       'editors': ['group:g0', 'user8'],
                                       'name': 'rule2',
                                       'selector': 'blinov2',
                                       'subrules': []},
                                      {'config': {'foo': 3},
                                       'config_source': 'cs3',
                                       'ctime': 1444000005,
                                       'mtime': 1444000005,
                                       'desc': 'ruledesc3',
                                       'editors': ['user9'],
                                       'name': 'rule3',
                                       'selector': 'blinov3',
                                       'subrules': []},
                                      ],
                       'stype': 'yaml',
                       'stype_options': None,
                       'subrules': []},
             'affected_rules': ['rule3'],
             'path': 'sec',
             'result': 'model_error',
             'revision': 5,
             'what': 'delete_rule',
             'when': 1444000006,
             'who': 'user3',
             'who_groups': []}
        ])


class EnsureVolatileTestCase(ModelTestCase):
    def test_new(self):
        self.storage._ensure_volatile(
            vtype='testvol', key='vol0', source='src',
            now=self.storage._get_ts(), meta={"meta": "data"},
        )
        self.assert_volatiles(vtype='testvol', expected=[
            {'atime': 1444000002,
             'ctime': 1444000002,
             'etime': 1444000002,
             'key': "hash('vol0')",
             'raw_key': 'vol0',
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {"meta": "data"},
             'mtime': None,
             'source': 'src',
             'value': None,
             'vtype': 'testvol'}
        ])

    def test_same_source(self):
        self.database.volatile.insert_one(
            {'atime': datetime(2015, 1, 1, 2, 0, 2).timestamp(),
             'ctime': datetime(2015, 1, 1, 3, 0, 2).timestamp(),
             'etime': datetime(2015, 1, 1, 4, 0, 2).timestamp(),
             'key': "hash('vol3')",
             'last_status': 'modified',
             'mcount': 123,
             'tcount': 234,
             'ucount': 456,
             'pcount': 10,
             'ecount': 11,
             'locked': True,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {},
             'mtime': datetime(2015, 9, 14, 15, 22, 30).timestamp(),
             'source': model._serialize('src2'),
             'value': model._serialize({'some': 'calculated', 'val': 'ue'}),
             'vtype': 'testvol'}
        )
        self.storage._ensure_volatile(
            vtype='testvol', key='vol3', source='src2',
            now=datetime(2015, 2, 3, 4, 5, 6).timestamp(), meta={"new": True}
        )
        self.assert_volatiles(vtype='testvol', expected=[
            {'atime': datetime(2015, 1, 1, 2, 0, 2).timestamp(),
             'ctime': datetime(2015, 1, 1, 3, 0, 2).timestamp(),
             'etime': datetime(2015, 1, 1, 4, 0, 2).timestamp(),
             'key': "hash('vol3')",
             'last_status': 'modified',
             'mcount': 123,
             'tcount': 234,
             'ucount': 456,
             'pcount': 10,
             'ecount': 11,
             'locked': True,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {"new": True},
             'mtime': datetime(2015, 9, 14, 15, 22, 30).timestamp(),
             'source': 'src2',
             'value': {'some': 'calculated', 'val': 'ue'},
             'vtype': 'testvol'}
        ])

    def test_same_source_force_expire(self):
        self.database.volatile.insert_one(
            {'atime': datetime(2015, 1, 1, 2, 0, 2).timestamp(),
             'ctime': datetime(2015, 1, 1, 3, 0, 2).timestamp(),
             'etime': datetime(2015, 1, 1, 4, 0, 2).timestamp(),
             'key': "hash('vol3')",
             'last_status': 'error',
             'mcount': 3,
             'tcount': 4,
             'ucount': 5,
             'pcount': 10,
             'ecount': 0,
             'locked': True,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {},
             'mtime': datetime(2015, 9, 14, 15, 22, 30).timestamp(),
             'source': model._serialize('src2'),
             'value': model._serialize({'some': 'calculated', 'val': 'ue'}),
             'vtype': 'testvol'}
        )
        self.storage._ensure_volatile(
            vtype='testvol', key='vol3', source='src2',
            now=datetime(2015, 2, 3, 4, 5, 6).timestamp(), meta={"new": True},
            force_expire=True
        )
        self.assert_volatiles(vtype='testvol', expected=[
            {'atime': datetime(2015, 1, 1, 2, 0, 2).timestamp(),
             'ctime': datetime(2015, 1, 1, 3, 0, 2).timestamp(),
             'etime': datetime(2015, 2, 3, 4, 5, 6).timestamp(),
             'key': "hash('vol3')",
             'last_status': 'error',
             'mcount': 3,
             'tcount': 4,
             'ucount': 5,
             'pcount': 0,
             'ecount': 0,
             'locked': True,
             'lock_id': None,
             'utime': None,
             'ttime': None,
             'proclog': [],
             'meta': {"new": True},
             'mtime': datetime(2015, 9, 14, 15, 22, 30).timestamp(),
             'source': 'src2',
             'value': {'some': 'calculated', 'val': 'ue'},
             'vtype': 'testvol'}
        ])

    def test_different_source(self):
        self.database.volatile.insert_one(
            {'atime': datetime(2015, 1, 1, 2, 0, 2).timestamp(),
             'ctime': datetime(2015, 1, 1, 3, 0, 2).timestamp(),
             'etime': datetime(2015, 1, 1, 4, 0, 2).timestamp(),
             'key': "hash('vol0')",
             'last_status': 'same',
             'mcount': 44,
             'tcount': 55,
             'ucount': 22,
             'pcount': 14,
             'ecount': 15,
             'locked': True,
             'lock_id': None,
             'utime': None,
             'meta': {},
             'mtime': datetime(2015, 9, 14, 15, 22, 30).timestamp(),
             'source': model._serialize('src2'),
             'value': model._serialize({'some': 'calculated', 'val': 'ue'}),
             'vtype': 'testvol'}
        )
        self.storage._ensure_volatile(
            vtype='testvol', key='vol0', source='src',
            now=datetime(2015, 5, 6, 7, 8, 9).timestamp(),
            meta={"some": "meta"},
        )
        self.assert_volatiles(vtype='testvol', expected=[
            {'atime': datetime(2015, 1, 1, 2, 0, 2).timestamp(),
             'ctime': datetime(2015, 5, 6, 7, 8, 9).timestamp(),
             'etime': datetime(2015, 5, 6, 7, 8, 9).timestamp(),
             'key': "hash('vol0')",
             'last_status': 'new',
             'mcount': 0,
             'tcount': 0,
             'ucount': 0,
             'pcount': 0,
             'ecount': 0,
             'locked': False,
             'lock_id': None,
             'utime': None,
             'meta': {"some": "meta"},
             'mtime': datetime(2015, 9, 14, 15, 22, 30).timestamp(),
             'source': 'src',
             'value': {'some': 'calculated', 'val': 'ue'},
             'vtype': 'testvol'}
        ])


class IsOwnerGetAllOwnersTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = [
        {"creator": "user1",
         "name": "root1",
         "owners": ['o1', 'o2', 'group:superusers'],
         "desc": "rootdesc",
         "subsections": [
             {"creator": "user1",
              "name": "child1",
              "owners": ['group:somegroup'],
              "desc": "child1 desc",
              "subsections": [
                  {"creator": "user1",
                   "name": "child11",
                   "desc": "child11 desc",
                   "owners": ["o5", "o2", 'group:gr1', 'group:gr3'],
                   "subsections": []},
                  {"creator": "user1",
                   "name": "child12",
                   "desc": "child12 desc",
                   "owners": [],
                   "subsections": []},
              ]},
         ]},
    ]

    def test_is_owner(self):
        assert_is_owner = lambda username, usergroups, path: \
                self.assertTrue(self.storage.is_owner(username, usergroups, path))
        assert_is_owner('o1', [], 'root1')
        assert_is_owner('o1', [], 'root1.child1')
        assert_is_owner('o2', [], 'root1.child1.child12')
        assert_is_owner('o9', ['gr1', 'gr2', 'superusers'], 'root1')
        assert_is_owner('o12', ['superusers'], 'root1.child1')
        assert_is_owner('o133', ['gg', 'superusers', 'ggg'], 'root1.child1.child12')
        assert_is_owner('o2', [], 'root1.child1')
        assert_is_owner('o5', ['somegroup', 'gr2'], 'root1.child1')
        assert_is_owner('o5', ['somegroup', 'gr2'], 'root1.child1.child12')
        assert_is_owner('o5', [], 'root1.child1.child11')
        assert_is_owner('o7', ['gr3'], 'root1.child1.child11')
        assert_is_owner('o7', ['somegroup'], 'root1.child1.child11')

        assert_is_not_owner = lambda username, usergroups, path: \
                self.assertFalse(self.storage.is_owner(username, usergroups, path))
        assert_is_not_owner('o3', [], 'root1')
        assert_is_not_owner('o3', ['othergroup'], 'root1.child1')
        assert_is_not_owner('o3', ['somegroup'], 'root1')
        assert_is_not_owner('o4', ['gr2', 'gr4'], 'root1.child1.child11')

    def test_get_all_owners(self):
        self.assertEquals(self.storage.get_all_owners(""),
                          ["user1", "user2"])
        self.assertEquals(self.storage.get_all_owners("root1"),
                          ['group:superusers', "o1", "o2", "user1", "user2"])
        self.assertEquals(self.storage.get_all_owners("root1.child1"),
                          ['group:somegroup', 'group:superusers',
                           "o1", "o2", "user1", "user2"])
        self.assertEquals(self.storage.get_all_owners("root1.child1.child11"),
                          ['group:gr1', 'group:gr3',
                           'group:somegroup', 'group:superusers',
                           "o1", "o2", "o5", "user1", "user2"])
        self.assertEquals(self.storage.get_all_owners("root1.child1.child12"),
                          ['group:somegroup', 'group:superusers',
                           "o1", "o2", "user1", "user2"])

        self.storage.set_section_owners("o1", [],
                                        "root1.child1", 5, ['u3', 'u7'])
        self.assertEquals(self.storage.get_all_owners("root1.child1.child11"),
                          ['group:gr1', 'group:gr3', 'group:superusers',
                            "o1", "o2", "o5", "u3", "u7", "user1", "user2"])
        self.storage.set_section_owners("o1", [], "root1", 6, ['o2'])
        self.assertEquals(self.storage.get_all_owners("root1.child1.child11"),
                          ['group:gr1', 'group:gr3',
                           "o2", "o5", "u3", "u7", "user1", "user2"])
        self.storage.set_section_owners("user2", [], "", 7, [])
        self.assertEquals(self.storage.get_all_owners("root1.child1.child11"),
                          ['group:gr1', 'group:gr3',
                           "o2", "o5", "u3", "u7", "user2"])


class GetDashboardTestCase(_PrefilledTreeTestCase):
    SECTION_TREE = [
        {"creator": "user1",
         "name": "root1",
         "owners": ['o1', 'o2', 'group:superusers'],
         "desc": "rootdesc",
         "subsections": [
             {"creator": "user1",
              "name": "child1",
              "owners": ['group:somegroup'],
              "desc": "child1 desc",
              "subsections": [
                  {"creator": "user1",
                   "name": "child11",
                   "desc": "child11 desc",
                   "owners": ["o5", "o2", 'group:gr1', 'group:gr3'],
                   "subsections": []},
                  {"creator": "user1",
                   "name": "child12",
                   "desc": "child12 desc",
                   "owners": [],
                   "subsections": []},
              ]},
             {"creator": "user1",
              "name": "child2",
              "owners": ['group:somegroup'],
              "desc": "child2 desc",
              "subsections": [
                  {"creator": "user1",
                   "name": "child21",
                   "desc": "child21 desc",
                   "owners": ['group:gr1', 'o1', 'user1'],
                   "subsections": []},
                  {"creator": "user1",
                   "name": "child22",
                   "desc": "child22 desc",
                   "owners": ['o7'],
                   "subsections": []},
              ]},
         ]},
    ]

    def _test_owned(self, username, usergroups, expected_structure):
        dashboard = self.storage.get_dashboard(username, usergroups)['owned']
        structure = {name: {} for name in dashboard}
        stack = [(v, structure[k]) for k, v in dashboard.items()]
        while stack:
            node, dct = stack.pop()
            for subnode_name, subnode in node['subsections'].items():
                subdct = dct[subnode_name] = {}
                stack.append((subnode, subdct))
        self.assertEquals(structure, expected_structure)

    def test(self):
        self._test_owned('o1', ['gr1', 'gr3'], {
            'root1': {'child1': {'child11': {}, 'child12': {}},
                      'child2': {'child21': {}, 'child22': {}}}
        })
        self._test_owned('o112', ['g9', 'superusers'], {
            'root1': {'child1': {'child11': {}, 'child12': {}},
                      'child2': {'child21': {}, 'child22': {}}}
        })
        self._test_owned('user1', [], {
            'root1': {'child1': {'child11': {}, 'child12': {}},
                      'child2': {'child21': {}, 'child22': {}}}
        })
        self._test_owned('o112', ['g9'], {})
        self._test_owned('o7', ['g9'], {
            'root1.child2.child22': {}
        })
        self._test_owned('o7', ['g9'], {
            'root1.child2.child22': {}
        })
        self._test_owned('o7', ['gr1'], {
            'root1.child1.child11': {},
            'root1.child2.child21': {},
            'root1.child2.child22': {}
        })
        self._test_owned('o7', ['gr1', 'somegroup'], {
            'root1.child1': {'child11': {}, 'child12': {}},
            'root1.child2': {'child21': {}, 'child22': {}}
        })

    def test_used_to_be_owner(self):
        self.storage.set_section_owners('user1', [], 'root1', 8, ['o1'])
        self._test_owned('o112', ['g9', 'superusers'], {})
        self._test_owned('o2', ['g9', 'superusers'], {})
        self._test_owned('o1', ['gr1', 'gr3'], {
            'root1': {'child1': {'child11': {}, 'child12': {}},
                      'child2': {'child21': {}, 'child22': {}}}
        })

    def _test_marked(self, username, expected_paths):
        marked = self.storage.get_dashboard(username, [])['marked']
        paths = list(marked)
        self.assertEquals(sorted(paths), sorted(expected_paths))

    def test_mark_unmark(self):
        self._test_marked('user1', [])
        self._test_marked('o1', [])
        self._test_marked('o7', [])
        self.storage.mark_section('user1', 'root1.child1')
        self._test_marked('user1', ['root1.child1'])
        self.storage.mark_section('user1', 'root1.child2.child22')
        self._test_marked('user1', ['root1.child1', 'root1.child2.child22'])
        self._test_marked('o7', [])
        self.storage.unmark_section('user1', 'root1.child1')
        self._test_marked('user1', ['root1.child2.child22'])

    def test_editable(self):
        #import ipdb; ipdb.set_trace()
        self.storage.create_rule(
            'user1', [], 'root1.child2.child22', old_revision=4,
            parent_rule=None, rulename='rule1', desc='ruledesc',
            editors=['e1', 'group:eg1'], selector='blinov expr',
            config={'resource_id': 44}, config_source='cs',
        )
        self.storage.create_rule(
            'user1', [], 'root1.child2.child21', old_revision=5,
            parent_rule=None, rulename='rule2', desc='ruledesc',
            editors=['e2', 'group:eg1'], selector='blinov expr',
            config={'resource_id': 44}, config_source='cs',
        )
        self.storage.create_rule(
            'user1', [], 'root1.child1.child12', old_revision=7,
            parent_rule=None, rulename='rule3', desc='ruledesc',
            editors=['group:eg1'], selector='blinov expr',
            config={'resource_id': 44}, config_source='cs',
        )
        editable = self.storage.get_dashboard('e1', ['eg1'])['editable']
        self.assertEquals(sorted(editable), [
            'root1.child1.child12', 'root1.child2.child21',
            'root1.child2.child22'
        ])
        editable = self.storage.get_dashboard('e1', ['eg1'])['editable']
        self.assertEquals(sorted(editable), [
            'root1.child1.child12', 'root1.child2.child21',
            'root1.child2.child22'
        ])
        editable = self.storage.get_dashboard('e1', ['gg'])['editable']
        self.assertEquals(sorted(editable), ['root1.child2.child22'])
        self.storage.delete_rule('user1', [], 'root1.child1.child12',
                                 8, 'rule3')
        editable = self.storage.get_dashboard('e1', ['eg1'])['editable']
        self.assertEquals(sorted(editable), [
            'root1.child2.child21', 'root1.child2.child22'
        ])


class ForceVolatileUpdateTestCase(ModelTestCase):
    def test(self):
        self.database.volatile.insert_one(
            {'vtype': 'testvol', 'key': "hash('vol0')"}
        )
        self.mock_ts_return_value = 1444000123
        res = self.storage.force_volatile_update('testvol', "hash('vol0')")
        self.assertEquals(res, 1444000124)
        vrec = self.database.volatile.find_one(
            {'vtype': 'testvol'},
            {'etime': 1, '_id': 0, 'locked': 1, 'lock_id': 1}
        )
        self.assertEquals(vrec, {'etime': 1444000124, 'lock_id': None,
                                 'locked': False})

    def test_not_found(self):
        with self.assertRaises(errors.NotFound):
            self.storage.force_volatile_update('testvol', 'vol0')


class GetUpdatedVolatileTestCase(ModelTestCase):
    def test(self):
        self.assertIs(self.storage.get_updated_volatile('tv', 'key', 0), None)
        self.database.volatile.insert_one(
            {'vtype': 'tv', 'key': "hash('vol1')", 'utime': 12345,
             'value': model._serialize('tvvalue'),
             'source': model._serialize('tvsource')}
        )
        self.assertIs(self.storage.get_updated_volatile('tv', "hash('vol1')", 12346), None)
        self.assertIs(self.storage.get_updated_volatile('tv', "hash('vol1')", 12345), None)
        self.mock_ts_return_value = 1444000009
        self.assertEquals(
            self.storage.get_updated_volatile('tv', "hash('vol1')", 12344),
            {'key': "hash('vol1')", 'source': 'tvsource', 'utime': 12345,
             'value': 'tvvalue', 'vtype': 'tv'}
        )


class SaveSectionResourceAliasesTestCase(ModelTestCase):
    def test_success(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="sec",
            desc="desc", owners=['group:grrr'],
            stype='sandbox_resource',
            stype_options={'resource_type': 'SKYNET_BINARY'}
        )
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=2, parent_rule=None,
            rulename='r', desc='', editors=['user6'],
            selector='blinov expr', config={'resource_id': 44},
            config_source={'cs': 1, 'rtype': 'by_id'},
        )
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=3, parent_rule='r',
            rulename='sr', desc='', editors=['user7'],
            selector='blinov expr', config={'resource_id': 45},
            config_source={'cs': 1, 'rtype': 'by_id'},
        )
        self.storage.create_rule(
            'user1', [], 'sec', old_revision=4, parent_rule='r',
            rulename='sr2', desc='', editors=[],
            selector='blinov expr', config={'resource_id': 45},
            config_source={'cs': 1, 'rtype': 'by_id'},
        )
        self.reset_audit()

        self.storage.save_section_resource_aliases(
            username='user9', usergroups=['grrr'], path='sec',
            old_revision=5, aliases=[
                {'name': 'alias1',
                 'resource': 123,
                 'description': 'res123'},
                {'name': 'alias2',
                 'resource': 234,
                 'description': 'res234'},
            ]
        )

        self.assert_audit('sec', [
            {'affected_rules': [],
             'extra': {'new': {'#10': {'id': '#10',
                                       'name': 'alias2',
                                       'resource_description': 'res234',
                                       'resource_id': 234},
                               '#9': {'id': '#9',
                                      'name': 'alias1',
                                      'resource_description': 'res123',
                                      'resource_id': 123}},
                       'prev': {}},
             'path': 'sec',
             'result': 'success',
             'revision': 6,
             'what': 'save_aliases',
             'when': 1444000006,
             'who': 'user9',
             'who_groups': ['grrr']}
        ])

        self.assertEquals(self.storage._find_section('sec')['stype_options'], {
            'aliases': [{'id': '#9',
                         'name': 'alias1',
                         'resource_description': 'res123',
                         'resource_id': 123},
                        {'id': '#10',
                         'name': 'alias2',
                         'resource_description': 'res234',
                         'resource_id': 234}],
            'resource_type': 'SKYNET_BINARY'
        })

        self.storage.edit_rule_config(
            'user1', [], 'sec', 6,
            rulename='r', config={'resource_id': 123},
            config_source={'rtype': 'by_alias', 'alias_id': '#7'}
        )
        self.storage.edit_rule_config(
            'user1', [], 'sec', 7,
            rulename='sr', config={'resource_id': 123},
            config_source={'rtype': 'by_alias', 'alias_id': '#7'}
        )

        self.reset_audit()

        self.storage.save_section_resource_aliases(
            username='user9', usergroups=['grrr'], path='sec',
            old_revision=8, aliases=[
                {'name': 'alias1.1',
                 'id': '#7',
                 'resource': 124,
                 'description': 'res124'},
                {'name': 'alias3',
                 'resource': 345,
                 'description': 'res345'},
            ]
        )

        self.assert_audit('sec', [{
            'affected_rules': ['r', 'sr'],
            'extra': {'new': {'#17': {'id': '#17',
                                      'name': 'alias3',
                                      'resource_description': 'res345',
                                      'resource_id': 345},
                              '#7': {'id': '#7',
                                     'name': 'alias1.1',
                                     'resource_description': 'res124',
                                     'resource_id': 124}},
                      'prev': {'#10': {'id': '#10',
                                       'name': 'alias2',
                                       'resource_description': 'res234',
                                       'resource_id': 234},
                               '#9': {'id': '#9',
                                      'name': 'alias1',
                                      'resource_description': 'res123',
                                      'resource_id': 123}}},
            'path': 'sec',
            'result': 'success',
            'revision': 9,
            'what': 'save_aliases',
            'when': 1444000009,
            'who': 'user9',
            'who_groups': ['grrr']
        }])

        sec = self.storage._find_section('sec')
        self.assertEquals(sec['stype_options'], {
            'aliases': [{'id': '#7',
                         'name': 'alias1.1',
                         'resource_description': 'res124',
                         'resource_id': 124},
                        {'id': '#17',
                         'name': 'alias3',
                         'resource_description': 'res345',
                         'resource_id': 345}],
            'resource_type': 'SKYNET_BINARY'
        })
        self.maxDiff = None
        self.assertEquals(sec['rules'], [{
            'config': {'resource_id': 124},
            'config_source': {'alias_id': '#7',
                              'alias_name': 'alias1.1',
                              'description': 'res124',
                              'resource': 124,
                              'rtype': 'by_alias'},
            'ctime': 1444000003,
            'desc': '',
            'editors': ['user6'],
            'mtime': 1444000009,
            'name': 'r',
            'selector': 'blinov expr',
            'subrules': [{'config': {'resource_id': 124},
                          'config_source': {'alias_id': '#7',
                                            'alias_name': 'alias1.1',
                                            'description': 'res124',
                                            'resource': 124,
                                            'rtype': 'by_alias'},
                          'ctime': 1444000004,
                          'desc': '',
                          'editors': ['user7'],
                          'mtime': 1444000009,
                          'name': 'sr',
                          'selector': 'blinov expr'},
                         {'config': {'resource_id': 45},
                          'config_source': {'cs': 1, 'rtype': 'by_id'},
                          'ctime': 1444000005,
                          'desc': '',
                          'editors': [],
                          'mtime': 1444000005,
                          'name': 'sr2',
                          'selector': 'blinov expr'}
                         ]
        }])

    def test_not_an_owner(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="sec",
            desc="desc", owners=['group:grrr'],
            stype='sandbox_resource',
            stype_options={'resource_type': 'SKYNET_BINARY'}
        )
        with self.assertRaises(errors.Unauthorized):
            self.storage.save_section_resource_aliases(
                username='user9', usergroups=[], path='sec',
                old_revision=2, aliases=[]
            )

    def test_deleting_alias_in_use(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="sec",
            desc="desc", owners=['group:grrr'],
            stype='sandbox_resource',
            stype_options={'resource_type': 'SKYNET_BINARY'}
        )
        self.storage.save_section_resource_aliases(
            username='user1', usergroups=[], path='sec',
            old_revision=2, aliases=[
                {'name': 'alias1',
                 'resource': 1112,
                 'description': 'alias uno'}
            ]
        )
        aliases = self.storage._find_section('sec')['stype_options']['aliases']
        aid = aliases[0]['id']

        self.storage.create_rule(
            'user1', [], 'sec', old_revision=3, parent_rule=None,
            rulename='r', desc='', editors=[],
            selector='blinov expr', config={'resource_id': 112},
            config_source={'alias_id': aid, 'rtype': 'by_alias'},
        )

        with self.assertRaises(errors.ModelError) as ar:
            self.storage.save_section_resource_aliases(
                username='user9', usergroups=[], path='sec',
                old_revision=4, aliases=[]
            )

        self.assertEquals(ar.exception.description,
                          ("alias %r is missing" % (aid, )))

    def test_id_not_unique(self):
        self.storage.create_section(
            username="user1", usergroups=[],
            parent_path="", parent_rev=1, name="sec",
            desc="desc", owners=['group:grrr'],
            stype='sandbox_resource',
            stype_options={'resource_type': 'SKYNET_BINARY'}
        )
        with self.assertRaises(errors.Unauthorized) as ar:
            self.storage.save_section_resource_aliases(
                username='user9', usergroups=[], path='sec',
                old_revision=2, aliases=[
                    {'name': 'a1',
                     'resource': 123,
                     'description': 'descr',
                     'id': '0' * 32},
                    {'name': 'a2',
                     'resource': 123,
                     'description': 'descr',
                     'id': '0' * 32},
                ]
            )
        self.assertEquals(
            ar.exception.description,
            "alias id '00000000000000000000000000000000' is not unique"
        )
