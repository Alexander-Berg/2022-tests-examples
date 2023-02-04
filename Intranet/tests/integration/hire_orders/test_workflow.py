import pytest

from decimal import Decimal
from unittest.mock import patch, call, Mock, ANY

from constance.test import override_config
from django.conf import settings

from intranet.femida.src.candidates.choices import (
    CANDIDATE_STATUSES,
    CONSIDERATION_STATUSES,
    CONTACT_TYPES,
    VERIFICATION_STATUSES,
    VERIFICATION_RESOLUTIONS,
)
from intranet.femida.src.celery_app import NoRetry
from intranet.femida.src.candidates.deduplication import MAYBE_DUPLICATE
from intranet.femida.src.communications.choices import MESSAGE_TYPES
from intranet.femida.src.core.workflow import ActionProhibitedError
from intranet.femida.src.hire_orders.choices import (
    HIRE_ORDER_STATUSES as STATUSES,
    HIRE_ORDER_ACTIVE_STATUSES as ACTIVE_STATUSES,
    HIRE_ORDER_RESOLUTIONS as RESOLUTIONS,
)
from intranet.femida.src.hire_orders.tasks import perform_hire_order_action_task
from intranet.femida.src.hire_orders.workflow import HireOrderWorkflow
from intranet.femida.src.interviews.choices import APPLICATION_STATUSES
from intranet.femida.src.offers.choices import OFFER_STATUSES, PROBATION_PERIOD_TYPE_TO_UNITS
from intranet.femida.src.startrek.utils import StatusEnum
from intranet.femida.src.utils.datetime import shifted_now
from intranet.femida.src.vacancies.choices import VACANCY_TYPES, VACANCY_STATUSES

from intranet.femida.tests import factories as f
from intranet.femida.tests.mock.offers import FakeNewhireAPI
from intranet.femida.tests.utils import Contains, assert_not_raises


pytestmark = pytest.mark.django_db


@pytest.fixture(params=(True, False))
def maybe_duplicate_on_demand(request, maybe_duplicate):
    if request.param:
        return maybe_duplicate
    maybe_duplicate.contacts.all().delete()
    maybe_duplicate.delete()
    return None


@pytest.fixture(params=(
    'has_hire_orders',
    'has_applications',
    'has_verifications',
    'has_offers',
    'is_employee',
    'has_some_problems',
))
def maybe_duplicate_error(request, maybe_duplicate):
    errors = {
        'has_hire_orders': '- Кандидат уже проходит через процесс автонайма',
        'has_applications': '- Кандидат претендует на другие вакансии',
        'has_verifications': '- Кандидат не прошёл проверку на конфликт интересов',
        'has_offers': '- У кандидата есть активный оффер',
        'is_employee': '- Кандидат является действующим сотрудником',
        'has_login': '- Есть похожий кандидат с логином',
        'has_some_problems': (
            '- У кандидата есть активный оффер\n'
            '- Кандидат является действующим сотрудником'
        ),
    }
    if request.param in ('is_employee', 'has_login', 'has_some_problems'):
        maybe_duplicate.login = 'maybe-dup'
        maybe_duplicate.save()

    if request.param == 'has_hire_orders':
        f.create_hire_order_for_status(STATUSES.vacancy_on_approval, candidate=maybe_duplicate)
    if request.param == 'has_applications':
        f.ApplicationFactory(candidate=maybe_duplicate)
    if request.param == 'has_verifications':
        f.create_actual_verification(
            candidate=maybe_duplicate,
            resolution=VERIFICATION_RESOLUTIONS.nohire,
            application__status=APPLICATION_STATUSES.closed,
        )
    if request.param in ('has_offers', 'has_some_problems'):
        f.OfferFactory(candidate=maybe_duplicate, status=OFFER_STATUSES.on_approval)
    if request.param in ('is_employee', 'has_some_problems'):
        f.create_user(
            username=maybe_duplicate.login,
            is_dismissed=False,
            department__id=settings.OUTSTAFF_DEPARTMENT_ID,
        )
    return maybe_duplicate, errors.get(request.param)


