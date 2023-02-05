from data_types.price_item import PriceItem, generate_prices
from data_types.chain import Chain
from lib.server import server


def generate_prices_for_chain(chain, user):
    generated_prices = generate_prices(user, chain)
    prices = server.get_prices(user, chain) >> 200
    prices = PriceItem.list_from_json(prices)
    assert prices == generated_prices
    return generated_prices


def test_get_regional_pricelist(regional_chains, user):
    expected_pricelists = []
    for chain in regional_chains:
        expected_pricelists.append(generate_prices_for_chain(chain, user))

    for chain, expected_pricelist in zip(regional_chains, expected_pricelists):
        prices = PriceItem.list_from_json(server.get_prices(user, chain) >> 200)
        assert prices == expected_pricelist


def test_get_regional_chain_pricelist(regional_chain_companies, user):
    expected_pricelists = []

    for company in regional_chain_companies:
        expected_pricelists.append(generate_prices_for_chain(company.parent_chain, user))

    for chain_company, expected_pricelist in zip(regional_chain_companies, expected_pricelists):
        prices = PriceItem.list_from_json(server.get_prices(user, chain_company, is_chain=True) >> 200)
        assert prices == expected_pricelist


def test_get_deleted_price_chain(regional_chain_companies, user):
    expected_pricelists = []

    for company in regional_chain_companies:
        expected_pricelists.append(generate_prices_for_chain(company.parent_chain, user))

    for chain_company, expected_pricelist in zip(regional_chain_companies, expected_pricelists):
        ids_list = [price.id for price in expected_pricelist]
        server.delete_prices(user, chain_company.parent_chain, ids_list) >> 200
        prices = PriceItem.list_from_json(server.get_prices(user, chain_company, is_chain=True) >> 200)
        assert len(prices) == 0


def test_get_regional_chain_price(regional_chain_companies, user):
    expected_pricelists = []

    for company in regional_chain_companies:
        expected_pricelists.append(generate_prices_for_chain(company.parent_chain, user))

    for chain_company, expected_pricelist in zip(regional_chain_companies, expected_pricelists):
        expected_ids = [price.id for price in expected_pricelist]

        prices_json = server.get_prices_by_id(user, chain_company, id=expected_ids) >> 200
        prices = prices_json['items']

        assert len(prices) == len(expected_pricelist)
        for price in prices:
            assert PriceItem.from_json(price) in expected_pricelist


def test_get_deleted_price_chain_reject(regional_chain_companies, user):
    expected_pricelists = []

    for company in regional_chain_companies:
        expected_pricelists.append(generate_prices_for_chain(company.parent_chain, user))

    for chain_company, expected_pricelist in zip(regional_chain_companies, expected_pricelists):
        for expected_price in expected_pricelist:
            server.delete_prices(user, chain_company.parent_chain, [expected_price.id]) >> 200
            resp = server.get_prices_by_id(user, chain_company, id=expected_price.id) >> 200
            assert len(resp['items']) == 0


def test_get_regional_chain_price_bad_parameters(regional_chain_companies, user):
    for chain_company in regional_chain_companies:
        resp = server.get_prices_by_id(user, chain_company) >> 200
        assert len(resp['items']) == 0

        server.get_prices_by_id(user, chain_company, id='string_id') >> 422


def test_get_chain_regions(regional_chains, user):
    assert len(set(chain.permalink for chain in regional_chains)) == 1
    chain_regions = server.get_chain_regions(user, regional_chains[0].permalink) >> 200
    unique_chain_regions = set(region['id'] for region in chain_regions)
    assert len(unique_chain_regions) == len(chain_regions)
    assert all(chain.region_id in unique_chain_regions for chain in regional_chains)


def test_get_chain_regions_for_regional_manager(regional_chains, alien_user):
    assert len(set(chain.permalink for chain in regional_chains)) == 1
    chain_permalink = regional_chains[0].permalink
    server.get_chain_regions(alien_user, chain_permalink) >> 403

    alien_user_regions = {213, 1}
    for chain in regional_chains:
        if chain.region_id in alien_user_regions:
            chain.register(alien_user)

    alien_user_actual_regions = {
        r['id'] for r in (server.get_chain_regions(alien_user, chain_permalink) >> 200)
    }
    assert all(r in alien_user_actual_regions for r in alien_user_regions)


def test_chain_regions_access_sprav_synch_with_pagination(user):
    chain_count = 5
    chains = [Chain.generate(user) for _ in range(chain_count)]

    for chain in chains:
        assert len(server.get_chain_regions(user, chain.permalink) >> 200) != 0


def test_get_chain_regions_depends_on_global_role(regional_chains, alien_user):
    assert len(set(chain.permalink for chain in regional_chains)) == 1
    permalink = regional_chains[0].permalink
    server.get_chain_regions(alien_user, permalink) >> 403

    all_regions = set(chain.region_id for chain in regional_chains)

    chain = next(chain for chain in regional_chains if chain.region_id != 10000)
    chain.register(alien_user, global_role='client')

    client_regions = {
        r['id'] for r in (server.get_chain_regions(alien_user, permalink) >> 200)
    }
    assert chain.region_id in client_regions
    assert client_regions < all_regions

    chain.register(alien_user, global_role='superuser')
    superuser_regions = {
        r['id'] for r in (server.get_chain_regions(alien_user, permalink) >> 200)
    }
    assert chain.region_id in superuser_regions

    assert superuser_regions >= client_regions
    assert superuser_regions == all_regions
