from lib.server import server, get_default_server, ADVERT_TVM_ID
import lib.sprav as sprav
from data_types.company import Company
from data_types.price_item import PriceItem

import maps.automotive.libs.large_tests.lib.db as db


def test_creation_forbidden_without_tvm(user):
    server.post_company(user, permalink=123) >> 403


def test_creation_forbidden_with_illegal_tvm(user):
    server = get_default_server(tvm_id=ADVERT_TVM_ID + 1)
    server.post_company(user, permalink=123) >> 403


def test_invalid_permalink_type(user):
    server = get_default_server(tvm_id=ADVERT_TVM_ID)
    server.post_company(user, permalink="qwe") >> 422
    server.post_company(user, permalink=-1) >> 422
    server.post_company(user, permalink=0) >> 422


def get_sync_status(permalink):
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT sync_state
                FROM organizations
                WHERE company_permalink=%s
            """, (permalink,))
            return cur.fetchone()[0] == 'SpravSynched'


def test_creation_with_right_tvm(user):
    permalink = 123
    server = get_default_server(tvm_id=ADVERT_TVM_ID)
    server.post_company(user, permalink) >> 200

    assert not get_sync_status(permalink)

    company = Company(permalink=permalink)
    server.get_prices(user, company) >> 422

    price_item = PriceItem()
    server.post_price(user, price_item, company) >> 403


def test_creation_and_sync(user):
    permalink = 123
    company = Company(permalink=permalink)
    advert_server = get_default_server(tvm_id=ADVERT_TVM_ID)
    advert_server.post_company(user, permalink) >> 200

    assert not get_sync_status(permalink)

    server.get_prices(user, company) >> 403

    assert not get_sync_status(permalink)

    sprav.add_company(company)
    server.get_prices(user, company) >> 403
    assert not get_sync_status(permalink)

    sprav.add_company_permission(company, user)
    server.get_prices(user, company) >> 200
    assert get_sync_status(permalink)


def test_creation_already_exists(user):
    permalink = 123
    company = Company(permalink=permalink)
    company.register(user=user)

    server.get_prices(user, company) >> 200  # force sync with sprav

    assert get_sync_status(permalink)

    advert_server = get_default_server(tvm_id=ADVERT_TVM_ID)
    advert_server.post_company(user, permalink) >> 200

    assert get_sync_status(permalink)
