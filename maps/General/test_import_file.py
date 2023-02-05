import lib.async_processor as async_processor
import lib.s3 as s3
import time

from data_types.user import User
from data_types.company import Company
from data_types.price_item import PriceItem, generate_prices, MAX_URL_LENGTH

from lib.import_file import (
    generate_yml_body,
    generate_yml_body_from_items,
    generate_invalid_yml_body,
    generate_excel_body,
    generate_excel_body_from_items,
    MAX_ITEMS_COUNT
)
from lib.random import printable_str
from lib.server import server

import maps.automotive.libs.large_tests.lib.db as db

import pytest
import rstr
import random


SELECT_IMPORT_FILE_BY_PERMALINK = """
SELECT if.id, file_name
    FROM import_files as if
LEFT JOIN organizations as o
    ON if.organization_id = o.id
WHERE o.{} = %s
"""


def post_test_import_file(user, organization, file_type):
    file_body = None
    if file_type == 'xls':
        file_body = generate_excel_body(1)
    elif file_type == 'yml':
        file_body = generate_yml_body(1)
    else:
        raise RuntimeError('Unknown file type')
    server.post_import_file(user, f'test.{file_type}', organization, file_body) >> 200
    async_processor.perform_work("PricesImporter")
    user.forget_about_last_import()


def test_import_file_upload(user, organization):
    yml_fileobj = generate_yml_body(1)

    with async_processor.detached():
        server.post_import_file(user, rstr.urlsafe(1, 20), organization, yml_fileobj) >> 200
        import_status = server.get_import_file_status(user, organization) >> 200
        assert import_status['status'] == 'Waiting'

    permalink = 'company_permalink'
    if organization.organization_type == 'chain':
        permalink = 'chain_permalink'

    with db.get_connection() as conn:
        cur = conn.cursor()
        cur.execute(SELECT_IMPORT_FILE_BY_PERMALINK.format(permalink), (organization.permalink,))
        row = cur.fetchone()
        assert row is not None

        uploaded_file = s3.get_object(row[1])['Body'].read().decode('utf-8')
        yml_fileobj.seek(0)
        assert uploaded_file == yml_fileobj.read().decode('utf-8')


@pytest.mark.parametrize('status', [
                         ('Waiting', 422),
                         ('InProgress', 422),
                         ('Processed', 200),
                         ('FailedToProcess', 200),
                         ('Duplicate', 200)])
def test_import_file_duplicate(user, organization, status):
    yml_fileobj = generate_yml_body(1)

    with async_processor.detached():
        yml_fileobj.seek(0)
        server.post_import_file(user, rstr.urlsafe(1, 20), organization, yml_fileobj) >> 200

        permalink = 'company_permalink'
        if organization.organization_type == 'chain':
            permalink = 'chain_permalink'

        with db.get_connection() as conn:
            cur = conn.cursor()
            cur.execute(SELECT_IMPORT_FILE_BY_PERMALINK.format(permalink),
                        (organization.permalink,))
            row = cur.fetchone()
            assert row is not None

            cur.execute('UPDATE import_files SET import_status = %s WHERE id = %s',
                        (status[0], row[0]))
            conn.commit()
            yml_fileobj.seek(0)
            server.post_import_file(user,
                                    rstr.urlsafe(1, 20),
                                    organization,
                                    yml_fileobj) >> status[1]
            if status[1] == 200:
                cur.execute(SELECT_IMPORT_FILE_BY_PERMALINK.format(permalink),
                            (organization.permalink,))
                row = cur.fetchone()
                assert row is not None

                uploaded_file = s3.get_object(row[1])['Body'].read().decode('utf-8')
                yml_fileobj.seek(0)
                assert uploaded_file == yml_fileobj.read().decode('utf-8')


def test_import_file_status(user, organization):
    server.get_import_file_status(user, organization) >> 422


def test_import_file_status_file_type_yml(user, organization):
    yml_fileobj = generate_yml_body(1)
    server.post_import_file(user, rstr.urlsafe(1, 20), organization, yml_fileobj) >> 200
    import_status = server.get_import_file_status(user, organization) >> 200
    assert import_status['file_type'] == 'Yml'


