from datetime import date
from unittest.mock import patch, Mock

import pytest
from django.conf import settings

from intranet.femida.src.offers.choices import BONUS_TYPES
from intranet.femida.src.offers.startrek.serializers import (
    JobIssueFieldsExternalSerializer,
    RelocationIssueFieldsSerializer,
    SignupIssueFieldsSerializer,
    WelcomeBonusIssueFieldsSerializer,
)
from intranet.femida.src.offers.tasks import (
    create_bonus_issue_task,
    create_relocation_issue_task,
    create_signup_issue_task,
)
from intranet.femida.src.offers.workflow import OfferWorkflow
from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_fields_existence


@patch('intranet.femida.src.offers.tasks.create_bonus_issue_task.si')
def test_accept_action_bonus_task_call(create_bonus_issue_task_patched):
    """
    Проверяем, что при принятии оффера вызывается таска создания бонус тикета
    """
    offer = f.OfferFactory()
    offer_data = {
        'bank_details': {},
        'full_name': 'Full Name',
        'join_at': date(2020, 1, 1),
        'profile': {},
    }
    wf = OfferWorkflow(offer, user=None)
    wf.perform_action('accept', **offer_data)

    assert create_bonus_issue_task_patched.called


@pytest.mark.parametrize('bonus_type, bonus, bonus2, st_key, called', (
    (BONUS_TYPES.welcome, 100.00, 50.00, 'BONUS-1', True),
    (BONUS_TYPES.welcome, 0.00, 00.00, '', False),
    (BONUS_TYPES.signup, 100.00, 50.00, '', False),
    (None, 100.00, 50.00, '', False),
    (None, 0.00, 0.00, '', False),
))
@patch(
    target='intranet.femida.src.offers.startrek.issues.create_issue',
    return_value=Mock(key='BONUS-1'),
)
def test_accept_action_bonus_ticket_creation(
    create_issue, bonus_type, bonus, bonus2, st_key, called
):
    """
    Проверяем, что таска создает бонус тикет, только если тип бонуса welcome и задан размер бонуса
    """
    offer = f.OfferFactory(
        bonus=bonus,
        bonus_2year=bonus2,
        bonus_type=bonus_type,
    )
    f.OfferProfileFactory(offer=offer)

    create_bonus_issue_task(offer.id)
    offer.refresh_from_db()

    assert create_issue.called is called
    assert offer.startrek_bonus_key == st_key


@pytest.mark.parametrize('data, st_key, called', (
    ({'signup_bonus': 100.00}, 'SIGNUP-1', True),
    ({'bonus': 100.00, 'bonus_type': BONUS_TYPES.signup}, 'SIGNUP-1', True),
    ({'bonus': 100.00, 'bonus_type': BONUS_TYPES.welcome}, '', False),
    ({'signup_bonus': 0.00}, '', False),
    ({'bonus': 0.00, 'bonus_type': BONUS_TYPES.welcome}, '', False),
))
@patch(
    target='intranet.femida.src.offers.startrek.issues.create_issue',
    return_value=Mock(key='SIGNUP-1'),
)
def test_accept_action_signup_ticket_creation(create_issue, data, st_key, called):
    """
    Проверяем, что создается сайнап тикет, если тип бонуса signup и задан размер бонуса.
    Проверяем совместимость со старым сайнапом
    """
    offer = f.OfferFactory(
        **data
    )
    f.OfferProfileFactory(offer=offer)

    create_signup_issue_task(offer.id)
    offer.refresh_from_db()

    assert create_issue.called is called
    assert offer.startrek_signup_key == st_key


