from unittest import mock
from datetime import datetime, timezone, timedelta
import pytest
from decimal import Decimal
import pytz
import webtest
from marshmallow import validates_schema

from billing.apikeys.apikeys.balalance_wrapper.balance import CommonBalance
from billing.apikeys.apikeys.balance2apikeys import Balance2Apikeys
from billing.apikeys.apikeys.controllers import ProjectServiceLinkSchema

from .utils import (
    FakeReadPreferenceSettings,
    FakeServiceStartrekClient,
    mock_datetime,
    new_tarifficator_state_content,
    new_add_next_state_to_tarifficator_state,
)


@validates_schema
def validate_person(self, data, **kwargs):
    pass


@pytest.fixture
def web_app():
    with mock.patch('billing.apikeys.apikeys.mapper.context.ReadPreferenceSettings', new=FakeReadPreferenceSettings):
        with mock.patch('billing.apikeys.apikeys.startrek_wrapper.ServiceStartrekClient', new=FakeServiceStartrekClient):
            from billing.apikeys.apikeys.handles_ui import logic
            return webtest.TestApp(logic)


def test_schedule_contractless_tariff(mongomock, web_app, simple_link, empty_contractless_accessable_tariff):
    project = simple_link.project
    service = simple_link.service
    user = project.user

    res = web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {'scheduled_tariff_cc': empty_contractless_accessable_tariff.cc},
        headers={'X-User-Id': '{}'.format(user.uid)},
    )
    simple_link.reload()

    assert res.status_int in (302, 303)
    assert simple_link.config.scheduled_tariff_date is not None
    assert simple_link.config.scheduled_tariff == empty_contractless_accessable_tariff.cc


def test_schedule_contractless_tariff_selected_date(
        mongomock, web_app, link_with_fake_contractless_tariff, empty_contractless_accessable_tariff):
    link = link_with_fake_contractless_tariff
    another_tariff = empty_contractless_accessable_tariff
    project = link.project
    service = link.service
    user = project.user
    dt = datetime.utcnow() + timedelta(days=3)
    desired_date = datetime(dt.year, dt.month, dt.day, tzinfo=timezone(timedelta(seconds=3 * 60 * 60)))
    dt = dt - timedelta(days=1)
    actual_switch_utc = datetime(dt.year, dt.month, dt.day, 21, tzinfo=timezone(timedelta(seconds=0)))

    res = web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {
            'scheduled_tariff_cc': another_tariff.cc,
            'scheduled_tariff_date': desired_date.isoformat()  # '<YYYY>-<MM>-<DD>T<HH>:<MM>:<SS>+<HH>:<MM>'
        },
        headers={'X-User-Id': '{}'.format(user.uid)},
    )
    link.reload()

    assert res.status_int in (302, 303)
    assert link.config.scheduled_tariff_date == actual_switch_utc
    assert link.config.scheduled_tariff == empty_contractless_accessable_tariff.cc


def test_schedule_contractless_tariff_selected_date_tz_unaware(
        mongomock, web_app, link_with_fake_contractless_tariff, empty_contractless_accessable_tariff):
    """
    Даты без tzinfo считаются по-умолчанию в utc для системы.
    """
    link = link_with_fake_contractless_tariff
    another_tariff = empty_contractless_accessable_tariff
    project = link.project
    service = link.service
    user = project.user
    dt = datetime.utcnow() + timedelta(days=3)
    desired_date = datetime(dt.year, dt.month, dt.day)
    actual_switch_utc = datetime(dt.year, dt.month, dt.day, 21, tzinfo=timezone(timedelta(seconds=0)))

    res = web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {
            'scheduled_tariff_cc': another_tariff.cc,
            'scheduled_tariff_date': desired_date.isoformat()  # '<YYYY>-<MM>-<DD>T<HH>:<MM>:<SS>'
        },
        headers={'X-User-Id': '{}'.format(user.uid)},
    )
    link.reload()

    assert res.status_int in (302, 303)
    assert link.config.scheduled_tariff_date == actual_switch_utc
    assert link.config.scheduled_tariff == empty_contractless_accessable_tariff.cc


