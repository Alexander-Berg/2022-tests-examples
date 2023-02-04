import pytest

from unittest.mock import Mock, ANY, patch

from constance import config
from django.conf import settings
from django.db.models import prefetch_related_objects

from intranet.femida.src.candidates.choices import CONTACT_TYPES
from intranet.femida.src.oebs.api import OebsPersonError
from intranet.femida.src.offers.choices import (
    DOCUMENT_TYPES,
    OFFER_ATTACHMENT_TYPES,
    OFFER_DOCS_PROCESSING_STATUSES,
    OFFER_STATUSES,
)
from intranet.femida.src.offers.controllers import create_bp_assignment, create_oebs_person
from intranet.femida.src.offers.workflow import OfferWorkflow
from intranet.femida.src.offers.yang.controllers import update_offers_from_yt
from intranet.femida.src.offers.helpers import get_candidate_alive_verifications_prefetch
from intranet.femida.src.staff.bp_registry import BPRegistryError

from intranet.femida.tests import factories as f
from intranet.femida.tests.mock.offers import FakeBPRegistryAPI, FakeOebsHireAPI
from intranet.femida.tests.utils import assert_not_raises


_BASE_DATA = {
    'offer_id': 1,
    'docs_request_count': 0,
}
_RUSSIAN_PASSPORT_DATA = {
    'result': 'OK',
    'errors': [],
    'data': {
        'last_name': 'Фамилия',
        'first_name': 'Имя',
        'middle_name': 'Отчество',
        'gender': 'М',
        'birthday': '1991-01-01',
        'document_type': DOCUMENT_TYPES.russian_passport,
        'passport_series_number': '12 34',
        'passport_number': '123456',
        'issuer': 'МВД г. Москвы',
        'issue_date': '2010-05-05',
        'issuer_subdivision_code': '123-456',
        'registration_date': '2020-02-02',
        'birthCountry': 'RU',
        'birthRegion': 'Регион',
        'birthDistrict': 'Район',
        'birthCity': 'Населенный пункт',
    },
}
_OTHER_PASSPORT_DATA = {
    'result': 'OK',
    'errors': [],
    'data': {
        'last_name': 'Фамилия',
        'first_name': 'Имя',
        'middle_name': 'Отчество',
        'gender': 'М',
        'birthday': '1991-01-01',
        'document_type': DOCUMENT_TYPES.other,
        'passport_series_number': '12 34',
        'passport_number': '123456',
        'issuer': 'МВД г. Москвы',
        'issue_date': '2010-05-05',
        'issuer_subdivision_code': '123-456',
        'registration_date': '2020-02-02',
        'birthCountry': 'KZ',
        'birthRegion': 'Регион',
        'birthDistrict': 'Район',
        'birthCity': 'Населенный пункт',
    },
}
_SNILS_DATA = {
    'result': 'OK',
    'errors': [],
    'data': '538-922-753 29',
}
_ADDRESS_DATA = {
    'result': 'OK',
    'errors': [],
    'data': {
        'kladr_code': '12345678901234567',
        'zip_code': '123456',
        'building': '1',
        'block': '2',
        'apartment': '3',
        'address': 'г. Москва, ул. Льва Толстого, д. 1, стр. 2, кв. 3',
    },
}
_BAD_DATA = {
    'result': 'BAD',
    'errors': ['bad_result'],
}
_BLOCK_DATA = {
    'result': 'BLOCK',
}

SUCCESSFUL_DATA = {
    **_BASE_DATA,
    'passport': _RUSSIAN_PASSPORT_DATA,
    'snils': _SNILS_DATA,
    'residence_address': _ADDRESS_DATA,
    'registration_address': _ADDRESS_DATA,
}
OTHER_DOC_TYPE_DATA = {
    **_BASE_DATA,
    'passport': _OTHER_PASSPORT_DATA,
    'snils': _SNILS_DATA,
    'residence_address': _ADDRESS_DATA,
    'registration_address': _ADDRESS_DATA,
}
PARTIAL_SUCCESSFUL_DATA = {
    **_BASE_DATA,
    'passport': _BAD_DATA,
    'snils': _SNILS_DATA,
    'residence_address': _ADDRESS_DATA,
    'registration_address': _BAD_DATA
}
PARTIAL_MISSING_DATA = {
    **_BASE_DATA,
    'passport': None,
    'snils': _SNILS_DATA,
    'residence_address': _ADDRESS_DATA,
    'registration_address': None
}
PARTIAL_BLOCK_DATA = {
    **_BASE_DATA,
    'passport': _RUSSIAN_PASSPORT_DATA,
    'snils': _BLOCK_DATA,
    'residence_address': _ADDRESS_DATA,
    'registration_address': _ADDRESS_DATA,
}
UNSUCCESSFUL_DATA = {
    **_BASE_DATA,
    'passport': _BAD_DATA,
    'snils': _BAD_DATA,
    'residence_address': _BAD_DATA,
    'registration_address': _BAD_DATA
}


