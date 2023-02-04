# -*- encoding: utf-8 -*-
import json
import re
import zipfile
from collections import namedtuple
from datetime import datetime, timedelta
from io import BytesIO
from json import loads

import pytest
from butils.application import getApplication
from hamcrest import assert_that, is_, contains_exactly, has_entries, is_not, empty, has_length
from hamcrest import equal_to, has_key
from lxml import etree
from mock import patch, Mock
from typing import Optional, List

from tests import ws_responses
from tests.ws_responses import REG_START_DT
from yb_darkspirit import scheme
from yb_darkspirit.api.errors import NotFound
from yb_darkspirit.api.handlers.registrations import StartRegistrationProcess, _is_suitable_sw_version
from yb_darkspirit.api.schemas import StartRegistrationSchema
from yb_darkspirit.application.plugins.dbhelper import Session
from yb_darkspirit.core import pdf_parsing
from yb_darkspirit.core.cash_register.state_checks import ChecksManager
from yb_darkspirit.core.cash_register.state_manager import DsStateManager
from yb_darkspirit.interactions import TrackerClient
from yb_darkspirit.interactions.ofd import OfdPrivateClient, OfdSyncStatus
from yb_darkspirit.interface import CashRegister, WorkflowResult
from yb_darkspirit.process.registration import RegistrationProcess
from yb_darkspirit.process.reregistration import ReRegistrationProcess
from yb_darkspirit.core.cash_register.process_states import run_workflow
from yb_darkspirit.process.schemas import RegistrationConfigSchema

REGISTRATION_XSD_FILE = './tests/xsd/KO_ZVLREGKKT_1_800_11_05_04_01.xsd'
ResponseClass = namedtuple('Response', ('status_code', 'content',))


class RegistrationReportStatusResponse(object):
    def __init__(self, status_code, body):
        self.status_code = status_code
        self.body = body

    def json(self):
        return self.body


def _get_registration(session, cr_wrapper):
    return (
        session.query(scheme.Registration)
               .filter_by(cash_register_id=cr_wrapper.cash_register_id)
               .one()
    )


@patch.object(TrackerClient, 'get_last_attachment', Mock(return_value=('content', 'an url')))
@patch.object(pdf_parsing, 'parse_reg_pdf',
              Mock(return_value={'sn': '3820049015521', 'rnm': '12345', 'inn': '111', 'kpp': '222'}))
@patch.object(TrackerClient, 'get_status', Mock(return_value='closed'))
def test_parse_pdf_card(test_client):
    resp = test_client.post(
        '/v1/registrations/parse-registration-card',
        json={
            'ticket': 'SPIRITSUP-00000',
            'cr_serial_number': '3820049015521'
        }
    )
    assert_that(resp.status_code, is_(200))
    assert_that(
        json.loads(resp.data),
        contains_exactly(u'ticket_status', u'file_url', u'last_attachment')
    )


def verify_xml(doc_root, checks):
    for elem_xpath, props in checks.items():
        elem = doc_root.find(elem_xpath)
        assert elem is not None, u'Element {} is not found'.format(elem_xpath)
        if isinstance(props, basestring):
            assert elem.text == props, u'Element {} does not contain text'.format(elem_xpath)
            continue
        for prop_name, prop_val in props.items():
            assert elem.get(prop_name, None) == prop_val, \
                u'Mismatch on attribute {} for element {}'.format(prop_name, elem_xpath)


