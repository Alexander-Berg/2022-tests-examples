from balance.mapper import Order, Product
from balance.xmlizer import getxmlizer, xml_text
from tests.balance_tests.xmlizer.xmlizer_common import create_client, create_order, create_passport


def test_order_xmlize(session):
    session = session
    agency = create_client(session, is_agency=1, name="Roga and Kopyta")
    client = create_client(session, agency=agency, name="Kisa")
    order = create_order(session, client=client, text="Test Order", passport_id=create_passport(session),
                         agency=agency)
    session.add(order)
    session.flush()
    xmlized_order = getxmlizer(order).xmlize()
    assert xml_text(order.id) == xmlized_order.find('id').text
    assert xml_text(order.dt) == xmlized_order.find('dt').text
    assert xmlized_order.find('memo').text is None
    assert xml_text(order.text) == xmlized_order.find('text').text
    assert xml_text(order.consume_qty) == xmlized_order.find('consume-qty').text
    assert xml_text(order.consume_sum) == xmlized_order.find('consume-sum').text
    assert xml_text(order.completion_qty) == xmlized_order.find('completion-qty').text
    assert xml_text(order.completion_sum) == xmlized_order.find('completion-sum').text

    assert xml_text(order.client.id) == xmlized_order.find('client/id').text
    assert xml_text(order.client.name) == xmlized_order.find('client/name').text
    assert xml_text(order.client.agency.id) == xmlized_order.find('agency/id').text

    assert xml_text(order.manager.manager_code) == xmlized_order.find('manager/manager-code').text

    assert xml_text(order.service.id) == xmlized_order.find('service/id').text
    assert xml_text(order.service.name) == xmlized_order.find('service/name').text
    assert xml_text(order.service.cc) == xmlized_order.find('service/cc').text
    assert order.service.url_orders == xmlized_order.find('service/url-orders').text

    assert xml_text(order.service_order_id) == xmlized_order.find('service-order-id').text
    assert xml_text(order.passport_id) == xmlized_order.find('passport-id').text

    order = Order(product=session.query(Product).get(1475))
    session.add(order)
    getxmlizer(order).xmlize()
    order.service_id = 555
    order.service_order_id = 5001408
    assert xml_text(5001408) == getxmlizer(order).xmlize().find('service-order-id').text
