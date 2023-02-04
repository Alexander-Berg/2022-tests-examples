# -*- coding: utf-8 -*-
import unittest.mock
from unittest.mock import (
    patch,
    ANY,
    Mock,
)
from hamcrest import (
    assert_that,
    equal_to,
    none,
    has_entries,
    contains_string,
    has_properties,
)

from intranet.yandex_directory.src.yandex_directory.common.exceptions import (
    DuplicateDomain,
    ImmediateReturn,
)
from intranet.yandex_directory.src.yandex_directory.passport.exceptions import BirthdayInvalid
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.utils import from_punycode
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DomainModel,
    OrganizationMetaModel,
    ServiceModel,
    UserModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import enable_service

from testutils import (
    mocked_blackbox,
    assert_not_called,
    assert_called,
    assert_called_once,
    TestCase,
    create_organization,
    get_random_string,
    override_settings,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    force_text,
)

from intranet.yandex_directory.src.yandex_directory.core.sms.tasks import SendSmsTask, sms_domain_confirmed
from intranet.yandex_directory.src.yandex_directory.core.utils.domain import (
    sync_domains_with_webmaster,
    sync_domain_state, # эта функция используется внутри sync_domains_with_webmaster
    assert_can_add_invalidated_alias,
    generate_tech_domain,
    delete_domain_from_passport,
    delete_domain_with_accounts,
)
from intranet.yandex_directory.src.yandex_directory.core.exceptions import (
    MigrationFileParsingError,
)


class TestSyncWithWebmaster(TestCase):
    def setUp(self):
        super(TestSyncWithWebmaster, self).setUp()
        org_id = self.organization['id']
        # помимо u'not_yandex_test.ws.autotest.yandex.ru', который master
        # создаем ещё три домена
        self.not_owned = DomainModel(self.main_connection).create('domain1.com', org_id)
        self.owned1 = DomainModel(self.main_connection).create('domain2.com', org_id, owned=True)
        self.owned2 = DomainModel(self.main_connection).create('domain3.com', org_id, owned=True)

    def test_do_nothing_if_organization_does_not_exists(self):
        # Если в процессе синка всех организаций, организацию откатили, то
        # в базе не будет данных про неё. В этом случае, sync_domains должен
        # просто проигнорировать такую организацию.
        # Из-за того, что это не учитывалось, у нас возникала такая ошибка:
        # https://st.yandex-team.ru/DIR-3942

        org_id = 100500 # Этот id придуман, такой организации нет

        # На всякий случай проверим это
        assert not OrganizationMetaModel(self.meta_connection).find({'id': org_id})

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_organization_admin_uid') as get_admin:
            sync_domains_with_webmaster(
                self.meta_connection,
                self.main_connection,
                org_id,
            )
        # В таком случаее, мы даже пытаться не должны получить админа организации
        assert_not_called(get_admin)

    def test_sync_with_webmaster_adds_domain_to_passport_when_it_is_validated(self):
        # Для домена с флагом via_webmaster, команда sync_domains_with_webmaster
        # должна добавить его в паспорт, как подтверждённый, но только после того,
        # как вебмастер ответит, что домен подтверждён.

        org_id = self.organization['id']
        admin_id = self.user['id']
        domain_name = self.not_owned['name']

        # Сделаем вид, что домен был добавлен для подтверждения через вебмастер
        DomainModel(self.main_connection).update_one(
            domain_name,
            org_id,
            data={'via_webmaster': True},
        )
        domain = DomainModel(self.main_connection).get(domain_name, org_id)

        with patch('intranet.yandex_directory.src.yandex_directory.webmaster.update_domain_state_if_verified') as mocked_update_domain_state_if_verified:

            sync_domain_state(
                self.meta_connection,
                self.main_connection,
                org_id,
                admin_id,
                domain,
            )

        assert_called_once(
            mocked_update_domain_state_if_verified,
            ANY,
            ANY,
            org_id,
            admin_id,
            domain,
            send_sms=True,
        )


