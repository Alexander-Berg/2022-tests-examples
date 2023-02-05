from data_types.price_item import PriceItem, generate_prices
from data_types.price_overwrite import PriceOverwrite
from data_types.published_item import PublishedItem
from data_types.chain import Chain
from data_types.company import Company
import lib.async_processor as async_processor

from lib.server import server

from collections import Counter
import pytest


def make_and_post_item(user, company, is_hidden=None, empty_external_id=False):
    return generate_prices(user, company, 1, is_hidden=is_hidden, force_no_external_id=empty_external_id)[0]


def get_posted_items(user, company):
    response = (server.get_prices(user, company) >> 200)["items"]
    return [PriceItem.from_json(item) for item in response]


def some_items_published(user, company):
    items = get_posted_items(user, company)
    for item in items:
        if item.moderation['status'] == "Published":
            return True
    return False


def check_publishing(user, company):
    async_processor.perform_all_work()
    assert some_items_published(user, company)


def check_published_item(published_item, price_item, org=None):
    published_item.compare_with_price(price_item)
    if org is not None:
        assert published_item.organization_id == org.db_id
        if isinstance(org, Company):
            assert published_item.permalink == org.permalink
        else:
            assert published_item.permalink is None


def check_one_published_item(price_item, company):
    published_items = PublishedItem.get_all()
    assert len(published_items) == 1
    check_published_item(published_items[0], price_item, company)


def check_status_is_published(published_items):
    assert all(map(lambda item: item.status == 'Published', published_items))


def test_item_published_simple_case(user, company):
    price_item = make_and_post_item(user, company, is_hidden=False)
    check_publishing(user, company)
    check_one_published_item(price_item, company)

    history = price_item.get_history_from_db()
    assert len(history) > 0

    last_history_record = history[-1]
    diff = last_history_record['diff']
    assert diff['status'] == {
        'old': 'ReadyForPublishing',
        'new': 'Published',
    }


def test_item_published_without_moderation(user, company):
    price_item = PriceItem(is_hidden=False)
    price_item.without_group()
    price_item.saveToDb(company)

    check_publishing(user, company)
    check_one_published_item(price_item, company)


def test_publish_hidden_item(user, company):
    price_item = make_and_post_item(user, company, is_hidden=True)
    check_publishing(user, company)
    check_one_published_item(price_item, company)


@pytest.mark.parametrize("field_to_overwrite", ["is_hidden", "is_popular", "availability"])
def test_overwrite_item(user, field_to_overwrite):
    chain = Chain.generate(user=user)
    office = Company.generate(user=user, parent_chain=chain)

    price_item = make_and_post_item(user, chain, is_hidden=False)
    overwrite = PriceOverwrite.create_overwrite(price_item, field_to_overwrite)

    server.edit_chain_prices(user, [overwrite], office)

    check_publishing(user, chain)

    check_one_published_item(price_item, office)
    published_items = PublishedItem.get_all()
    check_status_is_published(published_items)
    assert len(published_items) == 1
    published_items[0].compare_with_price(price_item)


def test_dont_overwrite_hidden_item(user, company):
    chain = Chain.generate(user=user)
    office = Company.generate(user=user, parent_chain=chain)

    price_item = make_and_post_item(user, chain, is_hidden=True)

    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=False)
    server.edit_chain_prices(user, [overwrite], office)

    check_publishing(user, chain)

    check_one_published_item(price_item, office)
    published_items = PublishedItem.get_all()
    check_status_is_published(published_items)
    assert len(published_items) == 1
    assert published_items[0].is_hidden


def test_publish_chain_and_company_items(user, company):
    chain = Chain.generate(user=user)
    office1 = Company.generate(user=user, parent_chain=chain)
    office2 = Company.generate(user=user, parent_chain=chain)

    price_item = make_and_post_item(user, chain, is_hidden=False)
    company_price_item = make_and_post_item(user, company, is_hidden=False)

    async_processor.perform_all_work()
    assert len(PublishedItem.get_all()) == 3

    published_items = PublishedItem.get_all()
    check_status_is_published(published_items)
    ids_counter = Counter(item.id for item in published_items)
    assert ids_counter[price_item.id] == 2
    assert ids_counter[company_price_item.id] == 1

    org_ids_counter = Counter(item.organization_id for item in published_items)
    assert org_ids_counter[office1.db_id] == 1
    assert org_ids_counter[office2.db_id] == 1
    assert org_ids_counter[company.db_id] == 1


def test_publish_item_hidden_for_one_office(user):
    chain = Chain.generate(user=user)
    office1 = Company.generate(user=user, parent_chain=chain)
    office2 = Company.generate(user=user, parent_chain=chain)

    price_item = make_and_post_item(user, chain, is_hidden=False)

    price_item.is_hidden = True
    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
    server.edit_chain_prices(user, [overwrite], office1)

    async_processor.perform_all_work()

    published_items = PublishedItem.get_all()
    assert len(published_items) == 2
    check_status_is_published(published_items)

    org_ids_counter = Counter(item.organization_id for item in published_items)
    assert org_ids_counter[office1.db_id] == 1
    assert org_ids_counter[office2.db_id] == 1

    is_hidden_counter = Counter(item.is_hidden for item in published_items)
    assert is_hidden_counter[True] == 1
    assert is_hidden_counter[False] == 1