@patch('intranet.femida.src.hire_orders.workflow.perform_hire_order_action_task.delay')
@patch('intranet.femida.src.hire_orders.workflow.PrepareCandidateAction._validate_candidate')
def test_prepare_candidate_created(mocked_validate, mocked_task, hire_order_raw_data,
                                   simple_hire_order, maybe_duplicate_on_demand):
    f.create_waffle_switch('enable_candidate_main_recruiter')
    candidate_data = hire_order_raw_data['candidate']
    perform_hire_order_action_task(simple_hire_order.id, 'prepare_candidate')
    simple_hire_order.refresh_from_db()

    decision = MAYBE_DUPLICATE if maybe_duplicate_on_demand else None
    mocked_validate.assert_called_once_with(maybe_duplicate_on_demand, decision)
    mocked_task.assert_called_once_with(simple_hire_order.id, 'create_vacancy')
    assert simple_hire_order.status == STATUSES.candidate_prepared
    assert simple_hire_order.resolution == ''

    candidate = simple_hire_order.candidate
    assert candidate is not None
    assert candidate != maybe_duplicate_on_demand
    assert candidate.first_name == candidate_data['first_name']
    assert candidate.last_name == candidate_data['last_name']
    assert candidate.source == candidate_data['source']
    assert candidate.status == CANDIDATE_STATUSES.in_progress
    assert list(candidate.responsibles.all()) == [simple_hire_order.recruiter]
    assert candidate.main_recruiter == simple_hire_order.recruiter
    assert set(candidate.contacts.values_list('account_id', flat=True)) == {candidate_data['email']}

    considerations = list(candidate.considerations.all())
    assert len(considerations) == 1
    consideration = considerations[0]
    assert consideration.state == CONSIDERATION_STATUSES.in_progress
    assert consideration.created_by == simple_hire_order.recruiter


@patch('intranet.femida.src.hire_orders.workflow.perform_hire_order_action_task.delay')
@pytest.mark.parametrize('cand_status, cons_status, is_new_cons', (
    (CANDIDATE_STATUSES.in_progress, CONSIDERATION_STATUSES.in_progress, False),
    (CANDIDATE_STATUSES.closed, CONSIDERATION_STATUSES.archived, True),
))
def test_prepare_candidate_found(mocked_task, hire_order_raw_data, simple_hire_order,
                                 definitely_duplicate, cand_status, cons_status, is_new_cons):
    candidate_data = hire_order_raw_data['candidate']
    definitely_duplicate.status = cand_status
    definitely_duplicate.source = 'source_that_must_stay'
    definitely_duplicate.save()
    existing_consideration = f.ConsiderationFactory(
        candidate=definitely_duplicate,
        state=cons_status,
    )
    old_main_email = f.CandidateContactFactory(
        candidate=definitely_duplicate,
        type=CONTACT_TYPES.email,
        is_main=True,
        account_id='old-main@email.com',
        normalized_account_id='old-main@email.com',
    )

    perform_hire_order_action_task(simple_hire_order.id, 'prepare_candidate')
    simple_hire_order.refresh_from_db()

    mocked_task.assert_called_once_with(simple_hire_order.id, 'create_vacancy')
    assert simple_hire_order.status == STATUSES.candidate_prepared
    assert simple_hire_order.resolution == ''

    candidate = simple_hire_order.candidate
    assert candidate == definitely_duplicate
    assert candidate.status == CANDIDATE_STATUSES.in_progress
    assert simple_hire_order.recruiter in set(candidate.responsibles.all())
    new_source = candidate_data['source'] if is_new_cons else 'source_that_must_stay'
    assert candidate.source == new_source

    considerations = list(candidate.considerations.filter(state=CONSIDERATION_STATUSES.in_progress))
    assert len(considerations) == 1
    consideration = considerations[0]
    assert consideration.state == CONSIDERATION_STATUSES.in_progress
    assert (consideration.created_by == simple_hire_order.recruiter) is is_new_cons
    assert (consideration != existing_consideration) is is_new_cons

    old_main_email.refresh_from_db()
    new_main_email = candidate.contacts.exclude(id=old_main_email.id).get()
    assert not old_main_email.is_main
    assert new_main_email.is_main
    assert new_main_email.account_id == hire_order_raw_data['candidate']['email']


@patch('intranet.femida.src.hire_orders.workflow.perform_hire_order_action_task.delay')
def test_prepare_candidate_failed(mocked_task, simple_hire_order, maybe_duplicate_error):
    maybe_duplicate, error = maybe_duplicate_error
    perform_hire_order_action_task(simple_hire_order.id, 'prepare_candidate')
    simple_hire_order.refresh_from_db()

    assert not mocked_task.called
    assert simple_hire_order.status == STATUSES.closed
    assert simple_hire_order.resolution == RESOLUTIONS.invalid_candidate
    assert simple_hire_order.resolution_description == error
    assert simple_hire_order.candidate == maybe_duplicate