@pytest.mark.parametrize('is_bso_kkt', [False, True])
def test_create_applications(
        is_bso_kkt,
        cr_wrapper_maker,
        response_maker,
        session,
        yandex_taxi_firm_inn,
        yandex_ofd_inn
):
    """
    Пока схема не создаётся с нуля, нужна касса без записей в t_registration.
    """
    cr_wrapper = cr_wrapper_maker.nonconfigured_fs_ready_fiscal(is_bso_kkt)
    create_applications_response = response_maker.create_applications_response(cr_wrapper, is_bso_kkt)

    registration = _get_registration(session, cr_wrapper)

    assert registration.state == scheme.REGISTRATION_NEW
    # TODO: check state_dt
    assert registration.firm_inn == yandex_taxi_firm_inn
    assert registration.ofd_inn == yandex_ofd_inn
    assert registration.fiscal_storage == cr_wrapper.fiscal_storage
    assert registration.oebs_address_code == cr_wrapper.oebs_address_code
    # TODO: check signer properly
    assert registration.signer is not None

    assert registration.registration_number is None
    assert registration.document is None
    assert registration.end_document is None
    assert registration.parent is None
    assert_that(registration.is_bso_kkt, is_(is_bso_kkt))

    zips_zip = BytesIO(create_applications_response.get_data())
    with zipfile.ZipFile(zips_zip, "r") as zf:
        assert len(zf.infolist()) == 1
        archive_name = zf.infolist()[0].filename
        assert archive_name == u"get_registration_xml_inn_7704340310_Шулейко_{}.zip".format(
            datetime.now().strftime("%d.%m.%Y")
        )

        applications_zip = BytesIO(zf.read(archive_name))
        with zipfile.ZipFile(applications_zip, "r") as applications_zf:
            assert len(applications_zf.infolist()) == 1
            application_name = applications_zf.infolist()[0].filename

            expected_name = 'KO_ZVLREGKKT_{sono_init}_{sono_dest}_{inn}{kpp}_{today}_{n}.xml'.format(
                sono_init=registration.firm.sono_initial,
                sono_dest=registration.firm.sono_destination,
                inn=registration.firm.inn,
                kpp=registration.firm.kpp,
                today=datetime.now().strftime("%Y%m%d"),
                n=registration.cash_register.serial_number,
            )
            assert application_name == expected_name

            application = applications_zf.read(application_name)

            doc = etree.fromstring(application)

            xmlschema_doc = etree.parse(REGISTRATION_XSD_FILE)
            xmlschema = etree.XMLSchema(xmlschema_doc)
            xmlschema.assertValid(doc)

            expected = {
                u'./Документ': {
                    u'ДатаДок': datetime.now().strftime('%d.%m.%Y'),
                    u'КНД': u'1110061',
                    u'КодНО': u'7704',
                },
                u'./Документ/СвНП/НПЮЛ': {
                    u'ИННЮЛ': u'7704340310',
                    u'КПП': u'770501001',
                    u'НаимОрг': u'ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ЯНДЕКС.ТАКСИ"',
                    u'ОГРН': u'5157746192731',
                },
                u'./Документ/Подписант': {
                    u'ПрПодп': u'1',
                },
                u'./Документ/Подписант/ФИО': {
                    u'Имя': u'Даниил',
                    u'Отчество': u'Владимирович',
                    u'Фамилия': u'Шулейко',
                },
                u'./Документ/ЗаявРегККТ': {
                    u'ВидДок': u'1',
                    u'КодНОМУст': u'7704',
                },
                u'./Документ/ЗаявРегККТ/СведРегККТ': {
                    u'МоделККТ': u'РП Система 1ФА',
                    u'МоделФН': u'Шифровальное (криптографическое) средство защиты фискальных данных фискальный накопитель «ФН-1.1М исполнение Ин15-1М»',
                    u'ЗаводНомерККТ': u'00000000381002827555',
                    u'ЗаводНомерФН': u'9999078900003131',
                    u'ПрАвтоматУстр': u'1',
                    u'ПрИнтернет': u'1',
                    u'ПрПлатАгент': u'1' if registration.firm.agent else u'2',
                    u'ПрАвтоном': u'2',
                    u'ПрАзарт': u'2',
                    u'ПрАкцизТовар': u'2',
                    u'ПрБанкПлат': u'2',
                    u'ПрБланк': u'1' if is_bso_kkt else u'2',
                    u'ПрЛотерея': u'2',
                    u'ПрРазвозРазнос': u'2',
                    u'ПрИгорнЗавед': u'2',
                },
                u'./Документ/ЗаявРегККТ/СведРегККТ/СведОФД': {
                    u'ИННЮЛ': u'7704358518',
                    u'НаимОрг': u'ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "ЯНДЕКС.ОФД"',
                },
                u'./Документ/ЗаявРегККТ/СведРегККТ/СведАдрМУст': {
                    u'НаимМУст': u'taxi.yandex.ru',
                },
                u'./Документ/ЗаявРегККТ/СведРегККТ/СведАдрМУст/АдрМУстККТ/АдрФИАС': {
                    u'ИдНом': u'f11ef1b6-b01f-4d4d-8377-4f7b614bb24c',
                    u'Индекс': u'119034',
                },
                u'./Документ/ЗаявРегККТ/СведРегККТ/СведАдрМУст/АдрМУстККТ/АдрФИАС/Регион': u'77',
                u'./Документ/ЗаявРегККТ/СведРегККТ/СведАдрМУст/АдрМУстККТ/АдрФИАС/МуниципРайон': {
                    u'Наим': u'Муниципальный округ Хамовники',
                    u'ВидКод': u'3',
                },
                u'./Документ/ЗаявРегККТ/СведРегККТ/СведАдрМУст/АдрМУстККТ/АдрФИАС/ЭлУлДорСети': {
                    u'Наим': u'Льва Толстого',
                    u'Тип': u'улица',
                },
                u'./Документ/ЗаявРегККТ/СведРегККТ/СведАдрМУст/АдрМУстККТ/АдрФИАС/Здание': {
                    u'Номер': u'16',
                    u'Тип': u'дом',
                },
            }

            verify_xml(doc, expected)


