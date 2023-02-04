from datetime import date
from decimal import Decimal

import pytest

from unittest.mock import patch, Mock, PropertyMock
from waffle.testutils import override_switch

from constance.test import override_config
from django.conf import settings
from django.core.files.base import ContentFile
from django.test import override_settings
from django.urls.base import reverse

from intranet.femida.src.candidates.choices import (
    VERIFICATION_RESOLUTIONS,
    VERIFICATION_STATUSES,
    CONTACT_TYPES,
)
from intranet.femida.src.core.models import LanguageTag
from intranet.femida.src.core.switches import TemporarySwitch
from intranet.femida.src.hire_orders.choices import HIRE_ORDER_STATUSES
from intranet.femida.src.offers import choices
from intranet.femida.src.offers.startrek.choices import REJECTION_REASONS
from intranet.femida.src.startrek.utils import StatusEnum
from intranet.femida.src.staff.choices import GEOGRAPHY_KINDS
from intranet.femida.src.utils.datetime import shifted_now
from intranet.femida.src.vacancies.choices import VACANCY_TYPES, VACANCY_ROLES

from intranet.femida.tests import factories as f
from intranet.femida.tests.mock.offers import FakeNewhireAPI
from intranet.femida.tests.utils import eager_task


pytestmark = pytest.mark.django_db


class FakeLoginValidator:

    def __init__(self, *args, **kwargs):
        return

    def validate(self, *args, **kwargs):
        return True


def _get_dep_id_by_emp_type(employee_type):
    return (
        settings.EXTERNAL_DEPARTMENT_ID
        if employee_type in choices.EXTERNAL_EMPLOYEE_TYPES
        else settings.YANDEX_DEPARTMENT_ID
    )


def test_offer_list(su_client):
    f.create_offer()
    url = reverse('api:offers:list')
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('employee_type', (
    choices.EMPLOYEE_TYPES.new,
    choices.EMPLOYEE_TYPES.rotation,
))
def test_offer_detail(su_client, employee_type):
    offer = f.create_offer(employee_type=employee_type)
    url = reverse('api:offers:detail', kwargs={'pk': offer.id})
    response = su_client.get(url)
    assert response.status_code == 200


# FIXME: при включении вернуть фикстуру module_db
# @pytest.fixture(scope='module')
@pytest.fixture
def offer_base_data():
    return {
        'full_name': 'First Last',
        'position': f.PositionFactory.create().id,
        'staff_position_name': 'position',
        'join_at': '2018-01-01',
        'probation_period_type': choices.PROBATION_PERIOD_TYPES.six_months,
        'form_type': choices.FORM_TYPES.russian,
        'employee_type': choices.EMPLOYEE_TYPES.new,
    }


# FIXME: при включении вернуть фикстуру module_db
# @pytest.fixture(scope='module')
@pytest.fixture
def offer_draft_update_data(offer_base_data):
    data = offer_base_data.copy()
    department = f.DepartmentFactory()
    f.create_department_chief(department)
    geography = f.GeographyFactory(
        url=settings.DEFAULT_VACANCY_GEOGRAPHY,
        ancestors=[0],
        kind=GEOGRAPHY_KINDS.rus,
    )
    data.update({
        'org': f.OrganizationFactory.create().id,
        'department': department.id,
        'grade': 25,
        'payment_type': choices.PAYMENT_TYPES.monthly,
        'payment_currency': f.currency().id,
        'salary': 100000,
        'employment_type': choices.EMPLOYMENT_TYPES.full,
        'work_place': choices.WORK_PLACES.office,
        'office': f.OfficeFactory.create().id,
        'contract_type': choices.CONTRACT_TYPES.fixed_term,
        'contract_term': 1000,
        'vmi': True,
        'profession': f.ProfessionFactory.create().id,
        'geography': geography.id,
    })
    return data


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('employee_type', (
    choices.EMPLOYEE_TYPES.new,
    choices.EMPLOYEE_TYPES.former,
    choices.EMPLOYEE_TYPES.current,
    choices.EMPLOYEE_TYPES.rotation,
    choices.EMPLOYEE_TYPES.intern,
))
@override_settings(YANDEX_DEPARTMENT_ID=100500, EXTERNAL_DEPARTMENT_ID=500100)
def test_draft_offer_update(su_client, offer_draft_update_data, employee_type):
    f.create_waffle_switch('enable_offer_profession_in_main_form')
    username = None if employee_type == choices.EMPLOYEE_TYPES.new else 'too-long-username'
    if username:
        f.create_user(
            username=username,
            is_dismissed=employee_type == choices.EMPLOYEE_TYPES.former,
            department__id=_get_dep_id_by_emp_type(employee_type),
        )

    offer = f.create_offer()
    url = reverse('api:offers:detail', kwargs={'pk': offer.id})
    data = offer_draft_update_data.copy()
    data.update({
        'employee_type': employee_type,
        'username': username,
    })
    response = su_client.put(url, data)
    assert response.status_code == 200, response.content

    offer.refresh_from_db()
    assert offer.probation_period == (6 if offer.is_external else 0)
    assert offer.probation_period_unit == choices.PROBATION_PERIOD_UNITS.month


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('employee_type', (
    choices.EMPLOYEE_TYPES.new,
    choices.EMPLOYEE_TYPES.former,
    choices.EMPLOYEE_TYPES.current,
    choices.EMPLOYEE_TYPES.rotation,
    choices.EMPLOYEE_TYPES.intern,
))
@pytest.mark.parametrize('geography_kind', (
    GEOGRAPHY_KINDS.rus,
    GEOGRAPHY_KINDS.international,
))
@override_settings(YANDEX_DEPARTMENT_ID=100500, EXTERNAL_DEPARTMENT_ID=500100)
def test_draft_offer_update_custom_geography(
    su_client,
    offer_draft_update_data,
    employee_type,
    geography_kind,
):
    f.create_waffle_switch('enable_offer_profession_in_main_form')
    custom_geography = f.GeographyFactory(
        is_deleted=False,
        ancestors=[0],
        oebs_code='a code',
        kind=geography_kind,
    )
    username = None if employee_type == choices.EMPLOYEE_TYPES.new else 'too-long-username'
    if username:
        f.create_user(
            username=username,
            is_dismissed=employee_type == choices.EMPLOYEE_TYPES.former,
            department__id=_get_dep_id_by_emp_type(employee_type),
        )

    offer = f.create_offer()
    offer.vacancy.geography_international = geography_kind == GEOGRAPHY_KINDS.international
    offer.vacancy.save()
    url = reverse('api:offers:detail', kwargs={'pk': offer.id})
    data = offer_draft_update_data.copy()
    data.update({
        'employee_type': employee_type,
        'username': username,
        'geography': custom_geography.id,
    })
    response = su_client.put(url, data)
    assert response.status_code == 200

    offer.refresh_from_db()
    assert offer.probation_period == (6 if offer.is_external else 0)
    assert offer.probation_period_unit == choices.PROBATION_PERIOD_UNITS.month
    assert offer.geography == custom_geography


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
def test_draft_offer_update_hourly_rate(su_client, offer_draft_update_data):
    # TODO: удалить после релиза FEMIDA-5447
    f.create_waffle_switch('enable_hourly_rate', True)
    offer = f.create_offer(payment_type=choices.PAYMENT_TYPES.hourly)
    url = reverse('api:offers:detail', kwargs={'pk': offer.id})
    data = offer_draft_update_data.copy()
    data.update({
        'payment_type': choices.PAYMENT_TYPES.monthly,
        'hourly_rate': 100,
    })
    response = su_client.put(url, data)
    assert response.status_code == 200, response.content

    response_data = response.json()
    assert Decimal(response_data['hourly_rate']) == data['hourly_rate']


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('update_data, signup_bonus, signup_2year_bonus', (
    ({}, '0.00', '0.00'),
    ({'signup_bonus': '100.00'}, '100.00', '0.00'),
    ({'signup_2year_bonus': '100.00'}, '0.00', '100.00'),
))
def test_draft_offer_update_signup_bonus(su_client, offer_draft_update_data, update_data,
                                         signup_bonus, signup_2year_bonus):
    offer = f.create_offer()
    url = reverse('api:offers:detail', kwargs={'pk': offer.id})
    data = offer_draft_update_data | update_data
    response = su_client.put(url, data)
    assert response.status_code == 200, response.content

    response_data = response.json()
    assert response_data['signup_bonus'] == signup_bonus
    assert response_data['signup_2year_bonus'] == signup_2year_bonus


