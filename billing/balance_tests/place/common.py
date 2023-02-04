import pytest

from tests import object_builder as ob


@pytest.fixture()
def client(session):
    yield ob.ClientBuilder.construct(session)


@pytest.fixture()
def distribution_tag(session, client):
    yield ob.DistributionTagBuilder.construct(session, client_id=client.id)


@pytest.fixture()
def page_data(session):
    yield ob.PageDataBuilder.construct(session)


@pytest.fixture()
def page_data_2nd(session):
    yield ob.PageDataBuilder.construct(session)


@pytest.fixture()
def place(session, client, distribution_tag, page_data):
    yield ob.PlaceBuilder.construct(session, client=client, tag=distribution_tag, products=[page_data])
