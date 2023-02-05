import lib.async_processor as async_processor
from data_types.company import Company
from data_types.external_item import ExternalItem
from data_types.price_item import PriceItem
from data_types.synchronization import (
    Synchronization,
    SynchronizedItem,
    generate_items_and_upload_to_s3,
    generate_synchronization_attempts
)

from lib.server import server

import random


def generate_synchronizations(user, organization, count=1, **kwargs):
    generated_synchronizations = []
    url = kwargs.get("url")
    if not url:
        _, url = generate_items_and_upload_to_s3()
    for _ in range(count):
        sync = Synchronization(**kwargs, url=url, is_enabled=True)

        response = server.post_synchronization(user, organization, sync) >> 200
        sync = Synchronization.from_json(response)
        generated_synchronizations.append(sync)

    return generated_synchronizations


def test_get_synchronizations(user, company):
    generated_sync = generate_synchronizations(user, company, count=1)[0]

    syncs = Synchronization.list_from_json(server.get_synchronizations(user, company) >> 200)
    assert len(syncs) == 1
    sync = syncs[0]
    assert generated_sync.to_json() == sync.to_json()

    generated_attempts = generate_synchronization_attempts(sync)
    syncs = Synchronization.list_from_json(server.get_synchronizations(user, company) >> 200)
    assert len(syncs) == 1
    sync = syncs[0]

    generated_attempts[-1].assert_equal_to_json(sync.status)


def test_get_synchronization_by_id(user, company):
    generated_sync = generate_synchronizations(user, company, count=1)[0]

    sync = Synchronization.from_json(
        server.get_synchronization_by_id(user, company, generated_sync.id) >> 200)
    assert generated_sync.to_json() == sync.to_json()

    generated_attempts = generate_synchronization_attempts(sync)
    sync = Synchronization.from_json(
        server.get_synchronization_by_id(user, company, generated_sync.id) >> 200)

    history_attempts = sync.history.get("items", [])
    assert len(generated_attempts) == len(history_attempts)
    for generated_attempt, attempt in zip(reversed(generated_attempts), history_attempts):
        generated_attempt.assert_equal_to_json(attempt)


def test_edit_synchronization(user, company):
    generated_sync = generate_synchronizations(user, company, count=1)[0]
    edited_sync = Synchronization(id=generated_sync.id)

    sync = Synchronization.from_json(
        server.edit_synchronization(user, company, edited_sync) >> 200)
    assert edited_sync.to_json() == sync.to_json()


def test_delete_synchronization(user, company):
    generated_sync = generate_synchronizations(user, company, count=1)[0]

    server.delete_synchronization(user, company, generated_sync.id) >> 200

    syncs = Synchronization.list_from_json(server.get_synchronizations(user, company) >> 200)
    assert len(syncs) == 0
    assert len(SynchronizedItem.get_all()) == 0


def test_get_synchronizations_wrong_organization(user):
    invalid_organization = Company()
    error = server.get_synchronizations(user, invalid_organization) >> 403
    assert error["code"] == "PERMISSION_DENIED"


def test_alien_synchronization(user, company, alien_user, alien_company):
    sync = generate_synchronizations(user, company, count=1, is_primary=False)[0]
    alien_sync = generate_synchronizations(alien_user, alien_company, count=1, is_primary=False)[0]

    error = server.get_synchronization_by_id(user, company, alien_sync.id) >> 422
    assert error["code"] == "SYNCHRONIZATION_NOT_FOUND"

    error = server.edit_synchronization(user, company, alien_sync) >> 422
    assert error["code"] == "SYNCHRONIZATION_NOT_FOUND"

    error = server.edit_synchronization(alien_user, alien_company, sync) >> 422
    assert error["code"] == "SYNCHRONIZATION_NOT_FOUND"


def test_post_more_than_one_primary_synchronization(user, company):
    generate_synchronizations(user, company, count=1, is_primary=True)[0]

    sync2 = Synchronization(is_primary=True)
    error = server.post_synchronization(user, company, sync2) >> 422

    assert error["code"] == "ONLY_ONE_PRIMARY_SYNCHRONIZATION_ALLOWED"


def test_edit_more_than_one_primary_synchronization(user, company):
    generate_synchronizations(user, company, count=1, is_primary=True)[0]

    sync2 = Synchronization(is_primary=False)
    rsp = server.post_synchronization(user, company, sync2) >> 200
    sync2 = Synchronization.from_json(rsp)
    sync2.is_primary = True
    error = server.edit_synchronization(user, company, sync2) >> 422

    assert error["code"] == "ONLY_ONE_PRIMARY_SYNCHRONIZATION_ALLOWED"


