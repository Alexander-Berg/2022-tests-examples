import pytest

from unittest.mock import patch, ANY

from intranet.femida.src.offers.choices import CITIZENSHIP, OFFER_ATTACHMENT_TYPES
from intranet.femida.src.offers.oebs.controllers import check_oebs_login

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import Contains


pytestmark = pytest.mark.django_db

patch_get_login_by_document = patch(
    target='intranet.femida.src.offers.oebs.controllers.OebsAPI.get_login_by_document',
    new=lambda x, y: None,
)


def _create_offer_with_verification(citizenship=CITIZENSHIP.RU, include_attachments=True,
                                    org_country_code='RU'):
    raw_data = {'params': {'inn': '123', 'snils': '456'}}
    verification = f.create_actual_verification(raw_data=raw_data)
    org = f.OrganizationFactory(
        country_code=org_country_code,
    )
    offer = f.OfferFactory(
        candidate=verification.candidate,
        startrek_hr_key='HR-1',
        org=org,
    )
    f.OfferProfileFactory(offer=offer, citizenship=citizenship)
    if include_attachments:
        f.OfferAttachmentFactory(offer=offer, type=OFFER_ATTACHMENT_TYPES.passport_page)
        f.OfferAttachmentFactory(offer=offer, type=OFFER_ATTACHMENT_TYPES.snils)
    return offer


def _assert_manager_invited(task, comment_part):
    task.assert_called_once_with(
        operation_class='IssueCommentOperation',
        key='HR-1',
        operation_id=ANY,
        text=Contains(comment_part),
    )


@patch('intranet.femida.src.startrek.operations.execute_issue_operation_task.si')
@patch('intranet.femida.src.offers.oebs.controllers.logger.warning')
@patch('intranet.femida.src.offers.oebs.controllers.save_offer_data_in_yt.delay')
def test_check_oebs_login_no_verification(mocked_save_offer, mocked_warning, mocked_operation_task):
    offer = f.OfferFactory(startrek_hr_key='HR-1')
    f.OfferProfileFactory(offer=offer)
    check_oebs_login(offer)
    assert not mocked_save_offer.called
    mocked_warning.assert_called_once_with(ANY, Contains('no verification'))
    _assert_manager_invited(mocked_operation_task, 'недостаточно актуальных данных')


@patch('intranet.femida.src.startrek.operations.execute_issue_operation_task.si')
@patch('intranet.femida.src.offers.oebs.controllers.save_offer_data_in_yt.delay')
@patch(
    target='intranet.femida.src.offers.oebs.controllers.OebsAPI.get_login_by_document',
    new=lambda x, y: 'login',
)
def test_check_oebs_login_found(mocked_save_offer, mocked_operation_task):
    offer = _create_offer_with_verification()
    check_oebs_login(offer)
    assert not mocked_save_offer.called
    _assert_manager_invited(mocked_operation_task, 'в Оракле найдено физическое лицо')


@patch('intranet.femida.src.startrek.operations.execute_issue_operation_task.si')
@patch('intranet.femida.src.offers.oebs.controllers.save_offer_data_in_yt.delay')
@patch_get_login_by_document
def test_check_oebs_login_not_found_oohrc_org(mocked_save_offer, mocked_operation_task):
    offer = _create_offer_with_verification(org_country_code='BY')
    check_oebs_login(offer)
    assert not mocked_save_offer.called
    _assert_manager_invited(mocked_operation_task, 'кандидат нанимается в юр.лицо')


@patch('intranet.femida.src.startrek.operations.execute_issue_operation_task.si')
@patch('intranet.femida.src.offers.oebs.controllers.save_offer_data_in_yt.delay')
@patch_get_login_by_document
def test_check_oebs_login_not_found_foreigner(mocked_save_offer, mocked_operation_task):
    offer = _create_offer_with_verification(citizenship=CITIZENSHIP.KZ)
    check_oebs_login(offer)
    assert not mocked_save_offer.called
    _assert_manager_invited(mocked_operation_task, 'кандидат является иностранным гражданином')


@patch('intranet.femida.src.startrek.operations.execute_issue_operation_task.si')
@patch('intranet.femida.src.offers.oebs.controllers.save_offer_data_in_yt.delay')
@patch_get_login_by_document
def test_check_oebs_login_no_docs(mocked_save_offer, mocked_operation_task):
    offer = _create_offer_with_verification(include_attachments=False)
    check_oebs_login(offer)
    assert not mocked_save_offer.called
    _assert_manager_invited(mocked_operation_task, 'кандидат не приложил все необходимые документы')


@patch('intranet.femida.src.startrek.operations.execute_issue_operation_task.si')
@patch('intranet.femida.src.offers.oebs.controllers.save_offer_data_in_yt.delay')
@patch_get_login_by_document
def test_check_oebs_login_not_found(mocked_save_offer, mocked_operation_task):
    offer = _create_offer_with_verification()
    check_oebs_login(offer)

    mocked_save_offer.assert_called_once_with(offer.id)
    mocked_operation_task.assert_called_once_with(
        operation_class='IssueCommentOperation',
        key='HR-1',
        operation_id=ANY,
        text=Contains('Запущен процесс создания физического лица'),
    )