@patch(
    target='intranet.femida.src.vacancies.startrek.issues.create_issue',
    return_value=Mock(key='TJOB-222'),
)
def test_create_vacancy(mocked_create_issue, simple_hire_order):
    currency = f.currency('USD')
    office = f.OfficeFactory()
    vacancy_data = simple_hire_order.raw_data['vacancy']
    offer_data = simple_hire_order.raw_data['offer']
    offer_data['payment_currency'] = 'USD'
    offer_data['office'] = office.id
    simple_hire_order.status = STATUSES.candidate_prepared
    simple_hire_order.save()

    perform_hire_order_action_task(simple_hire_order.id, 'create_vacancy')
    simple_hire_order.refresh_from_db()

    assert simple_hire_order.status == STATUSES.vacancy_on_approval
    vacancy = simple_hire_order.vacancy
    assert vacancy is not None
    assert vacancy.type == VACANCY_TYPES.autohire
    assert vacancy.recruiters == [simple_hire_order.recruiter]
    assert vacancy.startrek_key == 'TJOB-222'
    assert vacancy.name == vacancy_data['name']
    assert vacancy.department.url == vacancy_data['department']
    assert vacancy.hiring_manager.username == vacancy_data['hiring_manager']
    assert vacancy.profession_id == vacancy_data['profession']
    assert list(vacancy.abc_services.values_list('slug', flat=True)) == vacancy_data['abc_services']
    assert list(vacancy.cities.all()) == [office.city]
    assert vacancy.pro_level_min == 3
    assert vacancy.pro_level_max == 3

    issue_fields = vacancy.issue_data['fields']
    assert issue_fields['maxSalary'] == '{} {}'.format(offer_data['salary'], currency)
    assert issue_fields['salarySystem'] == 'Фиксированная'
    assert issue_fields['vacancyType'] == 'масснайм'

    mocked_create_issue.assert_called_once_with(
        queue='TJOB',
        summary=ANY,
        description=ANY,
        author=simple_hire_order.recruiter.username,
        access=ANY,
        **issue_fields,
    )
    call_kwargs = mocked_create_issue.call_args[1]
    assert 'comment' not in call_kwargs, call_kwargs


@patch('intranet.femida.src.hire_orders.workflow.perform_hire_order_action_task.delay')
def test_create_application(mocked_task):
    candidate = f.CandidateFactory()
    consideration = f.ConsiderationFactory(candidate=candidate)
    vacancy = f.VacancyFactory()
    city = f.VacancyCityFactory(vacancy=vacancy).city
    hire_order = f.HireOrderFactory(
        status=STATUSES.vacancy_on_approval,
        candidate=candidate,
        vacancy=vacancy,
        application=None,
        offer=None,
        raw_data={'application_message': 'message #1'},
    )
    perform_hire_order_action_task(hire_order.id, 'create_application')
    hire_order.refresh_from_db()

    mocked_task.assert_called_once_with(hire_order.id, 'create_offer')
    assert hire_order.status == STATUSES.vacancy_prepared
    application = hire_order.application
    assert application.status == APPLICATION_STATUSES.in_progress
    assert application.candidate_id == hire_order.candidate_id
    assert application.vacancy_id == hire_order.vacancy_id
    assert application.consideration_id == consideration.id
    assert list(hire_order.candidate.target_cities.all()) == [city]
    candidate_profession_ids = list(
        hire_order.candidate.candidate_professions
        .values_list('profession_id', flat=True)
    )
    assert candidate_profession_ids == [vacancy.profession_id]

    messages = application.messages.all()
    assert len(messages) == 1
    assert messages[0].type == MESSAGE_TYPES.internal
    assert messages[0].author == hire_order.recruiter
    assert messages[0].text == 'message #1'


