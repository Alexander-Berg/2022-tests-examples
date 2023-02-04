import pytest

from unittest.mock import patch

from intranet.femida.src.candidates.signals import (
    verification_failed,
    verification_on_check,
    verification_succeeded,
)
from intranet.femida.src.hire_orders.choices import HIRE_ORDER_RESOLUTIONS as RESOLUTIONS
from intranet.femida.src.offers.signals import (
    offer_confirmed,
    offer_accepted,
    offer_newhire_approved,
    offer_newhire_ready,
    offer_closed,
    offer_unapproved,
)
from intranet.femida.src.vacancies.signals import vacancy_approved, vacancy_unapproved

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('signal, attr, action_name, action_params', (
    (vacancy_approved, 'vacancy', 'create_application', {}),
    (offer_confirmed, 'offer', 'create_verification', {}),
    (verification_on_check, 'candidate', 'check_verification', {}),
    (verification_succeeded, 'candidate', 'send_offer', {}),
    (offer_accepted, 'offer', 'accept_offer', {}),
    (offer_newhire_approved, 'offer', 'approve_preprofile', {}),
    (offer_newhire_ready, 'offer', 'finish_preprofile', {}),
    (offer_closed, 'offer', 'close_offer', {}),
    (vacancy_unapproved, 'vacancy', 'cancel', {'resolution': RESOLUTIONS.vacancy_unapproved}),
    (offer_unapproved, 'offer', 'cancel', {'resolution': RESOLUTIONS.offer_unapproved}),
    (verification_failed, 'candidate', 'cancel', {'resolution': RESOLUTIONS.verification_failed}),
))
@patch('intranet.femida.src.hire_orders.signals.perform_hire_order_action_task.delay')
def test_signals(mocked_task, signal, attr, action_name, action_params):
    verification = f.create_actual_verification()
    hire_order = f.HireOrderFactory(
        candidate=verification.candidate,
        application=verification.application,
        vacancy=verification.application.vacancy,
    )
    signal.send(None, **{attr: getattr(hire_order, attr)})
    mocked_task.assert_called_once_with(hire_order.id, action_name, **action_params)
