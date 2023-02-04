import pytest

from unittest.mock import call, patch, Mock

from django.contrib.auth.models import Permission
from django.urls import reverse

from intranet.femida.src.hire_orders.choices import HIRE_ORDER_STATUSES
from intranet.femida.src.hire_orders.models import HireOrder
from intranet.femida.src.offers import choices as offer_choices
from intranet.femida.src.startrek.utils import StatusEnum

from intranet.femida.tests import factories as f
from intranet.femida.tests.mock.offers import FakeNewhireAPI
from intranet.femida.tests.utils import ctx_combine, eager_task


pytestmark = pytest.mark.django_db


def _check_state(hire_order, mocked_action_task, *, status, called_actions):
    hire_order.refresh_from_db()
    assert hire_order.status == status
    calls = [call(hire_order.id, i) for i in called_actions]
    mocked_action_task.assert_has_calls(calls)
    mocked_action_task.reset_mock()


@eager_task('intranet.femida.src.hire_orders.workflow.perform_hire_order_action_task')
def test_hire_order_smoke_success(mocked_action_task, client, hire_order_raw_data):
    """
    Проверяет базовый успешный сценарий автонайма целиком
    """
    f.create_waffle_switch('enable_bp_always_valid')
    f.RawTemplateFactory()
    user = f.create_user()
    user.user_permissions.add(Permission.objects.get(codename='can_use_hire_orders'))
    user.user_permissions.add(Permission.objects.get(codename='can_use_api_for_forms_constructor'))
    client.login(user.username)
    issue = Mock(key='TJOB-1')

    # - Создаётся заказ
    # - Создаётся кандидат
    # - Создаётся вакансия и отправляется на согласование
    url = reverse('private-api:hire_orders:list')
    with patch('intranet.femida.src.vacancies.startrek.issues.create_issue', return_value=issue):
        response = client.post(url, hire_order_raw_data)
    assert response.status_code == 201, response.content
    uuid = response.json()['uuid']
    hire_order = HireOrder.objects.get(uuid=uuid)
    _check_state(
        hire_order, mocked_action_task,
        status=HIRE_ORDER_STATUSES.vacancy_on_approval,
        called_actions=['prepare_candidate', 'create_vacancy'],
    )

    # - Срабатывает триггер в Трекере "вакансия согласована"
    # - Вакансия берётся в работу
    # - Кандидат добавляется на вакансию
    # - Создаётся оффер и отправляется на согласование
    issue.status.key = StatusEnum.in_progress
    issue.bpNumber = 1991
    issue.recruitmentPartner.id = f.create_recruiter().username
    ctx_managers = ctx_combine(
        patch('intranet.femida.src.vacancies.tasks.get_issue', return_value=issue),
        eager_task('intranet.femida.src.api.vacancies.views.vacancy_approve_by_issue_task'),
    )
    with ctx_managers:
        url = reverse('private-api:tracker-vacancy-approve-by-issue')
        response = client.post(url, {'issue_key': issue.key})
    assert response.status_code == 200, response.content
    _check_state(
        hire_order, mocked_action_task,
        status=HIRE_ORDER_STATUSES.offer_on_approval,
        called_actions=['create_application', 'create_offer'],
    )

    # - Срабатывает триггер в Трекере "оффер согласован"
    # - Кандидату отправляется анкета на КИ
    issue.status.key = StatusEnum.resolved
    ctx_managers = ctx_combine(
        patch('intranet.femida.src.offers.tasks.get_issue', return_value=issue),
        eager_task('intranet.femida.src.api.offers.views.offer_confirm_by_issue_task'),
    )
    with ctx_managers:
        url = reverse('private-api:tracker-offer-confirm-by-issue')
        response = client.post(url, {'issue_key': issue.key})
    assert response.status_code == 200, response.content
    _check_state(
        hire_order, mocked_action_task,
        status=HIRE_ORDER_STATUSES.verification_sent,
        called_actions=['create_verification'],
    )

    # - Кандидат заполняет анкету на КИ
    # - Начинается проверка на КИ
    verification = hire_order.candidate.verifications.alive().first()
    url = reverse('private-api:forms-verification')
    data = {'params': {'uuid': verification.uuid.hex}}
    response = client.post(url, data)
    assert response.status_code == 200, response.content
    _check_state(
        hire_order, mocked_action_task,
        status=HIRE_ORDER_STATUSES.verification_on_check,
        called_actions=['check_verification'],
    )

    # - От вендора поступает сигнал об успешной проверке
    # - Кандидату отправляется оффер
    url = reverse('private-api:forms-verification-success')
    data['params']['result'] = 'Нормальный чувак'
    response = client.post(url, data)
    assert response.status_code == 200, response.content
    _check_state(
        hire_order, mocked_action_task,
        status=HIRE_ORDER_STATUSES.offer_sent,
        called_actions=['send_offer'],
    )

    # - Кандидат принимает оффер
    url = reverse('external-api:offers-accept', kwargs={'uid': hire_order.offer.link.uid.hex})
    data = {
        'last_name': 'Last',
        'first_name': 'First',
        'last_name_en': 'Last',
        'first_name_en': 'First',
        'gender': offer_choices.GENDER.M,
        'birthday': '01.01.1990',
        'citizenship': offer_choices.CITIZENSHIP.RU,
        'employment_book': offer_choices.EMPLOYMENT_BOOK_OPTIONS.absent,
        'residence_address': 'somewhere',
        'phone': '+77773334455',
        'os': offer_choices.OPERATING_SYSTEMS.mac,
        'home_email': 'email@example.com',
        'photo': [f.OfferAttachmentFactory.create(offer=hire_order.offer).attachment_id],
        'username': 'username',
        'join_at': '2020-10-10',
        'is_agree': True,
        'nda_accepted': True,
    }
    with patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI):
        response = client.post(url, data)
    assert response.status_code == 200, response.content
    _check_state(
        hire_order, mocked_action_task,
        status=HIRE_ORDER_STATUSES.offer_accepted,
        called_actions=['accept_offer'],
    )
