import pytest

from billing.yandex_pay.yandex_pay.core.entities.enums import CardNetwork, TSPType


@pytest.mark.parametrize('trust_string, expected', (
    ('VISA', CardNetwork.VISA),
    ('MasterCARD', CardNetwork.MASTERCARD),
    ('MasterCard', CardNetwork.MASTERCARD),
    ('Mir', CardNetwork.MIR),
    ('VisaElectron', CardNetwork.VISAELECTRON),
    ('Maestro', CardNetwork.MAESTRO),
    ('MastercardeLite', CardNetwork.MASTERCARD),
    ('AMERicANEXPRESS', CardNetwork.AMEX),
    ('DisCOVER', CardNetwork.DISCOVER),
    ('JCB', CardNetwork.JCB),
    ('Unionpay', CardNetwork.UNIONPAY),
    ('UZCARD', CardNetwork.UZCARD),
    ('Unknown_card', CardNetwork.UNKNOWN),
))
def test_case_insensitive_converting_card_network_from_trust_string(trust_string, expected):
    assert CardNetwork.from_trust_string(trust_string) == expected


@pytest.mark.parametrize('tsp_type, expected', (
    (TSPType.MASTERCARD, CardNetwork.MASTERCARD),
    (TSPType.VISA, CardNetwork.VISA),
    ('unknown_tsp_type', CardNetwork.UNKNOWN),
))
def test_converting_card_network_from_tsp_type(tsp_type, expected):
    assert CardNetwork.from_tsp_type(tsp_type) == expected
