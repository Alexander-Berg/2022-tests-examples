from balance.son_schema import partners

from tests.balance_tests.place.common import *  # noqa


def test_serialization(session, place, client, distribution_tag, page_data):
    s = partners.PlaceSchema().dump(place).data
    assert s['client'] == client.id
    assert s['url'] == place.url
    assert s['version_id'] == place.version_id
    assert s['tag']['id'] == distribution_tag.id
    assert s['tag']['name'] == distribution_tag.name
    product = [p for p in s['products'] if p['page_id'] == page_data.page_id][0]
    assert product['nds'] == page_data.nds
