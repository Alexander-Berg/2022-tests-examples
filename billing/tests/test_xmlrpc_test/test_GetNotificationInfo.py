from balance import constants

from tests import object_builder as ob


def test_get_notifications_info(session, test_xmlrpc_srv):
    order = ob.OrderBuilder.construct(session, service_id=constants.DIRECT_SERVICE_ID,
                                      product_id=constants.DIRECT_PRODUCT_ID)
    result = test_xmlrpc_srv.GetNotificationInfo(constants.NOTIFY_ORDER_OPCODE, order.id)
    assert result['args'][0]['ServiceOrderID'] == order.service_order_id
    assert result['args'][0]['ServiceID'] == constants.DIRECT_SERVICE_ID