def test_schedule_contractless_not_accessable_tariff(
        mongomock, web_app, simple_link, empty_contractless_tariff):
    project = simple_link.project
    service = simple_link.service
    user = project.user

    res = web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {'scheduled_tariff_cc': empty_contractless_tariff.cc},
        headers={'X-User-Id': '{}'.format(user.uid)},
        status='4*',
    )
    simple_link.reload()

    assert res.status_int == 400 and 'Tariff is not attachable' in res


def test_schedule_tariffless_tariff(mongomock, web_app, simple_link, empty_tariffless_tariff):
    project = simple_link.project
    service = simple_link.service
    user = project.user

    simple_link.balance_contract_id = '123'
    simple_link.save()

    res = web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {'scheduled_tariff_cc': empty_tariffless_tariff.cc},
        headers={'X-User-Id': '{}'.format(user.uid)},
    )
    simple_link.reload()

    assert res.status_int in (302, 303)
    assert simple_link.config.scheduled_tariff_date is not None
    assert simple_link.config.scheduled_tariff == empty_tariffless_tariff.cc


def test_schedule_non_applicable_tariffless_tariff(mongomock, web_app, simple_link, empty_tariffless_tariff):
    project = simple_link.project
    service = simple_link.service
    user = project.user

    res = web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {'scheduled_tariff_cc': empty_tariffless_tariff.cc},
        headers={'X-User-Id': '{}'.format(user.uid)},
        status='4*',
    )
    simple_link.reload()

    assert res.status_int == 400 and 'Tariff is not attachable' in res


@mock.patch.object(ProjectServiceLinkSchema, 'validate_person', validate_person)
def test_project_service_link_update_person_non_contractless_tariff(
        mongomock, web_app, link_with_fake_tariff):
    project = link_with_fake_tariff.project
    service = link_with_fake_tariff.service
    user = project.user

    # не офертный тариф
    res = web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {'balance_person': {'id': '123'}},
        headers={'X-User-Id': '{}'.format(user.uid)},
        status='4*',
    )

    assert res.status_int == 400 and 'Tariff is not contractless' in res


@mock.patch.object(ProjectServiceLinkSchema, 'validate_person', validate_person)
def test_project_service_link_update__person_contractless_tariff(
        mongomock, web_app, link_with_fake_contractless_tariff):
    project = link_with_fake_contractless_tariff.project
    service = link_with_fake_contractless_tariff.service
    user = project.user

    res = web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {'balance_person': {'id': '123'}},
        headers={'X-User-Id': ' {}'.format(user.uid)},
    )

    assert res.status_int == 302


def test_create_balance_contract(mongomock, web_app, simple_link, empty_tariff, ph_person, ur_person):
    project = simple_link.project
    service = simple_link.service
    user = project.user

    # недостаточно данных
    res = web_app.post_json(
        '/project/{}/service_link/{}/balance_contract'.format(project.id, service.id),
        {},
        headers={'X-User-Id': '{}'.format(user.uid)},
        status='4*',
    )

    assert res.status_int == 400 and "Missing data" in res

    # плательщик - физик (а должен быть юрик)
    res = web_app.post_json(
        '/project/{}/service_link/{}/balance_contract'.format(project.id, service.id),
        {'person': ph_person.id, 'tariff': empty_tariff.cc},
        headers={'X-User-Id': '{}'.format(user.uid)},
        status='4*',
    )

    assert res.status_int == 400 and "Balance person type must be 'ur' if it is Russian tax resident or 'yt' otherwise" in res

    # контракт уже существует
    def create_contract(self, user_uid, person, tariff):
        raise CommonBalance.DuplicateError

    with mock.patch.object(Balance2Apikeys, 'create_contract', create_contract):
        res = web_app.post_json(
            '/project/{}/service_link/{}/balance_contract'.format(project.id, service.id),
            {'person': ur_person.id, 'tariff': empty_tariff.cc},
            headers={'X-User-Id': '{}'.format(user.uid)},
            status='4*',
        )

    assert res.status_int == 409


