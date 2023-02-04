# -*- coding: utf-8 -*-
from datetime import timedelta
from unittest.mock import patch, Mock
from testutils import (
    TestCase,
    override_settings,
)
from sqlalchemy.exc import IntegrityError
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.models.domain import (
    DomainModel,
    WebmasterDomainLogModel,
    domain_action,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import utcnow, to_punycode, from_punycode
from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationModel,
    ActionModel,
)
from intranet.yandex_directory.src.yandex_directory.core.actions import action
from intranet.yandex_directory.src.yandex_directory.common.exceptions import (
    DomainNotValidatedError,
    DomainNotFound,
)
from testutils import (
    create_organization,
    assert_called_once,
    assert_not_called,
)
from hamcrest import (
    assert_that,
    equal_to,
    calling,
    raises,
    contains,
    contains_inanyorder,
    none,
    has_entries,
    not_none,
    has_length,
    greater_than_or_equal_to,
)

from intranet.yandex_directory.src.yandex_directory.core.utils.domain import domain_is_tech


class TestDomainModel_create(TestCase):
    def setUp(self):
        super(TestDomainModel_create, self).setUp()
        # при создании организации автоматичсеки создается домен и добавляется
        # в табличку с доменами
        self.domain_name = 'борщ.рф'

    def test_simple_create(self):
        # Создаем домен и проверяем, что он появился в базе.
        domain = DomainModel(self.main_connection).create(
            name=self.domain_name,
            org_id=self.organization['id'],
        )
        assert_that(
            domain,
            has_entries(
                name=self.domain_name,
                master=False,
                display=False,
                org_id=self.organization['id'],
                via_webmaster=True,
            )
        )

        # проверим, что в базе домен хранится в punicode
        all_domains = self.main_connection.execute('SELECT name from ydir.domains').fetchall()
        assert_that(
            all_domains,
            contains_inanyorder(
                contains('not_yandex_test.ws.autotest.yandex.ru'),
                contains(self.domain_name.encode('idna').decode()),
            )
        )

        # test data in database
        domain_from_db = DomainModel(self.main_connection).get(domain['name'],
                                           org_id=self.organization['id'])
        self.assertIsNotNone(domain_from_db)
        self.assertEqual(domain_from_db['name'], self.domain_name)
        self.assertEqual(domain_from_db['org_id'], self.organization['id'])
        self.assertEqual(domain['master'], False)
        self.assertEqual(domain['display'], False)

    def test_create_with_webmaster_activation(self):
        # Проверим, что если в настрояках выставлен флаг, что подтверждение
        # должно происходить через вебмастер, то и домен будет создан
        # с соответствующим признаком
        domain = DomainModel(self.main_connection).create(
            name=self.domain_name,
            org_id=self.organization['id'],
        )
        assert_that(
            domain,
            has_entries(
                name=self.domain_name,
                master=False,
                display=False,
                org_id=self.organization['id'],
                # Сейчас по умолчанию это поле выставляется в False
                via_webmaster=True,
            )
        )

    def test_create_with_same_name(self):
        # При создании домена с одинаковыми именами - выпрыгивает ошибка
        # IntegrityError из sqlalchemy
        domain = DomainModel(self.main_connection).create(
            name=self.domain_name,
            org_id=self.organization['id']
        )
        self.assertIsNotNone(domain)

        with self.assertRaises(IntegrityError):
            DomainModel(self.main_connection).create(
                name=self.domain_name,
                org_id=self.organization['id']
            )

    def test_create_with_uppercase(self):
        # Создаем домен c именем в верхнем регистре и проверим, что он
        # приводится его в нижний
        domain_name_uppercase = 'DOMAIN.COM'
        domain = DomainModel(self.main_connection).create(
            name=domain_name_uppercase,
            org_id=self.organization['id'],
        )
        self.assertEqual(domain['name'], domain_name_uppercase.lower())
        self.assertEqual(domain['org_id'], self.organization['id'])