def test_import_file_status_file_type_excel(user, organization):
    excel_fileobj = generate_excel_body(1)
    server.post_import_file(user, 'test.xls', organization, excel_fileobj) >> 200
    import_status = server.get_import_file_status(user, organization) >> 200
    assert import_status['file_type'] == 'Excel'


def test_import_file_status_file_url(user, organization):
    server.post_import_file(user, 'test.xls', organization, generate_excel_body(1)) >> 200
    import_status = server.get_import_file_status(user, organization) >> 200
    file_url = import_status['file_url']
    assert type(file_url) == str
    assert 'HTTP://s3/maps-geoapp-goods-imports-testing/' in file_url


def test_import_file_history(user, organization):
    post_test_import_file(user, organization, 'yml')
    post_test_import_file(user, organization, 'xls')
    post_test_import_file(user, organization, 'xls')

    import_status = server.get_import_file_status(user, organization) >> 200

    history_items = import_status['history']['items']
    assert len(history_items) == 2
    for history_item in history_items:
        assert history_item['status'] == 'Processed'
        assert 'file_url' in history_item
        assert 'update_time' in history_item
        assert 'message' in history_item
        assert 'is_read' in history_item
        assert 'parse_info' in history_item

    assert import_status['file_type'] == 'Excel'
    assert history_items[0]['file_type'] == 'Excel'
    assert history_items[1]['file_type'] == 'Yml'


def test_import_file_without_history(user, organization):
    post_test_import_file(user, organization, 'yml')
    import_status = server.get_import_file_status(user, organization) >> 200
    history_items = import_status['history']['items']
    assert len(history_items) == 0


def test_import_file_history_limit(user, organization):
    for _ in range(15):
        post_test_import_file(user, organization, 'xls')

    import_status = server.get_import_file_status(user, organization) >> 200
    history_items = import_status['history']['items']
    assert len(history_items) == 10

    import_status = server.get_import_file_status(user, organization, history_limit=5) >> 200
    history_items = import_status['history']['items']
    assert len(history_items) == 5


def generate_company_and_user():
    user = User()
    user.register()
    company = Company.generate(user)
    return company, user


def test_import_per_user_order():
    company1, user1 = generate_company_and_user()
    company2 = Company.generate(user1)
    company3, user2 = generate_company_and_user()
    company4 = Company.generate(user2)

    server.post_import_file(user1, 'test.xls', company1, generate_excel_body(1)) >> 200
    server.post_import_file(user1, 'test.xls', company2, generate_excel_body(1)) >> 200
    server.post_import_file(user2, 'test.xls', company3, generate_excel_body(1)) >> 200
    server.post_import_file(user2, 'test.xls', company4, generate_excel_body(1)) >> 200

    with db.get_connection() as conn:
        def get_files_info():
            cur = conn.cursor()
            cur.execute("""
                SELECT
                    author_uid,
                    EXTRACT(epoch from last_state_time)
                FROM import_files
                WHERE import_status = 'Processed'
                ORDER BY last_state_time ASC
            """)
            return [(str(row[0]), row[1]) for row in cur.fetchall()]

        upload_period_seconds = 3

        async_processor.perform_work("PricesImporter")
        assert len(get_files_info()) == 2

        time.sleep(upload_period_seconds + 0.5)

        async_processor.perform_work("PricesImporter")
        assert len(get_files_info()) == 4

        files = get_files_info()
        first_users = [uid for uid, _ in files[:2]]
        assert first_users == [user1.uid, user2.uid] or \
            first_users == [user2.uid, user1.uid]

        user1_times = sorted([time for uid, time in files if uid == user1.uid])
        user2_times = sorted([time for uid, time in files if uid == user2.uid])
        assert user1_times[1] - user1_times[0] > upload_period_seconds
        assert user2_times[1] - user2_times[0] > upload_period_seconds


def test_cancel_file_uploading(user, organization):
    with async_processor.detached():
        server.post_import_file(user, 'test.xls', organization, generate_excel_body(1)) >> 200

        import_status = server.get_import_file_status(user, organization) >> 200
        assert import_status['status'] == 'Waiting'

        server.cancel_file_uploading(user, organization) >> 200

        import_status = server.get_import_file_status(user, organization) >> 200
        assert import_status['status'] == 'Cancelled'
        assert import_status['parse_info'] is None