@patch('intranet.femida.src.offers.yang.controllers.create_oebs_person_and_assignment', Mock())
@pytest.mark.parametrize('data, status, requests_count', (
    (SUCCESSFUL_DATA, OFFER_DOCS_PROCESSING_STATUSES.finished, 0),
    (OTHER_DOC_TYPE_DATA, OFFER_DOCS_PROCESSING_STATUSES.finished, 0),
    (PARTIAL_SUCCESSFUL_DATA, OFFER_DOCS_PROCESSING_STATUSES.need_information, 1),
    (UNSUCCESSFUL_DATA, OFFER_DOCS_PROCESSING_STATUSES.need_information, 1),
))
def test_update_offer_processed_data(data, status, requests_count):
    offer = f.OfferFactory.create(
        id=data['offer_id'],
        status=OFFER_STATUSES.accepted,
        docs_request_count=0,
        docs_processing_status=OFFER_DOCS_PROCESSING_STATUSES.in_progress,
    )
    update_offers_from_yt([data])
    offer.refresh_from_db()

    assert offer.docs_processing_status == status
    assert offer.docs_request_count == requests_count

    assert offer.passport_data == data['passport'].get('data')
    assert offer.snils_number == data['snils'].get('data')
    assert offer.residence_address_data == data['residence_address'].get('data')
    assert offer.registration_address_data == data['registration_address'].get('data')


@patch('intranet.femida.src.offers.yang.controllers.create_oebs_person_and_assignment', Mock())
def test_update_offer_missing_data():
    passport_data = SUCCESSFUL_DATA['passport']['data']
    registration_address_data = SUCCESSFUL_DATA['registration_address']['data']
    data = dict(PARTIAL_MISSING_DATA)
    data['docs_request_count'] = 1
    offer = f.OfferFactory.create(
        id=data['offer_id'],
        status=OFFER_STATUSES.accepted,
        docs_request_count=1,
        docs_processing_status=OFFER_DOCS_PROCESSING_STATUSES.in_progress,
        passport_data=passport_data,
        registration_address_data=registration_address_data,
    )

    update_offers_from_yt([data])
    offer.refresh_from_db()

    assert offer.docs_processing_status == OFFER_DOCS_PROCESSING_STATUSES.finished
    assert offer.docs_request_count == data['docs_request_count']

    assert offer.passport_data == passport_data
    assert offer.snils_number == data['snils']['data']
    assert offer.residence_address_data == data['residence_address']['data']
    assert offer.registration_address_data == registration_address_data


def test_update_offer_no_processed_data():
    offer = f.OfferFactory.create(
        id=1,
        status=OFFER_STATUSES.accepted,
        docs_request_count=0,
        docs_processing_status=OFFER_DOCS_PROCESSING_STATUSES.in_progress,
    )
    processed_data = []
    update_offers_from_yt(processed_data)
    offer.refresh_from_db()

    assert offer.docs_processing_status == OFFER_DOCS_PROCESSING_STATUSES.in_progress
    assert offer.docs_request_count == 0

    assert offer.passport_data is None
    assert offer.snils_number is None
    assert offer.residence_address_data is None
    assert offer.registration_address_data is None


@patch('intranet.femida.src.notifications.candidates.send_email.delay')
def test_send_docs_request_message_to_candidate(mocked_action):
    offer = f.OfferFactory.create(
        id=1,
        status=OFFER_STATUSES.accepted,
        docs_request_count=config.OFFER_DOCS_REQUEST_LIMIT - 1,
        docs_processing_status=OFFER_DOCS_PROCESSING_STATUSES.in_progress,
        creator__username='bad-guy',
    )
    main_email = f.CandidateContactFactory(
        candidate=offer.candidate,
        type=CONTACT_TYPES.email,
        is_main=True,
    )
    f.CandidateContactFactory(
        candidate=offer.candidate,
        type=CONTACT_TYPES.email,
        is_main=False,
    )
    data = dict(UNSUCCESSFUL_DATA)
    data['docs_request_count'] = config.OFFER_DOCS_REQUEST_LIMIT - 1
    update_offers_from_yt([data])

    mocked_action.assert_called_once_with(
        subject=ANY,
        body=ANY,
        to=[main_email.normalized_account_id],
        cc=['bad-guy@yandex-team.ru'],
        from_email=settings.JOB_EMAIL_VERBOSE,
        message_id=ANY,
        is_external=True,
    )


