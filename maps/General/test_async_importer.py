from .test_publishing import check_one_published_item

from lib.avatars import add_erroneous_url, get_delete_invocation_counts, get_upload_invocation_counts
from lib.import_file import generate_yml_body, generate_invalid_yml_body, generate_photo_url, generate_items, generate_yml_body_from_items, generate_excel_body_from_items
from lib.server import server
import lib.async_processor as async_processor

from data_types.chain import Chain
from data_types.company import Company
from data_types.price_item import PriceItem
from data_types.price_overwrite import PriceOverwrite
from data_types.published_item import PublishedItem

import maps.automotive.libs.large_tests.lib.db as db

import yatest

from io import SEEK_END
import os
import pytest


@pytest.fixture
def import_status(user, company):
    return lambda: server.get_import_file_status(user, company) >> 200


@pytest.fixture
def price_items(user, company):
    def price_items_impl(limit=1000):
        return (server.get_prices(user, company, limit=limit) >> 200)["items"]
    return price_items_impl


@pytest.fixture
def post_import_n_prices(user, company):
    def impl(n=0, yml_fileobj=None, xls_fileobj=None):
        if yml_fileobj is None and xls_fileobj is None:
            yml_fileobj = generate_yml_body(n)
        if yml_fileobj:
            fileobj = yml_fileobj
            filename = 'test_import.xml'
        else:
            fileobj = xls_fileobj
            filename = 'test_import.xls'
        server.post_import_file(user, filename, company, fileobj=fileobj) >> 200
    return impl


@pytest.fixture
def wait_for_successful_import(import_status, price_items):
    def impl(timeout=5, moderation_timeout=10, moderation_statuses=None):
        async_processor.perform_work(["PricesImporter"])
        async_processor.perform_work(["PhotoUploader"], len(price_items()))
        async_processor.perform_all_work()

        assert import_status()["status"] == "Processed"
        expected_statuses = {
            "ReadyForPublishing",
            "Published",
        } if moderation_statuses is None else set(moderation_statuses)
        assert all(i["moderation"]["status"] in expected_statuses for i in price_items())
    return impl


def test_simple_import_completed(price_items, post_import_n_prices, wait_for_successful_import):
    items_count = 7
    post_import_n_prices(items_count)
    wait_for_successful_import()

    items = price_items()
    assert len(items) == items_count
    assert all(
        len(i["photos"]) != 0 and all(p.get('template_url') is not None for p in i["photos"])
        for i in items)


def test_illformed_file(import_status, price_items, post_import_n_prices):
    items_count = 2
    yml_fileobj = generate_yml_body(items_count)
    yml_fileobj.seek(-10, SEEK_END)
    post_import_n_prices(yml_fileobj=yml_fileobj)

    async_processor.perform_all_work(allow_failure=True)
    assert import_status()["status"] == "FailedToProcess"
    assert len(price_items()) == 0


def test_file_with_warnings(user, company, import_status, price_items, wait_for_successful_import):
    filepath = 'maps/goods/lib/prices_parser/it/data/prod_import_4.xlsx'
    items_count = 14

    with open(os.path.join(yatest.common.source_path(), filepath), 'rb') as f:
        server.post_import_file(user, os.path.basename(filepath), company, fileobj=f) >> 200
    wait_for_successful_import()

    assert import_status()["message"].strip() != ''
    assert len(price_items()) == items_count


def test_file_without_groups(user, company, price_items, wait_for_successful_import):
    filepath = 'maps/goods/lib/prices_parser/it/data/price-list-template-no-groups.xls'
    items_count = 11

    with open(os.path.join(yatest.common.source_path(), filepath), 'rb') as f:
        server.post_import_file(user, os.path.basename(filepath), company, fileobj=f) >> 200
    wait_for_successful_import()

    assert len(price_items()) == items_count


def test_import_overrides_items(user, price_items, post_import_n_prices, wait_for_successful_import):
    for items_count in [2, 1, 0]:
        user.forget_about_last_import()

        post_import_n_prices(items_count)
        wait_for_successful_import()

        assert len(price_items()) == items_count