@override_switch('enable_offer_pdf_generation', active=True)
@eager_task('intranet.femida.src.offers.controllers.generate_offer_pdf_task')
@patch(
    target='intranet.femida.src.oebs.api.OebsFormulaAPI._post',
    return_value=b'file',
)
@pytest.mark.parametrize('update_data, had_pdf, has_pdf', (
    ({'grade': 16}, False, True),
    ({'grade': 19}, False, False),
    ({'grade': 19}, True, False),
))
def test_draft_offer_update_generate_pdf(mocked_api, mocked_task, su_client,
                                         offer_draft_update_data, update_data, had_pdf, has_pdf):
    offer = f.create_offer(
        office=f.OfficeFactory(),
        grade=15,
        payment_type=choices.PAYMENT_TYPES.monthly,
    )
    if had_pdf:
        f.OfferAttachmentFactory(
            offer=offer,
            type=choices.OFFER_ATTACHMENT_TYPES.offer_pdf,
        )
    url = reverse('api:offers:detail', kwargs={'pk': offer.id})
    data = offer_draft_update_data | update_data
    response = su_client.put(url, data)
    assert response.status_code == 200, response.content
    assert (offer.offer_pdf is not None) == has_pdf


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('update_data, bonus, bonus_2year, bonus_type', (
    ({}, '0.00', '0.00', ''),
    (
        {'bonus': '100.00', 'bonus_2year': '50.00', 'bonus_type': 'welcome'},
        '100.00', '50.00', choices.BONUS_TYPES.welcome,
    ),
    (
        {'bonus': '200.00', 'bonus_2year': '0.00', 'bonus_type': 'signup'},
        '200.00', '0.00', choices.BONUS_TYPES.signup,
    ),
))
def test_draft_offer_update_bonus(su_client, offer_draft_update_data, update_data,
                                  bonus, bonus_2year, bonus_type):
    offer = f.create_offer()
    url = reverse('api:offers:detail', kwargs={'pk': offer.id})
    data = offer_draft_update_data | update_data
    response = su_client.put(url, data)
    assert response.status_code == 200, response.content

    response_data = response.json()
    assert response_data['bonus'] == bonus
    assert response_data['bonus_2year'] == bonus_2year
    assert response_data['bonus_type'] == bonus_type


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('update_data', (
    {},
    {'relocation_package': 'silver'},
    {'relocation_package': 'gold'},
    {'relocation_package': 'platinum'},
))
def test_offer_relocation_package_update(su_client, offer_draft_update_data, update_data):
    offer = f.create_offer()
    url = reverse('api:offers:detail', kwargs={'pk': offer.id})
    data = offer_draft_update_data | update_data
    response = su_client.put(url, data)
    assert response.status_code == 200, response.content

    response_data = response.json()
    assert response_data['relocation_package'] == update_data.get('relocation_package', '')


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('employee_type', (
    choices.EMPLOYEE_TYPES.new,
    choices.EMPLOYEE_TYPES.former,
    choices.EMPLOYEE_TYPES.current,
    choices.EMPLOYEE_TYPES.rotation,
    choices.EMPLOYEE_TYPES.intern,
))
@override_settings(YANDEX_DEPARTMENT_ID=100500, EXTERNAL_DEPARTMENT_ID=500100)
def test_on_approval_offer_update(su_client, offer_base_data, employee_type):
    username = None if employee_type == choices.EMPLOYEE_TYPES.new else 'username'
    if username:
        f.create_user(
            username=username,
            is_dismissed=employee_type == choices.EMPLOYEE_TYPES.former,
            department__id=_get_dep_id_by_emp_type(employee_type),
        )

    offer = f.create_offer(
        status=choices.OFFER_STATUSES.on_approval,
        employee_type=employee_type,
    )
    url = reverse('api:offers:detail', kwargs={'pk': offer.id})
    data = offer_base_data.copy()
    data.update({
        'employee_type': employee_type,
        'username': username,
    })
    response = su_client.put(url, data)
    assert response.status_code == 200