def test_create_offer(simple_hire_order):
    offer_data = simple_hire_order.raw_data['offer']
    vacancy = f.create_vacancy(
        type=VACANCY_TYPES.autohire,
        status=VACANCY_STATUSES.in_progress,
    )
    vacancy.add_recruiter(simple_hire_order.recruiter)
    simple_hire_order.application = f.create_application(
        vacancy=vacancy,
        status=APPLICATION_STATUSES.in_progress
    )
    simple_hire_order.vacancy = vacancy
    simple_hire_order.status = STATUSES.vacancy_prepared
    simple_hire_order.save()

    perform_hire_order_action_task(simple_hire_order.id, 'create_offer')
    simple_hire_order.refresh_from_db()

    assert simple_hire_order.status == STATUSES.offer_on_approval
    offer = simple_hire_order.offer
    assert offer is not None

    for field_name, value in offer_data.items():
        if field_name in {'office', 'position', 'org'}:
            assert getattr(offer, field_name).id == value
        elif field_name not in {'salary', 'payment_currency', 'join_at', 'probation_period_type'}:
            assert getattr(offer, field_name) == value

    assert offer.salary == Decimal(offer_data['salary'])
    assert offer.payment_currency.code == offer_data['payment_currency']
    assert str(offer.join_at) == offer_data['join_at']
    assert offer.probation_period, offer.probation_period_unit == (
        PROBATION_PERIOD_TYPE_TO_UNITS[offer_data['probation_period_type']]
    )
    assert offer.department == vacancy.department
    assert offer.staff_position_name == offer.position.name_ru
    assert offer.is_confirmed_by_boss == offer_data['is_confirmed_by_boss']
    assert offer.is_internal_phone_needed == offer_data['is_internal_phone_needed']
    assert offer.is_sip_redirect_needed == offer_data['is_sip_redirect_needed']


@override_config(SKIP_VERIFICATION_DEPARTMENT_IDS='100500')
@patch('intranet.femida.src.candidates.workflow.send_verification_form_to_candidate')
@patch('intranet.femida.src.hire_orders.workflow.perform_hire_order_action_task.delay')
@pytest.mark.parametrize('given_sender, expected_sender', (
    ('', 'best-recruiter@yandex-team.ru'),
    ('me@yandex-team.ru', 'me@yandex-team.ru'),
))
@pytest.mark.parametrize('force_verification_sending, department_id, is_send_called', (
    (True, 100500, True),
    (True, 100501, True),
    (False, 100501, True),
    (False, 100500, False),
))
def test_create_verification(mocked_task, mocked_send, force_verification_sending, department_id,
                             is_send_called, given_sender, expected_sender):
    application = f.create_application(status=APPLICATION_STATUSES.in_progress)
    hire_order = f.HireOrderFactory(
        status=STATUSES.offer_on_approval,
        candidate=application.candidate,
        vacancy=application.vacancy,
        application=application,
        offer__department__id=department_id,
        recruiter=f.create_recruiter(username='best-recruiter'),
        raw_data={
            'mail': {'sender': given_sender},
            'force_verification_sending': force_verification_sending,
        },
    )
    email = f.CandidateContactFactory(
        candidate=hire_order.candidate,
        type=CONTACT_TYPES.email,
        account_id='newbie@ya.ru',
    )

    perform_hire_order_action_task(hire_order.id, 'create_verification')
    hire_order.refresh_from_db()

    if department_id == 100500:
        mocked_task.assert_called_once_with(hire_order.id, 'send_offer')
    else:
        assert not mocked_task.called

    if is_send_called:
        assert hire_order.status == STATUSES.verification_sent

        verifications = list(hire_order.candidate.verifications.all())
        assert len(verifications) == 1
        verification = verifications[0]

        assert verification.status == VERIFICATION_STATUSES.new
        assert verification.candidate_id == hire_order.candidate_id
        assert verification.application_id == hire_order.application_id
        assert verification.created_by_id == hire_order.recruiter_id

        mocked_send.assert_called_once_with(
            verification_id=verification.id,
            subject='От компании Яндекс',
            text=Contains(verification.link),
            receiver=email.account_id,
            sender=expected_sender,
        )
    else:
        assert not mocked_send.called


