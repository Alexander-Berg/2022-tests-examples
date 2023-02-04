# coding=utf-8
import copy

from btestlib.constants import PaystepPaymentResultText
from simpleapi.common.utils import LuhnNumber
from simpleapi.common.utils import SimpleRandom
from simpleapi.data.defaults import triggers, Status

__author__ = 'fellow'

random = SimpleRandom()

DEFAULT_3DS_PASSWORD = '12345678'
EXPIRED_YEAR = "11"
EXPIRATION_YEAR = "25"
EXPIRATION_YEAR_LONG = "2025"
EXPIRATION_MONTH = "01"


class CardType(object):
    REAL = 'REAL_CARD'


class CardBrand(object):
    MasterCard = 500000
    Visa = 400000
    emu_MasterCard = 510000
    new_api_3ds = 530000


class ProcessingType(object):
    EMULATOR = 'EMULATOR_PROCESSING'
    TEST = 'TEST_PROCESSING'


def join_pan(brand, length=16):
    # stupid python cannot into len(int)
    remainder = 10 ** (length - len(str(brand)) - 1)
    return brand * remainder


class EmulatorProcessing(object):
    @staticmethod
    def base_brand():
        return CardBrand.emu_MasterCard

    card_mask = {
        'cardholder': 'TEST TEST',
        'cvn': '874',
        'expiration_month': EXPIRATION_MONTH,
        'expiration_year': EXPIRATION_YEAR_LONG,
        'descr': 'emulator_card'
    }

    @staticmethod
    def get_card(brand=None, length=16, **params):
        card = EmulatorProcessing.card_mask.copy()
        brand = brand or EmulatorProcessing.base_brand()
        card.update({'card_number': LuhnNumber.get_new_number(join_pan(brand, length))})
        card.update({'type': get_card_type(card)})
        card.update(params)
        return card


class TestProcessing(object):
    @staticmethod
    def base_brand():
        return CardBrand.MasterCard

    card_mask = {
        'cardholder': 'TEST TEST',
        'cvn': '874',
        'expiration_month': EXPIRATION_MONTH,
        'expiration_year': EXPIRATION_YEAR_LONG,
        'descr': 'test_stand_card'
    }

    @staticmethod
    def get_card(brand=None, length=16, **params):
        card = TestProcessing.card_mask.copy()
        brand = brand or TestProcessing.base_brand()
        card.update({'card_number': LuhnNumber.get_new_number(join_pan(brand, length))})
        card.update({'type': get_card_type(card)})
        card.update(params)
        return card


def get_random_fraud_pan():
    return random.choice(['5000009172986887',
                          '5000003092869711',
                          '5000007776560488',
                          '5000006173006269'
                          ])


processing_types = {
    ProcessingType.EMULATOR: EmulatorProcessing(),
    ProcessingType.TEST: TestProcessing()
}

REAL_CARD = {
    'card_number': LuhnNumber.get_new_number(),
    'cardholder': 'TEST TEST',
    'cvn': '874',
    'expiration_month': EXPIRATION_MONTH,
    'expiration_year': EXPIRATION_YEAR_LONG,
    'descr': 'real_card',
}


# В джаве для теста CreditCardValid3DSPaymentTest используется именно такая карта:
ALPHA_PAYSTEP_VISA = {
    "card_number": "4111111111111111",
    "cardholder": "rbs test",
    "cvn": "123",
    "expiration_month": "12",
    "expiration_year": "24",
    "is_3ds": True,
    "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT,
    'type': 'VISA',
}

# пока используется только в тестах на параметр в list_payment_methods
FRAUD_CARD = {
    'card_number': get_random_fraud_pan(),
    'cardholder': 'FRAUD TEST',
    'cvn': '888',
    'expiration_month': EXPIRATION_MONTH,
    'expiration_year': EXPIRATION_YEAR_LONG,
    'type': 'MasterCard'
}


