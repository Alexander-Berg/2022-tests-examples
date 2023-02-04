from balance.mapper.orders import get_printable_docs_func
from tests import object_builder as ob
from balance import mapper
import pytest


def create_invoice(client, orders, paysys_id, quantity=100, overdraft=0):
    return ob.InvoiceBuilder(
        overdraft=overdraft,
        paysys=ob.Getter(mapper.Paysys, paysys_id),
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(quantity=quantity, order=order)
                    for order in orders
                ]
            )
        )
    ).build(client.session).obj


@pytest.mark.parametrize('is_docs_separated', [True, False])
@pytest.mark.parametrize('is_docs_detailed', [True, False])
def test_client(session, is_docs_separated, is_docs_detailed):
    client = ob.ClientBuilder.construct(session)
    client.is_docs_separated = is_docs_separated
    client.is_docs_detailed = is_docs_detailed
    assert get_printable_docs_func(client)(client) == (is_docs_detailed, is_docs_separated)


@pytest.mark.parametrize('is_docs_separated', [True, False])
@pytest.mark.parametrize('is_docs_detailed', [True, False])
@pytest.mark.parametrize('w_agencies_printable_doc_types', [
    True,
    False])
def test_agency(session, is_docs_separated, is_docs_detailed, w_agencies_printable_doc_types):
    agency = ob.ClientBuilder.construct(session, is_agency=True)
    client = ob.ClientBuilder.construct(session)
    agency.is_docs_separated = is_docs_separated
    agency.is_docs_detailed = is_docs_detailed
    if w_agencies_printable_doc_types:
        client.agencies_printable_doc_types = {str(agency.id): (True, True)}
        assert get_printable_docs_func(agency)(client) == [True, True]
    else:
        assert get_printable_docs_func(agency)(client) == (is_docs_detailed, is_docs_separated)
