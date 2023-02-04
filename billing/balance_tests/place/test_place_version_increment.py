import datetime

from tests.balance_tests.place.common import *  # noqa


def test_place_update(session, place):
    version = place.version_id
    place.dt += datetime.timedelta(days=1)
    session.add(place)
    session.flush()
    session.refresh(place)
    assert place.version_id == version + 1


def test_add_product(session, place, page_data_2nd):
    version = place.version_id
    place.products.append(page_data_2nd)
    session.add(place)
    session.flush()
    session.refresh(place)
    assert place.version_id == version + 1