class RBS(object):
    acs_password = '12345678'

    # Примечания:
    # cardholder name - от 2 слов в английской раскладке
    # для всех карт, включенных в 3dsec, пароль на ACS: 12345678
    class Success(object):
        class With3DS(object):
            card_mastecard = {
                "card_number": "5100000000000008",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'veres=y, pares=y',
                "is_3ds": True,
                "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT,
                'type': 'MasterCard',
            }
            card_discover = {
                "card_number": "6011000000000004",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'veres=y, pares=y',
                "is_3ds": True,
                "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT,
                'type': 'Discover',
            }
            card_maestro = {
                "card_number": "639002000000000003",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'veres=y, pares=a',
                "is_3ds": True,
                "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT,
                'type': 'Maestro',
            }

        class Without3DS(object):
            card_mastercard = {
                "card_number": "5555555555555599",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'veres=n',
                "is_3ds": False,
                "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT,
                'type': 'MasterCard',
            }
            card_visa = {
                "card_number": "4444000000001111",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'veres=n',
                "is_3ds": False,
                "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT,
                'type': 'VISA',
            }

    class Failed(object):
        class PaRes(object):
            card_mastercard = {
                "card_number": "5555555555555557",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'veres=y, pares=u',
                "is_3ds": True,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.DECLINED_BY_BANK,
                'type': 'MasterCard',
            }
            card_visa = {
                "card_number": "4444333322221111",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'veres=y, pares=u',
                "is_3ds": True,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.DECLINED_BY_BANK,
                'type': 'VISA',
            }

        class VeRes(object):
            card_visa = {
                "card_number": "4000000000000002",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'veres=u',
                "is_3ds": False,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.TRANSACTION_DECLINED,
                'type': 'VISA',
            }
            card_mastercard = {
                "card_number": "5555555544444442",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'veres=u',
                "is_3ds": False,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.TRANSACTION_DECLINED,
                'type': 'MasterCard',
            }

        class MsgFormat(object):
            card = {
                "card_number": "4444444444444422",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'invalid_message_format(913)',
                "is_3ds": False,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.DECLINED_BY_BANK
            }

        class CardLimitations(object):
            card = {
                "card_number": "4444444444444455",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'card_limitations_exceeded(902)',
                "is_3ds": False,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.BAD_TRANSACTION
            }

        class LimitExceed(object):
            card = {
                "card_number": "4444444444443333",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'limit_exceeded(123)',
                "is_3ds": False,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.TRANSACTIONS_LIMIT_EXCEEDED
            }

        class BlockedByLimit(object):
            card = {
                "card_number": "4444444444446666",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'blocked_by_limit(-20010)',
                "is_3ds": False,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.BAD_TRANSACTION
            }

        class NetworkRefused(object):
            card = {
                "card_number": "4444444411111111",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'network_refused_transaction(5)',
                "is_3ds": False,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.DECLINED_BY_BANK
            }

        class CommError(object):
            card = {
                "card_number": "4444444499999999",
                "cardholder": "RBS TEST",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'tdsec_communication_error(151017)',
                "is_3ds": False,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.TRANSACTION_DECLINED
            }

        class NoFunds(object):
            card = {
                "card_number": "5432543254325430",
                "cardholder": "RBS TEST",
                "cvn": "521",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR_LONG,
                'descr': 'insufficient_funds(116)',
                "is_3ds": False,
                "is_valid": False,
                "payment_result": PaystepPaymentResultText.NOT_ENOUGH_FUNDS
            }

    pool_3ds_cards = [
        Success.With3DS.card_mastecard,
        Success.With3DS.card_maestro,
        Success.With3DS.card_discover,
        Failed.PaRes.card_mastercard,
        Failed.PaRes.card_visa
    ]


