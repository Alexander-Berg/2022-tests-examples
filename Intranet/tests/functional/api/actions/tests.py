# coding: utf-8
from hamcrest import (
    assert_that,
    contains,
    contains_inanyorder,
    has_entries,
    all_of,
    has_length,
    equal_to,
)

from intranet.yandex_directory.src.yandex_directory.common.utils import lstring
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory import app

from testutils import (
    mocked_blackbox,
    create_department,
    create_user,
    create_group,
    TestCase,
    get_auth_headers,
    scopes,
    oauth_success,
    OAUTH_CLIENT_ID,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DomainModel,
    ActionModel,
    EventModel,
)

from unittest.mock import patch


class TestActions(TestCase):
    def test_actions_can_be_listed_for_whole_organization(self):
        # убедимся, что в базе нет никаких событий
        data = self.get_json('/actions/')
        assert len(data['result']) == 0

        # сделаем ещё один департамент
        department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )

        # переведем пользователя в другой отдел
        self.move(self.user, department)

        # и посмотрим, появилось ли действие в списке действий
        data = self.get_json('/actions/')

        assert_that(
            data['result'],
            contains(
                has_entries(
                    revision=self.organization['revision'] + 1,
                    name='user_modify',
                )
            )
        )

    def test_actions_can_be_listed_for_for_users(self):
        with scopes(scope.work_with_any_organization,
                    scope.work_on_behalf_of_any_user,
                    scope.write_users,
                    scope.read_actions):
            # убедимся, что в базе нет никаких событий
            data = self.get_json(
                '/actions/',
                as_uid=self.admin_uid,
            )
            assert len(data['result']) == 0

            # сделаем пару департаментов
            org_id = self.organization['id']
            first_dep = create_department(
                self.main_connection,
                org_id=org_id,
            )
            second_dep = create_department(
                self.main_connection,
                org_id=org_id,
            )

            # сделаем второго пользователя
            second_user = create_user(
                self.meta_connection,
                self.main_connection,
                user_id=101,
                nickname='petya',
                name={'first': lstring('Petya'), 'last': lstring('Ivanov')},
                email='petya@ya.ru',
                groups=[],
                org_id=org_id,
                department_id=1,
            )

            # переведем одного пользователя в первый отдел
            self.move(self.user, first_dep)
            self.move(second_user, second_dep)

            data = self.get_json('/actions/?object_id={0}'.format(self.user['id']))

            assert_that(
                data['result'],
                all_of(
                    has_length(1),
                    contains(
                        has_entries(
                            revision=self.organization['revision'] + 1,
                            name='user_modify',
                            object_type='user',
                            object=has_entries(id=self.user['id']),
                        )
                    )
                )
            )

            data = self.get_json('/actions/?type=user&object_id={0}'.format(second_user['id']))

            assert_that(
                data['result'],
                all_of(
                    has_length(1),
                    contains(
                        has_entries(
                            revision=self.organization['revision'] + 2,
                            name='user_modify',
                            object_type='user',
                            object=has_entries(id=second_user['id']),
                        )
                    )
                )
            )

    def move(self, user, dep):
        url = '/users/%s/' % user['id']
        data = {
            'department_id': dep['id'],
        }
        self.patch_json(url, data)

    def test_actions_change_group_admins(self):
        data = self.get_json('/actions/')
        assert len(data['result']) == 0

        user1 = self.create_user()
        user2 = self.create_user()
        group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            admins=[user1['id']]
        )
        # поменяем админов
        self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'admins': [
                    dict(type='user', id=user1['id']),
                    dict(type='user', id=user2['id']),
                ]
            }
        )
        # и посмотрим, появилось ли действие в списке действий
        data = self.get_json('/actions/')

        assert_that(
            data['result'],
            contains_inanyorder(
                has_entries(
                    revision=self.organization['revision'] + 1,
                    name='group_modify',
                ),
                has_entries(
                    revision=self.organization['revision'] + 2,
                    name='group_admins_change',
                )
            )
        )

    def test_actions_change_group_without_change_admins(self):
        data = self.get_json('/actions/')
        assert len(data['result']) == 0

        user1 = self.create_user()
        user2 = self.create_user()
        group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            admins=[user1['id']]
        )
        # поменяем мемберов группы:
        members = [
            {
                'type': 'user',
                'id': user1['id'],
            },
            {
                'type': 'user',
                'id': user2['id'],
            }
        ]
        self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'members': members
            }
        )
        # проверим, что дейстиве пришло только одно и оно не про админов
        data = self.get_json('/actions/')
        assert len(data['result']) == 1
        assert_that(
            data['result'][0],
            has_entries(
                revision=self.organization['revision'] + 1,
                name='group_modify',
            )
        )