def test_swap_primary_synchronizations(user, company):
    sync1 = generate_synchronizations(user, company, count=1, is_primary=True)[0]
    sync2 = generate_synchronizations(user, company, count=1, is_primary=False)[0]

    sync1.is_primary = False
    rsp = server.edit_synchronization(user, company, sync1) >> 200
    sync1 = Synchronization.from_json(rsp)

    assert not sync1.is_primary

    sync2.is_primary = True
    rsp = server.edit_synchronization(user, company, sync2) >> 200
    sync2 = Synchronization.from_json(rsp)

    assert sync2.is_primary


def test_synchronization_with_incorrect_url(user, company):
    sync = Synchronization(url="aaaaaaaaaaa")
    error = server.post_synchronization(user, company, sync) >> 422
    assert error["code"] == "BAD_SYNCHRONIZATION_URL"

    error = server.post_synchronization_test(user, company, sync_test=sync) >> 422
    assert error["code"] == "BAD_SYNCHRONIZATION_URL"


def test_synchronization_test_attempt_result_consistency(user, company):
    sync = Synchronization()

    created_sync_test_rsp = server.post_synchronization_test(user, company, sync_test=sync) >> 200

    assert created_sync_test_rsp["id"] is not None
    assert created_sync_test_rsp["url"] == sync.url
    assert created_sync_test_rsp["file_type"] == sync.file_type
    assert created_sync_test_rsp["status"]["sync_state"] == "Waiting"
    assert created_sync_test_rsp["status"]["message"] is None
    assert created_sync_test_rsp["status"]["parse_info"] is None

    got_sync_test_rsp = server.get_synchronization_test_by_id(
        user, company, sync_test_id=created_sync_test_rsp["id"]) >> 200

    assert created_sync_test_rsp == got_sync_test_rsp


def test_synchronization_test_attempt_multiple(user, company):
    _, url = generate_items_and_upload_to_s3()
    sync1 = Synchronization(url=url, is_enabled=True, is_primary=False, file_type="Yml")
    created_sync_test_rsp_1 = server.post_synchronization_test(user, company, sync_test=sync1) >> 200
    created_sync_test_rsp_2 = server.post_synchronization_test(user, company, sync_test=sync1) >> 200

    assert created_sync_test_rsp_1["id"] == created_sync_test_rsp_2["id"]

    _, url_new = generate_items_and_upload_to_s3()
    sync2 = Synchronization(url=url_new, is_enabled=True, is_primary=False, file_type="Yml")
    created_sync_test_rsp_2 = server.post_synchronization_test(user, company, sync_test=sync2) >> 200
    assert created_sync_test_rsp_1["id"] != created_sync_test_rsp_2["id"]

    def file_processed(id):
        import_status = (server.get_synchronization_test_by_id(
            user, company, sync_test_id=id) >> 200)["status"]
        return import_status["sync_state"] == "Success"

    async_processor.perform_work(threads=['PricesTestSynchronizer'])

    assert file_processed(created_sync_test_rsp_1["id"])
    assert file_processed(created_sync_test_rsp_2["id"])


def test_synchronization_test_attempt_yml_feed(user, company):
    item_count = 10
    with_photo_count = 5
    generated_items, url = generate_items_and_upload_to_s3(item_count=item_count, with_photo_count=with_photo_count)

    sync = Synchronization(url=url, is_enabled=True, is_primary=True, file_type="Yml")
    created_sync_test_rsp = server.post_synchronization_test(user, company, sync_test=sync) >> 200
    assert created_sync_test_rsp["url"] == sync.url

    def file_processed():
        import_status = (server.get_synchronization_test_by_id(
            user, company, sync_test_id=created_sync_test_rsp["id"]) >> 200)["status"]
        return import_status["sync_state"] == "Success"

    async_processor.perform_work(threads=['PricesTestSynchronizer'])
    assert file_processed()
    import_status = (server.get_synchronization_test_by_id(
        user, company, sync_test_id=created_sync_test_rsp["id"]) >> 200)["status"]
    assert import_status["parse_info"] is not None
    assert import_status["parse_info"]["total_items_count"] == item_count
    assert import_status["parse_info"]["items_with_photo_count"] == with_photo_count


def test_synchronization_attempt_yml_feed(user, company):
    generated_items, url = generate_items_and_upload_to_s3()

    sync = Synchronization(url=url, is_enabled=True, is_primary=True, file_type="Yml")
    created_sync_rsp = server.post_synchronization(user, company, sync) >> 200
    sync = Synchronization.from_json(created_sync_rsp)

    def file_processed():
        sync = Synchronization.list_from_json(
            server.get_synchronizations(user, company) >> 200)[0]
        return sync.status["sync_state"] == "Success" if sync.status is not None else False

    async_processor.perform_all_work()
    assert file_processed()

    prices = PriceItem.list_from_json(server.get_prices(user, company, sync_id=sync.id) >> 200)
    actual = {p.external_id: p.to_import_file_format() for p in prices}
    expected = {g["id"]: g for g in generated_items}
    assert actual == expected