class Payture(object):
    class Success(object):
        class With3DS(object):
            card_mastercard = {
                "card_number": "5486732058864471",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'mastercard',
                'type': 'MasterCard',
            }
            card_visa = {
                "card_number": "4111111111111111",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'visa',
                'type': 'VISA',
            }

        class Without3DS(object):
            card_first = {
                "card_number": "4111111111111112",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'success',
                'type': 'MasterCard',
            }
            card_second = {
                "card_number": "4111111111100031",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'success',
                'type': 'VISA',
            }
            card_third = {
                "card_number": "4111111111100023",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'success',
                'type': 'MasterCard',
            }

    class Failed(object):
        class With3DS(object):
            card_first = {
                "card_number": "4111111111111114",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'failed3ds',
            }
            card_second = {
                "card_number": "4111111111111115",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'failed3ds'
            }

        class NoFunds(object):
            card = {
                "card_number": "7000000000000007",
                "cardholder": "payture test",
                "cvn": "521",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'not enough funds'
            }

        # this error will catch on trust, not from payture
        class ExpiredCard(object):
            card = {
                "card_number": "8000000000000008",
                "cardholder": "payture test",
                "cvn": "521",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRED_YEAR,
                'descr': 'expired card'
            }

        class BlackListed(object):
            card = {
                "card_number": "7600000000000006",
                "cardholder": "payture test",
                "cvn": "521",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'card in black list'
            }

        class WrongCard(object):
            card = {
                "card_number": "1234561999999999",
                "cardholder": "payture test",
                "cvn": "374",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'wrong card'
            }

        class AmountExceed(object):
            card = {
                "card_number": "4111101000000046",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'AMOUNT_EXCEED'
            }

        class Issuer(object):
            blocked_card = {
                "card_number": "4111111111100072",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'ISSUER_BLOCKED_CARD'
            }

            card_fail = {
                "card_number": "4111111111100080",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'ISSUER_CARD_FAIL'
            }

        class ProcessingError(object):
            blocking = {
                "card_number": "4111111111100056",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'PROCESSING_ERROR in blocking'
            }

            debit = {
                "card_number": "4111111111100221",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'PROCESSING_ERROR in debit'
            }

            unblocking = {
                "card_number": "4111111111100627",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'PROCESSING_ERROR in unblocking'
            }

            refund = {
                "card_number": "4111111111102029",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'PROCESSING_ERROR in refund'
            }

        # this error will catch on trust, not from payture
        class TimeOut(object):
            blocking = {
                "card_number": "4100401111100062",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'TimeOut in blocking'
            }

            unblocking = {
                "card_number": "4100401111100724",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'TimeOut in unblocking'
            }

            debit = {
                "card_number": "4100401111100328",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'TimeOut in debit'
            }

            refund = {
                "card_number": "4100401111103025",
                "cardholder": "payture test",
                "cvn": "123",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'TimeOut in refund'
            }


class Private(object):
    class Valid(object):
        card_uah = {
            "card_number": "5534960020501739",
            "cardholder": "ukr test",
            "cvn": "247",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Private bank card UAH',
            "is_3ds": False,
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT,
            'type': 'MasterCard',
        }


class ING(object):
    class Valid(object):
        card = {
            "card_number": "4508034508034509",
            "cardholder": "try test",
            "cvn": "000",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'ING bank card TRY',
            "is_3ds": True,
            "3ds_password": 'a',
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT
        }


class Saferpay(object):
    class Valid(object):
        card_rub = {
            "card_number": "9010100052000004",
            "cardholder": "sw test",
            "cvn": "123",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'SW bank card RUB',
            "is_3ds": True,
            "3ds_password": 'a',
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT
        }

        card_chf = {
            "card_number": "9010101052000002",
            "cardholder": "sw test",
            "cvn": "123",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'SW bank card CHF',
            "is_3ds": False,
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT
        }

        card_usd = {
            "card_number": "9010400004000007",
            "cardholder": "sw test",
            "cvn": "123",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'SW bank card USD',
            "is_3ds": True,
            "3ds_password": 'a',
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT
        }

        card_eur = {
            "card_number": "9010500004000004",
            "cardholder": "sw test",
            "cvn": "123",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'SW bank card EUR',
            "is_3ds": True,
            "3ds_password": 'a',
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT
        }


class Ecommpay(object):
    """
    http://docs.ecommpay.com/ru/testing.html#%D0%A2%D0%B5%D1%81%D1%82%D0%BE%D0%B2%D1%8B%D0%B5-%D0%B4%D0%B0%D0%BD%D0%BD%D1%8B%D0%B5
    """

    class Success(object):
        class Without3DS(object):
            card_mastercard = {
                "card_number": "5555555555554444",
                "cardholder": "ECOMMPAY TEST",
                "cvn": "832",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'Ecommpay success card without 3ds',
                'type': 'MasterCard',
            }

    class ProcessingError(object):
        class Without3DS(object):
            card_mastercard = {
                "card_number": "4111111111111111",
                "cardholder": "ECOMMPAY TEST",
                "cvn": "832",
                "expiration_month": EXPIRATION_MONTH,
                "expiration_year": EXPIRATION_YEAR,
                'descr': 'Ecommpay failure card without 3ds'
            }


