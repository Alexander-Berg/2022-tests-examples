import tests.object_builder as ob


def test_EstimateDiscount(xmlrpcserver, session):
    inv = ob.InvoiceBuilder().build(session).obj
    product = inv.invoice_orders[0].product
    service = inv.invoice_orders[0].order.service
    product.service = service
    product.engine_id = service.id
    session.flush()

    xmlrpcserver.EstimateDiscount(
        {'AdjustQty': 0, 'ClientID': inv.client_id, 'PaysysID': inv.paysys_id},
        [{'ProductID': product.id, 'ClientID': inv.invoice_orders[0].client_id, 'Qty': 10, 'ID': 'xxx'}]
    )