def test_treat_files_without_valid_prices_as_invalid(
    user,
    company,
    price_items,
    post_import_n_prices,
    import_status,
    wait_for_successful_import
):
    ITEMS_COUNT = 12
    post_import_n_prices(ITEMS_COUNT)

    wait_for_successful_import()
    assert len(price_items()) == ITEMS_COUNT

    user.forget_about_last_import()

    yml_fileobj = generate_invalid_yml_body()
    server.post_import_file(user, 'test_import.xml', company, fileobj=yml_fileobj) >> 200

    async_processor.perform_all_work()
    assert import_status()["status"] == "FailedToProcess"
    assert len(price_items()) == ITEMS_COUNT

    user.forget_about_last_import()

    post_import_n_prices(0)
    wait_for_successful_import()
    assert len(price_items()) == 0


def test_upload_price_with_broken_image_url(
    price_items,
    post_import_n_prices,
    wait_for_successful_import
):
    items_count = 1
    first_item_id = '1'
    erroneous_url = generate_photo_url(first_item_id)
    add_erroneous_url(
        url=erroneous_url,
        code=400,
        description='cannot process image: some reason')
    post_import_n_prices(items_count)
    wait_for_successful_import()

    items = price_items()
    assert len(items) == items_count
    assert items[0]['external_info']['item_id'] == first_item_id
    assert len(items[0]['photos']) == 0


def test_invalid_files_does_not_override_existing_prices(
    user,
    import_status,
    price_items,
    post_import_n_prices,
    wait_for_successful_import
):
    items_count = 2
    post_import_n_prices(items_count)
    wait_for_successful_import(moderation_statuses=["Published"])

    success_items = price_items()
    assert len(success_items) == items_count

    user.forget_about_last_import()

    yml_fileobj = generate_yml_body(items_count)
    yml_fileobj.seek(-10, SEEK_END)
    post_import_n_prices(yml_fileobj=yml_fileobj)

    async_processor.perform_all_work(allow_failure=True)
    assert import_status()["status"] == "FailedToProcess"

    items = price_items()
    assert len(items) == items_count
    assert items == success_items


@pytest.mark.parametrize("erroneous_url", [False, True])
def test_same_source_url(user, company, alien_user, alien_company, import_status,
                         wait_for_successful_import, erroneous_url):
    item_count = 5
    items = generate_items(item_count=item_count, category_count=item_count)
    for price in items:
        price['photo_url'] = items[0]['photo_url']

    if erroneous_url:
        add_erroneous_url(
            url=items[0]['photo_url'],
            code=400,
            description='cannot process image: some reason')

    yml_fileobj = generate_yml_body_from_items(items)
    server.post_import_file(user, "some_file.xml", company, yml_fileobj) >> 200

    yml_fileobj.seek(0)
    server.post_import_file(alien_user, "some_file.xml", alien_company, yml_fileobj) >> 200

    async_processor.perform_work(["PricesImporter"])

    assert import_status()["status"] == "Processed"
    assert (server.get_import_file_status(alien_user, alien_company) >> 200)["status"] == "Processed"

    async_processor.perform_all_work()

    with db.get_connection() as conn:
        cur = conn.cursor()

        def uploaded_photos():
            cur.execute("""
                SELECT
                    COUNT(*) FILTER (WHERE  template_url IS NOT NULL),
                    COUNT(*) FILTER (WHERE  template_url IS NULL)
                FROM photos
            """)
            return cur.fetchone()

        if not erroneous_url:
            expected_uploaded_photos = (1, 0)
            expected_relations_count = (2 * item_count)
        else:
            expected_uploaded_photos = (0, 0)
            expected_relations_count = 0

        assert uploaded_photos() == expected_uploaded_photos

        cur.execute("SELECT COUNT(*) FROM photo_relations")
        row = cur.fetchone()
        assert row is not None
        assert int(row[0]) == expected_relations_count

        # no photos were uploaded and deleted in avatars
        assert get_delete_invocation_counts() == 0
        # photo was uploaded to avatars only once
        assert get_upload_invocation_counts() == 1