def test_offer_update_form(su_client):
    offer = f.create_offer()
    url = reverse('api:offers:update-form', kwargs={'pk': offer.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_offer_generated_text(su_client):
    offer = f.create_offer()
    url = reverse('api:offers:generated-text', kwargs={'pk': offer.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_offer_attachment_upload(client):
    offer = f.create_offer(status=choices.OFFER_STATUSES.sent)
    link = f.OfferLinkFactory.create(offer=offer)
    uid = link.uid.hex
    url = reverse('external-api:offers-attachment-upload', kwargs={'uid': uid})
    data = {
        'file': ContentFile('file', name='file.txt'),
    }
    response = client.post(url, data, format='multipart')
    assert response.status_code == 200


def test_offer_attachment_upload_too_large_file(client):
    offer = f.create_offer(status=choices.OFFER_STATUSES.sent)
    link = f.OfferLinkFactory.create(offer=offer)
    uid = link.uid.hex
    url = reverse('external-api:offers-attachment-upload', kwargs={'uid': uid})
    max_size = settings.OFFER_MAX_ATTACHMENT_SIZE * 1024 ** 2
    data = {
        'file': ContentFile('*' * (max_size + 1), name='file.txt'),
    }
    response = client.post(url, data, format='multipart')
    assert response.status_code == 400


@pytest.mark.parametrize('employee_type', (
    choices.EMPLOYEE_TYPES.new,
    choices.EMPLOYEE_TYPES.rotation,
))
@patch('intranet.femida.src.offers.workflow.OfferAction.job_issue', PropertyMock())
def test_offer_approve(su_client, employee_type):
    f.create_waffle_switch('enable_offer_profession_in_main_form')
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.ready_for_approval,
        employee_type=employee_type,
        budget_position_id=5000,  # VACANCY
        profession=f.ProfessionFactory.create(),
    )
    url = reverse('api:offers:approve', kwargs={'pk': offer.id})
    data = {
        'abc_services': [f.ServiceFactory.create().id],
        'professional_level': choices.PROFESSIONAL_LEVELS.expert,
        'salary_expectations_currency': f.currency().id,
        'source': choices.SOURCES.internal_reference,
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


@pytest.mark.parametrize('has_duplicate, status_code', (
    (False, 200),
    (True, 400),
))
def test_offer_approve_form(su_client, has_duplicate, status_code):
    f.create_waffle_switch('enable_has_duplicates_error')
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.ready_for_approval,
        budget_position_id=5000,  # VACANCY
    )
    if has_duplicate:
        f.DuplicationCaseFactory(first_candidate=offer.candidate)
    url = reverse('api:offers:approve-form', kwargs={'pk': offer.id})
    response = su_client.get(url)
    assert response.status_code == status_code


def test_offer_reapprove(su_client):
    offer = f.create_offer(status=choices.OFFER_STATUSES.sent)
    url = reverse('api:offers:reapprove', kwargs={'pk': offer.id})
    response = su_client.post(url)
    assert response.status_code == 200


@pytest.mark.parametrize(
    'employee_type, has_link, expected_status_code, expected_errors', (
        pytest.param(
            choices.EMPLOYEE_TYPES.new, True, 200, {},
            id='new-hasLink',
        ),
        pytest.param(
            choices.EMPLOYEE_TYPES.new, False, 400, {'offer_text': ['Link must be in message']},
            id='new-noLink',
        ),
        pytest.param(
            choices.EMPLOYEE_TYPES.rotation, True,
            403, {},
            id='rotation',
        ),
    )
)
@patch('intranet.femida.src.offers.workflow.IssueCommentOperation', Mock())
def test_offer_send(su_client, employee_type, has_link, expected_status_code, expected_errors):
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.on_approval,
        budget_position_id=15000,  # OFFER
        employee_type=employee_type,
        salary=90000,
    )
    # Note: Негативные кейсы для Verification проверяем в test_offer_send_form
    f.VerificationFactory(
        candidate=offer.candidate,
        application=offer.application,
        expiration_date=shifted_now(days=1),
        status=VERIFICATION_STATUSES.closed,
        resolution=VERIFICATION_RESOLUTIONS.hire,
    )
    link = f.OfferLinkFactory.create(offer=offer)
    url = reverse('api:offers:send', kwargs={'pk': offer.id})
    data = {
        'receiver': 'email@email.com',
        'subject': 'Subject',
        'message': 'Message',
        'offer_text': f'Offer text: {str(link) if has_link else ""}',
    }

    response = su_client.post(url, data)
    assert response.status_code == expected_status_code, response.content

    result = response.json()
    assert len(result.get('errors', {})) == len(expected_errors), response.content
    for key, value in expected_errors.items():
        assert key in result and result[key] == value, response.content


@pytest.mark.parametrize('first_resolution, final_resolution, status_code', (
    (VERIFICATION_RESOLUTIONS.nohire, VERIFICATION_RESOLUTIONS.hire, 200),
    (VERIFICATION_RESOLUTIONS.hire, VERIFICATION_RESOLUTIONS.nohire, 400),
))
def test_offer_send_form_two_verification(su_client, first_resolution, final_resolution,
                                          status_code):
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.on_approval,
        budget_position_id=15000,  # OFFER
        salary=90000,
    )
    f.VerificationFactory(
        candidate=offer.candidate,
        application=offer.application,
        expiration_date=shifted_now(days=-1),
        status=VERIFICATION_STATUSES.closed,
        resolution=first_resolution,
    )
    f.VerificationFactory(
        candidate=offer.candidate,
        application=offer.application,
        expiration_date=shifted_now(days=1),
        status=VERIFICATION_STATUSES.closed,
        resolution=final_resolution,
    )
    url = reverse('api:offers:send-form', kwargs={'pk': offer.id})
    response = su_client.get(url)
    assert response.status_code == status_code, response.content
    if final_resolution == VERIFICATION_RESOLUTIONS.nohire:
        assert response.data['error'][0]['code'] == 'verification_resolution_negative'


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
def test_offer_check_login(su_client):
    offer = f.create_offer()
    url = reverse('api:offers:check-login', kwargs={'pk': offer.pk})
    data = {
        'employee_type': choices.EMPLOYEE_TYPES.new,
        'username': 'username',
    }
    response = su_client.get(url, data)
    assert response.status_code == 200


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
def test_offer_check_login_external(client):
    offer = f.create_offer(status=choices.OFFER_STATUSES.sent)
    link = f.OfferLinkFactory.create(offer=offer)
    uid = link.uid.hex
    url = reverse('external-api:offers-check-login', kwargs={'uid': uid})
    data = {
        'username': 'username',
    }
    response = client.get(url, data)
    assert response.status_code == 200


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@patch('intranet.femida.src.offers.workflow.archive_vacancy_publications')
@pytest.mark.parametrize('is_enabled_new_publications_archive', (False, True))
@pytest.mark.parametrize('employee_type', choices.EXTERNAL_EMPLOYEE_TYPES._db_values)
def test_offer_accept_russian_form(mocked_archive_publications, client, employee_type, is_enabled_new_publications_archive):
    f.create_waffle_switch(TemporarySwitch.ENABLE_NEW_PUBLICATIONS_ARCHIVE, is_enabled_new_publications_archive)
    russia = f.country(code='RU')
    organization = f.OrganizationFactory(country_code="RU")
    offer = f.create_offer(
        form_type=choices.FORM_TYPES.russian,
        employee_type=employee_type,
        status=choices.OFFER_STATUSES.sent,
        org=organization,
    )
    offer.office.city.country = russia
    offer.office.city.save()
    employment_book = choices.EMPLOYMENT_BOOK_OPTIONS.paper
    photo = f.AttachmentFactory.create()
    passport_page = f.AttachmentFactory.create()
    snils = f.AttachmentFactory.create()
    for att in (photo, passport_page, snils):
        f.OfferAttachmentFactory.create(
            offer=offer,
            attachment=att,
        )
    link = f.OfferLinkFactory.create(offer=offer)

    url = reverse('external-api:offers-accept', kwargs={'uid': link.uid.hex})
    data = {
        'username': 'username',
        'join_at': '01.01.2019',
        'last_name': 'Last',
        'first_name': 'First',
        'last_name_en': 'Last',
        'first_name_en': 'First',
        'gender': choices.GENDER.M,
        'birthday': '01.01.1990',
        'citizenship': choices.CITIZENSHIP.RU,
        'employment_book': employment_book,
        'residence_address': 'something',
        'phone': '+79203334455',
        'home_email': 'email@example.com',
        'os': choices.AVAILABLE_OPERATING_SYSTEMS.mac,
        'photo': [photo.id],
        'passport_pages': [passport_page.id],
        'snils': [snils.id],
        'is_agree': True,
        'nda_accepted': True,
        'is_eds_needed': False,

        # Банковские реквизиты
        'bic': '123456789',
        'bank_name': 'Yandex Money',
        'bank_account': '01234567890123456789',
    }
    response = client.post(url, data)
    assert response.status_code == 200

    if is_enabled_new_publications_archive:
        mocked_archive_publications.assert_called_once_with(offer.vacancy)
        assert not offer.vacancy.is_published
    else:
        mocked_archive_publications.assert_not_called()


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('employee_type', choices.EXTERNAL_EMPLOYEE_TYPES._db_values)
@pytest.mark.usefixtures('language_tags_fixture')
def test_offer_accept_international_form_without_spoken_languages(client, employee_type):
    offer = f.create_offer(
        form_type=choices.FORM_TYPES.international,
        employee_type=employee_type,
        status=choices.OFFER_STATUSES.sent,
    )
    photo = f.AttachmentFactory.create()
    f.OfferAttachmentFactory.create(offer=offer, attachment=photo)
    link = f.OfferLinkFactory.create(offer=offer)
    main_language_str = 'en'
    main_language = LanguageTag.objects.get(tag=main_language_str)
    url = reverse('external-api:offers-accept', kwargs={'uid': link.uid.hex})
    data = {
        'username': 'username',
        'join_at': '01.01.2019',
        'last_name': 'Last  Name',  # 2 пробела
        'first_name': ' First ',
        'gender': choices.GENDER.M,
        'birthday': '01.01.1990',
        'citizenship': choices.CITIZENSHIP.ZZ,
        'residence_address': 'something',
        'phone': '+79203334455',
        'home_email': 'email@example.com',
        'os': choices.AVAILABLE_OPERATING_SYSTEMS.mac,
        'photo': [photo.id],
        'main_language': main_language.pk,
        'is_agree': True,
        'nda_accepted': True,
        'preferred_first_and_last_name': ' First   Last     Names ',
    }
    response = client.post(url, data)

    assert response.status_code == 200
    assert offer.profile.last_name == 'Last Name'
    assert offer.profile.first_name == 'First'
    assert offer.profile.last_name == offer.profile.last_name_en
    assert offer.profile.first_name == offer.profile.first_name_en
    assert offer.profile.preferred_first_and_last_name == 'First Last Names'


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('langs, main_result, spoken_result', [
    (['ru'], 'ru', []),
    (['ru', 'en'], 'ru', ['en']),
    (['en', 'en', 'en'], 'en', []),
    (['en', 'en', 'ru'], 'en', ['ru']),
    (['en', 'bg', 'el'], 'en', ['bg', 'el']),
])
@pytest.mark.usefixtures('language_tags_fixture')
def test_offer_accept_international_form_with_spoken_languages(client, langs, main_result, spoken_result):
    offer = f.create_offer(
        form_type=choices.FORM_TYPES.international,
        employee_type=choices.EXTERNAL_EMPLOYEE_TYPES.new,
        status=choices.OFFER_STATUSES.sent,
    )
    photo = f.AttachmentFactory.create()
    f.OfferAttachmentFactory.create(offer=offer, attachment=photo)
    link = f.OfferLinkFactory.create(offer=offer)

    main_language = LanguageTag.objects.get(tag=langs[0])
    if langs[1:]:
        spoken_languages = {'spoken_languages': [x.pk for x in LanguageTag.objects.filter(tag__in=langs[1:])]}
    else:
        spoken_languages = {}

    url = reverse('external-api:offers-accept', kwargs={'uid': link.uid.hex})
    data = {
        'username': 'username',
        'join_at': '01.01.2019',
        'last_name': 'Last  Name',  # 2 пробела
        'first_name': ' First ',
        'gender': choices.GENDER.M,
        'birthday': '01.01.1990',
        'citizenship': choices.CITIZENSHIP.ZZ,
        'residence_address': 'something',
        'phone': '+79203334455',
        'home_email': 'email@example.com',
        'os': choices.AVAILABLE_OPERATING_SYSTEMS.mac,
        'photo': [photo.id],
        'main_language': main_language.pk,
        'is_agree': True,
        'nda_accepted': True,
        'preferred_first_and_last_name': ' First   Last     Names ',
        **spoken_languages,
    }
    response = client.post(url, data)
    assert response.status_code == 200
    main_language = offer.candidate.main_language
    spoken_language_tags = [x.tag for x in offer.candidate.spoken_languages]
    assert main_language.tag == main_result
    assert sorted(spoken_language_tags) == sorted(spoken_result)


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('autofill_from_verification, autofill_from_candidate, status_code', (
    (True, False, 200),
    (False, True, 200),
    (False, False, 400),
))
def test_offer_accept_with_autofill(client, autofill_from_verification, autofill_from_candidate,
                                    status_code):
    russia = f.country(code='RU')
    offer = f.create_offer(status=choices.OFFER_STATUSES.sent)
    offer.office.city.country = russia
    offer.office.city.save()
    if autofill_from_verification:
        f.create_actual_verification(
            candidate=offer.candidate,
            application=offer.application,
            raw_data={'params': {
                'last_name': 'Last',
                'first_name': 'First',
                'birthday': '01.01.1990',
                'russian_citizenship': 'Да',
                'registration_address': 'something',
                'the_same_address': 'Да',
                'phone': '+79203334455',
                'email': 'email@example.com',
            }},
        )
    elif autofill_from_candidate:
        f.create_actual_verification(
            candidate=offer.candidate,
            application=offer.application,
            raw_data={'params': {
                'russian_citizenship': 'Да',
                'registration_address': 'something',
                'the_same_address': 'Да',
            }},
        )
        offer.candidate.birthday = date(1990, 1, 1)
        offer.candidate.save()
        f.CandidateContactFactory(
            candidate=offer.candidate,
            account_id='+79203334455',
            type=CONTACT_TYPES.phone,
            is_main=True,
        )
        f.CandidateContactFactory(
            candidate=offer.candidate,
            account_id='email@example.com',
            type=CONTACT_TYPES.email,
            is_main=True,
        )

    f.HireOrderFactory(
        status=HIRE_ORDER_STATUSES.offer_sent,
        candidate=offer.candidate,
        vacancy=offer.vacancy,
        application=offer.application,
        offer=offer,
        recruiter=f.create_recruiter(username='best-recruiter'),
        raw_data={'autofill_offer': True},
    )
    photo = f.AttachmentFactory.create()
    passport_page = f.AttachmentFactory.create()
    snils = f.AttachmentFactory.create()
    for att in (photo, passport_page, snils):
        f.OfferAttachmentFactory.create(
            offer=offer,
            attachment=att,
        )
    link = f.OfferLinkFactory.create(offer=offer)
    uid = link.uid.hex
    url = reverse('external-api:offers-accept-form', kwargs={'uid': uid})
    response = client.get(url)
    assert response.status_code == 200
    response_data = response.json().get('data', {})
    data = {name: field['value'] for name, field in response_data.items()}
    data.update({
        'username': 'username',
        'last_name_en': 'Last',
        'first_name_en': 'First',
        'gender': choices.GENDER.M,
        'employment_book': choices.EMPLOYMENT_BOOK_OPTIONS.paper,
        'os': choices.AVAILABLE_OPERATING_SYSTEMS.mac,
        'photo': [photo.id],
        'passport_pages': [passport_page.id],
        'snils': [snils.id],
        'is_eds_needed': False,

        # Банковские реквизиты
        'bic': '123456789',
        'bank_name': 'Yandex Money',
        'bank_account': '01234567890123456789',
        'card_number': '1234123412341234',
    })
    url = reverse('external-api:offers-accept', kwargs={'uid': uid})
    response = client.post(url, data)
    assert response.status_code == status_code


def test_offer_accept_form(client):
    offer = f.create_offer(status=choices.OFFER_STATUSES.sent)
    link = f.OfferLinkFactory.create(offer=offer)
    uid = link.uid.hex
    url = reverse('external-api:offers-accept-form', kwargs={'uid': uid})
    response = client.get(url)
    assert response.status_code == 200


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
def test_offer_update_join_at(su_client):
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.accepted,
        newhire_id=1,
        newhire_status=choices.OFFER_NEWHIRE_STATUSES.ready,
    )
    f.OfferProfileFactory.create(offer=offer)
    url = reverse('api:offers:update-join-at', kwargs={'pk': offer.id})
    data = {
        'join_at': '2018-01-01',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
def test_offer_update_join_at_form(su_client):
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.accepted,
        newhire_id=1,
    )
    f.OfferProfileFactory.create(offer=offer)
    url = reverse('api:offers:update-join-at-form', kwargs={'pk': offer.id})
    response = su_client.get(url)
    assert response.status_code == 200