class TestActionByOuterAdmin(TestCase):
    def test_actions_groups_by_outer_admin(self):
        # Создадим группу от имени внешнего администратора
        # И поменяем у группы админа - на внутреннего админа организации.
        # В итоге - должно прилететь 3 события: group_add, group_modify, group_admins_changed
        headers = get_auth_headers(as_outer_admin={
            'org_id': self.organization['id'],
            'id': self.outer_admin['id'],
        })

        label = 'group_by_admin'
        group_data = {
            'name': self.group_name,
            'label': label,
            'admins': [{'id': self.create_user()['id']}],
        }
        response = self.post_json('/groups/', group_data, headers=headers)
        self.patch_json('/groups/%d/' % response['id'], {
            'admins': [dict(type='user', id=self.user['id'])]
        }, headers=headers)

        # проверим, что 3 события: group_add, group_modify, group_admins_change
        # сохранились в логе от лица внешнего админа
        response_action = self.get_json('/actions/')
        assert_that(
            response_action['result'][0],
            has_entries(
                # у нас было действие что увеличило ревизию на 1
                revision=self.organization['revision'] + 1,
                name='group_add',
                author_id=self.outer_admin['id'],
            )
        )

        assert_that(
            response_action['result'][1],
            has_entries(
                revision=self.organization['revision'] + 2,
                name='group_modify',
                author_id=self.outer_admin['id'],
            )
        )

        assert_that(
            response_action['result'][2],
            has_entries(
                revision=self.organization['revision'] + 3,
                name='group_admins_change',
                author_id=self.outer_admin['id'],
            )
        )

    def test_actions_department_by_outer_admin(self):
        # Создадим департамент от имени внешнего администратора.
        # И поменяем у департамента название.
        # В итоге - должно прилететь 2 события: department_add, department_modify
        headers = get_auth_headers(as_outer_admin={
            'org_id': self.organization['id'],
            'id': self.outer_admin['id'],
        })
        data = {
            'label': 'department_by_admin',
            'name': self.department_name,
            'parent': {'id': self.department['id']},
        }
        response = self.post_json('/departments/', data, headers=headers)
        self.patch_json('/departments/%d/' % response['id'], {
            'name': {'ru': '', 'en': 'new_name_for_dep_admin'},
        }, headers=headers)

        # проверим, что пришло 2 события: department_add, department_modify
        # и они сохранились в логе от лица внешнего админа
        response_action = self.get_json('/actions/')

        assert_that(
            response_action['result'][0],
            has_entries(
                revision=self.organization['revision'] + 1,
                name='department_add',
                author_id=self.outer_admin['id'],
            )
        )

        assert_that(
            response_action['result'][1],
            has_entries(
                revision=self.organization['revision'] + 2,
                name='department_modify',
                author_id=self.outer_admin['id'],
            )
        )


class TestActionDomainAddDelete(TestCase):
    def setUp(self):
        super(TestActionDomainAddDelete, self).setUp()
        self.domain_name = 'new_domain.com'
        self.domain_name_lower_case = self.domain_name.lower()
        self.domain_in_punycode = self.domain_name_lower_case.encode('idna')
        self.master_domain = self.label + self.domain_part
        self.master_domain_in_punycode = self.master_domain.encode('idna')

        self.headers = get_auth_headers(as_outer_admin={
            'org_id': self.organization['id'],
            'id': self.outer_admin['id'],
        })

    def test_actions_and_events_for_domain_addition_and_deletion(self):
        # В этом тесте мы проверяем что при добавлении и удалении домена у организации логгируются
        # действия domain_add и domain_delete в таблице Actions.
        # Заодно проверяем, что вызвались необходимые события domain_added и domain_deleted.

        self.clean_actions_and_events()

        data = {
            'name': self.domain_name,
        }

        self.post_json('/domains/', data, headers=self.headers)

        action = ActionModel(self.main_connection).find(
            filter_data={
                'revision': self.organization['revision'] + 1,
                'org_id': self.organization['id']
            },
        )[0]
        # Проверяем, что вызвалось действие domain_add
        assert_that(
            action,
            has_entries(
                revision=self.organization['revision'] + 1,
                name='domain_add',
                author_id=self.outer_admin['id'],
            )
        )
        # Проверяем, что сохранён добавленный домен
        assert_that(
            action['object'],
            has_entries(
                name=self.domain_name,
                org_id=self.organization['id'],
            )
        )

        self.delete_json('/domains/%s/' % (self.domain_name), headers=self.headers, expected_code=200)

        action = ActionModel(self.main_connection).find(
            filter_data={
                'revision': self.organization['revision'] + 2,
                'org_id': self.organization['id']
            },
        )[0]

        # Проверяем, что вызвалось действие domain_delete
        assert_that(
            action,
            has_entries(
                revision=self.organization['revision'] + 2,
                name='domain_delete',
                author_id=self.outer_admin['id'],
            )
        )
        # Проверяем, что сохранён удалённый домен
        assert_that(
            action['object'],
            has_entries(
                name=self.domain_name,
                org_id=self.organization['id'],
            )
        )
        # Проверяем что вызывались события domain_added и domain_deleted
        event_model = EventModel(self.main_connection)
        events = [x['name'] for x in event_model.find()]
        events.sort()
        expected = ['domain_added', 'domain_deleted']
        assert_that(events, equal_to(expected))