class TestDomainModel_update_one(TestCase):
    def setUp(self):
        super(TestDomainModel_update_one, self).setUp()
        self.name = 'борщ.рф'
        self.another_name = 'супа.net'
        self.mocked_blackbox.hosted_domains.return_value = {
            "hosted_domains": [
                {
                    "domain": to_punycode(self.organization_domain),
                    "domid": "1",
                },
                {
                    "domain": to_punycode(self.name.lower()),
                    "domid": "2",
                }
            ]
        }

    def test_update_name(self):
        # Проверим, что имя домена обновляется
        domain = DomainModel(self.main_connection).create(
            name=self.name,
            org_id=self.organization['id']
        )
        self.assertIsNotNone(domain['name'])
        self.assertEqual(domain['name'], self.name)
        self.assertEqual(domain['org_id'], self.organization['id'])

        DomainModel(self.main_connection).update_one(
            name=domain['name'],
            org_id=self.organization['id'],
            data={
                'name': self.another_name,
            }
        )
        # домена с прошлым именем - уже не найти
        old_domain = DomainModel(self.main_connection).get(domain['name'],
                                       org_id=self.organization['id'])
        self.assertIsNone(old_domain)

        # а вот с новым именем домен есть
        update_domain = DomainModel(self.main_connection).get(self.another_name,
                                          org_id=self.organization['id'])
        self.assertEqual(update_domain['name'], self.another_name)
        self.assertEqual(update_domain['org_id'], self.organization['id'])

    def test_update_master(self):
        # Проверим, что признак master у домена обновляется,
        # а если другой домен уже был master-ом, то у него этот признак -
        # снимается
        # Тут же проверяем и display, потому что теперь display должен совпадает с master
        # https://st.yandex-team.ru/DIR-2042, https://st.yandex-team.ru/DIR-1992, https://st.yandex-team.ru/DIR-2158

        self.mocked_blackbox.hosted_domains.side_effect = [{
            "hosted_domains": [
                {
                    "domain": self.organization_domain,
                    "domid": "1",
                    "master_domain": "",
                    "born_date": '2016-05-24 00:40:28',
                    "mx": "1",
                    "admin": self.admin_uid,
                },
            ]}, {
            "hosted_domains": [
                {
                    "domain": to_punycode(self.name),
                    "domid": "2",
                    "master_domain": self.organization_info['domain']['name'],
                    "born_date": '2016-05-24 00:40:28',
                    "mx": "1",
                    "admin": self.admin_uid,
                }
            ]
        }
        ]

        domain_master = DomainModel(self.main_connection).create(
            name=self.name,
            org_id=self.organization['id'],
            owned=True,
        )
        self.assertEqual(domain_master['name'], self.name)
        self.assertEqual(domain_master['org_id'], self.organization['id'])

        # сделаем первый домен мастером
        DomainModel(self.main_connection).update_one(
            name=self.name,
            org_id=self.organization['id'],
            data=dict(master=True)
        )

        assert_called_once(
            self.mocked_passport.set_master_domain,
            '1',
            '2'
        )

        old_master = DomainModel(self.main_connection).get(
            domain_name=self.organization_domain,
            org_id=self.organization['id'],
        )
        assert_that(
            old_master,
            has_entries(
                owned=True,
                master=False
            )
        )
        new_master = DomainModel(self.main_connection).get(
            domain_name=self.name,
            org_id=self.organization['id'],
        )
        assert_that(
            new_master,
            has_entries(
                owned=True,
                master=True
            )
        )

    def test_already_changed_in_passport(self):
        # случай когда мастер домен уже поменялся в паспорте
        # и старый мастер уже алиас нового мастера

        self.mocked_blackbox.hosted_domains.side_effect = [{
            "hosted_domains": [
                {
                    "domain": to_punycode(self.organization_domain),
                    "domid": "1",
                    "master_domain": to_punycode(self.name),
                    "born_date": '2016-05-24 00:40:28',
                    "mx": "1",
                    "admin": self.admin_uid,
                },
            ]}, {
            "hosted_domains": [
                {
                    "domain": to_punycode(self.name),
                    "domid": "2",
                    "master_domain": "",
                    "born_date": '2016-05-24 00:40:28',
                    "mx": "1",
                    "admin": self.admin_uid,
                }
            ]
        }]

        domain_master = DomainModel(self.main_connection).create(
            name=self.name,
            org_id=self.organization['id'],
            owned=True,
        )
        self.assertEqual(domain_master['name'], self.name)
        self.assertEqual(domain_master['org_id'], self.organization['id'])

        # сделаем первый домен мастером
        DomainModel(self.main_connection).update_one(
            name=self.name,
            org_id=self.organization['id'],
            data=dict(master=True)
        )

        assert_not_called(
            self.mocked_passport.set_master_domain
        )

        old_master = DomainModel(self.main_connection).get(
            domain_name=self.organization_domain,
            org_id=self.organization['id'],
        )
        assert_that(
            old_master,
            has_entries(
                owned=True,
                master=False
            )
        )
        new_master = DomainModel(self.main_connection).get(
            domain_name=self.name,
            org_id=self.organization['id'],
        )
        assert_that(
            new_master,
            has_entries(
                owned=True,
                master=True
            )
        )

    def test_update_old_master_not_exist_in_passport(self):
        # проверка для случая когда старого мастера уже нет в паспорте

        domain_master = DomainModel(self.main_connection).create(
            name=self.name,
            org_id=self.organization['id'],
            owned=True,
        )
        self.assertEqual(domain_master['name'], self.name)
        self.assertEqual(domain_master['org_id'], self.organization['id'])

        # замокаем блекбокс, чтобы он не ломился в паспорт
        # в паспорте у админа только новый мастер домен
        self.mocked_blackbox.hosted_domains.side_effect = [
            {

            },
            {
            "hosted_domains": [
                {
                    "domain": to_punycode(self.name.lower()),
                    "domid": "2",
                    "master_domain": "",
                    "born_date": '2016-05-24 00:40:28',
                    "mx": "1",
                    "admin": self.admin_uid,
                }
            ]
        }
        ]
        # сделаем первый домен мастером
        DomainModel(self.main_connection).update_one(
            name=self.name,
            org_id=self.organization['id'],
            data=dict(master=True)
        )

        assert_not_called(
            self.mocked_passport.set_master_domain,
        )

        old_master = DomainModel(self.main_connection).get(
            domain_name=self.organization_domain,
            org_id=self.organization['id'],
        )
        assert_that(
            old_master,
            has_entries(
                owned=True,
                master=False
            )
        )
        new_master = DomainModel(self.main_connection).get(
            domain_name=self.name,
            org_id=self.organization['id'],
        )
        assert_that(
            new_master,
            has_entries(
                owned=True,
                master=True
            )
        )

    def test_update_master_and_display(self):
        # Проверим, что оба признака меняются.

        domain = DomainModel(self.main_connection).create(
            name=self.name,
            org_id=self.organization['id'],
            owned=True,
        )
        self.assertEqual(domain['name'], self.name)
        self.assertEqual(domain['org_id'], self.organization['id'])
        self.assertEqual(domain['master'], False)
        self.assertEqual(domain['display'], False)

        self.mocked_blackbox.hosted_domains.side_effect = [{
            "hosted_domains": [
                {
                    "domain": self.organization_domain,
                    "domid": "1",
                    "master_domain": "",
                    "born_date": '2016-05-24 00:40:28',
                    "mx": "1",
                    "admin": self.admin_uid,
                },
            ]}, {
            "hosted_domains": [
                {
                    "domain": to_punycode(self.name),
                    "domid": "2",
                    "master_domain": self.organization_info['domain']['name'],
                    "born_date": '2016-05-24 00:40:28',
                    "mx": "1",
                    "admin": self.admin_uid,
                }
            ]
        }]
        with patch('intranet.yandex_directory.src.yandex_directory.passport.client.PassportApiClient.set_master_domain', Mock(return_value=True)):
            DomainModel(self.main_connection).update_one(
                name=self.name,
                org_id=self.organization['id'],
                data=dict(master=True, display=True)
            )
        update_domain = DomainModel(self.main_connection).get(domain_name=self.name, org_id=self.organization['id'])

        self.assertEqual(update_domain['name'], self.name)
        self.assertEqual(update_domain['org_id'], self.organization['id'])
        self.assertEqual(update_domain['master'], True)
        self.assertEqual(update_domain['display'], True)