class Bilderlings(object):
    class Valid(object):
        card_visa1 = {
            "card_number": "4314220000000049",
            "cardholder": "sw test",
            "cvn": "123",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'SW bank card RUB',
            "is_3ds": False,
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT
        }

        card_visa2 = {
            "card_number": "4314220000000056",
            "cardholder": "sw test",
            "cvn": "123",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'SW bank card RUB',
            "is_3ds": True,
            "3ds_password": 'hint',
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT
        }

        card_mastercard1 = {
            "card_number": "5413330000000001",
            "cardholder": "sw test",
            "cvn": "589",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'SW bank card RUB',
            "is_3ds": False,
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT
        }

        card_mastercard2 = {
            "card_number": "5413330000000019",
            "cardholder": "sw test",
            "cvn": "589",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'SW bank card RUB',
            "is_3ds": True,
            "3ds_password": 'hint',
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT
        }


class Prior(object):
    class Valid(object):
        card = {
            "card_number": "5413330000000019",  # номер карты на стороне приора захардкожен
            "cardholder": "sw test",
            "cvn": "589",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'SW bank card RUB',
            "is_3ds": False,
            "3ds_password": 'hint',
            "is_valid": True,
            "payment_result": PaystepPaymentResultText.SUCCESS_PAYMENT
        }


Sberbank = copy.deepcopy(RBS)  # у сбера те же тестовые карты, что и у рбс


class NotEnoughMoney(object):
    card = {
        "card_number": "4408896253205448",
        "cardholder": "Sberbank TEST",
        "cvn": "123",
        "expiration_month": EXPIRATION_MONTH,
        "expiration_year": EXPIRATION_YEAR,
        'descr': 'Not enough money (116)',
        "is_3ds": False,
        "is_valid": False,
        "payment_result": PaystepPaymentResultText.NOT_ENOUGH_FUNDS
    }


class ResponseTimeout(object):
    card = {
        "card_number": "4012888888881881",
        "cardholder": "Sberbank TEST",
        "cvn": "123",
        "expiration_month": EXPIRATION_MONTH,
        "expiration_year": EXPIRATION_YEAR,
        'descr': 'RESPONSE_TIMEOUT(151019)',
        "is_3ds": False,
        "is_valid": False,
    }


class CannotSendRequest(object):
    card = {
        "card_number": "4563960122001999",
        "cardholder": "Sberbank TEST",
        "cvn": "123",
        "expiration_month": EXPIRATION_MONTH,
        "expiration_year": EXPIRATION_YEAR,
        'descr': 'CANNOT_SEND_REQUEST(151018)',
        "is_3ds": False,
        "is_valid": False,
    }


class SpasiboTrue(object):
    card = {
        "card_number": "4276010013296064",
        "cardholder": "Sberbank TEST",
        "cvn": "555",
        "expiration_month": EXPIRATION_MONTH,
        "expiration_year": EXPIRATION_YEAR,
        'descr': "SPASIBO AMOUNT = true",
        "is_3ds": False,
        "is_valid": False,
    }


class SpasiboDisabled(object):
    card = {
        "card_number": "4276010013866254",
        "cardholder": "Sberbank TEST",
        "cvn": "272",
        "expiration_month": EXPIRATION_MONTH,
        "expiration_year": EXPIRATION_YEAR,
        'descr': "SPASIBO AMOUNT = disable",
        "is_3ds": False,
        "is_valid": False,
    }


class ActivityFraud(object):
    card = {
        "card_number": "4276010019663648",
        "cardholder": "Sberbank TEST",
        "cvn": "774",
        "expiration_month": EXPIRATION_MONTH,
        "expiration_year": EXPIRATION_YEAR,
        'descr': "Activity, FRAUD = true",
        "is_3ds": False,
        "is_valid": False,
    }


Sberbank.Failed.NotEnoughMoney = NotEnoughMoney
Sberbank.Failed.ResponseTimeout = ResponseTimeout
Sberbank.Failed.CannotSendRequest = CannotSendRequest

Sberbank.Success.SpasiboTrue = SpasiboTrue
Sberbank.Success.SpasiboDisabled = SpasiboDisabled
Sberbank.Failed.ActivityFraud = ActivityFraud