@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('employee_type, status_code', (
    (choices.EMPLOYEE_TYPES.new, 200),
    (choices.EMPLOYEE_TYPES.former, 200),
    (choices.EMPLOYEE_TYPES.current, 200),
    (choices.EMPLOYEE_TYPES.rotation, 403),
    (choices.EMPLOYEE_TYPES.intern, 403),
))
@override_settings(YANDEX_DEPARTMENT_ID=100500, EXTERNAL_DEPARTMENT_ID=500100)
def test_offer_update_username(su_client, employee_type, status_code):
    username = 'username'
    if employee_type != choices.EMPLOYEE_TYPES.new:
        f.create_user(
            username='username',
            is_dismissed=employee_type == choices.EMPLOYEE_TYPES.former,
            department__id=_get_dep_id_by_emp_type(employee_type),
        )
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.accepted,
        newhire_id=1,
        newhire_status=choices.OFFER_NEWHIRE_STATUSES.approved,
        employee_type=employee_type,
    )
    f.OfferProfileFactory.create(offer=offer)
    url = reverse('api:offers:update-username', kwargs={'pk': offer.id})
    data = {
        'employee_type': employee_type,
        'username': username,
    }
    response = su_client.post(url, data)
    assert response.status_code == status_code


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
def test_offer_update_username_form(su_client):
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.accepted,
        newhire_id=1,
    )
    f.OfferProfileFactory.create(offer=offer)
    url = reverse('api:offers:update-username-form', kwargs={'pk': offer.id})
    response = su_client.get(url)
    assert response.status_code == 200


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
@patch('intranet.femida.src.offers.workflow.update_oebs_assignment_task.delay')
def test_offer_update_department(mocked_update_oebs_assignment, su_client):
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.accepted,
        newhire_id=1,
        newhire_status=choices.OFFER_NEWHIRE_STATUSES.approved,
        oebs_person_id=1,
    )
    url = reverse('api:offers:update-department', kwargs={'pk': offer.id})
    data = {
        'department': f.DepartmentFactory(ancestors=[offer.department.id]).id,
    }
    response = su_client.post(url, data)
    assert response.status_code == 200

    mocked_update_oebs_assignment.assert_called_once_with(offer.id)