def test_get_service(mongomock, web_app, service_fabric):
    service_fabric()
    service_fabric()

    res = web_app.get('/service')
    assert len(res.json['data']) == 2

    res = web_app.get('/service/1')
    assert len(res.json) == 1 and res.json['data']['id'] == 1


def test_project_service_link_create(mongomock, web_app, simple_link):
    project = simple_link.project
    service = simple_link.service
    user = project.user

    res = web_app.put_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        headers={'X-User-Id': '{}'.format(user.uid)},
        status='4*',
    )

    assert res.status_int == 400 and 'Service is not attachable in ui' in res.json['error']

    service.attachable_in_ui = True
    service.questionnaire_id = '1'
    service.save()

    res = web_app.put_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        headers={'X-User-Id': '{}'.format(user.uid)},
        status='4*',
    )

    assert res.status_int == 400 and 'Service can only be plugged using the questionnaire' in res.json['error']

    service.questionnaire_id = None
    service.save()

    res = web_app.put_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        headers={'X-User-Id': '{}'.format(user.uid)},
    )

    assert res.status_int == 200

    simple_link.delete()
    res = web_app.put_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        headers={'X-User-Id': '{}'.format(user.uid)},
    )

    assert res.status_int == 201


def test_project_service_link_simple_hide(mongomock, web_app, simple_link):
    project = simple_link.project
    service = simple_link.service
    user = project.user

    web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {'hidden': True},
        headers={'X-User-Id': '{}'.format(user.uid)},
    )

    simple_link.reload()
    assert simple_link.hidden
    assert simple_link.config.banned


def test_project_service_link_tariff_hide(mongomock, web_app, link_with_fake_contractless_tariff):
    project = link_with_fake_contractless_tariff.project
    service = link_with_fake_contractless_tariff.service
    user = project.user

    web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {'hidden': True},
        headers={'X-User-Id': '{}'.format(user.uid)},
    )

    link_with_fake_contractless_tariff.reload()
    assert link_with_fake_contractless_tariff.hidden
    # В данном случае связка не блокируется, блокировка произойдет при непосредственном отрыве тарифа
    # Другой тест test_project_service_link_without_tariff_hide проверяет блокировку, если тарифа не было
    assert not link_with_fake_contractless_tariff.config.banned
    assert link_with_fake_contractless_tariff.config.scheduled_tariff is None


def test_project_service_link_without_tariff_hide(mongomock, web_app, link_without_any_tariff):
    project = link_without_any_tariff.project
    service = link_without_any_tariff.service
    user = project.user

    web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {'hidden': True},
        headers={'X-User-Id': '{}'.format(user.uid)},
    )

    link_without_any_tariff.reload()
    assert link_without_any_tariff.hidden
    assert link_without_any_tariff.config.banned
    assert link_without_any_tariff.config.scheduled_tariff is None


def test_project_service_link_contract_hide(mongomock, web_app, link_with_fake_tariff):
    project = link_with_fake_tariff.project
    service = link_with_fake_tariff.service
    user = project.user

    res = web_app.patch_json(
        '/project/{}/service_link/{}'.format(project.id, service.id),
        {'hidden': True},
        headers={'X-User-Id': '{}'.format(user.uid)},
        status='4*',
    )

    assert res.status_int == 400 and 'Tariff can not be shut down' in res.json['error']


def test_project_create(mongomock, web_app, user):
    user.n_project_slots = 0
    user.save()

    res = web_app.post_json(
        '/project',
        {},
        headers={'X-User-Id': '{}'.format(user.uid)},
        status='4*',
    )

    assert res.status_int == 400 and 'Projects number limit has been reached' in res.json['error']

    user.n_project_slots = 1
    user.save()
    res = web_app.post_json(
        '/project',
        {},
        headers={'X-User-Id': '{}'.format(user.uid)},
    )

    assert res.status_int == 302