def test_publish_item_for_chain_without_offices(user):
    chain_with_office = Chain.generate(user=user)
    office = Company.generate(user=user, parent_chain=chain_with_office)
    officeless_chain = Chain.generate(user=user)

    make_and_post_item(user, officeless_chain, is_hidden=False)
    price_item_with_office = make_and_post_item(user, chain_with_office, is_hidden=False)

    check_publishing(user, chain_with_office)

    published_items = PublishedItem.get_all()
    check_status_is_published(published_items)

    assert len(published_items) == 1
    check_published_item(published_items[0], price_item_with_office, office)


def test_delete_published_items(user, company):
    price_item = make_and_post_item(user, company, is_hidden=False)

    check_publishing(user, company)

    published_items = PublishedItem.get_all()
    assert len(published_items) == 1

    server.delete_pricelist(user, company) >> 200
    async_processor.perform_all_work()
    assert PublishedItem.get_all()[0].status == 'Deleted'

    assert len(price_item.get_history_from_db()) == 3
    delete_record = price_item.get_history_from_db()[2]
    assert delete_record['diff']['status']['new'] == 'Deleted'


def test_delete_item_by_id(user, company):
    price_item = make_and_post_item(user, company, is_hidden=False)

    check_publishing(user, company)

    published_items = PublishedItem.get_all()
    assert len(published_items) == 1

    server.delete_prices(user, company, [price_item.id]) >> 200
    async_processor.perform_all_work()
    assert PublishedItem.get_all()[0].status == 'Deleted'


def test_revert_overwrite_item(user):
    chain = Chain.generate(user=user)
    office = Company.generate(user=user, parent_chain=chain)

    def check_publishing_and_get_items(price_item):
        check_publishing(user, chain)

        check_one_published_item(price_item, office)
        return PublishedItem.get_all()

    def get_one_item_is_hidden(price_item):
        published_items = check_publishing_and_get_items(price_item)
        assert len(published_items) == 1
        return published_items[0].is_hidden

    price_item = make_and_post_item(user, chain, is_hidden=False)

    price_item.is_hidden = True
    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
    server.edit_chain_prices(user, [overwrite], office)

    assert get_one_item_is_hidden(price_item) is True

    price_item.is_hidden = False
    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
    server.edit_chain_prices(user, [overwrite], office)

    async_processor.perform_all_work()
    assert get_one_item_is_hidden(price_item) is False


def test_chain_items_published_for_new_company(user):
    chain = Chain.generate(user)
    office1 = Company.generate(user=user, parent_chain=chain)
    make_and_post_item(user, chain, is_hidden=False)

    check_publishing(user, chain)

    office2 = Company.generate(user=user, parent_chain=chain)
    async_processor.perform_all_work()
    assert len(PublishedItem.get_all()) == 2

    published_items = PublishedItem.get_all()

    org_ids_counter = Counter(item.organization_id for item in published_items)
    assert org_ids_counter[office1.db_id] == 1
    assert org_ids_counter[office2.db_id] == 1


def attach_office_to_chain(user, office, chain):
    oid_bak = office.db_id
    office.db_id = None
    office.parent_chain = chain
    office.register(user)
    office.saveToDb()
    assert office.db_id == oid_bak


def test_chain_items_unpublished_after_chain_dettached(user):
    chain1 = Chain.generate(user)
    make_and_post_item(user, chain1, is_hidden=False)
    office = Company.generate(user=user, parent_chain=chain1)

    async_processor.perform_all_work()
    assert len(PublishedItem.get_all()) == 1

    chain2 = Chain.generate(user)
    attach_office_to_chain(user, office, chain2)

    async_processor.perform_all_work()
    assert PublishedItem.get_all()[0].status == 'Deleted'


def test_chain_items_changed_after_move_company_to_other_chain(user):
    chain1 = Chain.generate(user)
    office = Company.generate(user=user, parent_chain=chain1)
    price_item1 = make_and_post_item(user, chain1, is_hidden=False)

    chain2 = Chain.generate(user)
    price_item2 = make_and_post_item(user, chain2, is_hidden=False)

    async_processor.perform_all_work()
    assert len(PublishedItem.get_all()) == 1

    def check_published_items_statuses(d):
        get_status = lambda: dict(
            Counter((item.status, item.id) for item in PublishedItem.get_all())
        )
        async_processor.perform_all_work()
        assert get_status() == d, "Expected status: %s, actual: %s" % (d, get_status())

    check_published_items_statuses({
        ('Published', price_item1.id): 1,
    })

    attach_office_to_chain(user, office, chain2)
    check_published_items_statuses({
        ('Deleted', price_item1.id): 1,
        ('Published', price_item2.id): 1
    })

    attach_office_to_chain(user, office, chain1)
    check_published_items_statuses({
        ('Published', price_item1.id): 1,
        ('Deleted', price_item2.id): 1
    })

    attach_office_to_chain(user, office, None)
    check_published_items_statuses({
        ('Deleted', price_item1.id): 1,
        ('Deleted', price_item2.id): 1
    })


def test_publish_multiple_offices(user):
    chain = Chain.generate(user=user)
    Company.generate(user=user, parent_chain=chain)
    Company.generate(user=user, parent_chain=chain)

    price_item = make_and_post_item(user, chain, is_hidden=False)

    async_processor.perform_all_work()
    assert len(PublishedItem.get_all()) == 2

    published_items = PublishedItem.get_all()
    for published_item in published_items:
        check_published_item(published_item, price_item)
