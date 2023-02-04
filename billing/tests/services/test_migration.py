from datetime import datetime

import hamcrest as hm
import pytest

from billing.library.python.calculator.models.method import MigrationInfoModel
from billing.library.python.calculator.services.migration import MigrationService


class TestMigrationService:
    CLIENT_ID = 1
    FIRM_ID = 121

    @pytest.mark.parametrize('namespace, firm_id, client_id, migration_info, on_dt, expected', [
        pytest.param(
            'taxi', FIRM_ID, CLIENT_ID, [], datetime(2021, 1, 1), (False, False), id='Empty migrated info'
        ),
        pytest.param(
            'taxi', FIRM_ID, CLIENT_ID, [
                MigrationInfoModel('taxi', datetime(2021, 1, 1), False, 'Client', CLIENT_ID),
                MigrationInfoModel('other', datetime(2021, 1, 1), True, 'Client', CLIENT_ID),
            ], datetime(2021, 1, 1), (True, False), id='Filter by client'
        ),
        pytest.param(
            'taxi', FIRM_ID, CLIENT_ID, [
                MigrationInfoModel('taxi', datetime(2021, 1, 1), True, 'Firm', FIRM_ID),
                MigrationInfoModel('taxi', datetime(2021, 1, 1), False, '', 0),
            ], datetime(2021, 1, 1), (True, True), id='Filter by Firm/priority'
        ),
        pytest.param(
            'taxi', FIRM_ID, CLIENT_ID, [
                MigrationInfoModel('taxi', datetime(2021, 1, 1), False, 'Namespace', -1),
                MigrationInfoModel('taxi', datetime(2021, 1, 1), False, 'Firm', FIRM_ID),
                MigrationInfoModel('taxi', datetime(2021, 1, 1), False, '', 0),
                MigrationInfoModel('taxi', datetime(2021, 1, 1), True, 'Client', CLIENT_ID),
            ], datetime(2021, 1, 1), (True, True), id='Filter by Client/priority'
        ),
        pytest.param(
            'taxi', FIRM_ID, CLIENT_ID, [
                MigrationInfoModel('taxi', datetime(2021, 1, 1), False, 'Firm', FIRM_ID),
                MigrationInfoModel('taxi', datetime(2021, 1, 1), True, '', 0),
                MigrationInfoModel('taxi', datetime(2021, 1, 2), True, 'Client', CLIENT_ID),
            ], datetime(2021, 1, 1), (True, False), id='Filter by Firm with non migrated Client/priority'
        ),
        pytest.param(
            'taxi', FIRM_ID, CLIENT_ID, [
                MigrationInfoModel('taxi', datetime(2021, 1, 2), False, 'Firm', FIRM_ID),
                MigrationInfoModel('taxi', datetime(2021, 1, 2), True, '', 0),
                MigrationInfoModel('taxi', datetime(2021, 1, 2), True, 'Client', CLIENT_ID),
            ], datetime(2021, 1, 1), (False, False), id='Not migrated'
        ),
        pytest.param(
            'taxi', FIRM_ID, CLIENT_ID, [
                MigrationInfoModel('taxi', datetime(2021, 1, 1), False, 'Namespace', -1),
            ], datetime(2021, 1, 1), (True, False), id='Filter by Namespace'
        ),
        pytest.param(
            'taxi', FIRM_ID, CLIENT_ID, [
                MigrationInfoModel('taxi', datetime(2021, 1, 1), True, 'Namespace', -1),
                MigrationInfoModel('taxi', datetime(2021, 1, 1), False, 'Firm', FIRM_ID),
            ], datetime(2021, 1, 1), (True, False), id='Filter by Firm/priority'
        ),
    ])
    def test_status(
        self,
        namespace,
        firm_id,
        client_id,
        migration_info,
        on_dt,
        expected,
    ):
        migration_service = MigrationService(namespace)

        actual = migration_service.status(firm_id, client_id, migration_info, on_dt)

        hm.assert_that(actual, hm.equal_to(expected))