@pytest.mark.parametrize('relocation, data, queue', (
    (True, {'signup_bonus': 100.00}, settings.STARTREK_RELOCATION_QUEUE),
    (True, {'bonus': 100.00, 'bonus_type': BONUS_TYPES.signup}, settings.STARTREK_RELOCATION_QUEUE),
    (False, {'signup_bonus': 100.00}, settings.STARTREK_SIGNUP_QUEUE),
    (False, {'bonus': 100.00, 'bonus_type': BONUS_TYPES.signup}, settings.STARTREK_SIGNUP_QUEUE),
))
@patch(
    target='intranet.femida.src.offers.startrek.issues.create_issue',
    return_value=Mock(key='SIGNUP-1'),
)
def test_accept_action_create_signup_with_relocation(create_issue, relocation, data, queue):
    """
    Проверяем, что сайнап тикет не создается, если есть релокация
    """
    offer = f.OfferFactory(
        need_relocation=relocation,
        **data
    )
    f.OfferProfileFactory(offer=offer)

    create_signup_issue_task(offer.id)
    create_relocation_issue_task(offer.id)
    offer.refresh_from_db()

    create_issue.assert_called_once()
    assert create_issue.call_args.kwargs['queue'] == queue


@pytest.mark.parametrize('bonus_type', (
    BONUS_TYPES.welcome,
    BONUS_TYPES.signup,
))
@patch(
    target='intranet.femida.src.offers.tasks.create_signup_issue',
    return_value=Mock(),
)
@patch(
    target='intranet.femida.src.offers.tasks.create_welcome_bonus_issue',
    return_value=Mock(),
)
def test_accept_action_call_signup_or_bonus_task(
    create_welcome_bonus_issue, create_signup_issue, bonus_type
):
    """
    Проверяем, что создается тикет в очереди, соответствующей типу бонуса
    """
    offer = f.OfferFactory(
        bonus=100,
        bonus_2year=100,
        bonus_type=bonus_type,
    )
    f.OfferProfileFactory(offer=offer)

    create_bonus_issue_task(offer.id)
    create_signup_issue_task(offer.id)

    assert create_signup_issue.called is (bonus_type == BONUS_TYPES.signup)
    assert create_welcome_bonus_issue.called is (bonus_type == BONUS_TYPES.welcome)


@pytest.mark.parametrize('bonus_type, create_switch', (
    (BONUS_TYPES.welcome, True),
    (BONUS_TYPES.signup, True),
    (BONUS_TYPES.welcome, False),
    (BONUS_TYPES.signup, False),
))
def test_enable_new_bonus_switch(bonus_type, create_switch):
    """
    Проверяем, свитч для сериализаторов
    """
    # TODO: удалить после релиза FEMIDA-7240
    if create_switch:
        f.create_waffle_switch('enable_new_bonus', True)
    offer = f.OfferFactory(
        bonus=100,
        bonus_2year=100,
        bonus_type=bonus_type,
    )

    assert_fields_existence(
        JobIssueFieldsExternalSerializer(offer).data,
        [
            settings.STARTREK_JOB_WELCOME_GROSS_FIELD,
            settings.STARTREK_JOB_WELCOME_2YEAR_GROSS_FIELD,
            settings.STARTREK_JOB_SIGNUP_GROSS_FIELD,
            settings.STARTREK_JOB_SIGNUP_2YEAR_GROSS_FIELD,
        ],
        create_switch,
    )
    assert_fields_existence(
        SignupIssueFieldsSerializer(offer).data,
        [
            settings.STARTREK_SIGNUP_GROSS_FIELD,
            settings.STARTREK_SIGNUP_2YEAR_GROSS_FIELD,
        ],
        create_switch,
    )
    assert_fields_existence(
        RelocationIssueFieldsSerializer(offer).data,
        [
            settings.STARTREK_RELOCATION_SIGNUP_GROSS_FIELD,
            settings.STARTREK_RELOCATION_SIGNUP_2YEAR_GROSS_FIELD,
        ],
        create_switch,
    )
    assert_fields_existence(
        WelcomeBonusIssueFieldsSerializer(offer).data,
        [
            settings.STARTREK_BONUS_WELCOME_GROSS_FIELD,
            settings.STARTREK_BONUS_WELCOME_2YEAR_GROSS_FIELD,
        ],
        create_switch,
    )