class TestSyncWithWebmasterLogException(TestCase):
    def test_log_warning(self):
        # проверим, что исключение с log_level='WARNING' логируется как warning
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.sync_domain_state') as mocked_sync_domain_state, \
                patch('intranet.yandex_directory.src.yandex_directory.common.utils.log') as log:
            mocked_sync_domain_state.side_effect = BirthdayInvalid
            sync_domains_with_webmaster(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
            )
            assert_called_once(
                log.trace().warning,
                'Error during sync domain state'
            )

    def test_log_error(self):
        # проверим, что исключение без log_level='WARNING' логируется как error
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.sync_domain_state') as mocked_sync_domain_state, \
                patch('intranet.yandex_directory.src.yandex_directory.common.utils.log') as log:
            mocked_sync_domain_state.side_effect = MigrationFileParsingError
            sync_domains_with_webmaster(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
            )
            assert_called_once(
                log.trace().error,
                'Error during sync domain state'
            )


class TestCanAdminAddInvalidatedAlias(TestCase):
    N = 10

    def test_difference_admins_try_to_add_not_validated_alias(self):
        # Проверяем случай с неподтвержденными алиасами у 2х разных админов разных организаций.
        # adm1 - org1 - domain1.org
        #             - alias.org (not validate)
        # adm2 - org2 - domain2.org
        #             + alias.org (can adm2 do it?) -> Yes (return: True)
        alias = 'alias.org'
        adm1_uid = 123
        adm2_uid = 456
        org_info1 = create_organization(self.meta_connection, self.main_connection, label=get_random_string(self.N),
                                        domain_part='domain1.org', admin_uid=adm1_uid)
        org_info2 = create_organization(self.meta_connection, self.main_connection, label=get_random_string(self.N),
                                        domain_part='domain2.org', admin_uid=adm2_uid)
        DomainModel(self.main_connection).create(name=alias, org_id=org_info1['organization']['id'], owned=False)

        result = assert_can_add_invalidated_alias(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            org_id=org_info2['organization']['id'],
            alias=alias
        )
        assert result is True

    def test_difference_admins_try_to_add_validated_alias(self):
        # Этот тест показывает, что функция can_admin_add_invalidated_alias не работает
        # с подтвержденными доменными алиасами
        #
        # adm1 - org1 - domain1.org
        #             - alias.org (validate)
        # adm2 - org2 - domain2.org
        #             + alias.org (can adm2 do it?) -> No (return: False) - тут должно быть False,
        #             а на деле функция возвращает True

        alias = 'alias.org'
        adm1_uid = 123
        adm2_uid = 456
        org_info1 = create_organization(self.meta_connection, self.main_connection, label=get_random_string(self.N),
                                        domain_part='domain1.org', admin_uid=adm1_uid)
        org_info2 = create_organization(self.meta_connection, self.main_connection, label=get_random_string(self.N),
                                        domain_part='domain2.org', admin_uid=adm2_uid)
        DomainModel(self.main_connection).create(name=alias, org_id=org_info1['organization']['id'], owned=True)

        # result == True
        result = assert_can_add_invalidated_alias(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            org_id=org_info2['organization']['id'],
            alias=alias,
        )
        assert result is True

    def test_one_admin_and_try_to_add_validated_aliases(self):
        # Проверяем, что нельзя добавить один алиас в соседнюю организацию, принадлежащую одному внешнему админу.
        # adm1 - org1 - domain1.org
        #             - alias.org (not validated)
        #      - org2 - domain2.org
        #             - alias.org (can adm1 do it?) -> No (return: False)
        alias = 'alias.org'
        adm1_uid = 123
        org_info1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label=get_random_string(self.N),
            domain_part='domain1.org',
            admin_uid=adm1_uid,
        )
        org_info2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label=get_random_string(self.N),
            domain_part='domain2.org',
            admin_uid=adm1_uid
        )

        # Представим, что домен уже добавлен в другую организацию
        conflicting_org_id = org_info1['organization']['id']
        DomainModel(self.main_connection).create(
            name=alias,
            org_id=conflicting_org_id,
            # Не важно, подтверждён он или нет, всё равно
            # при попытке добавить его в другую организацию у этого же пользователя,
            # должна быть ошибка.
            owned=False,
        )

        result = None
        try:
            assert_can_add_invalidated_alias(
                meta_connection=self.meta_connection,
                main_connection=self.main_connection,
                org_id=org_info2['organization']['id'],
                alias=alias,
            )
        except DuplicateDomain as exc:
            result = exc

        assert_that(
            result,
            has_properties(
                status_code=409,
                message=contains_string('Domain already added into another your organization'),
                params=has_entries(
                    conflicting_org_id=conflicting_org_id,
                ),
            )
        )

    def test_outer_and_inner_admins_try_to_add_not_validated_alias(self):
        # Проверяем случай с неподтвержденными алиасами, когда у внешнего админа одной организации уже есть
        # неподтвержденный алиас, а внутренний админ с другой организацией тоже хочет добавить такой же алиас:
        # adm - org1 - foo.com
        #             - test.com (not validated)
        # inner_adm  - org2 - bar.com
        #             - test.com (can inner_adm do it?) -> Yes (return True)
        alias = 'alias.org'
        adm = 123
        inner_adm = 113 * 10 ** 13 + 100
        org_info1 = create_organization(self.meta_connection, self.main_connection, label=get_random_string(self.N),
                                        domain_part='domain1.org', admin_uid=adm)
        org_info2 = create_organization(self.meta_connection, self.main_connection, label=get_random_string(self.N),
                                        domain_part='domain2.org', admin_uid=inner_adm)
        DomainModel(self.main_connection).create(name=alias, org_id=org_info1['organization']['id'], owned=False)
        result = assert_can_add_invalidated_alias(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            org_id=org_info2['organization']['id'],
            alias=alias,
        )
        assert result is True

    def test_not_ready_organization(self):
        # Проверяем, что можно добавить алиас в соседнюю организацию, принадлежащую одному внешнему админу
        # Если первая организация еще находится в процессе миграции

        alias = 'alias.org'
        admin_id = 123
        org_info1 = create_organization(self.meta_connection, self.main_connection, label=get_random_string(self.N),
                                        domain_part='domain1.org', admin_uid=admin_id)
        org_info2 = create_organization(self.meta_connection, self.main_connection, label=get_random_string(self.N),
                                        domain_part='domain2.org', admin_uid=admin_id)

        DomainModel(self.main_connection).create(name=alias, org_id=org_info2['organization']['id'], owned=False)
        OrganizationMetaModel(self.meta_connection).update(
            update_data={'ready': False},
            filter_data={'id': org_info2['organization']['id']},
        )

        result = assert_can_add_invalidated_alias(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            org_id=org_info1['organization']['id'],
            alias=alias,
        )
        assert result is True


