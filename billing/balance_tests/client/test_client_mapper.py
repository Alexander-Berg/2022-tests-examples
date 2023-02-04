from tests import object_builder as ob
from tests.balance_tests.client.client_common import create_client


def test_creator_uid(session):
    client = create_client(session)
    session.flush()
    assert session.oper_id
    assert client.creator_uid == session.oper_id