def test_project_create_with_name(mongomock, web_app, user):
    import random
    project_random_name = str(hash(random.random()))

    res = web_app.post_json(
        '/project',
        {'name': project_random_name},
        headers={'X-User-Id': str(user.uid)},
    )

    assert res.status_int in [302, 303]
    location = res.headers['Location']

    res = web_app.get(
        location,
        headers={'X-User-Id': '{}'.format(user.uid)}
    )

    assert res.json['data']['name'] == project_random_name


def test_project_rename(mongomock, web_app, project):
    import random
    project_random_name = str(hash(random.random()))

    web_app.patch_json(
        '/project/{}'.format(project.id),
        {'name': project_random_name},
        headers={'X-User-Id': str(project.user.uid)},
    )

    project.reload()
    assert project.name == project_random_name


def test_list_keys_with_default_sort_params_where_newest_keys_go_first(
        mongomock, web_app, simple_link, simple_keys_pair):
    project = simple_link.project
    service = simple_link.service
    user = project.user
    key_old, key_new = simple_keys_pair

    res = web_app.get(
        '/project/{}/service_link/{}/key?'.format(project.id, service.id),
        headers={'X-User-Id': '{}'.format(user.uid)},
    )

    assert res.json['sort'] == ['-id']
    assert (res.json['data'][0]['name'], res.json['data'][1]['name']) == (key_new.name, key_old.name)


def test_list_keys_with_custom_sort_params(mongomock, web_app, simple_link, simple_keys_pair):
    project = simple_link.project
    service = simple_link.service
    user = project.user
    key_old, key_new = simple_keys_pair

    res = web_app.get(
        '/project/{}/service_link/{}/key?_sort_params=id'.format(project.id, service.id),
        headers={'X-User-Id': '{}'.format(user.uid)},
    )

    assert res.json['sort'] == ['id']
    assert (res.json['data'][0]['name'], res.json['data'][1]['name']) == (key_old.name, key_new.name)


def test_calculate_next_tariff_more_expensive_with_discount(mongomock, web_app, tariff_to_upgrade_to, link_upgradable_with_discount):
    date_now = datetime(2020, 1, 1, tzinfo=pytz.utc)
    new_tarifficator_state_content(
        link_upgradable_with_discount,
        value='1000000',
        date_now=date_now,
        activated_and_paid_days_ago=180
    )
    project = link_upgradable_with_discount.project
    service = link_upgradable_with_discount.service
    user = project.user

    with mock.patch('billing.apikeys.apikeys.mapper.contractor.datetime', new=mock_datetime(date_now)):
        res = web_app.get(
            '/project/{}/service_link/{}/tariff/calculate_next'.format(project.id, service.id),
            {
                'tariff': tariff_to_upgrade_to.cc
            },
            headers={'X-User-Id': '{}'.format(user.uid)},
        ).json['data']
    date_close = tariff_to_upgrade_to.switch_date(date_now)
    assert res['on_date'] == date_close.isoformat()
    assert res['tariff'] == tariff_to_upgrade_to.cc

    assert Decimal(res['tarifficator_state']['state']['products']['777']['credited_deficit']) == Decimal('1508197')

    date = date_now + timedelta(days=50)
    with mock.patch('billing.apikeys.apikeys.mapper.contractor.datetime', new=mock_datetime(date_now)), \
            mock.patch('billing.apikeys.apikeys.handles_ui.datetime', new=mock_datetime(date_now)):
        res = web_app.get(
            '/project/{}/service_link/{}/tariff/calculate_next'.format(project.id, service.id),
            {
                'on_date': date.isoformat(),
                'tariff': tariff_to_upgrade_to.cc
            },
            headers={'X-User-Id': '{}'.format(user.uid)},
        ).json['data']

    date_close = tariff_to_upgrade_to.switch_date(date)
    assert res['on_date'] == date_close.isoformat()
    assert res['tariff'] == tariff_to_upgrade_to.cc
    assert Decimal(res['tarifficator_state']['state']['products']['777']['credited_deficit']) == Decimal('1644809')