class TestDomainModel_find(TestCase):
    def setUp(self):
        super(TestDomainModel_find, self).setUp()
        self.orginfo_gmail = create_organization(
            self.meta_connection,
            self.main_connection,
            name={'ru': 'ГеМейл'},
            label='gmail',
        )
        self.orginfo_yandex = create_organization(
            self.meta_connection,
            self.main_connection,
            name={'ru': 'Яндекс'},
            label='yandex',
        )
        self.orginfo_yandex_team = create_organization(
            self.meta_connection,
            self.main_connection,
            name={'ru': 'Яндекс Тим'},
            label='yandex-team',
        )

        self.domain_gmail = self.orginfo_gmail['domain']
        self.domain_yandex = self.orginfo_yandex['domain']
        self.domain_yandex_team = self.orginfo_yandex_team['domain']

    def test_find_by_master(self):
        # Проверим, что поиск по полю master без указания организации
        # возвращает все соответсвующие домены из всех организаций

        response = DomainModel(self.main_connection).find(
            filter_data={'master': True}
        )
        self.assertIsNotNone(response)
        # 4 == 3+1 - домен организации из TestCase
        self.assertEqual(len(response), 4)

    def test_find_by_display(self):
        # Проверим, что поиск по полю display без указания организации
        # возвращает все соответсвующие домены из всех организаций

        response = DomainModel(self.main_connection).find(
            filter_data={'display': True}
        )
        self.assertIsNotNone(response)
        # 4 == 3+1 - домен организации из TestCase
        self.assertEqual(len(response), 4)

    def test_find_by_domain_name(self):
        # Проверим, что поиск по названию домена - отрабатывает

        response = DomainModel(self.main_connection).find(filter_data={
            'name': self.domain_gmail['name']
        })
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['org_id'],
                         self.orginfo_gmail['organization']['id'])

    def test_one_not_found(self):
        # ищем 1 запись по домену которого нет в базе
        response = DomainModel(self.main_connection).find(filter_data={
            'name': 'domain_not_found.com'
        }, one=True)
        self.assertIsNone(response)


