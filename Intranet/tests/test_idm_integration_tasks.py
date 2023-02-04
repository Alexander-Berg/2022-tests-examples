from mock import patch

from staff_api.v3_0.idm.tasks import sync_fields_access_rules


def _create_role(
        subject,
        subject_type,
        resource,
        role,
        fields=None,
        deleted=False,
        handled=False,
        sort=None,
        query=None,
        filter_fields=None
):
    if role == 'partial_access':
        url = 'https://staff-api.test.yandex-team.ru/v3/office?_fields=%s' % ','.join(fields)
        if sort is not None:
            url += '&_sort=%s' % ','.join(sort)
        if query is not None:
            url += '&_query=%s' % query
        if filter_fields is not None:
            for field, value in filter_fields.items():
                url += '&%s=%s' % (field, value)

        role_fields = {
            'access_url': url,
        }
    else:
        role_fields = {}
    return {
        'role_type': 'resource_access',
        'subject': subject,
        'subject_type': subject_type,
        'resource': resource,
        'role': role,
        'fields': role_fields,
        'deleted': deleted,
        'handled': handled,
    }


def _create_person(login, uid):
    return {'login': login, 'uid': str(uid)}


def _create_access_rule(uid, subject_type, resource, fields, idm_role_id):
    return {
        'subject_id': str(uid),
        'resource': resource,
        'subject_type': subject_type,
        'field': fields,
        'idm_role_id': idm_role_id,
    }

def test_sync_rules_insert(test_persons_collection, test_access_rules_collection, test_idm_roles_collection):
    person = _create_person('vasya', 1)
    test_persons_collection.insert_one(person)

    fields = ['chief', 'education.date']
    role = _create_role(person['login'], 'user', 'person', 'partial_access', fields)
    role_id = test_idm_roles_collection.insert_one(role).inserted_id

    expected_result = {
        'subject_id': person['uid'],
        'resource': 'person',
        'subject_type': 'user',
        'idm_role_id': role_id,
    }

    with patch('static_api.lock.lock_manager'):
        sync_fields_access_rules()

    access_rules = list(test_access_rules_collection.find())
    assert len(access_rules) == len(fields)
    for access_rule in access_rules:
        access_rule.pop('_id')
        assert access_rule.pop('field') in fields
        assert access_rule == expected_result

    role_records = list(test_idm_roles_collection.find())

    assert len(role_records) == 1
    assert role_records[0]['handled']
    assert role_records[0]['_id'] == role_id


def test_sync_rules_insert_with_sort(test_persons_collection, test_access_rules_collection, test_idm_roles_collection):
    person = _create_person('vasya', 1)
    test_persons_collection.insert_one(person)

    fields = ['chief.guid', 'education.date']
    sort = ['-environment.shell', '-id']
    role = _create_role(person['login'], 'user', 'person', 'partial_access', fields, sort=sort)
    role_id = test_idm_roles_collection.insert_one(role).inserted_id

    expected_fields = fields + ['environment.shell', 'id']

    expected_result = {
        'subject_id': person['uid'],
        'resource': 'person',
        'subject_type': 'user',
        'idm_role_id': role_id,
    }

    with patch('static_api.lock.lock_manager'):
        sync_fields_access_rules()

    access_rules = list(test_access_rules_collection.find())

    assert len(access_rules) == len(expected_fields)
    for access_rule in access_rules:
        access_rule.pop('_id')
        assert access_rule.pop('field') in expected_fields
        assert access_rule == expected_result

    role_records = list(test_idm_roles_collection.find())

    assert len(role_records) == 1
    assert role_records[0]['handled']
    assert role_records[0]['_id'] == role_id


def test_sync_rules_insert_with_query(test_persons_collection, test_access_rules_collection, test_idm_roles_collection):
    person = _create_person('vasya', 1)
    test_persons_collection.insert_one(person)

    fields = ['chief.guid', 'education.date']
    query = '(chief.id <= 100500 and department_group.is_deleted != false) or chief.id != 200500'
    role = _create_role(person['login'], 'user', 'person', 'partial_access', fields, query=query)
    role_id = test_idm_roles_collection.insert_one(role).inserted_id

    expected_fields = fields + ['chief.id', 'department_group.is_deleted']

    expected_result = {
        'subject_id': person['uid'],
        'resource': 'person',
        'subject_type': 'user',
        'idm_role_id': role_id,
    }

    with patch('static_api.lock.lock_manager'):
        sync_fields_access_rules()

    access_rules = list(test_access_rules_collection.find())

    assert len(access_rules) == len(expected_fields)
    for access_rule in access_rules:
        access_rule.pop('_id')
        assert access_rule.pop('field') in expected_fields
        assert access_rule == expected_result

    role_records = list(test_idm_roles_collection.find())

    assert len(role_records) == 1
    assert role_records[0]['handled']
    assert role_records[0]['_id'] == role_id