def test_cancel_file_uploading_empty_queue(user, organization):
    server.cancel_file_uploading(user, organization) >> 204

    server.post_import_file(user, 'test.xls', organization, generate_excel_body(1)) >> 200

    def file_processed():
        import_status = server.get_import_file_status(user, organization) >> 200
        return import_status['status'] == 'Processed'

    async_processor.perform_all_work()
    assert file_processed()
    server.cancel_file_uploading(user, organization) >> 204


def test_import_file_after_cancelling(user, organization):
    with async_processor.detached():
        server.post_import_file(user, 'test1.xls', organization, generate_excel_body(1)) >> 200
        server.cancel_file_uploading(user, organization) >> 200

        server.post_import_file(user, 'test2.xls', organization, generate_excel_body(1)) >> 200
        import_status = server.get_import_file_status(user, organization) >> 200
        assert import_status['status'] == 'Waiting'
        assert import_status['parse_info'] is None


def test_import_file_mark_read(user, organization):
    server.read_file_uploading(user, organization) >> 204

    server.post_import_file(user, 'test.xls', organization, generate_excel_body(1)) >> 200

    def file_processed():
        import_status = server.get_import_file_status(user, organization) >> 200
        return import_status['status'] == 'Processed'

    async_processor.perform_all_work()
    assert file_processed()

    import_status = server.get_import_file_status(user, organization) >> 200
    assert import_status['is_read'] is False

    server.read_file_uploading(user, organization) >> 200

    import_status = server.get_import_file_status(user, organization) >> 200
    assert import_status['is_read'] is True

    server.read_file_uploading(user, organization) >> 200


def test_import_file_parse_info_success(user, organization):
    item_count = random.randint(1, 20)

    yml_fileobj = generate_yml_body(item_count=item_count)

    server.post_import_file(user, rstr.urlsafe(1, 20), organization, yml_fileobj) >> 200

    async_processor.perform_all_work()

    import_status = server.get_import_file_status(user, organization) >> 200
    assert import_status['parse_info']['total_items_count'] == item_count
    assert import_status['parse_info']['parsed_items_count'] == item_count
    assert import_status['parse_info']['items_with_photo_count'] == item_count


def test_import_file_parse_info_failure(user, organization):
    item_count = random.randint(1, 20)

    yml_fileobj = generate_invalid_yml_body(item_count=item_count)

    server.post_import_file(user, rstr.urlsafe(1, 20), organization, yml_fileobj) >> 200

    async_processor.perform_all_work()

    import_status = server.get_import_file_status(user, organization) >> 200
    assert import_status['parse_info']['total_items_count'] == item_count
    assert import_status['parse_info']['parsed_items_count'] == 0
    assert import_status['parse_info']['items_with_photo_count'] == 0


@pytest.mark.parametrize('generator_settings', [
    (generate_excel_body_from_items, '.xls'),
    (generate_yml_body_from_items, '.yml')
])
def test_import_file_status_with_duplicates(user, organization, generator_settings):
    generator, file_ext = generator_settings
    item_count = random.randint(1, 20)
    price = PriceItem(is_hidden=False).to_import_file_format()

    fileobj = generator([price for _ in range(0, item_count)])

    filename = rstr.urlsafe(1, 20) + file_ext
    server.post_import_file(user, filename, organization, fileobj) >> 200

    async_processor.perform_all_work()

    import_status = server.get_import_file_status(user, organization) >> 200
    assert import_status['parse_info']['total_items_count'] == item_count
    assert import_status['parse_info']['parsed_items_count'] == 1
    assert import_status['parse_info']['items_with_photo_count'] == 0


