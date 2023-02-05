from data_types.user import User
from data_types.price_item import PriceItem, generate_prices
from data_types.price_overwrite import PriceOverwrite
from data_types.util import generate_image
from lib.server import server

from io import StringIO


def test_get_organization_groups_permission_required(user, alien_organization):
    server.get_company_groups(user, alien_organization) >> 403


def test_get_groups_auth_required():
    unregistered_user = User()
    server.get_groups(unregistered_user) >> 401


def test_get_organization_price_auth_required(organization):
    unregistered_user = User()
    SOME_PRICE_ID = 1
    server.get_prices_by_id(unregistered_user, organization, id=SOME_PRICE_ID) >> 401


def test_get_regional_chain_price_auth_required(regional_chain_companies):
    unregistered_user = User()
    SOME_PRICE_ID = 1
    for chain_company in regional_chain_companies:
        server.get_prices_by_id(unregistered_user, chain_company, id=SOME_PRICE_ID) >> 401


def test_get_organization_price_permission_required(user, alien_user, alien_organization):
    price_item = PriceItem.from_json(server.post_price(alien_user, PriceItem(), alien_organization) >> 200)
    server.get_prices_by_id(user, alien_organization, id=price_item.id) >> 403


def test_get_regional_chain_pricelist(regional_chain_companies, user, alien_user):
    expected_pricelists = []
    for company in regional_chain_companies:
        expected_pricelists.append(generate_prices(user, company.parent_chain))

    for chain_company, expected_pricelist in zip(regional_chain_companies, expected_pricelists):
        for expected_price in expected_pricelist:
            server.get_prices_by_id(alien_user, chain_company, id=expected_price.id) >> 403


def test_post_photo_url_auth_required():
    unregistered_user = User()
    url = 'http://example.com'

    server.post_photos_url(unregistered_user, url) >> 401


def test_post_photo_file_auth_required():
    unregistered_user = User()
    image_data = generate_image()

    server.post_photo_file(unregistered_user, image_data) >> 401


def test_post_organization_price_permission_required(user, alien_organization, alien_user):
    price_item = PriceItem()
    server.post_price(user, price_item, alien_organization) >> 403


def test_edit_organization_price_permission_required(user, alien_organization, alien_user):
    price_item = PriceItem()
    price_item = PriceItem.from_json(server.post_price(alien_user, price_item, alien_organization) >> 200)

    server.edit_price(user, price_item, alien_organization) >> 403


def test_get_organization_prices_permission_required(user, alien_organization, alien_user):
    price_item = PriceItem()
    price_item = PriceItem.from_json(server.post_price(alien_user, price_item, alien_organization) >> 200)

    server.get_prices(user, alien_organization) >> 403


def test_delete_organization_pricelist_permission_required(user, alien_organization):
    server.delete_pricelist(user, alien_organization) >> 403


def test_delete_organization_price_permission_required(user, alien_organization, alien_user):
    price_item = PriceItem()
    price_item = PriceItem.from_json(server.post_price(alien_user, price_item, alien_organization) >> 200)

    server.delete_prices(user, alien_organization, [price_item.id]) >> 403


def test_get_company_chain_pricelist_permission_required(user, chain_company, alien_user):
    server.get_prices(alien_user, chain_company, is_chain=True) >> 403


def test_patch_company_chain_pricelist_permission_required(user, chain_company, chain, alien_user):
    price_item = PriceItem()
    price_item = PriceItem.from_json(server.post_price(user, price_item, chain) >> 200)

    price_item.is_hidden = not price_item.is_hidden
    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
    server.edit_chain_prices(alien_user, [overwrite], chain_company) >> 403


def test_get_import_file_permission_required(user, alien_organization):
    server.get_import_file_status(user, alien_organization) >> 403


def test_cancel_import_file_permission_required(user, alien_organization):
    server.cancel_file_uploading(user, alien_organization) >> 403


def test_read_import_file_permission_required(user, alien_organization):
    server.read_file_uploading(user, alien_organization) >> 403


def test_post_import_file_permission_required(user, alien_organization):
    fileobj = StringIO("")
    server.post_import_file(user, "file", alien_organization, fileobj=fileobj) >> 403


def test_get_company_feeds_permission_required(user, alien_company):
    server.get_feeds(user, alien_company) >> 403


def test_post_company_feeds_settings_permission_required(user, alien_company):
    server.post_feeds_settings(user, alien_company, selected_feed=None) >> 403