@override_config(SKIP_VERIFICATION_DEPARTMENT_IDS='100500')
@patch('intranet.femida.src.hire_orders.workflow.perform_hire_order_action_task.delay')
@pytest.mark.parametrize('verification_status, verification_resolution, next_action_name', (
    (VERIFICATION_STATUSES.new, '', None),
    (VERIFICATION_STATUSES.on_check, '', 'check_verification'),
    (VERIFICATION_STATUSES.on_ess_check, '', 'check_verification'),
    (VERIFICATION_STATUSES.closed, VERIFICATION_RESOLUTIONS.hire, 'send_offer'),
))
@pytest.mark.parametrize('force_verification_sending, department_id, is_next_action_called', (
    (True, 100500, True),
    (True, 100501, True),
    (False, 100501, True),
    (False, 100500, False),
))
def test_create_verification_already_exists(mocked_task, force_verification_sending, department_id,
                                            is_next_action_called, verification_status,
                                            verification_resolution, next_action_name):
    verification = f.VerificationFactory(
        status=verification_status,
        resolution=verification_resolution,
        expiration_date=shifted_now(months=3),
    )
    hire_order = f.create_hire_order_for_status(
        status=STATUSES.offer_on_approval,
        candidate=verification.candidate,
        application=verification.application,
        offer__department__id=department_id,
        raw_data={
            'force_verification_sending': force_verification_sending,
        },
    )

    perform_hire_order_action_task(hire_order.id, 'create_verification')
    hire_order.refresh_from_db()

    if is_next_action_called:
        assert hire_order.status == STATUSES.verification_sent
    calls = []
    if department_id == 100500:
        calls.append(call(hire_order.id, 'send_offer'))
    if is_next_action_called and next_action_name is not None:
        calls.append(call(hire_order.id, next_action_name))
    if calls:
        mocked_task.assert_has_calls(calls)
    else:
        assert not mocked_task.called


@patch('intranet.femida.src.hire_orders.workflow.perform_hire_order_action_task.delay')
@pytest.mark.parametrize('verification_resolution', (
    VERIFICATION_RESOLUTIONS.nohire,
    VERIFICATION_RESOLUTIONS.blacklist,
))
def test_create_verification_nohire_exists(mocked_task, verification_resolution):
    verification = f.create_actual_verification(resolution=verification_resolution)
    hire_order = f.create_hire_order_for_status(
        status=STATUSES.offer_on_approval,
        candidate=verification.candidate,
        application=verification.application,
    )

    with pytest.raises(NoRetry, match=r'^verification_resolution_negative$'):
        perform_hire_order_action_task(hire_order.id, 'create_verification')

    hire_order.refresh_from_db()
    assert not mocked_task.called
    assert hire_order.status == STATUSES.offer_on_approval


@override_config(SKIP_VERIFICATION_DEPARTMENT_IDS='100500')
@patch('intranet.femida.src.hire_orders.workflow.perform_hire_order_action_task.delay')
def test_create_verification_not_required(mocked_task):
    hire_order = f.create_hire_order_for_status(
        status=STATUSES.offer_on_approval,
        offer__department__id=100500,
    )
    perform_hire_order_action_task(hire_order.id, 'create_verification')

    mocked_task.assert_called_once_with(hire_order.id, 'send_offer')
    hire_order.refresh_from_db()
    assert hire_order.status == STATUSES.offer_on_approval