@pytest.mark.parametrize("is_bso_kkt", [False, True])
def test_configure(
        is_bso_kkt,
        cr_wrapper_maker,
        session,
):
    cr_wrapper = cr_wrapper_maker.with_ready_registration(is_bso_kkt)
    registration = _get_registration(session, cr_wrapper)

    assert registration.state == scheme.REGISTRATION_READY
    # TODO: check state_dt
    assert registration.registration_number is not None
    assert_that(registration.is_bso_kkt, is_(is_bso_kkt))
    # TODO: check that DS configures cash register properly


@pytest.mark.parametrize("is_bso_kkt", [False, True])
def test_create_registration_entry(
        is_bso_kkt,
        cr_wrapper_maker,
        test_client,
        yandex_taxi_firm_inn,
        yandex_ofd_inn,
):
    cr_wrapper = cr_wrapper_maker.nonconfigured_fs_ready_fiscal(is_bso_kkt)
    data = {
        'serial_numbers': [cr_wrapper.serial_number],
        'firm_inn': yandex_taxi_firm_inn,
        'ofd_inn': yandex_ofd_inn,
        'is_bso_kkt': is_bso_kkt,
    }
    response = test_client.post(
        '/v1/registrations/create-registration-entry',
        data=json.dumps(data),
        content_type="application/json",
    )
    assert_that(response.status_code, is_(200), response.data)
    regs = json.loads(response.data)
    assert_that(len(regs), is_(1))
    reg = regs[0]
    assert_that(reg, has_entries(
        cash_register_id=cr_wrapper.cash_register_id,
        fiscal_storage_id=cr_wrapper.fiscal_storage.id,
        ofd_inn=yandex_ofd_inn,
        firm_inn=yandex_taxi_firm_inn,
        is_bso_kkt=is_bso_kkt,
    ))


@patch.object(TrackerClient, 'get_last_attachment', Mock(return_value=('content', 'an url')))
@patch.object(TrackerClient, 'get_status', Mock(return_value='closed'))
def test_configure_auto(
        cr_wrapper_with_ready_registration,
        test_client,
        session,
):
    issue = 'ISSUE-42'
    cr_wrapper = cr_wrapper_with_ready_registration

    registration = _get_registration(session, cr_wrapper)
    cr_sn = cr_wrapper.serial_number
    with patch('yb_darkspirit.core.pdf_parsing.parse_reg_pdf') as pdf_mock:
        pdf_mock.return_value = {'sn': cr_sn, 'rnm': registration.registration_number, 'inn': '111', 'kpp': '222'}
        data = {
            'ticket': issue,
            'cr_serial_number': cr_sn
        }
        response = test_client.post(
            '/v1/registrations/configure-auto',
            data=json.dumps(data),
            content_type="application/json",
        )

        assert_that(response.status_code, is_(200), response.data)