def test_calculate_next_tariff_less_expensive_with_full_price(mongomock, web_app, tariff_to_upgrade_from, link_with_most_expensive_tariff):
    date_now = datetime.now(pytz.utc)
    new_tarifficator_state_content(
        link_with_most_expensive_tariff,
        value='2000000',
        date_now=date_now,
        activated_and_paid_days_ago=180
    )
    project = link_with_most_expensive_tariff.project
    service = link_with_most_expensive_tariff.service
    user = project.user

    res = web_app.get(
        '/project/{}/service_link/{}/tariff/calculate_next'.format(project.id, service.id),
        {
            'tariff': tariff_to_upgrade_from.cc
        },
        headers={'X-User-Id': '{}'.format(user.uid)},
    ).json['data']

    date_close = tariff_to_upgrade_from.switch_date(date_now)
    assert res['on_date'] == date_close.isoformat()
    assert res['tariff'] == tariff_to_upgrade_from.cc
    assert Decimal(res['tarifficator_state']['state']['products']['777']['credited_deficit']) == Decimal('1000000')

    date = date_now + timedelta(days=50)
    res = web_app.get(
        '/project/{}/service_link/{}/tariff/calculate_next'.format(project.id, service.id),
        {
            'on_date': date.isoformat(),
            'tariff': tariff_to_upgrade_from.cc
        },
        headers={'X-User-Id': '{}'.format(user.uid)},
    ).json['data']

    date_close = tariff_to_upgrade_from.switch_date(date)
    assert res['on_date'] == date_close.isoformat()
    assert res['tariff'] == tariff_to_upgrade_from.cc
    assert Decimal(res['tarifficator_state']['state']['products']['777']['credited_deficit']) == Decimal('1000000')


def test_get_change_tariff_issue_without_email(mongomock, web_app, simple_link):
    project = simple_link.project
    service = simple_link.service
    user = project.user
    assert not user.email

    res = web_app.get(
        '/project/{}/service_link/{}/issue'.format(project.id, service.id),
        headers={'X-User-Id': '{}'.format(user.uid)},
        expect_errors=True,
    )

    assert res.status_int == 404
    assert res.json['error'] == "Ticket can't be found: email is not set"


def test_create_change_tariff_issue_without_email(mongomock, web_app, simple_link):
    project = simple_link.project
    service = simple_link.service
    user = project.user
    assert not user.email

    res = web_app.put_json(
        '/project/{}/service_link/{}/issue'.format(project.id, service.id),
        {'tariff': service.cc + '_tariff_cc', 'comment': '123'},
        headers={'X-User-Id': '{}'.format(user.uid)},
        expect_errors=True,
    )

    assert res.status_int == 400
    assert res.json['error'] == "Ticket can't be created: email is not set"