def test_offer_update_department_form(su_client):
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.accepted,
        newhire_id=1,
        newhire_status=choices.OFFER_NEWHIRE_STATUSES.approved,
    )
    url = reverse('api:offers:update-department-form', kwargs={'pk': offer.id})
    response = su_client.get(url)
    assert response.status_code == 200


@patch(
    target='intranet.femida.src.offers.workflow.OfferAction.startrek_job_status',
    new=StatusEnum.in_progress,
)
@pytest.mark.parametrize('data, is_comment_required, status_code', (
    ({'comment': 'test'}, True, 200),
    ({}, False, 200),
    ({}, True, 400),
))
def test_offer_delete(su_client, data, is_comment_required, status_code):
    f.create_waffle_switch('require_comment_on_offer_delete', is_comment_required)
    offer = f.create_offer()
    url = reverse('api:offers:delete', kwargs={'pk': offer.id})
    response = su_client.post(url, data)
    assert response.status_code == status_code


@pytest.mark.parametrize('patched_status, status_code', (
    (StatusEnum.in_progress, 200),
    (StatusEnum.closed, 400),
))
def test_offer_delete_form(su_client, patched_status, status_code):
    patch_status = patch(
        target='intranet.femida.src.offers.workflow.OfferAction.startrek_job_status',
        new=patched_status,
    )
    with patch_status:
        offer = f.create_offer(status=choices.OFFER_STATUSES.on_approval)
        url = reverse('api:offers:delete-form', kwargs={'pk': offer.id})
        response = su_client.get(url)
        assert response.status_code == status_code


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
@patch('intranet.femida.src.offers.workflow.unarchive_vacancy_publications')
@pytest.mark.parametrize('is_enabled_new_publications_archive', (False, True))
def test_offer_reject(mocked_unarchive_publications, su_client, is_enabled_new_publications_archive):
    f.create_waffle_switch(TemporarySwitch.ENABLE_NEW_PUBLICATIONS_ARCHIVE, is_enabled_new_publications_archive)
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.accepted,
        newhire_id=1,
    )
    url = reverse('api:offers:reject', kwargs={'pk': offer.id})
    data = {
        'recruiter': f.create_recruiter().username,
        'rejection_reason': REJECTION_REASONS.money,
    }
    response = su_client.post(url, data)
    assert response.status_code == 200
    offer.refresh_from_db()
    assert offer.rejection
    assert offer.rejection.rejection_reason == REJECTION_REASONS.money

    if is_enabled_new_publications_archive:
        mocked_unarchive_publications.assert_called_once_with(offer.vacancy)
        assert offer.vacancy.is_published
    else:
        mocked_unarchive_publications.assert_not_called()