@pytest.mark.parametrize("is_bso_kkt", [False, True])
def test_register(
        is_bso_kkt,
        cr_wrapper_maker,
        session,
):
    cr_wrapper = cr_wrapper_maker.with_completed_registration(is_bso_kkt)
    registration = _get_registration(session, cr_wrapper)
    registration_report_dict = (ws_responses.BSO_REGISTRATION_REPORT
                                if is_bso_kkt
                                else ws_responses.DEFAULT_REGISTRATION_REPORT)
    registration_report_document = (
        session.query(scheme.Document)
               .filter_by(fiscal_storage=cr_wrapper.fiscal_storage,
                          fiscal_storage_number=registration_report_dict["id"])
               .one()
    )

    assert registration.state == scheme.REGISTRATION_COMPLETE
    assert registration.start_dt == registration_report_document.dt
    assert registration.document == registration_report_document
    assert registration.document.raw_document == registration_report_document.raw_document
    assert_that(registration.is_bso_kkt, is_(is_bso_kkt))


def test_create_report_application(
        cr_wrapper_with_completed_registration,
        test_client,
        tracker_client,
        fnsreg_client,
):
    issue = 'ISSUE-42'
    cr_wrapper = cr_wrapper_with_completed_registration

    cr_sn = cr_wrapper.serial_number
    fn_sn = '9999078900003131'
    tracker_client.find_reg_report_issue_serial_numbers.return_value = (cr_sn, fn_sn)
    tracker_client.attach_file.return_value = None

    fnsreg_client.generate_registration_report_application.return_value = ResponseClass(
        status_code=200,
        content='It looks like a valid application XML',
    )

    data = {'ticket': issue}
    response = test_client.post(
        '/v1/registrations/create-report-application-auto',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'issue': issue,
            'cr_serial_number': cr_sn,
            'fn_serial_number': fn_sn
        })
    )

    _, kwargs = tracker_client.attach_file.call_args
    assert kwargs['issue_key'] == issue


def test_upload_report_application_auto(
        cr_wrapper_with_completed_registration,
        test_client,
        tracker_client,
        fnsreg_client,
        documents_client,
        session
):
    issue = 'ISSUE-42'
    cr_wrapper = cr_wrapper_with_completed_registration
    cr_sn = cr_wrapper.serial_number
    fn_sn = '9999078900003131'
    application_xml = 'It looks like a valid application XML'
    registration_report_uuid = '4242-test'

    registration = _get_registration(session, cr_wrapper)
    tracker_client.find_reg_report_issue_serial_numbers.return_value = (cr_sn, fn_sn)
    tracker_client.get_last_attachment.return_value = (application_xml, 'application_auto_reg_ISSUE-42.xml')
    fnsreg_client.upload_registration_report_application.return_value = ResponseClass(status_code=200, content=None)
    documents_client.upload_registration_report_application.return_value = {'url': 's3://url'}

    data = {
        'ticket': issue,
        'registration_report_uuid': registration_report_uuid,
        'enable_kkt_validation': True,
    }

    response = test_client.post(
        '/v1/registrations/upload-report-application-auto',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            's3_url': 's3://url',
        })
    )

    fnsreg_client.upload_registration_report_application.assert_called_with(
        application_xml,
        registration_report_uuid=registration_report_uuid,
        kkt_sn=cr_sn,
        model_name=registration.cash_register.type.description,
        enable_kkt_validation=True,
    )

    _, kwargs = documents_client.upload_registration_report_application.call_args
    assert kwargs['cr_sn'] == cr_sn
    assert kwargs['fn_sn'] == fn_sn
    assert kwargs['doc'].read() == application_xml


def test_check_status_on_closed_ticket(
        test_client,
        tracker_client
):
    issue = 'ISSUE-42'
    registration_report_uuid = '4242-test'

    tracker_client.get_status.return_value = 'closed'

    data = {
        'ticket': issue,
        'registration_report_uuid': registration_report_uuid,
    }
    response = test_client.post(
        '/v1/registrations/check-report-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'success',
        })
    )


def test_check_status_rejected_registration(
        test_client,
        tracker_client,
        fnsreg_client
):
    issue = 'ISSUE-42'
    registration_report_uuid = '4242-test'

    tracker_client.get_status.return_value = 'open'
    fnsreg_client.get_registration_report_status.return_value = RegistrationReportStatusResponse(
        status_code=200,
        body={'status': 'rejected'},
    )

    data = {
        'ticket': issue,
        'registration_report_uuid': registration_report_uuid,
    }
    response = test_client.post(
        '/v1/registrations/check-report-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'rejected',
        })
    )