@patch('intranet.femida.src.offers.yang.controllers.create_oebs_person_and_assignment', Mock())
@pytest.mark.parametrize('data, is_last_attempt, called', (
    (SUCCESSFUL_DATA, True, False),
    (OTHER_DOC_TYPE_DATA, True, True),
    (PARTIAL_SUCCESSFUL_DATA, True, True),
    (UNSUCCESSFUL_DATA, True, True),
    (PARTIAL_BLOCK_DATA, False, True),
))
@patch('intranet.femida.src.offers.yang.controllers.notify_hr_about_docs_processing_problem')
def test_documents_error_notification(mocked_action, data, is_last_attempt, called):
    config.OFFER_DOCS_REQUEST_LIMIT = 100500
    docs_request_count = config.OFFER_DOCS_REQUEST_LIMIT if is_last_attempt else 100
    offer = f.OfferFactory.create(
        id=data['offer_id'],
        status=OFFER_STATUSES.accepted,
        docs_request_count=docs_request_count,
        docs_processing_status=OFFER_DOCS_PROCESSING_STATUSES.in_progress,
    )
    data = dict(data)
    data['docs_request_count'] = docs_request_count
    update_offers_from_yt([data])

    assert mocked_action.called is called
    if called:
        mocked_action.assert_called_once_with(offer)


@pytest.mark.parametrize('full_data', (True, False))
@patch('intranet.femida.src.offers.controllers.OfferCtl.update')
@patch('intranet.femida.src.yt.tasks.save_offer_data_in_yt.delay')
def test_process_additional_docs(mocked_task, mocked_controller, full_data):
    offer = f.OfferFactory.create()
    f.OfferAttachmentFactory(offer=offer, type=OFFER_ATTACHMENT_TYPES.passport_page)
    f.OfferAttachmentFactory(offer=offer, type=OFFER_ATTACHMENT_TYPES.snils)

    form_data = {
        'passport_pages': offer.attachments_by_type['passport_page'],
        'snils': offer.attachments_by_type['snils'],
        'residence_address': 'super address',
    } if full_data else {}

    offer_data = {
        'docs_processing_status': OFFER_DOCS_PROCESSING_STATUSES.in_progress,
        'profile': form_data
    }

    wf = OfferWorkflow(offer, user=None)
    action = wf.get_action('accept')
    action._process_additional_documents(form_data)

    mocked_controller.assert_called_once_with(offer_data)
    mocked_task.assert_called_once_with(offer.id)


def test_process_additional_docs_replacement():
    offer = f.OfferFactory.create()
    f.OfferProfileFactory(offer=offer)
    old_offer_attachment = f.OfferAttachmentFactory(
        offer=offer,
        type=OFFER_ATTACHMENT_TYPES.passport_page,
    )
    f.OfferAttachmentFactory(
        offer=offer,
        type=OFFER_ATTACHMENT_TYPES.snils,
    )

    new_attachment = f.AttachmentFactory()
    form_data = {
        'snils': [new_attachment],
    }

    wf = OfferWorkflow(offer, user=None)
    action = wf.get_action('accept')
    action._process_additional_documents(form_data)

    assert offer.attachments_by_type['passport_page'] == [old_offer_attachment.attachment]
    assert offer.attachments_by_type['snils'] == [new_attachment]


@patch('intranet.femida.src.offers.controllers.OebsHireAPI', FakeOebsHireAPI)
def test_create_oebs_person():
    offer = f.OfferFactory()
    f.VerificationFactory(
        candidate=offer.candidate,
        application=offer.application,
    )
    f.OfferProfileFactory(offer=offer)
    prefetch_related_objects([offer], get_candidate_alive_verifications_prefetch())

    with assert_not_raises(OebsPersonError):
        create_oebs_person(offer)

    offer.refresh_from_db()
    assert offer.oebs_person_id == int(FakeBPRegistryAPI.create_transaction())


@patch('intranet.femida.src.offers.controllers.BPRegistryAPI', FakeBPRegistryAPI)
def test_create_bp_assignment():
    offer = f.OfferFactory()

    with assert_not_raises(BPRegistryError):
        create_bp_assignment(offer)

    offer.refresh_from_db()
    assert offer.bp_transaction_id == FakeBPRegistryAPI.create_bp_assignment()['id']
