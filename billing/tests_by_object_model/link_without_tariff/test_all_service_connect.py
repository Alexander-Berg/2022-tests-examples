from apikeys.tests_by_object_model.apikeys_object_model import User, Link, Service
from apikeys.apikeys_steps import get_free_login_from_autotest_login_pull
import pytest


def test_links(db_connection):
    user = User(get_free_login_from_autotest_login_pull(db_connection)[0])
    user.clean_up(db_connection)
    user.create_user_project()
    services = db_connection['service'].find({'questionnaire_id':{'$exists': True}},{'_id':True })
    for services in services:
        Link(user, Service(services['_id']),db_connection)
