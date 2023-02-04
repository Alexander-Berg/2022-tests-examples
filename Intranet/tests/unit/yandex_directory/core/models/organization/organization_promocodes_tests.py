# -*- coding: utf-8 -*-
import datetime

from intranet.yandex_directory.src.yandex_directory.common.utils import utcnow
from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationPromocodeModel,
    PromocodeModel,
)
from intranet.yandex_directory.src.yandex_directory.core.exceptions import (
    PromocodeExpiredException,
    PromocodeInvalidException,
)

from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    is_not,
)

from testutils import (
    TestCase,
    create_organization,
)


class TestOrganizationPromocodeModel(TestCase):
    def test_get_active_for_organization_should_not_return_expired_active_code(self):
        # просто проверяем что мы можем активировать промокод
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        active_promocode = OrganizationPromocodeModel(self.main_connection).get_active_for_organization(
            org_id=self.organization['id'],
        )
        assert_that(
            active_promocode,
            is_not(None),
        )

        OrganizationPromocodeModel(self.main_connection).update(
            update_data={'expires_at': datetime.date(year=1000, day=1, month=1)}
        )

        active_promocode = OrganizationPromocodeModel(self.main_connection).get_active_for_organization(
            org_id=self.organization['id'],
        )
        assert_that(
            active_promocode,
            equal_to(None),
        )

    def test_simple_activation(self):
        # просто проверяем что мы можем активировать промокод
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )

        def check_promocode():
            promocodes = OrganizationPromocodeModel(self.main_connection).find()
            assert_that(
                len(promocodes),
                equal_to(1),
            )
            assert_that(
                promocodes[0],
                has_entries(
                    promocode_id=promocode['id'],
                    active=True,
                    org_id=self.organization['id'],
                )
            )

        # активируем промокод и проверим что сам промокод для
        # организации активировался
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )
        check_promocode()

        # активируем промокод второй раз, проверим что промокод все еще будет активирован
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )
        check_promocode()

    def test_activate_existing_promocode(self):
        # проверяем, что если организация активировала промокод, затем активировала второй
        # и после почему-то захотела вернуть первый, мы активируем первый, а второй деактивируем
        first_promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )
        second_promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_70',
            activate_before=datetime.date(year=2150, month=1, day=1),
            expires_at=datetime.date(year=2160, month=1, day=1),
            description={
                'ru': 'промо код 2',
                'en': 'promo code 2',
            },
            product_ids={
                'connect': {
                    1: 7890,
                },
            },
        )

        def _check_promocodes(activated_promocode):
            """
            Проверяем, что у организации два промокода и активирован только один из них
            """
            # теперь у нас должно быть два активированных промокода OrganizationPromocodeModel
            assert_that(
                org_promocode_model.count(),
                equal_to(2),
            )
            first_promocode_in_organization = org_promocode_model.find(
                filter_data={
                    'promocode_id': first_promocode['id'],
                },
                one=True,
            )
            assert_that(
                first_promocode_in_organization,
                has_entries(
                    promocode_id=first_promocode['id'],
                    active=True if activated_promocode == 'first' else False,
                )
            )
            second_promocode_in_organization = org_promocode_model.find(
                filter_data={
                    'promocode_id': second_promocode['id'],
                },
                one=True,
            )
            assert_that(
                second_promocode_in_organization,
                has_entries(
                    promocode_id=second_promocode['id'],
                    active=True if activated_promocode == 'second' else False,
                )
            )

        org_promocode_model = OrganizationPromocodeModel(self.main_connection)

        # активируем сначала первый, затем второй промокод
        org_promocode_model.activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=first_promocode['id'],
            author_id=None,
        )
        org_promocode_model.activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=second_promocode['id'],
            author_id=None,
        )

        _check_promocodes(activated_promocode='second')

        # активируем первый промокод
        org_promocode_model.activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=first_promocode['id'],
            author_id=None,
        )

        # теперь у организации активен первый промокод, для него обновилась дата активации,
        # и второй промокод деактивировался
        _check_promocodes(activated_promocode='first')

    def test_simple_activation_for_two_organizations(self):
        # просто проверяем что мы можем активировать один и то же промокод
        # в двух организациях
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )

        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-org',
        )['organization']

        # активируем промокод и проверим что сам промокод для
        # организации активировался
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=second_organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        promocodes = OrganizationPromocodeModel(self.main_connection).find()
        assert_that(
            len(promocodes),
            equal_to(2),
        )
        assert_that(
            [i['org_id'] for i in promocodes],
            equal_to([self.organization['id'], second_organization['id']])
        )

    def test_activate_expired_promocode(self):
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=utcnow() - datetime.timedelta(days=1000),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )
        with self.assertRaises(PromocodeExpiredException):
            OrganizationPromocodeModel(self.main_connection).activate_for_organization(
                org_id=self.organization['id'],
                promocode_id=promocode['id'],
                author_id=None,
            )

    def test_activate_expired_activation_date_promocode(self):
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=utcnow() - datetime.timedelta(days=1000),
            expires_at=datetime.date(year=2050, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )
        with self.assertRaises(PromocodeExpiredException):
            OrganizationPromocodeModel(self.main_connection).activate_for_organization(
                org_id=self.organization['id'],
                promocode_id=promocode['id'],
                author_id=None,
            )

    def test_activate_unknown_promocode(self):
        with self.assertRaises(PromocodeInvalidException):
            OrganizationPromocodeModel(self.main_connection).activate_for_organization(
                org_id=self.organization['id'],
                promocode_id='unknown_promocode',
                author_id=None,
            )

    def test_deactivate_expired_promocodes(self):
        promocode = PromocodeModel(self.meta_connection).create(
            id='promocode',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
            },
            product_ids={
                'connect': {
                    3: 12345,  # для 3 пользователей и больше цена будет по продукту 1234
                },
            },
        )
        # создадим две организации
        first_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']

        # активируем промокод в первой организации
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=first_organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )
        # и сделаем его просроченным
        OrganizationPromocodeModel(self.main_connection).update(
            update_data={'expires_at': datetime.date(year=1000, day=1, month=1)}
        )

        # активируем промокод во второй организации
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=second_organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        promocodes_count = OrganizationPromocodeModel(self.main_connection).count(
            filter_data={'active': True},
        )

        # всего у нас два активных промокода
        assert_that(
            promocodes_count,
            equal_to(2),
        )

        OrganizationPromocodeModel(self.main_connection).deactivate_expired_promocodes()

        promocodes = OrganizationPromocodeModel(self.main_connection).find(
            filter_data={'active': True},
            fields=['org_id'],
        )

        # теперь активный промокод только один
        assert_that(
            len(promocodes),
            equal_to(1),
        )
        assert_that(
            promocodes,
            equal_to([{'org_id': second_organization['id']}]),
        )

    def test_activation_for_two_organizations_with_activation_limit(self):
        # проверяем, что работает ограничение на количество активаций
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
            activation_limit=1,
        )

        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-org',
        )['organization']

        # активируем промокод и проверим, что второй раз его активировать нельзя
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )
        with self.assertRaises(PromocodeInvalidException):
            OrganizationPromocodeModel(self.main_connection).activate_for_organization(
                org_id=second_organization['id'],
                promocode_id=promocode['id'],
                author_id=None,
            )

        promocodes = OrganizationPromocodeModel(self.main_connection).find()
        assert_that(
            len(promocodes),
            equal_to(1),
        )
