import lib.async_processor as async_processor
import lib.s3 as s3

from lib.import_file import generate_items
from lib.server import server
from data_types.synchronization import (
    Synchronization,
    generate_items_and_upload_to_s3,
    upload_items_yml_to_s3,
)
from data_types.price_item import PriceItem

import random


def get_synchronization_sync_state(user, company, sync_id):
    return (server.get_synchronization_by_id(
        user, company, sync_id=sync_id) >> 200)["history"]["items"][0]["sync_state"]


def test_replication_works(user, company):
    generated_items, url = generate_items_and_upload_to_s3()

    sync = Synchronization(url=url, is_enabled=True, is_primary=True, file_type="Yml")
    server.post_synchronization(user, company, sync) >> 200

    async_processor.perform_all_work()

    replicated_items = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    assert len(replicated_items) == len(generated_items)


def test_long_descriptions_and_replication_validation(user, company):
    generated_items, url = generate_items_and_upload_to_s3(description_length=300)

    sync = Synchronization(url=url, is_enabled=True, is_primary=True, file_type="Yml")
    sync_id = (server.post_synchronization(user, company, sync) >> 200)["id"]

    async_processor.perform_all_work()

    replicated_items = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    assert len(replicated_items) == len(generated_items)
    assert (server.get_synchronization_by_id(
        user, company, sync_id=sync_id) >> 200)["history"]["items"][0]['message'] == ''
    assert 'Значение в поле "description" слишком длинное.' in (server.get_synchronization_by_id(
        user, company, sync_id=sync_id) >> 200)["history"]["items"][0]["replication_info"]['message']


def test_replication_excess_items_count(user, company):
    generated_items, url = generate_items_and_upload_to_s3(item_count=20)

    sync = Synchronization(url=url, is_enabled=True, is_primary=True, file_type="Yml")
    sync_id = (server.post_synchronization(user, company, sync) >> 200)["id"]

    async_processor.perform_all_work()

    replicated_items = PriceItem.list_from_json(server.get_prices(user, company) >> 200)

    DEFAULT_REPLICATION_LIMIT = 15
    assert len(replicated_items) == DEFAULT_REPLICATION_LIMIT
    assert (server.get_synchronization_by_id(
        user, company, sync_id=sync_id) >> 200)["history"]["items"][0]['message'] == ''
    assert "Превышен максимальный лимит товаров" in (server.get_synchronization_by_id(
        user, company, sync_id=sync_id) >> 200)["history"]["items"][0]["replication_info"]['message']


def test_replications_finished_successfully_without_synchronization(user, company):
    generated_items, url = generate_items_and_upload_to_s3()

    sync = Synchronization(url=url, is_enabled=True, is_primary=True, file_type="Yml")
    server.post_synchronization(user, company, sync) >> 200

    async_processor.perform_work(threads=['PricesReplicator'])


def test_replication_after_synchronization_is_deleted_does_not_clear_items(user, company):
    generated_items, url = generate_items_and_upload_to_s3()

    sync = Synchronization(url=url, is_enabled=True, is_primary=True, file_type="Yml")
    sync_id = (server.post_synchronization(user, company, sync) >> 200)["id"]

    async_processor.perform_all_work()
    server.delete_synchronization(user, company, sync_id) >> 200
    async_processor.perform_all_work()

    replicated_items = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    assert len(replicated_items) == len(generated_items)


def test_only_primary_synchronization_is_replicated(user, company):
    primary_generated_items, primary_url = generate_items_and_upload_to_s3(item_count=random.randint(1, 10))
    secondary_generated_items, secondary_url = generate_items_and_upload_to_s3(item_count=random.randint(1, 10))

    primary_sync = Synchronization(url=primary_url, is_enabled=True, is_primary=True, file_type="Yml")
    server.post_synchronization(user, company, primary_sync) >> 200

    secondary_sync = Synchronization(url=secondary_url, is_enabled=True, is_primary=False, file_type="Yml")
    server.post_synchronization(user, company, secondary_sync) >> 200

    async_processor.perform_all_work()

    replicated_items = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    assert len(replicated_items) == len(primary_generated_items)


def test_replication_of_failed_synchronization_does_not_clear_items(user, company):
    generated_items, url = generate_items_and_upload_to_s3()

    sync = Synchronization(url=url, is_enabled=True, is_primary=True, file_type="Yml")
    sync_id = (server.post_synchronization(user, company, sync) >> 200)["id"]

    async_processor.perform_all_work()
    bucket, key = s3.get_bucket_and_key(url)
    s3.delete_object(key=key, bucket=bucket)
    server.force_synchronization(user, company, sync_id) >> 200
    async_processor.perform_all_work(allow_failure=True)
    assert get_synchronization_sync_state(user, company, sync_id) == 'Error'

    replicated_items = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    assert len(replicated_items) == len(generated_items)


def test_last_successfull_synchronization_is_replicated(user, company):
    originally_generated_items = sorted(
        generate_items(item_count=random.randint(1, 10), category_count=5),
        key=lambda i: (i["title"], i["description"]))

    url = upload_items_yml_to_s3(originally_generated_items)
    bucket, key = s3.get_bucket_and_key(url)

    sync = Synchronization(url=url, is_enabled=True, is_primary=True, file_type="Yml")
    sync_id = (server.post_synchronization(user, company, sync) >> 200)["id"]
    async_processor.perform_all_work()

    s3.delete_object(key=key, bucket=bucket)
    server.force_synchronization(user, company, sync_id) >> 200
    async_processor.perform_work(threads=['PricesSynchronizer'], allow_failure=True)
    assert get_synchronization_sync_state(user, company, sync_id) == 'Error'

    later_generated_items = sorted(
        generate_items(item_count=random.randint(1, 10), category_count=5),
        key=lambda i: (i["title"], i["description"]))
    upload_items_yml_to_s3(later_generated_items, key=key, bucket=bucket)
    server.force_synchronization(user, company, sync_id) >> 200
    async_processor.perform_work(threads=['PricesSynchronizer'])
    assert get_synchronization_sync_state(user, company, sync_id) == 'Success'

    s3.delete_object(key=key, bucket=bucket)
    server.force_synchronization(user, company, sync_id) >> 200
    async_processor.perform_work(threads=['PricesSynchronizer'], allow_failure=True)
    assert get_synchronization_sync_state(user, company, sync_id) == 'Error'

    async_processor.perform_all_work()

    replicated_items = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    assert len(replicated_items) == len(later_generated_items)