@pytest.mark.parametrize('generator_settings', [
    (generate_excel_body, '.xls'),
    (generate_yml_body, '.yml')
])
def test_import_file_status_with_more_than_maximum_prices(user, organization, generator_settings):
    generator, file_ext = generator_settings
    item_count = random.randint(MAX_ITEMS_COUNT + 1, 2 * MAX_ITEMS_COUNT)

    fileobj = generator(item_count)

    filename = rstr.urlsafe(1, 20) + file_ext
    server.post_import_file(user, filename, organization, fileobj) >> 200

    async_processor.perform_all_work()

    import_status = server.get_import_file_status(user, organization) >> 200
    assert import_status['message'] == f'Предупреждение. Превышен максимальный лимит товаров: {MAX_ITEMS_COUNT}\n'
    assert import_status['parse_info']['total_items_count'] == MAX_ITEMS_COUNT
    assert import_status['parse_info']['parsed_items_count'] == MAX_ITEMS_COUNT
    assert import_status['parse_info']['items_with_photo_count'] == MAX_ITEMS_COUNT

    prices = server.get_prices(user, organization) >> 200
    assert prices['pager']['total'] == MAX_ITEMS_COUNT


@pytest.mark.parametrize('generator_settings', [
    (generate_excel_body_from_items, '.xls'),
    (generate_yml_body_from_items, '.yml')
])
def test_import_file_status_with_items_hit(user, organization, generator_settings):
    generator, file_ext = generator_settings
    generated_prices = generate_prices(
        user,
        organization,
        is_hidden=False,
        photo_count=0)
    prices = [price.to_import_file_format() for price in generated_prices]

    fileobj = generator(prices)

    filename = rstr.urlsafe(1, 20) + file_ext
    server.post_import_file(user, filename, organization, fileobj) >> 200

    async_processor.perform_all_work()

    import_status = server.get_import_file_status(user, organization) >> 200
    assert import_status['parse_info']['total_items_count'] == len(prices)
    assert import_status['parse_info']['parsed_items_count'] == len(prices)
    assert import_status['parse_info']['items_with_photo_count'] == 0


@pytest.mark.parametrize('generator_settings', [
    (generate_excel_body_from_items, '.xls'),
    (generate_yml_body_from_items, '.yml')
])
def test_import_file_with_backslash_in_category(user, company, generator_settings):
    generator, file_ext = generator_settings
    generated_prices = generate_prices(
        user,
        company,
        is_hidden=False,
        photo_count=0,
        group={'name': printable_str(5, 10) + '\\' + printable_str(5, 10)})
    prices = [price.to_import_file_format() for price in generated_prices]
    fileobj = generator(prices)

    filename = rstr.urlsafe(1, 20) + file_ext
    server.post_import_file(user, filename, company, fileobj) >> 200

    async_processor.perform_all_work()

    import_status = server.get_import_file_status(user, company) >> 200
    assert import_status['status'] == 'Processed'


@pytest.mark.parametrize('generator_settings', [
    (generate_excel_body_from_items, '.xls'),
    (generate_yml_body_from_items, '.yml')
])
@pytest.mark.parametrize('url_field', ["market_url", "photo"])
def test_import_file_with_long_url(user, company, generator_settings, url_field):
    generator, file_ext = generator_settings
    invalid_price_item, valid_price_item = PriceItem(is_hidden=False), PriceItem(is_hidden=False)

    url_prefix = "https://host.com/"
    long_url = url_prefix + rstr.urlsafe(MAX_URL_LENGTH - len(url_prefix) + 1)
    if url_field == "market_url":
        invalid_price_item.market_url = long_url
    else:
        invalid_price_item.photos = [long_url]

    fileobj = generator([
        invalid_price_item.to_import_file_format(),
        valid_price_item.to_import_file_format()
    ])

    filename = rstr.urlsafe(1, 20) + file_ext
    server.post_import_file(user, filename, company, fileobj) >> 200

    async_processor.perform_all_work()

    import_status = server.get_import_file_status(user, company) >> 200
    assert import_status["status"] == "Processed", import_status["message"]
    assert import_status['parse_info']['total_items_count'] == 2
    assert import_status['parse_info']['parsed_items_count'] == 2
    assert import_status['parse_info']['items_with_photo_count'] == 0

    prices = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    assert prices[0].title == valid_price_item.title
    if url_field == "market_url":
        assert prices[1].market_url is None
    else:
        assert len(prices[1].photos) == 0
