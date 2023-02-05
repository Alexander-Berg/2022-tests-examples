import maps.automotive.libs.large_tests.lib.db as db
import lib.async_processor as async_processor

from data_types.company import Company
from data_types.price_item import PriceItem, generate_prices

from lib.server import server


def insert_duplicates(permalink, head_permalink, is_handled=False):
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO companies_duplicates (permalink, head_permalink, is_handled)
                VALUES (%s, %s, FALSE)
                ON CONFLICT (permalink) DO UPDATE SET
                    head_permalink=EXCLUDED.head_permalink,
                    is_handled=FALSE
                """,
                (permalink, head_permalink))
            conn.commit()


def duplicate_handled(permalink):
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT is_handled
                FROM companies_duplicates
                WHERE permalink = %s
            """, (permalink,))
            return bool(cur.fetchone()[0])


def compare_prices(company_prices, head_prices):
    for (company_price, head_price) in zip(company_prices, head_prices):
        assert company_price.compare(head_price, ignore_fields={"id"})


def test_items_copy(user):
    company = Company.generate(user=user)
    head = Company.generate(user=user)

    company_prices = generate_prices(user, company, count=4)

    insert_duplicates(company.permalink, head.permalink)

    async_processor.perform_all_work()
    assert duplicate_handled(company.permalink)

    head_prices = PriceItem.list_from_json(server.get_prices(user, head) >> 200)
    compare_prices(company_prices, head_prices)


def test_duplicate_not_handled_until_head_exists(user):
    company = Company.generate(user=user)

    head = Company()
    head.register(user=user)

    company_prices = generate_prices(user, company, count=4)

    insert_duplicates(company.permalink, head.permalink)

    async_processor.perform_all_work()
    assert not duplicate_handled(company.permalink)

    head.saveToDb()

    async_processor.perform_all_work()
    assert duplicate_handled(company.permalink)

    head_prices = PriceItem.list_from_json(server.get_prices(user, head) >> 200)
    compare_prices(company_prices, head_prices)


def test_head_already_has_items(user):
    company = Company.generate(user=user)
    head = Company.generate(user=user)

    company_prices = generate_prices(user, company, count=4)
    head_prices = generate_prices(user, head, count=3)

    insert_duplicates(company.permalink, head.permalink)

    async_processor.perform_all_work()
    assert duplicate_handled(company.permalink)

    new_head_prices = PriceItem.list_from_json(server.get_prices(user, head) >> 200)
    compare_prices(head_prices, new_head_prices)

    new_company_prices = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    compare_prices(company_prices, new_company_prices)


def test_multiple_duplicates_one_has_prices(user):
    company1 = Company.generate(user=user)
    company2 = Company.generate(user=user)
    head = Company.generate(user=user)

    company2_prices = generate_prices(user, company2, count=2)

    insert_duplicates(company1.permalink, head.permalink)
    insert_duplicates(company2.permalink, head.permalink)

    async_processor.perform_all_work()
    assert duplicate_handled(company1.permalink)
    assert duplicate_handled(company2.permalink)

    head_prices = PriceItem.list_from_json(server.get_prices(user, head) >> 200)
    compare_prices(company2_prices, head_prices)


def test_multiple_duplicates_both_has_prices(user):
    company1 = Company.generate(user=user)
    company2 = Company.generate(user=user)
    head = Company.generate(user=user)

    company1_prices = generate_prices(user, company1, count=2)
    company2_prices = generate_prices(user, company2, count=3)

    insert_duplicates(company1.permalink, head.permalink)
    insert_duplicates(company2.permalink, head.permalink)

    async_processor.perform_all_work()
    assert duplicate_handled(company1.permalink)
    assert duplicate_handled(company2.permalink)

    head_prices = PriceItem.list_from_json(server.get_prices(user, head) >> 200)

    expected_prices = company1_prices if company1.permalink < company2.permalink else company2_prices
    compare_prices(expected_prices, head_prices)


def test_head_items_deleted(user):
    company = Company.generate(user=user)
    head = Company.generate(user=user)

    prices = [PriceItem() for i in range(5)]

    noExternalIdPrice = PriceItem()
    noExternalIdPrice.external_id = None
    prices.append(noExternalIdPrice)

    for price in prices:
        server.post_price(user, price, head) >> 200

        price.title = price.title[:len(price.title) // 2] + "_changed"
        server.post_price(user, price, company) >> 200

    head_prices = PriceItem.list_from_json(server.get_prices(user, head) >> 200)
    duplicate_prices = PriceItem.list_from_json(server.get_prices(user, company) >> 200)

    server.delete_pricelist(user, head) >> 200

    insert_duplicates(company.permalink, head.permalink)

    async_processor.perform_all_work()
    assert duplicate_handled(company.permalink)

    new_head_prices = PriceItem.list_from_json(server.get_prices(user, head) >> 200)
    compare_prices(duplicate_prices, new_head_prices)

    raw_head_items = (server.get_prices(user, head) >> 200)["items"]
    assert all(p["moderation"]["status"] in ["ReadyForPublishing", "Published"] for p in raw_head_items), \
        f"Unexpected statuses: {', '.join(p['moderation']['status'] for p in raw_head_items)}"

    # id should not change, cause of same external_id
    assert {p.id for p in new_head_prices if p.external_id is not None} == {p.id for p in head_prices if p.external_id is not None}