class TestDomainModify(TestCase):
    def setUp(self):
        super(TestDomainModify, self).setUp()
        self.headers = get_auth_headers(as_outer_admin={
            'org_id': self.organization['id'],
            'id': self.outer_admin['id'],
        })

    @oauth_success(OAUTH_CLIENT_ID)
    def test_master_domain_was_changed(self):
        # Проверяем смену мастер домена
        self.clean_actions_and_events()

        # Посмотрим на текущий мастер-домен:
        org_id = self.organization['id']
        domain = DomainModel(self.main_connection).get_master(org_id=org_id)
        assert_that(domain['name'], self.organization_domain)

        dname = 'new_master.com'
        new_master_domain = DomainModel(self.main_connection).create(dname, org_id, owned=True)
        data = {
            'master_domain': new_master_domain['name'],
        }

        with mocked_blackbox() as blackbox:
            self.mocked_passport.set_master_domain.return_value = True
            blackbox.hosted_domains.side_effect = [{
                "hosted_domains": [
                    {
                        "domain": domain['name'],
                        "domid": "1",
                        "master_domain": "",
                        "born_date": '2016-05-24 00:40:28',
                        "mx": "1",
                        "admin": self.admin_uid,
                    },
                ]
            }, {
                "hosted_domains": [
                    {
                        "domain": new_master_domain['name'],
                        "domid": "2",
                        "master_domain": self.organization_info['domain']['name'],
                        "born_date": '2016-05-24 00:40:28',
                        "mx": "1",
                        "admin": self.admin_uid,
                    },
                ],
            },
                {
                    "hosted_domains": [
                        {
                            "domain": new_master_domain['name'],
                            "domid": "2",
                            "master_domain": self.organization_info['domain']['name'],
                            "born_date": '2016-05-24 00:40:28',
                            "mx": "1",
                            "admin": self.admin_uid,
                        },
                    ]
                },
            ]

            response = self.patch_json('/organization/', data, headers=self.headers)
            assert_that(
                response['domains'],
                has_entries(
                    display='new_master.com',
                    master='new_master.com',
                    all=['new_master.com', 'not_yandex_test.ws.autotest.yandex.ru'],
                )
            )
            # Проверим, что сменили домен в паспорте
            assert_that(self.mocked_passport.set_master_domain.call_count, equal_to(1))
            # Проверим, что вызвали hosted_domain в паспорте
            assert_that(blackbox.hosted_domains.call_count, equal_to(3))

        action = ActionModel(self.main_connection).find(
            filter_data={
                'revision': self.organization['revision'] + 1,
                'org_id': org_id,
            },
            one=True
        )
        # Проверяем, что вызвалось действие domain_modify
        assert_that(
            action,
            has_entries(
                revision=self.organization['revision'] + 1,
                name='domain_master_modify',
                author_id=self.organization['admin_uid'],
            )
        )
        new_master_domain = DomainModel(self.main_connection).get_master(org_id=org_id)
        # Проверяем, что новый мастер домен приходит в нашем action-е
        assert_that(
            action['object'],
            has_entries(
                name=new_master_domain['name'],
                org_id=org_id,
            )
        )

        # Проверяем что вызывалось событие domain_master_changed
        event_model = EventModel(self.main_connection)
        events = [x['name'] for x in event_model.find()]
        expected = ['domain_master_changed']
        assert_that(events, equal_to(expected))