def test_check_status_in_process_registration(
        test_client,
        tracker_client,
        fnsreg_client,
):
    issue = 'ISSUE-42'
    registration_report_uuid = '4242-test'

    tracker_client.get_status.return_value = 'open'
    fnsreg_client.get_registration_report_status.return_value = RegistrationReportStatusResponse(
        status_code=200,
        body={'status': 'in_process'},
    )

    data = {
        'ticket': issue,
        'registration_report_uuid': registration_report_uuid,
    }
    response = test_client.post(
        '/v1/registrations/check-report-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'in_process',
        })
    )


def test_check_status_success_registration(
        test_client,
        tracker_client,
        fnsreg_client,
):
    issue = 'ISSUE-42'
    registration_report_uuid = '4242-test'

    tracker_client.get_status.return_value = 'open'
    tracker_client.attach_file.return_value = None
    tracker_client.close_ticket.return_value = None
    fnsreg_client.get_registration_report_status.return_value = RegistrationReportStatusResponse(
        status_code=200,
        body={
            'status': 'success',
            'documentBase64': 'ZG9j',  # doc
        }
    )

    data = {
        'ticket': issue,
        'registration_report_uuid': registration_report_uuid,
    }
    response = test_client.post(
        '/v1/registrations/check-report-status',
        data=json.dumps(data),
        content_type="application/json",
    )

    assert_that(response.status_code, is_(200), response.data)
    assert_that(
        json.loads(response.data),
        has_entries({
            'status': 'success',
        })
    )

    _, kwargs = tracker_client.attach_file.call_args
    assert kwargs['issue_key'] == issue

    tracker_client.close_ticket.assert_called_with(key=issue)


@pytest.mark.parametrize("is_bso_kkt", [False, True])
def test_create_fiscalization_xml(
        is_bso_kkt,
        cr_wrapper_maker,
        response_maker,
        session,
):
    cr_wrapper = cr_wrapper_maker.with_completed_registration(is_bso_kkt)
    fiscal_response = response_maker.create_fiscalization_xml_response(cr_wrapper)
    registration = _get_registration(session, cr_wrapper)

    zips_zip = BytesIO(fiscal_response.get_data())
    with zipfile.ZipFile(zips_zip, "r") as zf:
        assert len(zf.infolist()) == 1
        archive_name = zf.infolist()[0].filename
        assert archive_name == u"get_fiscalization_xml_inn_7704340310_Шулейко_{}.zip".format(
            datetime.now().strftime("%d.%m.%Y")
        ).encode("utf-8")

        applications_zip = BytesIO(zf.read(archive_name))
        with zipfile.ZipFile(applications_zip, "r") as applications_zf:
            assert len(applications_zf.infolist()) == 1
            application_name = applications_zf.infolist()[0].filename

            filename = registration.cash_register.serial_number.rjust(20, '0')
            assert re.match(filename + '.xml', application_name.encode())

            application = applications_zf.read(application_name)

            doc = etree.fromstring(application)

            expected = {
                u'./Документ': {
                    u'РегНомерККТ': u'0001709626002105',
                    u'ЗаводНомерККТ': u'00000000381002827555',
                    u'ЗначениеКПК': u'2689857838',
                    u'НомерКПК': u'1',
                    u'ДатаФП': u'2017-12-13T17:50:00',
                    u'ТипОтчет': u'1',
                },
            }

            verify_xml(doc, expected)


def test_close_fiscal_mode_due_to_overflow(
        cr_wrapper_with_expired_registration,
        session,
):
    cr_wrapper = cr_wrapper_with_expired_registration
    registration = _get_registration(session, cr_wrapper)
    close_fn_report_document = (
        session.query(scheme.Document)
               .filter_by(fiscal_storage=cr_wrapper.fiscal_storage)
               .order_by(scheme.Document.id.desc())
               .first()
    )

    assert registration.is_expired
    assert registration.end_dt == close_fn_report_document.dt
    assert registration.end_document == close_fn_report_document


