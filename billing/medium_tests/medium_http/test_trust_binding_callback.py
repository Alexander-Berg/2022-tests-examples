import httpretty
import pytest
import json

from tests import object_builder as ob


@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.parametrize('is_allowed', [True, False])
def test_config(medium_http, session,is_allowed):
    invoice = ob.InvoiceBuilder.construct(session)
    httpretty.register_uri(
        httpretty.GET,
        'https://trust-payments-dev.paysys.yandex.net:8028/trust-payments/v2/bindings/123',
        json.dumps(
            {
                'status': 'success',
                'purchase_token': 12234234,
                'orders': [{'order_id': invoice.id}],
                'developer_payload': json.dumps(
                    {'service_notification_url': 'https://check_responce_trust_binding_callback',
                     'service_payload': 234})
            })
    )
    httpretty.register_uri(
        httpretty.POST,
        'https://check_responce_trust_binding_callback/',
    )

    if is_allowed:
        session.config.__dict__['SERVICES_ALLOWED_TO_SEND_BINDING_NOTIFICATIONS'] = [invoice.invoice_orders[0].order.service_id]

    medium_http.request("/httpapi/trust_binding_callback", params={'purchase_token': 123,
                                                                   'service_id': invoice.invoice_orders[0].order.service_id})
    if is_allowed:
        assert len(httpretty.latest_requests()) == 3
        request = httpretty.last_request()
        assert request.parsed_body == {'payload': ['234'],
                                       'purchase_token': ['12234234'],
                                       'request_id': [str(invoice.request_id)]}
    else:
        assert len(httpretty.latest_requests()) == 1
