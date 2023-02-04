from apikeys.tests_by_object_model.apikeys_object_model import User,Link,Service
from apikeys.apikeys_steps_new import get_free_login_from_autotest_login_pull
from apikeys.API_apikeys import api as api
import pytest


@pytest.fixture(scope='session')
def prepare_apikeys_data(db_connection):
    user=User(get_free_login_from_autotest_login_pull())
    user.clean_up(db_connection)
    service=Service(37)
    test_link=Link(user,service,db_connection)
    return test_link

def test_api(test_link):
    pass