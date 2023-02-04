import pytest
import lib.server as server

import maps.automotive.libs.large_tests.lib.datasync as datasync
import maps.automotive.libs.large_tests.lib.db as db
import maps.automotive.libs.large_tests.lib.fakeenv as fakeenv

from data_types.user import User
from maps.automotive.libs.large_tests.lib.yacare import wait_for_yacare_startup


@pytest.fixture()
def user():
    user = User()
    user.register()
    yield user


@pytest.fixture(autouse=True)
def with_started_services():
    db.initialize("maps/automotive/carwashes/docs/db.sql")
    wait_for_yacare_startup(server.get_url(), server.get_host())
    datasync.add_collection(
        application_id="maps_common",
        dataset_id="ynavicarinfo",
        collection_id="carwashes",
    )
    datasync.set_default_collection(
        application_id="maps_common",
        dataset_id="ynavicarinfo",
        collection_id="carwashes",
    )


@pytest.fixture(autouse=True)
def with_clear_fakeenv():
    yield
    fakeenv.reset()
    db.reset()