def test_import_big_file(user, company, post_import_n_prices, import_status):
    yml_fileobj = generate_yml_body(item_count=120, category_count=80)
    post_import_n_prices(yml_fileobj=yml_fileobj)

    async_processor.perform_all_work()
    assert import_status()["status"] != "Waiting"

    prices = server.get_prices(user, company) >> 200
    assert prices["pager"]["total"] == 100


def test_import_same_prices_without_changes(user, company, post_import_n_prices, import_status):
    items = generate_items(item_count=10, category_count=5)
    xls_fileobj = generate_excel_body_from_items(items)
    post_import_n_prices(xls_fileobj=xls_fileobj)

    async_processor.perform_all_work()
    assert import_status()["status"] != "Waiting"

    prices = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    ids_before = {p.id for p in prices}

    assert len(prices) == len(items)

    user.forget_about_last_import()

    xls_fileobj.seek(0)
    post_import_n_prices(xls_fileobj=xls_fileobj)

    async_processor.perform_all_work()
    assert import_status()["status"] != "Waiting"

    prices = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    ids_after = {p.id for p in prices}
    assert ids_before == ids_after


def test_import_prices_with_partial_changes(user, company, post_import_n_prices, import_status):
    items = generate_items(item_count=10, category_count=5)
    for i in items:
        i["id"] = None

    post_import_n_prices(xls_fileobj=generate_excel_body_from_items(items, excludes=["id"]))

    async_processor.perform_all_work()
    assert import_status()["status"] == "Processed"

    prices = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    ids_before = {p.id for p in prices}

    assert len(prices) == len(items)

    user.forget_about_last_import()

    items[0]["title"] = items[0]["title"] + "_changed"
    post_import_n_prices(xls_fileobj=generate_excel_body_from_items(items, excludes=["id"]))

    async_processor.perform_all_work()
    assert import_status()["status"] == "Processed"

    prices_after = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    ids_after = {p.id for p in prices_after}

    ids_change = ids_after - ids_before
    assert len(ids_change) == 1

    changed_id = next(iter(ids_change))
    for p in prices_after:
        if p.id == changed_id:
            assert p.title == items[0]["title"]


def test_import_prices_with_minor_changes(user, company, post_import_n_prices, import_status):
    items = generate_items(item_count=10, category_count=5)
    for i in items:
        i["id"] = None

    post_import_n_prices(xls_fileobj=generate_excel_body_from_items(items, excludes=["id"]))

    async_processor.perform_all_work()
    assert import_status()["status"] == "Processed"

    prices_before = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    ids_before = {p.id for p in prices_before}

    assert len(prices_before) == len(items)

    user.forget_about_last_import()

    titles_to_items = {item["title"]: item for item in items}

    items[0]["description"] = items[0]["description"] + "_changed"
    items[1]["photo_url"] = items[1]["photo_url"] + "_changed"
    items[2]["is_popular"] = not items[2]["is_popular"]
    items[3]["is_out_of_stock"] = not items[3]["is_out_of_stock"]
    items[4]["price_value"] = items[4]["price_value"] + 10

    post_import_n_prices(xls_fileobj=generate_excel_body_from_items(items, excludes=["id"]))

    async_processor.perform_all_work()
    assert import_status()["status"] == "Processed"

    prices_after = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    ids_after = {p.id for p in prices_after}

    ids_change = ids_after - ids_before
    assert len(ids_change) == 0

    for price in prices_after:
        item = titles_to_items[price.title]
        assert item["description"] == price.description
        assert item["photo_url"] == price.photo_source_urls[0]
        assert item["is_popular"] == price.is_popular
        assert item["is_out_of_stock"] == (price.availability["status"] == "OutOfStock")
        assert item["price_value"] == price.price["value"]


