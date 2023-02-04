import pytest
import json

pytestmark = [
    pytest.mark.django_db,
]


class TestSuggest:
    def test_all_layers(self, client, wiki_users, groups):
        client.login('thasonic')
        response = client.get('/_api/svc/.suggest?text=thasonic&layers=people,departments,groups')
        assert response.status_code == 200
        data = json.loads(response.content)['data']
        assert len(data) == 3

    def test_people(self, client, wiki_users, groups):
        client.login('thasonic')
        wiki_users.chapson.groups.add(groups.department_org_42)
        wiki_users.chapson.staff.first_name = 'Chap'
        wiki_users.chapson.staff.first_name_en = 'Chap'
        wiki_users.chapson.staff.last_name = 'Son'
        wiki_users.chapson.staff.last_name_en = 'Son'
        wiki_users.chapson.staff.save()
        response = client.get('/_api/svc/.suggest?text=chap&layers=people')
        assert response.status_code == 200
        data = json.loads(response.content)['data']
        assert len(data) == 1
        chapson = wiki_users.chapson
        expected = [
            {
                'layer': 'people',
                'result': [
                    {
                        'cloud_id': '3dddadaaacc9',
                        'layer': 'people',
                        'click_urls': [],
                        'fields': [
                            {'type': 'login', 'value': 'chapson'},
                            {'type': 'department_name', 'value': 'DevOps'},
                            {'type': 'is_dismissed', 'value': False},
                            {'type': 'avatar_url', 'value': 'https://avatars.mds.yandex.net/get-yapic/islands-middle'},
                        ],
                        'id': chapson.staff.uid,
                        'title': 'Chap Son',
                        'url': '',
                    }
                ],
            }
        ]
        assert data[0]['layer'] == 'people'
        assert expected == data

    def test_people_v2(self, client, wiki_users, groups):
        client.login('thasonic')
        wiki_users.chapson.groups.add(groups.department_org_42)
        wiki_users.chapson.staff.first_name = 'Chap'
        wiki_users.chapson.staff.first_name_en = 'Chap'
        wiki_users.chapson.staff.last_name = 'Son'
        wiki_users.chapson.staff.last_name_en = 'Son'
        wiki_users.chapson.staff.save()
        response = client.get('/_api/svc/.suggest?text=chap&layers=people&version=2')
        assert response.status_code == 200
        data = json.loads(response.content)['data']
        chapson = wiki_users.chapson
        expected = [
            {
                'cloud_id': '3dddadaaacc9',
                'layer': 'people',
                'click_urls': [],
                'login': 'chapson',
                'department_name': 'DevOps',
                'is_dismissed': False,
                'avatar_url': 'https://avatars.mds.yandex.net/get-yapic/islands-middle',
                'id': chapson.staff.uid,
                'title': 'Chap Son',
                'url': '',
            }
        ]

        assert data['people']['result'] == expected

    def test_people_different_org(self, client, wiki_users, organizations, groups):
        wiki_users.chapson.orgs.remove(organizations.org_42)
        client.login('thasonic')
        response = client.get('/_api/svc/.suggest?text=chap&layers=people')
        assert response.status_code == 200
        data = json.loads(response.content)['data']
        assert len(data) == 1
        assert data[0]['layer'] == 'people'
        assert len(data[0]['result']) == 0

    def test_groups(self, client, wiki_users, groups):
        client.login('thasonic')
        response = client.get('/_api/svc/.suggest?text=acc&layers=groups')
        assert response.status_code == 200
        data = json.loads(response.content)['data']
        assert len(data) == 1
        expected = [
            {
                'layer': 'groups',
                'result': [
                    {'click_urls': [], 'fields': [], 'id': '5624', 'layer': 'groups', 'title': 'Accounting', 'url': ''}
                ],
            }
        ]
        assert expected == data

    def test_departments(self, client, wiki_users, groups):
        client.login('thasonic')
        response = client.get('/_api/svc/.suggest?text=dev&layers=departments')
        assert response.status_code == 200
        data = json.loads(response.content)['data']
        assert len(data) == 1
        expected = [
            {
                'layer': 'departments',
                'result': [
                    {'click_urls': [], 'fields': [], 'id': '5625', 'layer': 'departments', 'title': 'DevOps', 'url': ''}
                ],
            }
        ]
        assert expected == data
