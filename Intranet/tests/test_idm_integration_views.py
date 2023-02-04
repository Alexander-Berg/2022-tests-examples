# coding: utf-8

import json
from mock import patch

from django.test.client import Client
from django.core.urlresolvers import reverse


def test_list_fields_access(test_access_rules_collection, test_persons_collection):
    person = {'login': 'tester', 'uid': 1}
    test_persons_collection.insert_one(person)

    access_rules = [
        {
            'subject_id': '1',
            'resource': 'person',
            'subject_type': 'user',
            'field': '*',
            'idm_role_id': 1,
        },
        {
            'subject_id': '1',
            'resource': 'room',
            'subject_type': 'user',
            'field': 'id',
            'idm_role_id': 1,
        },
    ]
    test_access_rules_collection.insert_many(access_rules)
    client = Client()

    url = reverse('list-fields-access', args=('user', 'tester'))

    with patch('static_api.storage.manager.db.get_collection', return_value=test_access_rules_collection):
        response = client.get(url)

    assert response.status_code == 200
    assert json.loads(response.content) == {
        'group': [],
        'room': ['id'],
        'office': [],
        'person': ['*'],
        'departmentstaff': [],
        'equipment': [],
        'groupmembership': [],
        'organization': [],
        'position': [],
        'occupation': [],
        'table': [],
        'geography': [],
    }
