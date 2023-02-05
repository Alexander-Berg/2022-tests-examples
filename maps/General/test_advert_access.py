import pytest

from data_types.price_item import PriceItem
from data_types.synchronization import (
    upload_items_yml_to_s3,
    generate_items,
    generate_sync_and_upload,
    Synchronization
)
from lib.server import get_default_server, ADVERT_TVM_ID


@pytest.fixture()
def server():
    return get_default_server()


@pytest.fixture()
def advert_server():
    return get_default_server(tvm_id=ADVERT_TVM_ID)


@pytest.fixture()
def generated_items():
    return generate_items(count=10)


@pytest.fixture()
def uploaded_items(generated_items):
    url = upload_items_yml_to_s3(generated_items)
    return url


def test_get_synchronizations_allowed_for_advert(company, alien_user, server, advert_server):
    server.get_synchronizations(alien_user, company) >> 403
    advert_server.get_synchronizations(alien_user, company) >> 200


def test_get_synchronization_by_id_allowed_for_advert(company, alien_user, user, server, advert_server, uploaded_items):
    sync = generate_sync_and_upload(user, company, uploaded_items)
    server.get_synchronization_by_id(alien_user, company, sync_id=sync.id) >> 403
    advert_server.get_synchronization_by_id(alien_user, company, sync_id=sync.id) >> 200


def test_post_synchronization_allowed_for_advert(company, alien_user, server, advert_server, uploaded_items):
    sync = Synchronization(url=uploaded_items, is_enabled=True, is_primary=False, file_type="Yml")
    server.post_synchronization(alien_user, company, sync) >> 403
    advert_server.post_synchronization(alien_user, company, sync) >> 200


def test_post_primary_synchronization_disallowed_for_advert(company, alien_user, server, advert_server, uploaded_items):
    sync = Synchronization(url=uploaded_items, is_enabled=True, is_primary=True, file_type="Yml")
    server.post_synchronization(alien_user, company, sync) >> 403
    advert_server.post_synchronization(alien_user, company, sync) >> 422


def test_edit_synchronization_allowed_for_advert(company, alien_user, user, server, advert_server, uploaded_items):
    sync = Synchronization(url=uploaded_items, is_enabled=True, is_primary=False, file_type="Yml", name="sync 1")
    sync = Synchronization.from_json(server.post_synchronization(user, company, sync) >> 200)
    sync.name += ' edited'

    server.edit_synchronization(alien_user, company, sync) >> 403
    advert_server.edit_synchronization(alien_user, company, sync) >> 200

    sync.is_primary = True
    server.edit_synchronization(alien_user, company, sync) >> 403
    advert_server.edit_synchronization(alien_user, company, sync) >> 422


def test_edit_primary_synchronization_disallowed_for_advert(company, alien_user, user, server, advert_server, uploaded_items):
    sync = Synchronization(url=uploaded_items, is_enabled=True, is_primary=True, file_type="Yml", name="sync 1")
    sync = Synchronization.from_json(server.post_synchronization(user, company, sync) >> 200)
    sync.name += ' edited'

    server.edit_synchronization(alien_user, company, sync) >> 403
    advert_server.edit_synchronization(alien_user, company, sync) >> 422


def test_force_synchronization_allowed_for_advert(company, alien_user, user, server, advert_server, uploaded_items):
    sync = Synchronization(url=uploaded_items, is_enabled=True, is_primary=False, file_type="Yml")
    sync = Synchronization.from_json(server.post_synchronization(user, company, sync) >> 200)

    server.force_synchronization(alien_user, company, sync_id=sync.id) >> 403
    advert_server.force_synchronization(alien_user, company, sync_id=sync.id) >> 200


def test_force_primary_synchronization_disallowed_for_advert(company, alien_user, user, server, advert_server, uploaded_items):
    sync = Synchronization(url=uploaded_items, is_enabled=True, is_primary=True, file_type="Yml")
    sync = Synchronization.from_json(server.post_synchronization(user, company, sync) >> 200)

    server.force_synchronization(alien_user, company, sync_id=sync.id) >> 403
    advert_server.force_synchronization(alien_user, company, sync_id=sync.id) >> 422


def test_delete_synchronization_allowed_for_advert(company, alien_user, user, server, advert_server, uploaded_items):
    sync = Synchronization(url=uploaded_items, is_enabled=True, is_primary=False, file_type="Yml")
    sync = Synchronization.from_json(server.post_synchronization(user, company, sync) >> 200)

    server.delete_synchronization(alien_user, company, sync_id=sync.id) >> 403
    advert_server.delete_synchronization(alien_user, company, sync_id=sync.id) >> 200


def test_delete_primary_synchronization_disallowed_for_advert(company, alien_user, user, server, advert_server, uploaded_items):
    sync = Synchronization(url=uploaded_items, is_enabled=True, is_primary=True, file_type="Yml")
    sync = Synchronization.from_json(server.post_synchronization(user, company, sync) >> 200)

    server.delete_synchronization(alien_user, company, sync_id=sync.id) >> 403
    advert_server.delete_synchronization(alien_user, company, sync_id=sync.id) >> 422


def test_post_synchronization_test_allowed_for_advert(company, alien_user, server, advert_server, uploaded_items):
    sync = Synchronization(url=uploaded_items, is_enabled=True, is_primary=False, file_type="Yml")
    server.post_synchronization_test(alien_user, company, sync) >> 403
    advert_server.post_synchronization_test(alien_user, company, sync) >> 200


def test_get_synchronization_test_by_id_allowed_for_advert(company, alien_user, server, advert_server, uploaded_items):
    sync = Synchronization(url=uploaded_items, is_enabled=True, is_primary=False, file_type="Yml")
    sync = Synchronization.from_json(advert_server.post_synchronization_test(alien_user, company, sync) >> 200)

    server.get_synchronization_test_by_id(alien_user, company, sync_test_id=sync.id) >> 403
    advert_server.get_synchronization_test_by_id(alien_user, company, sync_test_id=sync.id) >> 200


def test_get_pricelist_allowed_for_advert(company, alien_user, user, server, advert_server, uploaded_items):
    sync = generate_sync_and_upload(user, company, uploaded_items)

    server.get_prices(alien_user, company, sync_id=sync.id) >> 403
    advert_server.get_prices(alien_user, company, sync_id=sync.id) >> 200


def test_get_prices_allowed_for_advert(company, alien_user, user, server, advert_server, uploaded_items):
    sync = generate_sync_and_upload(user, company, uploaded_items)
    prices_ids = [p.id for p in PriceItem.list_from_json(server.get_prices(user, company, sync_id=sync.id) >> 200)]

    server.get_prices_by_id(alien_user, company, sync_id=sync.id, id=prices_ids) >> 403
    advert_server.get_prices_by_id(alien_user, company, sync_id=sync.id, id=prices_ids) >> 200


def test_get_groups_allowed_for_advert(company, alien_user, user, server, advert_server, uploaded_items):
    sync = generate_sync_and_upload(user, company, uploaded_items)
    server.get_company_groups(alien_user, company, sync_id=sync.id) >> 403
    advert_server.get_company_groups(alien_user, company, sync_id=sync.id) >> 200
