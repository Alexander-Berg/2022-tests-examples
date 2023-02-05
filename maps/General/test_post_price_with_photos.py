from data_types.price_item import PriceItem
from lib.server import server

import random
import rstr


def generate_photos(user, photo_count=None):
    photo_count = photo_count or random.randint(1, 10)
    random_str = rstr.letters(20)
    photo_ids = []
    for i in range(photo_count):
        url = f'http://example.com/{random_str}/{i}'
        photo_json = server.post_photos_url(user, url) >> 200
        photo_ids.append(photo_json['id'])
    return photo_ids


def generate_bad_photo_ids(photos):
    first_bad_id = max([int(photo_id) for photo_id in photos]) + 1
    return [str(first_bad_id + i) for i in range(len(photos))]


def test_upload_price_with_photos(user, company):
    photo_ids = generate_photos(user)

    price_item = PriceItem(photos=photo_ids)
    posted_price = PriceItem.from_json(
        server.post_price(user, price_item, company) >> 200)

    assert posted_price == price_item


def test_upload_price_with_bad_photo(user, company):
    photo_ids = generate_photos(user)

    price_item = PriceItem(photos=photo_ids)
    response_json = server.post_price(user, price_item, company) >> 200
    posted_price = PriceItem.from_json(response_json)

    assert posted_price == price_item

    posted_price.photos = generate_bad_photo_ids(posted_price.photos)
    response_json = server.edit_price(user, posted_price, company) >> 200
    edited_price = PriceItem.from_json(response_json)

    assert edited_price != posted_price
    assert edited_price == price_item


def test_upload_price_with_invalid_photo_id(user, company):
    invalid_photo_id = str(random.randint(1, 1000))
    price_item = PriceItem(photos=[invalid_photo_id])

    response = server.post_price(user, price_item, company) >> 422
    assert response['code'] == 'PHOTO_NOT_FOUND'


def test_update_price_photos(user, company):
    initial_photo_ids = generate_photos(user)
    price_item = PriceItem(photos=initial_photo_ids)
    price_item = PriceItem.from_json(
        server.post_price(user, price_item, company) >> 200)

    price_item.photos = generate_photos(user)
    updated_price_item = PriceItem.from_json(
        server.edit_price(user, price_item, company) >> 200)

    assert updated_price_item == price_item


def test_remove_price_photos(user, company):
    initial_photo_ids = generate_photos(user)
    price_item = PriceItem(photos=initial_photo_ids)
    price_item = PriceItem.from_json(
        server.post_price(user, price_item, company) >> 200)

    price_item.photos = []
    updated_price_item = PriceItem.from_json(
        server.edit_price(user, price_item, company) >> 200)

    assert updated_price_item == price_item
