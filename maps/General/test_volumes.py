import rstr

from data_types.price_item import PriceItem
from lib.server import server
from lib.random import printable_str


def make_volume(vol_value, vol_unit):
    result = {}

    if vol_value:
        result.update({'value': vol_value})
    if vol_unit:
        result.update({'unit': vol_unit})

    return result


def test_upload_price_with_empty_volume(user, company):
    price = PriceItem()
    price.volume = None
    resp_price = server.post_price(user, price, company) >> 200

    result_price_item = PriceItem.from_json(resp_price)

    assert price == result_price_item


def test_upload_bad_volumes(user, company):
    random_string = rstr.letters(5, 10)

    price = PriceItem(volume=make_volume(random_string, 'Kilogram'))
    server.post_price(user, price, company) >> 422

    price = PriceItem(volume=make_volume(1.5, random_string))
    server.post_price(user, price, company) >> 422

    price = PriceItem(volume={})
    server.post_price(user, price, company) >> 422


def test_string_value_allowed_only_with_other_type(user, company):
    price1 = PriceItem(volume=make_volume(10.5, 'Kilogram'))
    server.post_price(user, price1, company) >> 200

    random_string = printable_str(5, 10)
    price2 = PriceItem(volume=make_volume(random_string, 'Kilogram'))
    server.post_price(user, price2, company) >> 422


def test_set_or_change_other_units_from_api(user, company):
    price = PriceItem()
    price.volume = make_volume(printable_str(5, 10), 'Other')
    server.post_price(user, price, company) >> 422

    price.saveToDb(company)

    price.volume['value'] += printable_str(2, 5)
    server.edit_price(user, price, company) >> 422

    price = PriceItem()
    resp_price = server.post_price(user, price, company) >> 200
    posted_price_item = PriceItem.from_json(resp_price)

    posted_price_item.volume['unit'] = 'Other'
    server.edit_price(user, posted_price_item, company) >> 422


def test_change_price_with_other_units(user, company):
    price = PriceItem()
    price.volume = make_volume(printable_str(5, 10), 'Other')
    price.saveToDb(company)

    price.title = printable_str(5, 20)
    server.edit_price(user, price, company) >> 200


def test_return_price_with_other_units(user, company):
    price = PriceItem()
    price.volume = make_volume(printable_str(5, 10), 'Other')
    price.saveToDb(company)

    response_json = server.get_prices_by_id(user, company, id=price.id) >> 200
    items = response_json['items']
    assert len(items) == 1
    response_price = PriceItem.from_json(items[0])
    assert price == response_price
    assert type(response_price.volume['value']) == str
