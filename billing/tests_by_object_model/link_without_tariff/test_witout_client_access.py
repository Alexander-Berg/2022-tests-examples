from apikeys.tests_by_object_model.apikeys_object_model import User, Link, Service
from apikeys.apikeys_steps import get_free_login_from_autotest_login_pull
import pytest


@pytest.mark.parametrize(
    'test_data', [{'service_cc': 'trends'}],
    ids=lambda x: x['service_cc'])
def test_link(db_connection,test_data):
    user = User(get_free_login_from_autotest_login_pull(db_connection)[0])
    user.clean_up(db_connection)
    user.create_user_project()
    service = Service(cc=test_data['service_cc'])
    link = Link(user, service, db_connection)
