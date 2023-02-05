from data_types.price_item import PriceItem, generate_prices, set_prices_age_days
from data_types.published_item import PublishedItem

from lib.server import server
import lib.async_processor as async_processor


def test_remove_old_deleted_items(user, company):
    prices_count = 5
    generate_prices(user, company, count=prices_count, is_hidden=False, force_no_external_id=True)

    async_processor.perform_all_work()
    assert len(PublishedItem.get_all()) == prices_count

    server.delete_pricelist(user, company) >> 200

    set_prices_age_days(20)

    async_processor.perform_all_work()
    assert PriceItem.get_items_count_from_db() == 0
    assert len(PublishedItem.get_all()) == 0


def test_keep_old_deleted_items_with_external_id(user, company):
    prices_count = 5
    generate_prices(user, company, count=prices_count, is_hidden=False)

    async_processor.perform_all_work()
    assert len(PublishedItem.get_all()) == prices_count

    server.delete_pricelist(user, company) >> 200

    set_prices_age_days(20)

    async_processor.perform_all_work()
    assert PriceItem.get_items_count_from_db() != 0

    published = PublishedItem.get_all()
    assert len(published) == prices_count
    for published_item in published:
        assert published_item.status == 'Deleted'