@patch('intranet.femida.src.offers.controllers.NewhireAPI', FakeNewhireAPI)
@pytest.mark.parametrize('status, newhire_id, is_safe_to_reject, status_code', (
    (choices.OFFER_STATUSES.accepted, 1, False, 200),
    (choices.OFFER_STATUSES.sent, None, False, 200),
    (choices.OFFER_STATUSES.accepted, None, True, 200),
    (choices.OFFER_STATUSES.accepted, None, False, 403),
))
def test_offer_reject_is_permitted(su_client, status, newhire_id, is_safe_to_reject, status_code):
    offer = f.create_offer(status=status, newhire_id=newhire_id)
    safe_to_reject_config = f'{offer.id}' if is_safe_to_reject else ''

    url = reverse('api:offers:reject', kwargs={'pk': offer.id})
    data = {
        'recruiter': f.create_recruiter().username,
        'rejection_reason': REJECTION_REASONS.money,
    }
    with override_config(OFFER_IDS_SAFE_TO_REJECT=safe_to_reject_config):
        response = su_client.post(url, data)
    assert response.status_code == status_code


@pytest.mark.parametrize('forbidden_actions, status_code', (
    ('[]', 200),
    ('["offer_reject"]', 403),
))
def test_offer_reject_is_forbidden_for_autohire(su_client, forbidden_actions, status_code):
    offer = f.OfferFactory(
        status=choices.OFFER_STATUSES.sent,
        application__vacancy__type=VACANCY_TYPES.autohire,
    )

    url = reverse('api:offers:reject', kwargs={'pk': offer.id})
    data = {
        'recruiter': f.create_recruiter().username,
        'rejection_reason': REJECTION_REASONS.money,
    }
    with override_config(AUTOHIRE_FORBIDDEN_ACTIONS=forbidden_actions):
        response = su_client.post(url, data)

    assert response.status_code == status_code
    if status_code == 403:
        assert response.json()['error'][0]['code'] == 'forbidden_for_autohire'