class TestDomainModel_delete(TestCase):
    def setUp(self):
        super(TestDomainModel_delete, self).setUp()
        self.domain_name = 'борщ.рф'
        DomainModel(self.main_connection).create(
            name=self.domain_name,
            org_id=self.organization['id'],
            owned=True,
        )

    def test_master(self):
        # запрещено удалять master_domain домен
        master_domain = DomainModel(self.main_connection).get_master(self.organization['id'])
        with self.assertRaises(RuntimeError):
            DomainModel(self.main_connection).delete(filter_data={'name': master_domain['name']})

    @override_settings(DOMAIN_PART='.tech.domain.yandex.ru')
    def test_del_tech_domain(self):
        # разрешаем удалять технические домены

        # создадим тех. домен
        tech_domain_end = '.tech.domain.yandex.ru'
        tech_domain = 'tech_domain' + tech_domain_end
        DomainModel(self.main_connection).create(
            name=tech_domain,
            org_id=self.organization['id'],
            owned=True
        )
        # пробуем удалить
        DomainModel(self.main_connection).delete(filter_data={'name': tech_domain})
        assert_that(
            DomainModel(self.main_connection).get(tech_domain, self.organization['id']),
            none()
        )

    def test_forse_remove(self):
        # можно удалить master домен с force_remove_all=True

        DomainModel(self.main_connection).delete(filter_data={'name': self.domain_name}, force_remove_all=True)
        assert_that(
            DomainModel(self.main_connection).get(self.domain_name, self.organization['id']),
            none()
        )


class TestDomainModel_is_tech(TestCase):
    def test_is_tech(self):
        # проверяем является ли домен техническим

        # произвальный домен
        assert_that(
            domain_is_tech('yandex.ru'),
            equal_to(False)
        )
        # поддомен для технического домена
        assert_that(
            domain_is_tech('yandex'+app.config['DOMAIN_PART']),
            equal_to(True)
        )


class Test_from_punycode(TestCase):
    def test_latin(self):
        # строка латиница в unicode и str
        assert_that(
            from_punycode('domain.com'),
            equal_to('domain.com')
        )

        assert_that(
            from_punycode('domain.com'),
            equal_to('domain.com')
        )

    def test_cyrillic(self):
        # строка кириллица в unicode и str
        assert_that(
            from_punycode('xn--d1acufc.xn--p1ai'),
            equal_to('домен.рф')
        )

        assert_that(
            from_punycode('xn--d1acufc.xn--p1ai'),
            equal_to('домен.рф')
        )