@patch('intranet.femida.src.offers.workflow.send_offer_to_candidate')
@pytest.mark.parametrize('test_case', (
    {
        'sender': 'best-recruiter@yandex-team.ru',
        'receiver': 'newbie@ya.ru',
        'message_sample': 'Ниже Вы можете найти предложение о работе в Яндекс',
        'is_mail_sent': True,
    },
    {
        'mail': {
            'sender': 'just-bot@yandex-team.ru',
            'offer_message': 'Эксклюзивное предложение о работе',
        },
        'sender': 'just-bot@yandex-team.ru',
        'receiver': 'newbie@ya.ru',
        'message_sample': 'Эксклюзивное предложение о работе',
        'is_mail_sent': True,
    },
    {
        'mail': {'offer_receiver': 'box@yandex-team.ru'},
        'sender': 'best-recruiter@yandex-team.ru',
        'receiver': 'box@yandex-team.ru',
        'message_sample': 'Ниже Вы можете найти предложение о работе в Яндекс',
        'is_mail_sent': True,
    },
    {
        'mail': {'offer_receiver': ''},
        'is_mail_sent': False,
    },
    {
        'mail': {'offer_receiver': None},
        'is_mail_sent': False,
    },
))
def test_send_offer(mocked_send, test_case):
    offer = f.create_offer(
        status=OFFER_STATUSES.on_approval,
        salary=90000,
        application=f.ApplicationFactory(
            vacancy__type=VACANCY_TYPES.autohire,
            vacancy__budget_position_id=15000,
        ),
    )
    f.OfferLinkFactory.create(offer=offer)
    f.create_actual_verification(
        candidate=offer.candidate,
        application=offer.application,
    )
    raw_data = {}
    mail_data = test_case.get('mail', {})
    if mail_data:
        raw_data['mail'] = mail_data
    hire_order = f.HireOrderFactory(
        status=STATUSES.verification_on_check,
        candidate=offer.candidate,
        vacancy=offer.vacancy,
        application=offer.application,
        offer=offer,
        recruiter=f.create_recruiter(username='best-recruiter'),
        raw_data=raw_data,
    )
    hire_order.vacancy.add_recruiter(hire_order.recruiter)
    f.CandidateContactFactory(
        candidate=hire_order.candidate,
        type=CONTACT_TYPES.email,
        account_id='newbie@ya.ru',
    )

    perform_hire_order_action_task(hire_order.id, 'send_offer')
    hire_order.refresh_from_db()

    assert hire_order.status == STATUSES.offer_sent
    assert hire_order.offer.status == OFFER_STATUSES.sent
    if test_case['is_mail_sent']:
        mocked_send.assert_called_once_with(
            offer_id=hire_order.offer_id,
            subject='Яндекс. Предложение о работе',
            message=Contains(test_case['message_sample']),
            offer_text=Contains('ООО Яндекс (далее – Компания) предлагает Вам должность'),
            receiver=test_case['receiver'],
            sender=test_case['sender'],
            attachments=[],
            bcc=[hire_order.offer.boss],
        )
    else:
        assert not mocked_send.called


@pytest.mark.parametrize('action_name, current_status, next_status, next_resolution', (
    ('check_verification', STATUSES.verification_sent, STATUSES.verification_on_check, ''),
    ('accept_offer', STATUSES.offer_sent, STATUSES.offer_accepted, ''),
    ('approve_preprofile', STATUSES.offer_accepted, STATUSES.preprofile_approved, ''),
    ('finish_preprofile', STATUSES.preprofile_approved, STATUSES.preprofile_ready, ''),
    ('close_offer', STATUSES.preprofile_ready, STATUSES.closed, RESOLUTIONS.hired),
))
def test_change_status(simple_hire_order, action_name, current_status,
                       next_status, next_resolution):
    simple_hire_order.status = current_status
    simple_hire_order.offer = f.OfferFactory()
    simple_hire_order.save()
    perform_hire_order_action_task(simple_hire_order.id, action_name)
    simple_hire_order.refresh_from_db()
    assert simple_hire_order.status == next_status
    assert simple_hire_order.resolution == next_resolution


@pytest.mark.parametrize('is_action_in_history, exception', (
    (True, None),
    (False, ActionProhibitedError),
))
def test_skip_action(is_action_in_history, exception):
    status = STATUSES.offer_accepted
    hire_order = f.HireOrderFactory(status=status)
    if is_action_in_history:
        f.HireOrderHistoryFactory(hire_order=hire_order, status=STATUSES.offer_sent)
    ctx = pytest.raises(exception) if exception else assert_not_raises()
    with ctx:
        perform_hire_order_action_task(hire_order.id, 'send_offer')
    if not exception:
        hire_order.refresh_from_db()
        assert hire_order.status == status


@patch(
    target='intranet.femida.src.offers.workflow.OfferAction.startrek_job_status',
    new=StatusEnum.in_progress,
)
@patch('intranet.femida.src.offers.workflow.OfferAction.startrek_hr_status', StatusEnum.in_progress)
@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('hire_order_status', ACTIVE_STATUSES._db_values)
def test_cancel(hire_order_status):
    hire_order = f.create_hire_order_for_status(status=hire_order_status)

    wf = HireOrderWorkflow(instance=hire_order, user=None)
    wf.perform_action('cancel', resolution=RESOLUTIONS.incorrect)
    hire_order.refresh_from_db()

    assert hire_order.status == STATUSES.closed
    if hire_order.candidate:
        assert hire_order.candidate.status == CANDIDATE_STATUSES.closed
    if hire_order.application:
        assert hire_order.application.status == APPLICATION_STATUSES.closed
    if hire_order.vacancy:
        assert hire_order.vacancy.status == VACANCY_STATUSES.closed
    if hire_order.offer:
        assert hire_order.offer.status == OFFER_STATUSES.deleted
    # TODO: добавить больше проверок на результат