def test_offer_reject_form(su_client):
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.accepted,
        newhire_id=1,
    )
    url = reverse('api:offers:reject-form', kwargs={'pk': offer.id})
    response = su_client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('data, status_code', (
    ({}, 200),
    ({'errors': [{'message': 'unknown_error'}]}, 400),
    ({'errors': {'phone_number': [{'code': 'invalid_phone_number'}]}}, 400),
))
@patch('intranet.femida.src.api.offers.base_views.NewhireAPI.attach_phone')
def test_offer_attach_eds_phone(mocked_attach_phone, data, status_code, client):
    mocked_attach_phone.return_value = (data, status_code)
    offer = f.create_offer(status=choices.OFFER_STATUSES.sent)
    link = f.OfferLinkFactory.create(offer=offer)
    uid = link.uid.hex
    phone = '+77777777777'

    url = reverse('external-api:offers-attach-eds-phone', kwargs={'uid': uid})
    response = client.post(url, data={'eds_phone': phone})
    offer.refresh_from_db()
    data = {
        'object_type': 'offer',
        'object_id': offer.id,
        'phone_number': phone,
    }

    assert response.status_code == status_code
    mocked_attach_phone.assert_called_once_with(data)
    if status_code != 400:
        assert offer.eds_phone == phone


@pytest.mark.parametrize('data, status_code', (
    ({}, 200),
    ({'errors': [{'message': 'incorrect_code'}]}, 400),
    ({'errors': {'code': [{'code': 'required'}]}}, 400),
))
@patch('intranet.femida.src.api.offers.base_views.NewhireAPI.verify_code')
def test_offer_verify_eds_phone(mocked_verify_code, data, status_code, client):
    mocked_verify_code.return_value = (data, status_code)
    is_eds_phone_verified = True if status_code == 200 else False
    offer = f.create_offer(status=choices.OFFER_STATUSES.sent)
    link = f.OfferLinkFactory.create(offer=offer)
    uid = link.uid.hex
    code = '123'

    url = reverse('external-api:offers-verify-eds-phone', kwargs={'uid': uid})
    response = client.post(url, data={'code': code})
    offer.refresh_from_db()
    data = {
        'object_type': 'offer',
        'object_id': offer.id,
        'code': code,
    }

    assert response.status_code == status_code
    mocked_verify_code.assert_called_once_with(data)
    assert offer.is_eds_phone_verified == is_eds_phone_verified


@pytest.mark.parametrize('org_country, form_type, fields', [
    ("RU", choices.FORM_TYPES.russian, {
        'bank_account', 'bank_name', 'bic', 'comment', 'employment_book',
        'first_name_en', 'is_eds_needed', 'last_name_en',
        'middle_name', 'passport_pages', 'snils',
    }),
    ("RU", choices.FORM_TYPES.international, {
        'bank_account', 'bank_name', 'bic', 'documents', 'is_eds_needed',
        'main_language', 'preferred_first_and_last_name', 'spoken_languages',
    }),
    ("in", choices.FORM_TYPES.russian, {
        'comment', 'first_name_en', 'last_name_en',
        'middle_name', 'passport_pages', 'snils',
    }),
    ("in", choices.FORM_TYPES.international, {
        'documents', 'main_language', 'preferred_first_and_last_name',
        'spoken_languages',
    }),
])
def test_offer_accept_form_has_all_fields(client, org_country, form_type, fields):
    organization = f.OrganizationFactory(country_code=org_country)
    offer = f.OfferFactory(status=choices.OFFER_STATUSES.sent, org=organization, form_type=form_type)
    link = f.OfferLinkFactory(offer=offer)
    base_fields = {'birthday', 'citizenship', 'first_name', 'gender',
                   'home_email', 'join_at', 'is_agree', 'last_name', 'nda_accepted',
                   'os', 'phone', 'photo', 'residence_address', 'username',
                   }
    assert not fields.intersection(base_fields)
    url = reverse('external-api:offers-accept-form', kwargs={'uid': link.uid.hex})
    fields = fields.union(base_fields)
    response = client.get(url, {})
    assert response.status_code == 200
    data = response.json()
    data_keys = set(data['data'].keys())
    assert not fields.symmetric_difference(data_keys)


def test_offer_accept_form_has_all_genders(client):
    organization = f.OrganizationFactory(country_code="RU")
    offer = f.OfferFactory(status=choices.OFFER_STATUSES.sent, org=organization, form_type=choices.FORM_TYPES.russian)
    link = f.OfferLinkFactory(offer=offer)
    url = reverse('external-api:offers-accept-form', kwargs={'uid': link.uid.hex})
    response = client.get(url, {})
    assert response.status_code == 200
    data = response.json()
    excepted_genders_set = {
        ('—', ''),
        ('offer_profile.gender.male', 'M'),
        ('offer_profile.gender.female', 'F'),
    }
    form_genders_set = data['structure']['gender']['choices']
    assert len(form_genders_set) == len(excepted_genders_set)
    genders_set = {(x['label'], x['value']) for x in form_genders_set}
    assert genders_set == excepted_genders_set


@pytest.mark.parametrize('perm,role,result', [
    ('hrbp_perm', VACANCY_ROLES.auto_observer, 403),
    ('hrbp_perm', VACANCY_ROLES.observer, 403),
    ('recruiter_perm', VACANCY_ROLES.main_recruiter, 200),
    ('recruiter_perm', VACANCY_ROLES.recruiter, 200),
])
@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
def test_user_edit_offer_with_perm(client, offer_draft_update_data, perm, role, result):
    offer = f.OfferFactory()

    user = f.create_user_with_perm(perm)
    f.VacancyMembershipFactory(role=role, member=user, vacancy=offer.vacancy)

    new_data = {'username': 'I cant'}
    data = offer_draft_update_data | new_data

    url = reverse('api:offers:detail', kwargs={'pk': offer.id})

    client.login(user.username)
    response = client.put(url, data)

    assert response.status_code == result