def test_close_fiscal_mode_not_called_when_not_needed(
        session,
        ws_mocks,
        cr_wrapper_with_created_fiscalization_xml_response,
):
    cr_wrapper = cr_wrapper_with_created_fiscalization_xml_response
    ws_mocks.cashmachines_status(
        cr_wrapper.long_serial_number,
        reset=True,
        json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD,
        content_type="application/json",
    )

    ds_state_manager = DsStateManager(ChecksManager.from_app(getApplication()))
    with patch.object(ds_state_manager, 'failed_checks_for_ds_state'):
        with patch.object(cr_wrapper.session, 'now') as mock_now:
            mock_now.return_value = REG_START_DT + timedelta(days=1)
            with patch.object(cr_wrapper, 'open_shift') as mock_open_shift:
                with patch.object(cr_wrapper, 'close_fiscal_mode') as mock_close_fiscal_mode:
                    run_workflow(cr_wrapper=cr_wrapper, session=cr_wrapper.session)
                    assert mock_close_fiscal_mode.call_count == 0
                    assert mock_open_shift.call_count == 1


def test_close_fiscal_mode_due_to_urgent_fs_expiration(
        session,
        ws_mocks,
        cr_wrapper_with_created_fiscalization_xml_response,
):
    cr_wrapper = cr_wrapper_with_created_fiscalization_xml_response
    ws_mocks.cashmachines_status(
        cr_wrapper.long_serial_number,
        reset=True,
        json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_URGENT_REPLACEMENT,
        content_type="application/json",
    )
    ws_mocks.cashmachines_close_fiscal_mode(
        cr_wrapper.long_serial_number,
        json=ws_responses.CLOSE_FN_REPORT,
        content_type="application/json"
    )

    ds_state_manager = DsStateManager(ChecksManager.from_app(getApplication()))
    with patch.object(ds_state_manager, 'failed_checks_for_ds_state'):
        # Without time change, fiscal mode would be closed any way despite the urgent status
        with patch.object(cr_wrapper.session, 'now') as mock_now:
            mock_now.return_value = REG_START_DT + timedelta(days=1)
            with patch.object(cr_wrapper, 'close_fiscal_mode', wraps=cr_wrapper.close_fiscal_mode) as wrapped_close:
                run_workflow(cr_wrapper=cr_wrapper, session=cr_wrapper.session)
                assert wrapped_close.call_count == 1
                assert wrapped_close.call_args.kwargs['reason'] == cr_wrapper.CLOSE_FISCAL_MODE_REASON_EXPIRED_URGENT

    registration = _get_registration(session, cr_wrapper)

    assert registration.is_expired


def test_close_fiscal_mode_due_to_close_fs_expiration(
        session,
        ws_mocks,
        cr_wrapper_with_created_fiscalization_xml_response,
):
    cr_wrapper = cr_wrapper_with_created_fiscalization_xml_response
    ws_mocks.cashmachines_status(
        cr_wrapper.long_serial_number,
        reset=True,
        json=ws_responses.CASHMACHINES_STATUS_CR_CLOSE_SHIFT_FS_FISCAL_GOOD,
        content_type="application/json",
    )
    ws_mocks.cashmachines_close_fiscal_mode(
        cr_wrapper.long_serial_number,
        json=ws_responses.CLOSE_FN_REPORT,
        content_type="application/json"
    )

    ds_state_manager = DsStateManager(ChecksManager.from_app(getApplication()))
    with patch.object(ds_state_manager, 'failed_checks_for_ds_state'):
        # Force time to be close to expiry
        with patch.object(cr_wrapper.session, 'now') as mock_now:
            fiscal_validity = cr_wrapper.fiscal_storage.type.validity_days
            mock_now.return_value = REG_START_DT + timedelta(days=(fiscal_validity - 6))
            with patch.object(cr_wrapper, 'close_fiscal_mode', wraps=cr_wrapper.close_fiscal_mode) as wrapped_close:
                run_workflow(cr_wrapper=cr_wrapper, session=cr_wrapper.session)
                assert wrapped_close.call_count == 1
                assert wrapped_close.call_args.kwargs['reason'] == cr_wrapper.CLOSE_FISCAL_MODE_REASON_EXPIRED_TIME_DIFF

    registration = _get_registration(session, cr_wrapper)

    assert registration.is_expired


