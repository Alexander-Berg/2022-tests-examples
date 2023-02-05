import lib.async_processor as async_processor
from data_types.price_item import PriceItem, MAX_TITLE_LENGTH
from data_types.synchronization import (
    Synchronization,
    generate_sync,
    upload_items_yml_to_s3,
    upload_yml_body_to_s3,
    generate_items,
    generate_sync_and_upload,
    process_sync
)
from lib.import_file import (
    generate_yml_body
)
from lib.server import server

from io import SEEK_END

import random
import rstr


def test_sync_illformed_url(user, company):
    url = f'https://example.com/{rstr.letters(25)}'
    sync = generate_sync_and_upload(user, company, url, 'Error', allow_failure=True)
    prices_json = server.get_prices(user, company, sync_id=sync.id) >> 200
    assert prices_json['pager']['total'] == 0


def test_sync_illformed_file(user, company):
    items_count = 2
    yml_fileobj = generate_yml_body(items_count)
    yml_fileobj.seek(-10, SEEK_END)
    url = upload_yml_body_to_s3(yml_fileobj)
    sync = generate_sync_and_upload(user, company, url, 'Error', allow_failure=True)
    prices_json = server.get_prices(user, company, sync_id=sync.id) >> 200
    assert prices_json['pager']['total'] == 0


def test_sync_file_with_warnings(user, company):
    items = generate_items()
    items[0]['title'] = 'a' * (MAX_TITLE_LENGTH*2)
    url = upload_items_yml_to_s3(items)
    sync = generate_sync_and_upload(user, company, url, 'Warning')
    prices_json = server.get_prices(user, company, sync_id=sync.id) >> 200
    assert prices_json['pager']['total'] == len(items)
    parse_info = sync.history['items'][0]['parse_info']
    assert parse_info['total_items_count'] == len(items)
    assert parse_info['parsed_items_count'] == len(items)


def test_sync_file_with_errors(user, company):
    items = generate_items(count=random.randint(2, 5))
    items[0]['title'] = ''
    url = upload_items_yml_to_s3(items)
    sync = generate_sync_and_upload(user, company, url, 'Warning')
    prices_json = server.get_prices(user, company, sync_id=sync.id) >> 200
    assert prices_json['pager']['total'] == len(items) - 1
    parse_info = sync.history['items'][0]['parse_info']
    assert parse_info['total_items_count'] == len(items)
    assert parse_info['parsed_items_count'] == len(items) - 1


def test_sync_treat_files_without_valid_prices_as_invalid(user, company):
    items = generate_items(count=1)
    items[0]['title'] = ''
    url = upload_items_yml_to_s3(items)
    sync = generate_sync_and_upload(user, company, url, 'Error')
    prices_json = server.get_prices(user, company, sync_id=sync.id) >> 200
    assert prices_json['pager']['total'] == 0
    parse_info = sync.history['items'][0]['parse_info']
    assert parse_info['total_items_count'] == 1
    assert parse_info['parsed_items_count'] == 0


def test_sync_overrides_items(user, company):
    sync = generate_sync(user, company, url=None)

    for items_count in [2, 1, 0]:
        items = generate_items(count=items_count)
        url = upload_items_yml_to_s3(items)
        sync.url = url
        sync = Synchronization.from_json(server.edit_synchronization(user, company, sync) >> 200)
        server.force_synchronization(user, company, sync_id=sync.id) >> 200
        async_processor.perform_work(threads=['PricesSynchronizer'])

        assert len(
            PriceItem.list_from_json(server.get_prices(user, company, sync_id=sync.id) >> 200)
        ) == items_count


def test_sync_items_with_duplicated_external_id(user, company):
    items = generate_items()
    for i in items:
        i['id'] = 'same-id'

    url = upload_items_yml_to_s3(items)
    sync = generate_sync_and_upload(user, company, url, 'Warning')
    prices_json = server.get_prices(user, company, sync_id=sync.id, limit=len(items)) >> 200
    assert prices_json['pager']['total'] == 1
    parse_info = sync.history['items'][0]['parse_info']
    assert parse_info['total_items_count'] == len(items)
    assert parse_info['parsed_items_count'] == 1


def test_sync_changes_info(user, company):
    items = generate_items()
    items[0]['title'] = 'before'
    key = rstr.letters(20)
    url_before = upload_items_yml_to_s3(items, key=key)
    sync = generate_sync_and_upload(user, company, url_before)

    changes_info = sync.history['items'][0]['changes_info']
    assert changes_info['added_items_count'] == len(items)
    assert changes_info['changed_items_count'] == 0
    assert changes_info['deleted_items_count'] == 0

    items[0]['title'] = 'after'
    url_after = upload_items_yml_to_s3(items, key=key)
    assert url_before == url_after
    server.force_synchronization(user, company, sync_id=sync.id) >> 200
    sync = process_sync(user, company, sync)
    changes_info = sync.history['items'][0]['changes_info']
    assert changes_info['added_items_count'] == 0
    assert changes_info['changed_items_count'] == 1
    assert changes_info['deleted_items_count'] == 0

    url_after = upload_items_yml_to_s3([], key=key)
    assert url_before == url_after
    server.force_synchronization(user, company, sync_id=sync.id) >> 200
    sync = process_sync(user, company, sync)
    changes_info = sync.history['items'][0]['changes_info']
    assert changes_info['added_items_count'] == 0
    assert changes_info['changed_items_count'] == 0
    assert changes_info['deleted_items_count'] == len(items)