@pytest.mark.parametrize('perm,role,result', [
    ('hrbp_perm', VACANCY_ROLES.auto_observer, 200),
    ('hrbp_perm', VACANCY_ROLES.observer, 200),
    ('recruiter_perm', VACANCY_ROLES.main_recruiter, 200),
    ('recruiter_perm', VACANCY_ROLES.recruiter, 200),
])
def test_user_view_offer_with_perm(client, perm, role, result):
    user = f.create_user_with_perm(perm)
    offer = f.OfferFactory()
    f.VacancyMembershipFactory(role=role, member=user, vacancy=offer.vacancy)

    client.login(user.username)

    url = reverse('api:offers:detail', kwargs={'pk': offer.id})
    response = client.get(url, {})

    assert response.status_code == result


@pytest.mark.parametrize('perm,role,result', [
    ('hrbp_perm', VACANCY_ROLES.auto_observer, 403),
    ('hrbp_perm', VACANCY_ROLES.observer, 403),
    ('recruiter_perm', VACANCY_ROLES.main_recruiter, 200),
    ('recruiter_perm', VACANCY_ROLES.recruiter, 200),
])
@patch('intranet.femida.src.offers.login.validators.NewhireAPI', FakeNewhireAPI)
def test_user_send_offer_with_perm(client, offer_draft_update_data, perm, role, result):
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.on_approval,
        budget_position_id=15000,
        employee_type=choices.EMPLOYEE_TYPES.new,
        salary=90000,
    )
    f.VerificationFactory(
        candidate=offer.candidate,
        application=offer.application,
        expiration_date=shifted_now(days=1),
        status=VERIFICATION_STATUSES.closed,
        resolution=VERIFICATION_RESOLUTIONS.hire,
    )
    link = f.OfferLinkFactory.create(offer=offer)
    data = {
        'receiver': 'email@email.com',
        'subject': 'Subject',
        'message': 'Message',
        'offer_text': f'Offer text: {str(link)}',
    }
    user = f.create_user_with_perm(perm)
    # под капотом у create_offer - create_heavy_vacancy, которая создает несколько ролей
    # удалим существующие одноименные роли, чтобы не мешались
    offer.vacancy.memberships.filter(role=role).delete()
    f.VacancyMembershipFactory(role=role, member=user, vacancy=offer.vacancy)

    url = reverse('api:offers:send', kwargs={'pk': offer.id})

    client.login(user.username)
    response = client.post(url, data)

    assert response.status_code == result


@pytest.mark.parametrize('grade,country_code,count', [
    (18, 'RU', 2),
    (18, 'EU', 1),
    (19, 'RU', 1),
])
def test_offer_attachments(su_client, grade, country_code, count):
    """
    Проверяем, что брошюра добавляется, если грейд кандидата ⩽ 18 и
    организация = одно из российских юр.лиц и
    ветка выхода = Яндекс
    """
    f.create_waffle_flag('enable_new_offer')
    org = f.OrganizationFactory(country_code=country_code)
    department = f.DepartmentFactory(id=1)
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.on_approval,
        budget_position_id=15000,  # OFFER
        salary=90000,
        grade=grade,
        org=org,
        department=department,
    )
    f.OfferAttachmentFactory(offer=offer, type=choices.OFFER_ATTACHMENT_TYPES.offer_pdf)
    f.BrochureFactory.create()
    f.VerificationFactory(
        candidate=offer.candidate,
        application=offer.application,
        expiration_date=shifted_now(days=1),
        status=VERIFICATION_STATUSES.closed,
        resolution=VERIFICATION_RESOLUTIONS.hire,
    )
    url = reverse('api:offers:send-form', kwargs={'pk': offer.id})

    response = su_client.get(url)
    attachments = response.json().get('data').get('attachments')
    assert len(attachments.get('value')) == count


@patch('intranet.femida.src.offers.workflow.send_offer_to_candidate')
def test_send_offer_brochure(send_offer_to_candidate, su_client):
    """
    Проверяем, что брошюра отправляется в кандидату
    """
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.on_approval,
        budget_position_id=15000,
        employee_type=choices.EMPLOYEE_TYPES.new,
        salary=90000,
        grade=16,
    )
    f.VerificationFactory(
        candidate=offer.candidate,
        application=offer.application,
        expiration_date=shifted_now(days=1),
        status=VERIFICATION_STATUSES.closed,
        resolution=VERIFICATION_RESOLUTIONS.hire,
    )
    link = f.OfferLinkFactory.create(offer=offer)
    brochure = f.BrochureFactory.create()
    pdf = f.OfferAttachmentFactory(offer=offer, type=choices.OFFER_ATTACHMENT_TYPES.offer_pdf)
    attachments = [pdf.attachment_id, brochure.attachment_id]
    data = {
        'receiver': 'email@email.com',
        'subject': 'Subject',
        'message': 'Message',
        'offer_text': f'Offer text: {str(link)}',
        'attachments': attachments,
    }

    url = reverse('api:offers:send', kwargs={'pk': offer.id})
    su_client.post(url, data)

    assert len(send_offer_to_candidate.call_args.kwargs['attachments']) == len(attachments)


def test_offer_send_without_text(su_client):
    offer = f.create_offer(
        status=choices.OFFER_STATUSES.on_approval,
        budget_position_id=15000,  # OFFER
        employee_type=choices.EMPLOYEE_TYPES.new,
        salary=90000,
    )
    f.VerificationFactory(
        candidate=offer.candidate,
        application=offer.application,
        expiration_date=shifted_now(days=1),
        status=VERIFICATION_STATUSES.closed,
        resolution=VERIFICATION_RESOLUTIONS.hire,
    )
    f.OfferLinkFactory.create(offer=offer)
    url = reverse('api:offers:send', kwargs={'pk': offer.id})
    data = {
        'receiver': 'email@email.com',
        'subject': 'Subject',
        'message': 'Message',
        'offer_text': '',
    }

    response = su_client.post(url, data)
    assert response.status_code == 200