class TestSendSmsText(TestCase):
    def test_send_sms(self):
        # функция отправик смс должна ставить задачу на отправку смс

        # проверим что с разными доменами все отправляется
        domains = [
            'домен.рф',
            'xn--d1acufc.xn--p1ai',
            'domain.com',
            'домен.рф',
            'xn--d1acufc.xn--p1ai',
            'domain.com',
        ]
        for lang in app.config['ORGANIZATION_LANGUAGES']:
            with patch.object(SendSmsTask, 'place_into_the_queue') as place_into_the_queue, \
                    patch('intranet.yandex_directory.src.yandex_directory.core.sms.tasks.lang_for_notification', return_value=lang):

                for domain in domains:
                    sms_domain_confirmed(
                        self.meta_connection,
                        self.main_connection,
                        self.organization['id'],
                        self.admin_uid,
                        domain,
                    )
                    args, kwargs = place_into_the_queue.call_args

                    try:
                        expected_domain = from_punycode(domain)
                    except UnicodeError:
                        expected_domain = domain

                    expected_domain = force_text(expected_domain)

                    assert_that(
                        kwargs,
                        has_entries(
                            uid=self.admin_uid,
                            text=contains_string(
                                expected_domain
                            ),
                        )
                    )


class TestGenerateTechDomainText(TestCase):

    @override_settings(DOMAIN_PART='.ya.com')
    def test_me(self):
        org_id = 1
        cases = [
            ('domain.my', 'domain-my-1.ya.com'),
            ('domain.my', 'domain-my-1.ya.com'),
            ('домен.рф', 'домен-рф-1.ya.com'),
            ('домен.рф', 'домен-рф-1.ya.com'),
        ]


        for domain, expected in cases:
            actual = generate_tech_domain(domain, org_id)
            assert_that(
                actual,
                equal_to(expected)
            )