class TestDomainModel_count_organizations_with_non_tech_domains(TestCase):
    create_organization = False

    def test_count_organizations_with_non_tech_domains(self):
        # проверяем что количество организаций, добавивших не технический домен считается правильно
        # создадим несколько организаций и домен в каждой из них, часть с owned=True, часть с owned=False
        for i in range(3):
            organization = create_organization(
                self.meta_connection,
                self.main_connection,
                name={'ru': 'org %s' % i},
                label='org_%s' % i,
            )['organization']
            DomainModel(self.main_connection).create(
                name='yandex_%s.ru' % i,
                org_id=organization['id'],
                owned=True,
            )

        for i in range(7):
            organization = create_organization(
                self.meta_connection,
                self.main_connection,
                name={'ru': 'org %s' % i},
                label='no_org_%s' % i,
            )['organization']
            DomainModel(self.main_connection).create(
                name='yandex_%s.ru' % i,
                org_id=organization['id'],
                owned=False,
            )

        # проверим число организаций, добавивших хотя бы один не технический домен
        assert_that(
            DomainModel(self.main_connection).count_organizations_with_non_tech_domains(),
            equal_to(OrganizationModel(self.main_connection).count())
        )

        # проверим число организаций, подтвердивших хотя бы один не технический домен
        domains = DomainModel(self.main_connection).find(filter_data={'owned': True})
        exp_owned_count = len(
            set(
                [
                    d['org_id']
                    for d in domains
                    if not domain_is_tech(d['name'])
                ]
            )
        )
        res = DomainModel(self.main_connection).count_organizations_with_non_tech_domains(owned=True)
        assert_that(
            res,
            equal_to(exp_owned_count)
        )


class TestDomainModel_get_validation_stat(TestCase):

    def setUp(self):
        super(TestDomainModel_get_validation_stat, self).setUp()

        domain_model = DomainModel(self.main_connection)
        for i in range(1, 50):
            domain_model.create(
                name='domain{}.com'.format(i),
                org_id=self.organization['id'],
            )

        for i in range(50, 100):
            domain = domain_model.create(
                name='domain{}.com'.format(i),
                org_id=self.organization['id'],
                owned=True,
                via_webmaster=bool(i % 2)
            )
            domain_model.update_one(
                name=domain['name'],
                org_id=self.organization['id'],
                data={
                    'created_at': utcnow() - timedelta(hours=i),
                    'validated_at': utcnow()
                }
            )

    def test_get_validation_stat(self):
        stat = DomainModel(self.main_connection).get_validation_stat(10)
        assert_that(
            stat,
            has_entries(
                pdd=has_entries(
                    added_count=not_none(),
                    added_owned=not_none(),
                    validation_delay=has_length(
                        greater_than_or_equal_to(1)
                    ),
                ),
                webmaster=has_entries(
                    added_count=not_none(),
                    added_owned=not_none(),
                    validation_delay=has_length(
                        greater_than_or_equal_to(1)
                    ),
                ),
            )
        )


class TestWebmasterDomainLogModel_get_validation_start_count(TestCase):
    def setUp(self):
        super(TestWebmasterDomainLogModel_get_validation_start_count, self).setUp()
        domain_model = DomainModel(self.main_connection)

        for i in range(3, 13):
            domain_name = 'domain{}.com'.format(i)
            domain = domain_model.create(
                name=domain_name,
                org_id=self.organization['id'],
                via_webmaster=True
            )
            domain_model.update_one(
                name=domain['name'],
                org_id=self.organization['id'],
                data={
                    'created_at': utcnow() - timedelta(days=i),
                }
            )
            # якобы запускали старт проверки по 2 раза
            WebmasterDomainLogModel(self.main_connection).create(
                org_id=self.organization['id'],
                uid=self.admin_uid,
                name=domain_name,
                action=domain_action.verify,
            )
            WebmasterDomainLogModel(self.main_connection).create(
                org_id=self.organization['id'],
                uid=self.admin_uid,
                name=domain_name,
                action=domain_action.verify,
            )

    def test_empty_data(self):
        # нет запусков проверок за указанные дни
        count = WebmasterDomainLogModel(self.main_connection).get_validation_start_count(1)
        assert_that(
            count,
            equal_to(0)
        )

    def test_start_count(self):
        # есть запуски проверок за указанные дни
        count = WebmasterDomainLogModel(self.main_connection).get_validation_start_count(13)
        assert_that(
            count,
            equal_to(10)
        )


class TestWebmasterDomainLogModel_get_last_verification_type(TestCase):
    def test_get_last_verification_type(self):
        domain_model = DomainModel(self.main_connection)
        org_id = self.organization['id']

        domain_name = 'domain.com'
        domain_model.create(
            name=domain_name,
            org_id=org_id,
            via_webmaster=True
        )

        # якобы запускали старт проверки по 2 раза
        WebmasterDomainLogModel(self.main_connection).create(
            org_id=org_id,
            uid=self.admin_uid,
            name=domain_name,
            action=domain_action.verify,
            verification_type='test1'
        )

        WebmasterDomainLogModel(self.main_connection).create(
            org_id=org_id,
            uid=self.admin_uid,
            name=domain_name,
            action=domain_action.verify,
            verification_type='test2'
        )
        WebmasterDomainLogModel(self.main_connection).filter(verification_type='test1').update(
            timestamp=utcnow() - timedelta(days=1)
        )

        last_verification_type = WebmasterDomainLogModel(self.main_connection).get_last_verification_type(
            org_id,
            domain_name,
            self.admin_uid
        )

        assert_that(
            last_verification_type,
            equal_to('test2')
        )