def test_personal_account_activate_promo_code(mongomock, web_app,
                                              link_with_fake_contractless_tariff,
                                              order_for_link_with_empty_contractless_tariff,
                                              empty_contractless_tariff_more_expensive,
                                              order_for_link_with_empty_contractless_tariff_more_expensive):
    project = link_with_fake_contractless_tariff.project
    service = link_with_fake_contractless_tariff.service
    user = project.user

    link_with_fake_contractless_tariff.balance_person_id = "1"
    link_with_fake_contractless_tariff.save()

    requested_amount = None

    def get_personal_account(self, link, tariff):
        pass

    def create_request_deposit_personal_account(
        self, user, order, tariff, qty, promo_code=None, turn_on_rows=None, service_promocode=None
    ):
        nonlocal requested_amount
        requested_amount = qty
        return 'url_to_balance_request'

    def create_request_and_invoice_deposit_personal_account(
        self, user, order, tariff, qty, promo_code=None, turn_on_rows=None, service_promocode=None, operator_uid=None
    ):
        nonlocal requested_amount
        requested_amount = qty
        return 'url_to_balance_invoice'

    with mock.patch.object(Balance2Apikeys, 'get_personal_account', get_personal_account),\
            mock.patch.object(Balance2Apikeys,
                              'create_request_deposit_personal_account',
                              create_request_deposit_personal_account),\
            mock.patch.object(Balance2Apikeys,
                              'create_request_and_invoice_deposit_personal_account',
                              create_request_and_invoice_deposit_personal_account):
        res = web_app.post_json(
            f'/project/{project.id}/service_link/{service.id}/personal_account/promo_code',
            {'promo_code': '1234-5678-1234-5678', 'with_invoice': 'False'},
            headers={'X-User-Id': '{}'.format(user.uid)},
        )

        assert res.status_int == 200
        assert requested_amount == '1000'
        assert res.json == {'data': {'url_to_balance': 'url_to_balance_request'}}

        link_with_fake_contractless_tariff.config.scheduled_tariff = empty_contractless_tariff_more_expensive.cc
        link_with_fake_contractless_tariff.save()

        res = web_app.post_json(
            f'/project/{project.id}/service_link/{service.id}/personal_account/promo_code',
            {'promo_code': '1234-5678-1234-5678', 'with_invoice': 'False'},
            headers={'X-User-Id': '{}'.format(user.uid)},
        )

        assert res.status_int == 200
        assert requested_amount == '2000'
        assert res.json == {'data': {'url_to_balance': 'url_to_balance_request'}}


def test_personal_account_activate_promo_code_with_transition_to_more_expensive_tariff(
    mongomock, web_app, tariff_to_upgrade_to, link_upgradable_with_discount,
    order_for_link_upgradable_with_discount_with_tariff_tier_1,
    order_for_link_upgradable_with_discount_with_tariff_tier_2,
):
    project = link_upgradable_with_discount.project
    service = link_upgradable_with_discount.service
    user = project.user

    link_upgradable_with_discount.balance_person_id = "1"
    link_upgradable_with_discount.config.scheduled_tariff = tariff_to_upgrade_to.cc
    link_upgradable_with_discount.config.scheduled_tariff_date = datetime(2020, 1, 1, 21, tzinfo=pytz.utc)
    link_upgradable_with_discount.save()
    date_now = datetime(2020, 1, 1, tzinfo=pytz.utc)
    new_tarifficator_state_content(
        link_upgradable_with_discount,
        value='1000000',
        date_now=date_now,
        activated_and_paid_days_ago=180
    )
    new_add_next_state_to_tarifficator_state(link_upgradable_with_discount, '1000000', '500000')

    requested_amount = None

    def get_personal_account(self, link, tariff):
        pass

    def create_request_deposit_personal_account(
        self, user, order, tariff, qty, promo_code=None, turn_on_rows=None, service_promocode=None
    ):
        nonlocal requested_amount
        requested_amount = qty
        return 'url_to_balance_request'

    def create_request_and_invoice_deposit_personal_account(
        self, user, order, tariff, qty, promo_code=None, turn_on_rows=None, service_promocode=None, operator_uid=None
    ):
        nonlocal requested_amount
        requested_amount = qty
        return 'url_to_balance_invoice'

    with mock.patch.object(Balance2Apikeys, 'get_personal_account', get_personal_account),\
            mock.patch.object(Balance2Apikeys,
                              'create_request_deposit_personal_account',
                              create_request_deposit_personal_account),\
            mock.patch.object(Balance2Apikeys,
                              'create_request_and_invoice_deposit_personal_account',
                              create_request_and_invoice_deposit_personal_account):
        res = web_app.post_json(
            f'/project/{project.id}/service_link/{service.id}/personal_account/promo_code',
            {'promo_code': '1234-5678-1234-5678', 'with_invoice': 'False'},
            headers={'X-User-Id': '{}'.format(user.uid)},
        )

    assert res.status_int == 200
    assert requested_amount == '1508197'
    assert res.json == {'data': {'url_to_balance': 'url_to_balance_request'}}
