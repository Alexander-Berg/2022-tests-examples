# coding: utf-8
__author__ = 'a-vasin'

from collections import namedtuple

from btestlib import utils


class CMNds(utils.ConstantsContainer):
    _CMNds = namedtuple('_CMNds', 'name, pct')
    constant_type = _CMNds

    NDS_18 = _CMNds(u'nds_18', 18)
    NDS_20 = _CMNds(u'nds_20', 20)
    NDS_10 = _CMNds(u'nds_10', 10)
    NDS_0 = _CMNds(u'nds_0', 0)
    NDS_NONE = _CMNds(u'nds_none', 0)
    NDS_18_118 = _CMNds(u'nds_18_118', 18)
    NDS_10_110 = _CMNds(u'nds_10_110', 10)
    NDS_20_120 = _CMNds(u'nds_20_120', 20)


NDS_BY_NAME = {c.name: c for c in CMNds.values() if isinstance(c, tuple)}


class State(object):
    OFFLINE = u'OFFLINE'
    OPEN_SHIFT = u'OPEN_SHIFT'
    CLOSE_SHIFT = u'CLOSE_SHIFT'
    NONCONFIGURED = u'NONCONFIGURED'
    FATAL_ERROR = u'FATAL_ERROR'
    NOT_MINE = u'NOT_MINE'
    OVERDUE_OPEN_SHIFT = u'OVERDUE_OPEN_SHIFT'


class PaymentType(utils.ConstantsContainer):
    constant_type = unicode

    CARD = u'card'
    EMONEY = u'emoney'
    EMONEY_OTHER = u'emoney_other'
    PREPAYMENT = u'prepayment'
    CREDIT = u'credit'
    EXTENSION = u'extension'


class ReceiptType(utils.ConstantsContainer):
    _ReceiptType = namedtuple("_ReceiptType", "id, name")
    constant_type = unicode

    INCOME = _ReceiptType(1, u'income')
    RETURN_INCOME = _ReceiptType(2, u'return_income')
    CHARGE = _ReceiptType(3, u'charge')
    RETURN_CHARGE = _ReceiptType(4, u'return_charge')


class TaxationType(utils.ConstantsContainer):
    constant_type = unicode

    OSN = u'OSN'
    USN_I = u'USN_income'
    USN_IMC = u'USN_income_minus_charge'
    ESN_CI = u'ESN_calc_income'
    ESN_A = u'ESN_agriculture'
    PATENT = u'patent'


class AgentType(utils.ConstantsContainer):
    constant_type = unicode

    NONE_AGENT = u'none_agent'
    PAYMENT_BANK_AGENT = u'payment_bank_agent'
    PAYMENT_BANK_SUBAGENT = u'payment_bank_subagent'
    PAYMENT_AGENT = u'payment_agent'
    PAYMENT_SUBAGENT = u'payment_subagent'
    CONFIDANT_AGENT = u'confidant_agent'
    COMMISSION_AGENT = u'commission_agent'
    AGENT = u'agent'


class PaymentTypeType(utils.ConstantsContainer):
    constant_type = unicode

    PREPAYMENT = u'prepayment'
    FULL_PREPAYMENT_WO_DELIVERY = u'full_prepayment_wo_delivery'
    IP_PAYMENT = u'ip_payment'
    PARTIAL_PREPAYMENT_WO_DELIVERY = u'partial_prepayment_wo_delivery'
    FULL_PAYMENT_W_DELIVERY = u'full_payment_w_delivery'
    PARTIAL_PAYMENT_W_DELIVERY = u'partial_payment_w_delivery'
    CREDIT_W_DELIVERY = u'credit_w_delivery'
    CREDIT_AFTER_DELIVERY = u'credit_after_delivery'


class WorkMode(utils.ConstantsContainer):
    constant_type = unicode

    CRYPT = u'crypt'
    AUTONOMOUS = u'autonomous'
    AUTOMATIC = u'automatic'
    IN_SERVICE_USAGE = u'in_service_usage'
    BSO = u'BSO'
    INTERNET_USAGE = u'internet_usage'


class Origin(utils.ConstantsContainer):
    constant_type = unicode

    ONLINE = u'online'
    KKT_FN = u'kkt_fn'
    FN = u'fn'


class DocumentType(utils.ConstantsContainer):
    constant_type = unicode

    REGISTRATION = u'RegistrationReport'
    RE_REGISTRATION = u'ReRegistrationReport'
    SHIFT_OPEN = u'ShiftOpenReport'
    SHIFT_CLOSE = u'ShiftCloseReport'
    RECEIPT = u'Receipt'
    BSO = u'BSO'
    CORRECTION_RECEIPT = u'CorrectionReceipt'
    CORRECTION_BSO = u'CorrectionBSO'
    CLOSE_FN = u'CloseFNReport'
    CURRENT = u'CurrentReport'


class Group(utils.ConstantsContainer):
    constant_type = unicode

    DEFAULT = u'_NOGROUP'
    RZHD = u'RZHD'