def test_import_prices_with_minor_changes_other_external_id(user, company, post_import_n_prices, import_status):
    items = generate_items(item_count=10, category_count=5)

    post_import_n_prices(xls_fileobj=generate_excel_body_from_items(items))

    async_processor.perform_all_work()
    assert import_status()["status"] == "Processed"

    prices_before = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    ids_before = {p.id for p in prices_before}

    assert len(prices_before) == len(items)

    user.forget_about_last_import()

    items[0]["description"] = items[0]["description"] + "_changed"
    items[0]["id"] = max(int(item["id"]) for item in items) + 1

    post_import_n_prices(xls_fileobj=generate_excel_body_from_items(items))

    async_processor.perform_all_work()
    assert import_status()["status"] == "Processed"

    prices_after = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    ids_after = {p.id for p in prices_after}

    ids_change = ids_after - ids_before
    assert len(ids_change) == 1

    changed_id = next(iter(ids_change))
    for p in prices_after:
        if p.id == changed_id:
            assert p.description == items[0]["description"]


def test_force_update(user):
    chain = Chain.generate(user=user)
    office = Company.generate(user=user, parent_chain=chain)

    def post_item_with_import_file(item, force_update=False):
        fileobj = generate_excel_body_from_items([item])
        filename = 'test_import.xls'
        server.post_import_file(
            user, filename, chain, fileobj=fileobj, force_update=force_update) >> 200

        async_processor.perform_all_work()
        assert (server.get_import_file_status(user, chain) >> 200)["status"] == "Processed"

        got_json = server.get_prices(user, chain) >> 200
        return next(
            i
            for i in PriceItem.list_from_json(got_json)
            if str(i.external_id) == str(item['id']))

    item = generate_items(item_count=1, category_count=1)[0]

    def do_test_iteration(force_update=False, should_make_hidden=False, is_hidden=True):
        user.forget_about_last_import()
        price_item = post_item_with_import_file(item, force_update)
        assert not price_item.is_hidden

        if should_make_hidden:
            price_item.is_hidden = True
            overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
            server.edit_chain_prices(user, [overwrite], office)

        async_processor.perform_all_work()

        price_item.is_hidden = is_hidden
        check_one_published_item(price_item, office)
        published_items = PublishedItem.get_all()
        assert len(published_items) == 1
        assert published_items[0].is_hidden == is_hidden

    do_test_iteration(force_update=False, should_make_hidden=True, is_hidden=True)
    # FIXME: if item isn't changed publisher won't republish overwritten items
    item['title'] += '_changed'
    do_test_iteration(force_update=False, should_make_hidden=False, is_hidden=True)
    item['title'] += '_changed'
    do_test_iteration(force_update=True, should_make_hidden=False, is_hidden=False)


def test_import_prices_with_duplicated_external_id(user, company, post_import_n_prices, import_status):
    items = generate_items(item_count=10, category_count=5)
    for i in items:
        i["id"] = "same-id"

    post_import_n_prices(xls_fileobj=generate_excel_body_from_items(items))

    async_processor.perform_all_work()
    assert import_status()["status"] == "Processed"

    prices = PriceItem.list_from_json(server.get_prices(user, company, limit=len(items)) >> 200)
    assert len(prices) == 1


def test_import_prices_with_external_id_conflict(user, company, post_import_n_prices, import_status):
    price_item = PriceItem(is_hidden=False, volume=None)
    external_id = price_item.external_id
    server.post_price(user, price_item, company) >> 200

    price_item.external_id = None
    server.post_price(user, price_item, company) >> 200

    prices = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    assert len(prices) == 2
    assert prices[0].external_id is None
    assert prices[1].external_id == external_id

    server.delete_prices(user, company, [prices[1].id]) >> 200

    price_item.external_id = external_id
    post_import_n_prices(xls_fileobj=generate_excel_body_from_items([price_item.to_import_file_format()]))

    async_processor.perform_all_work()
    assert import_status()["status"] == "Processed"

    prices = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    assert len(prices) == 1
    assert prices[0].external_id == external_id