@pytest.mark.parametrize('is_bso_kkt', [False, True])
@patch.object(OfdPrivateClient, 'check_cash_register_sync_status', Mock(return_value=OfdSyncStatus(success_count=1)))
def test_base_cash_register_info(
        is_bso_kkt,
        test_client,
        cr_wrapper_maker,
        session,
):
    cr_wrapper = cr_wrapper_maker.with_completed_registration(is_bso_kkt)
    resp = test_client.get(
        '/v1/registrations/get-base-cash-register-info/{serial_number}'.format(
            serial_number=cr_wrapper.cash_register.serial_number
        )
    )

    assert_that(resp.status_code, equal_to(200), resp.data)
    assert_that(loads(resp.data), [
        has_key('last_document'),
        has_key('fiscal_storage'),
        has_key('ofd_synced_documents'),
        has_key('documents_count'),
        has_key('cash_register'),
        has_key('has_active_registrations'),
    ])


@pytest.fixture
def process_cash_state():
    with patch('yb_darkspirit.core.cash_register.process_states.process_cash_state') as mock:
        yield mock


def test_run_workflow_for_adding_cash_register_to_ready_to_reregistration_process(
        session,
        cr_wrapper,
        ws_mocks,
        process_cash_state,
):
    process_cash_state.return_value = WorkflowResult.FISCAL_MODE_CLOSED, None
    run_workflow(cr_wrapper=cr_wrapper, session=cr_wrapper.session, start_reregistration_process=True)
    cash_register_process_num = (
        cr_wrapper.session.query(scheme.CashRegisterProcess)
            .filter_by(cash_register_id=cr_wrapper.cash_register.id)
            .filter_by(process=ReRegistrationProcess.name())
            .count()
    )
    assert_that(cash_register_process_num, equal_to(1))


def test_add_cash_registers_to_process(session, cr_wrapper):
    config = {'some_key': 'some_value'}
    with session.begin():
        StartRegistrationProcess._add_cash_registers_to_process(session, [cr_wrapper.cash_register], config)

    assert_that(cr_wrapper.current_process, is_not(None))
    assert_that(cr_wrapper.current_process.process, equal_to(RegistrationProcess.name()))
    assert_that(cr_wrapper.cash_register.ds_state, equal_to(scheme.DsCashState.IN_PROCESS.value))
    assert_that(cr_wrapper.current_process.config_field('some_key'), equal_to(config.get('some_key')))


@patch.object(ChecksManager, 'get_failed_checks_on_ready_for_registration', Mock(return_value=['failed_check']))
def test_filter_cash_registers_filters_bad_cash(session, cr_wrapper):
    checks_manager = ChecksManager.from_app(getApplication())
    cash_registers = StartRegistrationProcess._filter_cash_registers_on_ready_to_registration_check(
        checks_manager, [cr_wrapper.cash_register], 1
    )

    assert_that(cash_registers, empty())


@patch.object(ChecksManager, 'get_failed_checks_on_ready_for_registration', Mock(return_value=[]))
def test_filter_cash_registers_finds_cash_count_cashes(session, cr_wrapper):
    checks_manager = ChecksManager.from_app(getApplication())
    cash_registers_before_filter = [cr_wrapper.cash_register, cr_wrapper.cash_register, cr_wrapper.cash_register]
    cash_registers = StartRegistrationProcess._filter_cash_registers_on_ready_to_registration_check(
        checks_manager, cash_registers_before_filter, 2
    )

    assert_that(cash_registers, contains_exactly(*(cash_registers_before_filter[:2])))


def test_get_cash_registers(session, cr_wrapper_ready_to_registration):
    cash_registers = StartRegistrationProcess._get_cash_registers(
        session, cr_wrapper_ready_to_registration.whitespirit_url,
        cr_wrapper_ready_to_registration.cash_register.sw_version, True
    )

    assert_that(cash_registers, has_length(1))
    assert_that(cash_registers[0].id, equal_to(cr_wrapper_ready_to_registration.cash_register.id))


def test_get_cash_registers_doesnt_fetch_wrong_whitespirit(session, cr_wrapper_ready_to_registration):
    cash_registers = StartRegistrationProcess._get_cash_registers(
        session, cr_wrapper_ready_to_registration.whitespirit_url + 'some_tail',
        cr_wrapper_ready_to_registration.cash_register.sw_version, True
    )

    assert_that(cash_registers, has_length(0))


@pytest.mark.parametrize('sw_version', ['3.5.1', '2000.0.10'])
def test_get_cash_registers_doesnt_fetch_wrong_version_with_is_exact(
    session, cr_wrapper_ready_to_registration, sw_version
):
    cash_registers = StartRegistrationProcess._get_cash_registers(
        session, cr_wrapper_ready_to_registration.whitespirit_url,
        sw_version, True
    )

    assert_that(cash_registers, has_length(0))