class Tinkoff(object):
    class Valid(object):
        card_visa = {
            "card_number": "4300000000000777",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Visa - success',
        }

        card_mastercard = {
            "card_number": "5000000000000777",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Mastercard - success',
        }

    class NotEnoughMoneyIfGreaterThan1000(object):
        card_visa = {
            "card_number": "4000000000000002",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Visa - not enough money if greater than 1000',
        }

        card_mastercard = {
            "card_number": "5000000000000074",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Mastercard - not enough money if greater than 1000',
        }

    class ClearingError05(object):
        card_visa = {
            "card_number": "4000000000000010",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Visa - clearing error 05',
        }

        card_mastercard = {
            "card_number": "5000000000000017",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Mastercard - clearing error 05',
        }

    class NotEnoughMoney(object):
        card_visa = {
            "card_number": "4000000000000036",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Visa - not enough money',
        }

        card_mastercard = {
            "card_number": "5000000000000009",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Mastercard - not enough money',
        }

    class CancelError76(object):
        card_visa = {
            "card_number": "4000000000000044",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Visa - ',
        }

        card_mastercard = {
            "card_number": "5000000000000025",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Mastercard - ',
        }

    class TimeoutInternalError(object):
        card_visa = {
            "card_number": "4000000000000028",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Visa - timeout internal error',
        }

        card_mastercard = {
            "card_number": "5000000000000033",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Mastercard - timeout internal error',
        }

    class IncorrectCardNumber(object):
        card_visa = {
            "card_number": "4000000000000051",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Visa - incorrect card number',
        }

        card_mastercard = {
            "card_number": "5000000000000041",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Mastercard - incorrect card number',
        }

    class ExpiredCard(object):
        card_visa = {
            "card_number": "4000000000000069",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRED_YEAR,
            'descr': 'Visa - card expired',
        }

        card_mastercard = {
            "card_number": "5000000000000066",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRED_YEAR,
            'descr': 'Mastercard - card expired',
        }

    class RBSInternalError(object):
        card_visa = {
            "card_number": "4000000000000077",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Visa - internal error',
        }

        card_mastercard = {
            "card_number": "5000000000000058",
            "cardholder": "Tinkoff Test",
            "cvn": "874",
            "expiration_month": EXPIRATION_MONTH,
            "expiration_year": EXPIRATION_YEAR,
            'descr': 'Mastercard - internal error',
        }


class CVN(object):
    base_success = triggers[Status.base_success].cvn
    force_3ds = triggers[Status.force_3ds].cvn
    not_enough_funds_RC51 = triggers[Status.not_enough_funds_RC51].cvn
    do_not_honor_RC05 = triggers[Status.do_not_honor_RC05].cvn
    error_RC06 = triggers[Status.error_RC06].cvn
    invalid_transaction_RC12 = triggers[Status.invalid_transaction_RC12].cvn
    restricted_card_RC36 = triggers[Status.restricted_card_RC36].cvn
    transaction_not_permitted_RC57 = triggers[Status.transaction_not_permitted_RC57].cvn
    transaction_not_permitted_RC58 = triggers[Status.transaction_not_permitted_RC58].cvn
    restricted_card_RC62 = triggers[Status.restricted_card_RC62].cvn


# user can use card number with separators
def get_card_with_separator(card, separator):
    new_card = card.copy()
    old_pan = new_card['card_number']
    new_pan = ''
    for i in range(0, 16, 4):
        new_pan += old_pan[i:i + 4]
        if i != 12:
            new_pan += separator
    new_card['card_number'] = new_pan
    new_card['descr'] += "_with_separator_'{!r}".format(separator)
    return new_card


def get_masked_number(number):
    return number[: 6] + '****' + number[-4:]


def is_masked_number(number):
    return '****' in number


def get_masked_number_format(number):
    return u'•••• ' + number[-4:]


def current_processing_type_string():
    import os

    return os.environ.get('PROCESSING_TYPE', ProcessingType.TEST)


def current_processing_type():
    return processing_types.get(current_processing_type_string(), TestProcessing())


def get_card(processing_type=current_processing_type(), brand=None, length=16, **params):
    return processing_type.get_card(brand=brand, length=length, **params)


def get_card_type(card):
    if card['card_number'].startswith('4'):
        return 'VISA'
    elif card['card_number'].startswith('6'):
        return 'Maestro'
    return 'MasterCard'
