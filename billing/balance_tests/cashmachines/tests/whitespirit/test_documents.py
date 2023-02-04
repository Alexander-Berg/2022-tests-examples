# coding: utf-8
__author__ = 'a-vasin'

import pytest

from hamcrest import instance_of, any_of, only_contains, has_entry, contains_string, anything
from hamcrest.core.base_matcher import BaseMatcher

import cashmachines.whitespirit_steps as steps
from btestlib import reporter
from btestlib.matchers import contains_dicts_equal_to
from cashmachines.data.constants import *
from cashmachines.data.defaults import PRICE, QTY
from simpleapi.matchers.deep_equals import deep_equals_to
from test_receipts import create_receipt_content_template


def test_simple_receipt_document():
    receipt = steps.CMSteps.make_single_receipt(PRICE, QTY)
    sn, doc_number = receipt['kkt']['sn'], receipt['id']

    document = steps.ReceiptSteps.get_document(sn, doc_number)
    expected_document = create_expected_receipt_doc(receipt)
    utils.check_that(document, deep_equals_to(expected_document), u'Проверяем документ для чека')


def test_document_raw_form():
    receipt = steps.CMSteps.make_single_receipt(PRICE, QTY)
    sn, doc_number = receipt['kkt']['sn'], receipt['id']

    document = steps.ReceiptSteps.get_document(sn, doc_number, with_rawform=True)
    utils.check_that(document['rawform'], RawFormMatcher(), u'Проверяем raw form чека')


def test_document_full_form():
    receipt = steps.CMSteps.make_single_receipt(PRICE, QTY)
    sn, doc_number = receipt['kkt']['sn'], receipt['id']

    document = steps.ReceiptSteps.get_document(sn, doc_number, with_fullform=True)
    expected_receipt_params = create_extended_receipt_content_template(receipt)
    utils.check_that(document['fullform'], deep_equals_to(expected_receipt_params), u'Проверяем full form чека')


@pytest.mark.skip
def test_document_ofd_ticket():
    receipt = steps.CMSteps.make_single_receipt(PRICE, QTY)
    sn, doc_number = receipt['kkt']['sn'], receipt['id']

    wait_ofd_ticket(sn, doc_number)
    document = steps.ReceiptSteps.get_document(sn, doc_number, with_ofd_ticket=True)
    expected_document = create_expected_receipt_doc(receipt, with_ofd_ticket=True)
    utils.check_that(document, deep_equals_to(expected_document), u'Проверяем документ для чека с информацией о ОФД')


def test_document_printform():
    receipt = steps.CMSteps.make_single_receipt(PRICE, QTY)
    sn, doc_number = receipt['kkt']['sn'], receipt['id']

    with utils.check_mode(utils.CheckMode.IGNORED):
        response = steps.ReceiptSteps.get_document(sn, doc_number, with_printform=True)
    utils.check_that(response['value'], contains_string(u'Внутренняя ошибка устройство'), u'Проверяем ошибку')


# -------------------------------------
# Utils

def wait_ofd_ticket(sn, doc_number):
    with reporter.step(u'Ждем получения данных из ОФД'):
        utils.wait_until(lambda: steps.ReceiptSteps.get_document(sn, doc_number),
                         has_entry(u"ofd_ticket_received", True), timeout=10, sleep_time=1)


def create_extended_receipt_content_template(receipt):
    receipt_template = create_receipt_content_template(receipt, Origin.KKT_FN)

    receipt_template.update({
        u'receipt_calculated_content': receipt['receipt_calculated_content'],
        u'receipt_content': receipt['receipt_content'],
        u'receipt_extra_content': anything()
    })

    # a-vasin: из кассы достаются некоторые типы с нулями, Кирюха не хочет нулевые удалять
    receipt_template['receipt_calculated_content']['totals'] = contains_dicts_equal_to(
        receipt_template['receipt_calculated_content']['totals'], same_length=False, casted=False)
    receipt_template['receipt_content']['payments'] = contains_dicts_equal_to(
        receipt_template['receipt_content']['payments'], same_length=False, casted=False)

    # a-vasin: этих полей не будет, их нет в ФН
    del receipt_template['ofd']['inn']
    del receipt_template['ofd']['name']
    del receipt_template['receipt_calculated_content']['firm_url']
    del receipt_template['receipt_calculated_content']['qr']
    del receipt_template['receipt_content']['firm_url']
    del receipt_template['kkt']['version']

    return receipt_template


def create_expected_receipt_doc(receipt, with_ofd_ticket=None):
    return utils.remove_empty({
        u'amount': receipt['receipt_calculated_content']['total'],
        u'document_type': DocumentType.RECEIPT,
        u'dt': receipt['dt'],
        u'fp': receipt['fp'],
        u'id': receipt['id'],
        u'ofd_ticket_hex': instance_of(unicode) if with_ofd_ticket else None,
        u'ofd_ticket_received': any_of(False, True),
        u'receipt_type': receipt['receipt_content']['receipt_type']
    })


class RawFormMatcher(BaseMatcher):
    def _matches(self, item):
        return deep_equals_to({
            u'TagID': instance_of(int),
            u'TagType': instance_of(unicode),
            u'Value': any_of(instance_of(int), instance_of(unicode), only_contains(RawFormMatcher()))
        }).matches(item)

    def describe_to(self, description):
        description.append_text('check receipt row form')