def test_sync_rules_insert_with_filter(test_persons_collection, test_access_rules_collection, test_idm_roles_collection):
    person = _create_person('vasya', 1)
    test_persons_collection.insert_one(person)

    fields = ['chief.guid', 'education.date']
    role = _create_role(
        subject=person['login'],
        subject_type='user',
        resource='person',
        role='partial_access',
        fields=fields,
        filter_fields={'id': 1, 'login': 'test'},
    )
    role_id = test_idm_roles_collection.insert_one(role).inserted_id

    expected_fields = fields + ['id', 'login']

    expected_result = {
        'subject_id': person['uid'],
        'resource': 'person',
        'subject_type': 'user',
        'idm_role_id': role_id,
    }

    with patch('static_api.lock.lock_manager'):
        sync_fields_access_rules()

    access_rules = list(test_access_rules_collection.find())

    assert len(access_rules) == len(expected_fields)
    for access_rule in access_rules:
        access_rule.pop('_id')
        assert access_rule.pop('field') in expected_fields
        assert access_rule == expected_result

    role_records = list(test_idm_roles_collection.find())

    assert len(role_records) == 1
    assert role_records[0]['handled']
    assert role_records[0]['_id'] == role_id


def test_sync_rules_insert_multiple(
    test_idm_roles_collection,
    test_access_rules_collection,
    test_persons_collection,
):
    person = _create_person('vasya', 1)
    test_persons_collection.insert_one(person)

    fields = ['groups']
    other_fields = ['bicycles']
    role = _create_role(person['login'], 'user', 'person', 'partial_access', fields)
    other_role = _create_role(person['login'], 'user', 'person', 'partial_access', other_fields)
    role_ids = test_idm_roles_collection.insert_many([role, other_role]).inserted_ids

    expected_result = {
        'subject_id': person['uid'],
        'resource': 'person',
        'subject_type': 'user',
    }

    with patch('static_api.lock.lock_manager'):
        sync_fields_access_rules()

    access_rules = list(test_access_rules_collection.find())
    for access_rule in access_rules:
        access_rule.pop('_id')
        access_rule.pop('idm_role_id')

        assert access_rule.pop('field') in fields + other_fields
        assert access_rule == expected_result

    role_records = list(test_idm_roles_collection.find())

    assert len(role_records) == 2
    for role_record in role_records:
        assert role_record['_id'] in role_ids
        assert role_record['handled']


def test_sync_rules_insert_multiple_persons(
    test_persons_collection,
    test_access_rules_collection,
    test_idm_roles_collection,
):
    person = _create_person('vasya', 1)
    other_person = _create_person('petya', 2)
    test_persons_collection.insert_many([person, other_person])

    fields_p1 = ['chief.guid']
    fields_p2 = ['cars']
    other_fields = ['work_mode']
    role_p1 = _create_role(person['login'], 'user', 'person', 'partial_access', fields_p1)
    other_role = _create_role(other_person['login'], 'user', 'person', 'partial_access', other_fields)
    role_p2 = _create_role(person['login'], 'user', 'person', 'partial_access', fields_p2)
    test_idm_roles_collection.insert_many([
        role_p1,
        other_role,
        role_p2,
    ])

    expected_result = {
        'subject_id': person['uid'],
        'resource': 'person',
        'subject_type': 'user',
    }
    other_expected_result = {
        'subject_id': other_person['uid'],
        'resource': 'person',
        'subject_type': 'user',
    }

    with patch('static_api.lock.lock_manager'):
        sync_fields_access_rules()

    access_rules = test_access_rules_collection.find({'subject_id': person['uid']})

    for access_rule in access_rules:
        access_rule.pop('_id')
        access_rule.pop('idm_role_id')

        assert access_rule.pop('field') in fields_p1 + fields_p2
        assert access_rule == expected_result

    other_access_rules = test_access_rules_collection.find({'subject_id': other_person['uid']})

    for other_access_rule in other_access_rules:
        other_access_rule.pop('_id')
        other_access_rule.pop('idm_role_id')

        assert other_access_rule.pop('field') in other_fields
        assert other_access_rule == other_expected_result


def test_sync_rules_delete(settings, test_persons_collection, test_idm_roles_collection, test_access_rules_collection):
    person = _create_person('vasya', 1)
    test_persons_collection.insert_one(person)

    deleted_role = _create_role(person['login'], 'user', 'person', 'full_access', [], deleted=True)
    deleted_role_id = test_idm_roles_collection.insert_one(deleted_role).inserted_id

    access_rules = [
        _create_access_rule(person['uid'], 'user', 'person', settings.STATIC_API_WILDCARD_FIELD_ACCESS, deleted_role_id),
    ]
    test_access_rules_collection.insert_many(access_rules)

    access_rules = list(test_access_rules_collection.find())

    assert len(access_rules) == 1

    with patch('static_api.lock.lock_manager'):
        sync_fields_access_rules()

    access_rules = list(test_access_rules_collection.find())

    assert len(access_rules) == 0


def test_sync_rules_empty_collections(test_persons_collection, test_idm_roles_collection, test_access_rules_collection):
    with patch('static_api.lock.lock_manager'):
        sync_fields_access_rules()


def test_sync_rules_empty_roles_collection(
        test_persons_collection,
        test_idm_roles_collection,
        test_access_rules_collection
):
    person = _create_person('vasya', 1)
    test_persons_collection.insert_one(person)
    access_rules = [
        _create_access_rule(person['uid'], 'user', 'office', 'name', 'A'),
    ]
    test_access_rules_collection.insert_many(access_rules)

    with patch('static_api.lock.lock_manager'):
        sync_fields_access_rules()
