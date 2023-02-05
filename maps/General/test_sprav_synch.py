from data_types.company import Company
from data_types.chain import Chain
from lib.server import server
from lib.random import printable_str

import maps.automotive.libs.large_tests.lib.db as db

import random
import sys


def generate_permalink():
    return random.randint(0, sys.maxsize)


def get_chain_count(chain_permalink):
    with db.get_connection() as conn:
        cur = conn.cursor()
        cur.execute(
            """
            SELECT COUNT(*) AS cnt FROM organizations
            WHERE chain_permalink = %s
            """,
            (chain_permalink,))
        return cur.fetchone()[0]


def get_chains_by_permalink(chain_permalink):
    with db.get_connection() as conn:
        cur = conn.cursor()
        cur.execute(
            """
            SELECT chain_permalink, chain_region FROM organizations
            WHERE chain_permalink = %s
            """,
            (chain_permalink,))
        return cur.fetchall()


def get_company_count(company_permalink):
    with db.get_connection() as conn:
        cur = conn.cursor()
        cur.execute(
            """
            SELECT COUNT(*) AS cnt FROM organizations
            WHERE company_permalink = %s
            """,
            (company_permalink,))
        return cur.fetchone()[0]


def test_check_synch_with_sprav(user, chain):
    assert chain.region_id == 10000
    company = Company(
        permalink=generate_permalink(),
        parent_chain=chain)

    response = server.get_prices(user, company) >> 403
    assert response["code"] == "PERMISSION_DENIED"

    assert get_company_count(company.permalink) == 0

    company.register(user=user)

    server.get_prices(user, company) >> 200

    with db.get_connection() as conn:
        cur = conn.cursor()
        cur.execute(
            """
            SELECT parent_organization_id FROM organizations
            WHERE company_permalink = %s
            """,
            (company.permalink,))
        assert cur.fetchone()[0] == chain.db_id


def test_check_synch_chain_with_sprav(user):
    chain = Chain(region_id=213)

    response = server.get_prices(user, chain) >> 403
    assert response["code"] == "PERMISSION_DENIED"

    assert get_chain_count(chain.permalink) == 0

    chain.register(user=user)

    server.get_prices(user, chain) >> 200

    for actual_chain in get_chains_by_permalink(chain.permalink):
        assert actual_chain[0] == chain.permalink
        assert actual_chain[1] == chain.region_id


def test_company_synced_to_earth_chain(user):
    chain = Chain(region_id=213)
    company = Company(
        permalink=generate_permalink(),
        parent_chain=chain)

    response = server.get_prices(user, company) >> 403
    assert response["code"] == "PERMISSION_DENIED"

    assert get_chain_count(chain.permalink) == 0

    company.register(user=user)

    server.get_prices(user, company) >> 200

    for actual_chain in get_chains_by_permalink(chain.permalink):
        assert actual_chain[0] == chain.permalink
        assert actual_chain[1] == 10000


def test_company_synced_to_parent_chain_if_chain_in_db(user):
    chain = Chain(region_id=213)
    company = Company(
        permalink=generate_permalink(),
        parent_chain=chain)

    response = server.get_prices(user, company) >> 403
    assert response["code"] == "PERMISSION_DENIED"

    assert get_chain_count(chain.permalink) == 0

    chain.saveToDb()
    company.register(user=user)

    server.get_prices(user, company) >> 200

    for actual_chain in get_chains_by_permalink(chain.permalink):
        assert actual_chain[0] == chain.permalink
        assert actual_chain[1] == chain.region_id


def test_get_chain_regions_only_synced(regional_chains, alien_user):
    assert len(set(chain.permalink for chain in regional_chains)) == 1
    all_regions = set(chain.region_id for chain in regional_chains)
    permalink = regional_chains[0].permalink
    server.get_chain_regions(alien_user, permalink) >> 403

    regional_chains[0].register(alien_user, global_role='superuser')
    regions = {
        r['id'] for r in server.get_chain_regions(alien_user, permalink) >> 200
    }

    not_synced_region_id = 10174
    assert not_synced_region_id not in all_regions and not_synced_region_id not in regions

    not_synced_chain = Chain(
        permalink=permalink,
        region_id=not_synced_region_id)
    not_synced_chain.saveToDb()
    regions_wo_sync = {
        r['id'] for r in server.get_chain_regions(alien_user, not_synced_chain.permalink) >> 200
    }
    assert regions_wo_sync == regions

    not_synced_chain.register()
    regions_synced = {
        r['id'] for r in server.get_chain_regions(alien_user, not_synced_chain.permalink) >> 200
    }
    assert regions_wo_sync != regions_synced
    assert not_synced_region_id in regions_synced


def test_check_address_sync(user):
    company = Company()

    def get_company_address():
        with db.get_connection() as conn:
            cur = conn.cursor()
            cur.execute(
                """
                SELECT address, locale
                FROM company_info
                JOIN organizations ON company_info.organization_id = organizations.id
                WHERE organizations.company_permalink = %s
                """,
                (company.permalink,))
            row = cur.fetchone()
            return {"value": row[0], "locale": row[1]} if row else None

    company.address = None
    company.register(user)
    assert get_company_address() is None

    generate = lambda: printable_str(5, 50)
    same_value = generate()
    same_locale = generate()
    addresses = [
        {"value": generate(), "locale": generate()},
        None,
        {"value": same_value, "locale": generate()},
        {"value": same_value, "locale": same_locale},
        {"value": generate(), "locale": same_locale},
        {"locale": generate()},
        {"value": generate()},
    ]

    previous_valid_address = None
    for addr in addresses:
        company.address = addr
        company.register()
        server.get_prices(user, company) >> 200  # force sync with sprav
        addr_is_valid = addr is not None and addr.get("value") and addr.get("locale")
        assert get_company_address() == (addr if addr_is_valid else previous_valid_address)
        if addr_is_valid:
            previous_valid_address = addr
