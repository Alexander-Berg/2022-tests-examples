from data_types.price_item import PriceItem, generate_prices
from data_types.company import Company
from data_types.chain import Chain
from lib.server import server

import rstr


def check_error_report(response, code, localized_message):
    assert('uid' in response)
    assert(response['code'] == code)
    assert(response['message'] == localized_message)


def test_unexistent_file(user, company, feeds_prices):
    price_ids = (price.id for price in feeds_prices)
    resp = server.delete_prices(user, company, price_ids=price_ids) >> 422

    check_error_report(resp, 'ITEM_NOT_FOUND', 'Товар не найден')


def test_bad_access(user, organization_type):
    if organization_type == "company":
        invalid_organization = Company()
    else:
        invalid_organization = Chain()

    resp = server.delete_pricelist(user, invalid_organization) >> 403
    check_error_report(resp, 'PERMISSION_DENIED', 'Ошибка доступа')


def test_too_many_popular_prices(user, organization):
    generate_prices(
        user=user,
        organization=organization,
        count=PriceItem.MAX_POPULAR_COUNT,
        is_popular=True)

    price = PriceItem(is_popular=True)
    resp = server.post_price(user, price, organization) >> 422

    check_error_report(resp, 'MORE_THAN_TEN_POPULAR_ITEMS', 'Загружено более 10 популярных товаров')


def test_edit_external_id(user, company):
    price = PriceItem()
    price = PriceItem.from_json(server.post_price(user, price, company) >> 200)

    price.external_id = rstr.letters(10)
    resp = server.edit_price_raw_data(user, price.id, price.to_json(patch=False), company) >> 422

    check_error_report(resp, 'BAD_ITEM_EXTERNAL_ID', 'ID товара изменён или такой уже есть')


def test_bad_volume(user, company):
    price = PriceItem()
    price.volume['unit'] = 'Other'
    resp = server.post_price(user, price, company) >> 422

    check_error_report(resp, 'BAD_VOLUME_VALUE', 'Неверно указано количество товара')