class Test_delete_domain_with_accounts(TestCase):
    def setUp(self):
        super(Test_delete_domain_with_accounts, self).setUp()
        self.admin_uid = 123
        self.domain = 'my-org.ya.com'
        self.org1 = create_organization(self.meta_connection,
                                        self.main_connection,
                                        label='my-org',
                                        domain_part='.ya.com',
                                        admin_uid=self.admin_uid
                                        )['organization']

    def test_delete_master(self):
        # тестируем удаление мастера, если он есть в паспорте
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_domain_info_from_blackbox') \
                as get_domain_info_from_blackbox, \
            patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain._delete_domain_when_delete_org') as del_domain:
            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.admin_uid,
                'master_domain': None,
                'domain_id': 777777,
                'blocked': False,
            }
            delete_domain_from_passport(self.main_connection, self.org1['id'])
            del_domain.assert_called_once_with(
                self.main_connection,
                self.org1['id'],
                self.admin_uid,
                self.domain,
                None
            )

    def test_delete_master_with_alias(self):
        # тестируем удаление мастера и алиаса,
        # если они есть в паспорте как коннектные домены
        alias_name = 'new-domain.com'
        master_id = 777777

        DomainModel(self.main_connection).create(
            name=alias_name,
            org_id=self.org1['id']
        )

        def domain_info_bb(domain_name):
            if domain_name == self.domain:
                return {
                    'admin_id': self.admin_uid,
                    'master_domain': None,
                    'domain_id': master_id,
                }
            else:
                return {
                    'admin_id': self.admin_uid,
                    'master_domain': master_id,
                    'domain_id': 88888,
                }

        bb_return = Mock(side_effect=domain_info_bb)

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_domain_info_from_blackbox', bb_return),\
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain._delete_domain_when_delete_org') as del_domain:
            delete_domain_from_passport(self.main_connection, self.org1['id'])

            del_domain.assert_has_calls(
                [
                    unittest.mock.call(
                        self.main_connection,
                        self.org1['id'],
                        self.admin_uid,
                        alias_name,
                        None,
                        is_alias=True,
                        master_domain_id=master_id,
                    ),
                    unittest.mock.call(
                        self.main_connection,
                        self.org1['id'],
                        self.admin_uid,
                        self.domain,
                        None,
                    ),
                ]
            )

    def test_not_delete_another_org_with_domain(self):
        # проверяем, что домен не удляется, если у этого админа есть еще одна организация с таким доменом
        # создаем еще одну организацию с таким же доменом
        org2 = create_organization(self.meta_connection,
                                   self.main_connection,
                                   label='my-org2',
                                   domain_part='.ya.com',
                                   admin_uid=self.admin_uid
                                   )['organization']
        DomainModel(self.main_connection).filter(org_id=org2['id']).update(name=self.domain)

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_domain_info_from_blackbox') \
                as get_domain_info_from_blackbox, \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain._delete_domain_when_delete_org') as del_domain:
            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.admin_uid,
                'master_domain': None,
                'domain_id': 777777,
                'blocked': False,
            }
            delete_domain_from_passport(self.main_connection, self.org1['id'])
            assert_not_called(del_domain)

    def test_not_delete_other_admin_in_passport(self):
        # домен не удаляется из паспорта, если принадлежит другому админу
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_domain_info_from_blackbox') \
                as get_domain_info_from_blackbox, \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain._delete_domain_when_delete_org') as del_domain:
            get_domain_info_from_blackbox.return_value = {
                'admin_id': 67890,
                'master_domain': None,
                'domain_id': 777777,
                'blocked': False,
            }
            delete_domain_from_passport(self.main_connection, self.org1['id'])
            assert_not_called(del_domain)

    def test_not_delete_alias(self):
        # алиас не удаляется, если он алиас другого мастера
        alias_name = 'new-domain.com'
        master_id = 777777

        DomainModel(self.main_connection).create(
            name=alias_name,
            org_id=self.org1['id']
        )

        def domain_info_bb(domain_name):
            if domain_name == self.domain:
                return {
                    'admin_id': self.admin_uid,
                    'master_domain': None,
                    'domain_id': master_id,
                }
            else:
                # алиас принадлежит другому мастеру
                return {
                    'admin_id': self.admin_uid,
                    'master_domain': 99999,
                    'domain_id': 88888,
                }

        bb_return = Mock(side_effect=domain_info_bb)

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_domain_info_from_blackbox', bb_return),\
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain._delete_domain_when_delete_org') as del_domain:
            delete_domain_from_passport(self.main_connection, self.org1['id'])

            # удаляем только мастер
            del_domain.assert_called_once_with(
                self.main_connection,
                self.org1['id'],
                self.admin_uid,
                self.domain,
                None,
            )

    def test_not_delete_domain_with_accounts_in_passport(self):
        service_with_robot = ServiceModel(self.meta_connection).create(
            slug='fake-slug',
            name='Name',
            robot_required=True,
            client_id='fake-client-id',
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.org1['id'],
            service_with_robot['slug'],
        )
        self.process_tasks()

        robot = UserModel(self.main_connection).filter(
            is_robot=True,
            org_id=self.org1['id'],
        ).fields('id').scalar()

        assert_that(
            len(robot),
            equal_to(1)
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_domain_info_from_blackbox') \
                as get_domain_info_from_blackbox, \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_organization_admin_uid') as get_admin_uid, \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain._delete_domain_when_delete_org') as del_domain:
            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.admin_uid,
                'master_domain': None,
                'domain_id': 777777,
                'bocked': False,
            }
            get_admin_uid.return_value = 321
            self.mocked_blackbox.account_uids.return_value = [robot[0], 12345]

            with self.assertRaises(ImmediateReturn):
                delete_domain_with_accounts(self.main_connection, self.org1['id'])
            assert_not_called(del_domain)

    def test_delete_domain_with_robots_in_passport(self):
        org_id = self.organization['id']

        service_with_robot = ServiceModel(self.meta_connection).create(
            slug='fake-slug',
            name='Name',
            robot_required=True,
            client_id='fake-client-id',
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            service_with_robot['slug'],
        )
        self.process_tasks()

        robot = UserModel(self.main_connection).filter(
            is_robot=True,
            org_id=org_id,
        ).fields('id').scalar()

        assert_that(
            len(robot),
            equal_to(1)
        )

        # создаем группы и департаменты с uid
        group_uids = [7117, 6116]
        dep_uids = [8888]
        for uid in group_uids:
            self.create_group(org_id=org_id, uid=uid)
        self.create_department(org_id=org_id, uid=dep_uids[0])

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_domain_info_from_blackbox') \
                as get_domain_info_from_blackbox, \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain.get_organization_admin_uid') as get_admin_uid, \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.domain._delete_domain_when_delete_org') as del_domain:
            get_domain_info_from_blackbox.return_value = {
                'admin_id': self.admin_uid,
                'master_domain': None,
                'domain_id': 123,
                'blocked': False,
            }

            get_admin_uid.return_value = self.user['id']
            self.mocked_blackbox.account_uids.return_value = [robot[0], self.user['id']] + group_uids + dep_uids

            delete_domain_with_accounts(self.main_connection, org_id)
            del_domain.assert_called()