class TestDomainModel_change_master_domain(TestCase):

    def setUp(self):
        super(TestDomainModel_change_master_domain, self).setUp()
        self.clean_actions_and_events()

        self.owned_alas = DomainModel(self.main_connection).create(
            'new.master.domain.yandex.ru',
            self.organization['id'],
            owned=True,
            via_webmaster=True,
        )

        self.not_owned_alias = DomainModel(self.main_connection).create(
            'not.owned.master.domain.yandex.ru',
            self.organization['id'],
            owned=False,
            via_webmaster=True,
        )
        self.mocked_blackbox.hosted_domains.return_value = {
            "hosted_domains": [
                {
                    "domain": self.organization_domain,
                    "domid": "1",
                },
                {
                    "domain": to_punycode(self.owned_alas['name']),
                    "domid": "2",
                }
            ]
        }

    def test_me(self):
        self.mocked_blackbox.hosted_domains.side_effect = [{
            "hosted_domains": [
                {
                    "domain": self.organization_domain,
                    "domid": "1",
                    "master_domain": "",
                    "born_date": '2016-05-24 00:40:28',
                    "mx": "1",
                    "admin": self.admin_uid,
                },
            ]}, {
            "hosted_domains": [
                {
                    "domain": to_punycode(self.owned_alas['name']),
                    "domid": "2",
                    "master_domain": self.organization_info['domain']['name'],
                    "born_date": '2016-05-24 00:40:28',
                    "mx": "1",
                    "admin": self.admin_uid,
                }
            ]
        }
        ]
        DomainModel(self.main_connection).change_master_domain(
            self.organization['id'],
            self.owned_alas['name'],
        )

        assert_that(
            DomainModel(self.main_connection).get_master(self.organization['id']),
            has_entries(
                name=self.owned_alas['name']
            )
        )
        assert_that(
            ActionModel(self.main_connection).filter(org_id=self.organization['id']).all(),
            contains_inanyorder(
                has_entries(
                    name=action.domain_master_modify
                )
            )
        )

    def test_try_change_to_unknown_domain(self):
        assert_that(
            calling(DomainModel(self.main_connection).change_master_domain).with_args(
                self.organization['id'],
                'unknowm.com',
            ),
            raises(DomainNotFound)
        )


class TestDomainModel_delete_domain(TestCase):

    def setUp(self):
        super(TestDomainModel_delete_domain, self).setUp()
        self.domain_name = 'борщ.рф'
        DomainModel(self.main_connection).create(
            name=self.domain_name,
            org_id=self.organization['id'],
            owned=True,
            via_webmaster=True,
        )
        self.clean_actions_and_events()

    def test_delete_alias(self):
        self.mocked_blackbox.hosted_domains.side_effect = [
            {
                "hosted_domains": [{
                    "domain": to_punycode(self.organization_domain),
                    "domid": "1",
                    "master_domain": "",
                    "admin": self.admin_uid,
                    "born_date": "2007-01-01 00:00:00",
                    "mx": 1
                },
                ]
            },
            {
                "hosted_domains": [
                    {
                        "domain": to_punycode(self.domain_name.lower()),
                        "domid": "2",
                        "master_domain": self.organization_domain,
                        "admin": self.admin_uid,
                        "born_date": "2007-01-01 00:00:00",
                        "mx": 1
                    },

                ]
            },
        ]

        DomainModel(self.main_connection).delete_domain(self.domain_name, self.organization['id'], self.admin_uid)

        assert_called_once(
            self.mocked_passport.domain_alias_delete,
            '1',
            '2'
        )

        assert_that(
            DomainModel(self.main_connection).get(self.domain_name, self.organization['id']),
            none()
        )

        assert_that(
            ActionModel(self.main_connection).filter(org_id=self.organization['id']).all(),
            contains_inanyorder(
                has_entries(
                    name=action.domain_delete
                )
            )
        )
