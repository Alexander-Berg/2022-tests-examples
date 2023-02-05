import maps.automotive.libs.large_tests.lib.db as db
import rstr
import datetime

import lib.sprav as sprav
import lib.async_processor as async_processor

from data_types.price_item import PriceItem
from lib.server import server
from lib.import_file import generate_yml_body


def get_photo_author(photo_id):
    with db.get_connection() as conn:
        cur = conn.cursor()
        cur.execute(
            """
            SELECT author_uid FROM photos
            WHERE id = %s
            """,
            (photo_id,))
        return cur.fetchone()[0]


def get_group_author(group_id):
    with db.get_connection() as conn:
        cur = conn.cursor()
        cur.execute(
            """
            SELECT author_uid FROM groups
            WHERE id = %s
            """,
            (group_id,))
        return cur.fetchone()[0]


def parse_datetime(datetime_str):
    return datetime.datetime.fromisoformat(datetime_str.replace('Z', '+00:00').replace('000+00:00', '+00:00'))


def check_item_creation_record(record, price_item, user):
    assert record['item_id'] == price_item.id
    assert record['author_uid'] == int(user.uid)

    diff = record['diff']
    price_json = price_item.to_json()

    for key, value in price_json.items():
        if key == 'group':
            continue
        if key == 'photos':
            assert diff[key] == [int(photo_id) for photo_id in value]
            continue

        assert diff[key] == value

    fields = [
        'group_id',
        'last_edit_time',
        'last_status_change_time',
        'moderation_message',
        'organization_id',
        'source',
        'status'
    ]
    for field in fields:
        assert field in diff
    if price_item.photos is not None:
        assert 'photos' in diff

    if isinstance(diff['last_edit_time'], str):
        EPOCH_START = datetime.datetime.fromtimestamp(0, datetime.timezone.utc)
        assert parse_datetime(diff['last_edit_time']) != EPOCH_START
    else:
        old = parse_datetime(diff['last_edit_time']['old'])
        new = parse_datetime(diff['last_edit_time']['new'])
        assert old <= new


def test_photo_author(user):
    url = 'http://example.com'
    response = server.post_photos_url(user, url) >> 200
    photo_id = response['id']

    assert get_photo_author(photo_id) == int(user.uid)


def test_photo_author_persists(user, alien_user):
    url = 'http://example.com'
    response = server.post_photos_url(user, url) >> 200
    photo_id = response['id']

    response = server.post_photos_url(alien_user, url) >> 200
    reloaded_photo_id = response['id']

    assert photo_id == reloaded_photo_id
    assert get_photo_author(photo_id) == int(user.uid)


def test_group_author(user, company):
    price_item = PriceItem(group={'name': 'elephants'})
    response_json = server.post_price(user, price_item, company) >> 200
    result_price_item = PriceItem.from_json(response_json)
    group_id = result_price_item.group['id']

    assert get_group_author(group_id) == int(user.uid)


def test_group_author_persists(user, company, alien_user, alien_company):
    price_item = PriceItem(group={'name': 'elephants'})
    server.post_price(user, price_item, company) >> 200

    response_json = server.post_price(alien_user, price_item, alien_company) >> 200
    result_price_item = PriceItem.from_json(response_json)
    group_id = result_price_item.group['id']

    assert get_group_author(group_id) == int(user.uid)


def test_item_history_added_on_creation(user, company):
    url = 'http://example.com/photo1.jpg'
    first_photo_id = (server.post_photos_url(user, url) >> 200)['id']

    price_item = PriceItem(photos=[first_photo_id])
    response_json = server.post_price(user, price_item, company) >> 200
    price_item = PriceItem.from_json(response_json)

    history = price_item.get_history_from_db()

    assert len(history) == 1

    record = history[0]
    check_item_creation_record(record, price_item, user)
    assert record['diff']['photos'] == [int(first_photo_id)]


def test_item_history_added_on_edit(user, company, alien_user):
    sprav.add_company_permission(company, alien_user)

    price_item = PriceItem()
    response_json = server.post_price(user, price_item, company) >> 200
    price_item = PriceItem.from_json(response_json)
    old_title = price_item.title

    price_item.title = rstr.letters(5, 20)
    response_json = server.edit_price(alien_user, price_item, company) >> 200
    price_item = PriceItem.from_json(response_json)
    new_title = price_item.title

    history = price_item.get_history_from_db()

    assert len(history) == 2

    record = history[1]
    assert record['item_id'] == price_item.id
    assert record['author_uid'] == int(alien_user.uid)
    assert record['diff']['title'] == {
        'old': old_title,
        'new': new_title,
    }
    edit_time = record['diff']['last_edit_time']
    assert parse_datetime(edit_time['old']) <= parse_datetime(edit_time['new'])


def test_item_history_added_on_photo_edit(user, company):
    price_item = PriceItem()
    response_json = server.post_price(user, price_item, company) >> 200
    price_item = PriceItem.from_json(response_json)

    url = 'http://example.com/photo1.jpg'
    first_photo_id = (server.post_photos_url(user, url) >> 200)['id']
    price_item.photos = [first_photo_id]
    server.edit_price(user, price_item, company) >> 200

    url = 'http://example.com/photo2.jpg'
    second_photo_id = (server.post_photos_url(user, url) >> 200)['id']
    price_item.photos = [second_photo_id]
    server.edit_price(user, price_item, company) >> 200

    history = price_item.get_history_from_db()
    assert len(history) == 3

    assert 'photos' not in history[0]['diff']
    assert history[1]['diff']['photos'] == [int(first_photo_id)]
    assert history[2]['diff']['photos'] == [int(second_photo_id)]


def test_item_history_added_on_delete(user, company):
    price_item = PriceItem()
    response_json = server.post_price(user, price_item, company) >> 200
    price_item = PriceItem.from_json(response_json)
    server.delete_pricelist(user, company) >> 200

    history = price_item.get_history_from_db()
    assert len(history) >= 2

    delete_record = history[1]
    assert delete_record['diff']['status']['new'] == 'Deleted'


def test_item_history_added_on_file_import(user, company):
    yml_fileobj = generate_yml_body(1)

    server.post_import_file(user, rstr.urlsafe(1, 20), company, yml_fileobj) >> 200

    async_processor.perform_all_work()
    assert (server.get_import_file_status(user, company) >> 200)['status'] == 'Processed'

    prices = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    for price_item in prices:
        history = price_item.get_history_from_db()
        assert len(history) >= 1
        check_item_creation_record(history[0], price_item, user)
