from datetime import datetime

import pytest

import hamcrest as hm

from billing.hot.calculators.taxi.calculator.core import migration
from billing.hot.calculators.taxi.calculator.core.entities.method import MigrationInfo

CLIENT_ID = 1
FIRM_ID = 121


@pytest.mark.parametrize('namespace, firm_id, client_id, migration_info, on_dt, expected', [
    pytest.param(
        'taxi', FIRM_ID, CLIENT_ID, [], datetime(2021, 1, 1), (False, False), id='Empty migrated info'
    ),
    pytest.param(
        'taxi', FIRM_ID, CLIENT_ID, [
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 1), dry_run=False, filter='Client',
                          object_id=CLIENT_ID),
            MigrationInfo(namespace='other', from_dt=datetime(2021, 1, 1), dry_run=True, filter='Client',
                          object_id=CLIENT_ID),
        ], datetime(2021, 1, 1), (True, False), id='Filter by client'
    ),
    pytest.param(
        'taxi', FIRM_ID, CLIENT_ID, [
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 1), dry_run=True, filter='Firm',
                          object_id=FIRM_ID),
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 1), dry_run=False, filter='', object_id=0),
        ], datetime(2021, 1, 1), (True, True), id='Filter by Firm/priority'
    ),
    pytest.param(
        'taxi', FIRM_ID, CLIENT_ID, [
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 1), dry_run=False, filter='Firm',
                          object_id=FIRM_ID),
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 1), dry_run=False, filter='', object_id=0),
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 1), dry_run=True, filter='Client',
                          object_id=CLIENT_ID),
        ], datetime(2021, 1, 1), (True, True), id='Filter by Client/priority'
    ),
    pytest.param(
        'taxi', FIRM_ID, CLIENT_ID, [
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 1), dry_run=False, filter='Firm',
                          object_id=FIRM_ID),
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 1), dry_run=True, filter='', object_id=0),
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 2), dry_run=True, filter='Client',
                          object_id=CLIENT_ID),
        ], datetime(2021, 1, 1), (True, False), id='Filter by Firm with non migrated Client/priority'
    ),
    pytest.param(
        'taxi', FIRM_ID, CLIENT_ID, [
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 2), dry_run=False, filter='Firm',
                          object_id=FIRM_ID),
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 2), dry_run=True, filter='', object_id=0),
            MigrationInfo(namespace='taxi', from_dt=datetime(2021, 1, 2), dry_run=True, filter='Client',
                          object_id=CLIENT_ID),
        ], datetime(2021, 1, 1), (False, False), id='Not migrated'
    ),
])
def test_migration(namespace, firm_id, client_id, migration_info, on_dt, expected):
    actual = migration.status(namespace, firm_id, client_id, migration_info, on_dt)
    hm.assert_that(actual, hm.equal_to(expected))
