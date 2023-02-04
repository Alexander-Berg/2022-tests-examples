import pytest

from balance import offer
from tests.balance_tests.paystep.paystep_common import (create_paysys, create_request,
                            create_client, create_firm,
                            create_country, create_person_category,
                            create_pay_policy, create_currency,
                            create_price_tax_rate,
                            BANK)

from balance import paystep

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]

@pytest.mark.parametrize('request_ns', [offer.RequestNS, paystep.PaystepNS])
def test_base(session, firm, client, currency, request_ns):
    request_ = create_request(session, client=client, firm_id=firm.id)
    person_category = create_person_category(session, country=firm.country, ur=0, resident=1)
    paysys = create_paysys(session, firm=firm, iso_currency=currency.iso_code,
                           currency=currency.char_code, category=person_category.category,
                           group_id=0, payment_method_id=BANK, extern=1, cc='paysys_cc')

    create_pay_policy(session, firm_id=firm.id, legal_entity=0, paymethods_params=[(currency.char_code, BANK)],
                      region_id=firm.country.region_id, service_id=request_.request_orders[0].order.service.id,
                      is_agency=0)
    create_price_tax_rate(session, request_.request_orders[0].order.product, firm.country, currency)
    offer_id, rule_number = offer.process_rules(offer.PaystepNS(request_ns(request_), paysys), get_number_of_rule=True)
    assert offer_id == 0
    assert rule_number is None