@pytest.mark.parametrize('sw_version', ['3.5.1', '3.5.30'])
def test_get_cash_registers_not_exact(
    session, cr_wrapper_ready_to_registration, sw_version
):
    cash_registers = StartRegistrationProcess._get_cash_registers(
        session, cr_wrapper_ready_to_registration.whitespirit_url,
        sw_version, False
    )

    assert_that(cash_registers, has_length(1))
    assert_that(cash_registers[0].id, equal_to(cr_wrapper_ready_to_registration.cash_register.id))


def test_get_cash_registers_not_exact_doesnt_fetch_wrong_version(session, cr_wrapper_ready_to_registration):
    sw_version = '2000.5.30'
    cash_registers = StartRegistrationProcess._get_cash_registers(
        session, cr_wrapper_ready_to_registration.whitespirit_url,
        sw_version, False
    )

    assert_that(cash_registers, has_length(0))


@pytest.mark.parametrize(
    'version_a, version_b, ans', (
        ['1.0.0', '1.1.0', False], ['1.0.1', '1.0.1', True], ['1.0.1', '1.0.0', True], ['2000.0.5', '3.5.9', True],
    )
)
def test_version_comparison(version_a, version_b, ans):
    assert_that(_is_suitable_sw_version(version_a, version_b), equal_to(ans))


@patch.object(ChecksManager, 'get_failed_checks_on_ready_for_registration', Mock(return_value=[]))
def test_start_registration_starts_processes(session, cr_wrapper_ready_to_registration):
    processes = run_start_reg(session, cr_wrapper_ready_to_registration)

    assert_that(processes, has_length(1))
    assert_that(processes[0].cash_register.id, equal_to(cr_wrapper_ready_to_registration.cash_register_id))
    assert_that(cr_wrapper_ready_to_registration.cash_register.ds_state, equal_to(scheme.DsCashState.IN_PROCESS.value))


def test_start_registration_fails_on_no_cashes(session, cr_wrapper_nonconfigured_fs_ready_fiscal):
    with pytest.raises(NotFound):
        run_start_reg(session, cr_wrapper_nonconfigured_fs_ready_fiscal)


def test_start_registration_doesnt_start_processes_that_are_already_in_it(session, cr_wrapper_with_test_process):
    with session.begin():
        cr_wrapper_with_test_process.cash_register.ds_state = scheme.DsCashState.READY_TO_REGISTRATION.value

    with pytest.raises(NotFound):
        run_start_reg(session, cr_wrapper_with_test_process)


def run_start_reg(session, cr_wrapper, extra_data=None):
    # type: (Session, CashRegister, Optional[dict]) -> List[scheme.CashRegisterProcess]
    resource = StartRegistrationProcess()
    schema = StartRegistrationSchema()

    data = form_data(cr_wrapper)
    if extra_data:
        data.update(extra_data)

    with session.begin():
        resource.run(session, schema.load(data).data)

    return session.query(scheme.CashRegisterProcess).filter_by(process=RegistrationProcess.name()).all()


def form_data(cr_wrapper):  # type: (CashRegister) -> dict
    return {
        'whitespirits': {cr_wrapper.whitespirit_url[23: 25]: 1}, 'sw_version': cr_wrapper.cash_register.sw_version,
        'config': {
            'fns_app_version': '', 'firm_inn': cr_wrapper.firm_inn, 'is_bso_kkt': False,
        }
    }


def test_start_registration_initializes_config(session, cr_wrapper_ready_to_registration):
    data = form_data(cr_wrapper_ready_to_registration)
    data['config'] = {
        'fns_app_version': '1234', 'firm_inn': cr_wrapper_ready_to_registration.firm_inn, 'is_bso_kkt': False,
    }

    run_start_reg(session, cr_wrapper_ready_to_registration, extra_data=data)

    process = (
        session.query(scheme.CashRegisterProcess)
        .filter_by(process=RegistrationProcess.name())
        .one()
    )  # type: scheme.CashRegisterProcess

    assert_that(process.data['config'], equal_to(RegistrationConfigSchema().load(data['config']).data.__dict__))