def test_multiple_syncs(user, company):
    items_count_per_sync = 10
    syncs_count = random.randint(2, 4)
    meet_syncs = set()
    items_to_urls = [
        generate_items_and_upload_to_s3(item_count=items_count_per_sync)
        for _ in range(syncs_count)
    ]
    for _, url in items_to_urls:
        sync = Synchronization(url=url, is_enabled=True, is_primary=False, file_type="Yml")
        res_sync = Synchronization.from_json(
            server.post_synchronization(user, company, sync) >> 200)
        assert res_sync.url == sync.url
        meet_syncs.add(res_sync.id)

    def files_processed():
        syncs = Synchronization.list_from_json(
            server.get_synchronizations(user, company) >> 200)

        syncs_success_history = []
        for sync in syncs:
            history = Synchronization.from_json(
                server.get_synchronization_by_id(user, company, sync.id) >> 200).history['items']

            success = False
            for item in history:
                if item["sync_state"] == "Success":
                    success = True
                    break
            syncs_success_history.append(success)

        return all(syncs_success_history)

    for _ in range(syncs_count):
        async_processor.perform_work(threads=['PricesSynchronizer'])
    assert files_processed()

    synced_items = ExternalItem.get_all()
    for item in synced_items:
        assert str(item.synchronization_id) in meet_syncs
    assert len(synced_items) == items_count_per_sync * syncs_count


def test_force_synchronization(user, company):
    server.force_synchronization(user, company, 0) >> 422

    generated_sync = generate_synchronizations(user, company, count=1)[0]

    do_sync = lambda: async_processor.perform_work(threads=['PricesSynchronizer'])
    do_force = lambda: server.force_synchronization(user, company, sync_id=generated_sync.id) >> 200
    get_attempts = lambda: (server.get_synchronization_by_id(
        user, company, sync_id=generated_sync.id) >> 200)['history']['items']

    do_sync()
    assert len(get_attempts()) == 1
    assert get_attempts()[0]['sync_state'] == 'Success'

    do_sync()
    assert len(get_attempts()) == 1

    do_force()
    do_sync()
    assert len(get_attempts()) == 2

    do_force()
    do_force()
    do_sync()
    assert len(get_attempts()) == 3


def test_synchronization_is_tried_to_sync(user, company):
    sync = generate_synchronizations(user, company, count=1, name='test')[0]
    do_sync = lambda: async_processor.perform_work(threads=['PricesSynchronizer'])
    do_force = lambda: server.force_synchronization(user, company, sync_id=sync.id) >> 200
    is_tried_to_sync = lambda: Synchronization.from_json(server.get_synchronization_by_id(
        user, company, sync_id=sync.id) >> 200).is_tried_to_sync

    # Just created sync
    assert sync.is_tried_to_sync is False
    assert is_tried_to_sync() is False

    # After sync
    do_sync()
    assert is_tried_to_sync() is True

    # After url changed
    _, url = generate_items_and_upload_to_s3()
    sync.url = url
    sync = Synchronization.from_json(server.edit_synchronization(user, company, sync) >> 200)
    assert sync.is_tried_to_sync is False
    assert is_tried_to_sync() is False

    # After sync
    do_force()
    do_sync()
    assert is_tried_to_sync() is True

    # After name changed
    sync.name = 'edited name'
    sync = Synchronization.from_json(server.edit_synchronization(user, company, sync) >> 200)
    assert sync.is_tried_to_sync is True
    assert is_tried_to_sync() is True
    assert Synchronization.list_from_json(server.get_synchronizations(user, company) >> 200)[0].is_tried_to_sync is True


def only_one_attempt_is_presented():
    items = ExternalItem.get_all()
    if not items:
        return

    for item in items[1:]:
        assert item.synchronization_attempt_id == items[0].synchronization_attempt_id


def test_success_sync_keeps_single_attempt_items(user, company):
    times = random.randint(2, 4)
    generated_sync = generate_synchronizations(user, company, count=1)[0]

    do_sync = lambda: async_processor.perform_work(threads=['PricesSynchronizer'])
    do_force = lambda: server.force_synchronization(user, company, sync_id=generated_sync.id) >> 200

    for _ in range(times):
        do_force()
        do_sync()
        only_one_attempt_is_presented()
