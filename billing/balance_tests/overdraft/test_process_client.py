# -*- coding: utf-8 -*-

import pytest
import mock
import hamcrest
from decimal import Decimal as D
import datetime

from balance import exc, mapper
from balance.overdraft.limit import process_client
from balance.constants import (
    FirmId,
    ServiceId,
    ExternalOverdraftMergePolicy
)

from common import patch_sfop

VERY_LONG_ERROR = 'Very long error: ' + 'AAAAAA' * 666


SERVICE_ID = ServiceId.DIRECT
FIRM_ID = FirmId.YANDEX_OOO


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 999, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop()]))
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_first_overdraft(session, client):
    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=999,
                    fixed_limit=None,
                    description="external limit isn't allowed -> calculated internal limit will be used")
            ),
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 900, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop()]))
@pytest.mark.parametrize(
    'fixed_limit, external_limit, expected_limit, expected_limit_wo_tax, expected_description',
    (
        [1,    False, 333,         300,         "external limit isn't allowed -> fixed internal limit will be used"],
        [None, False, 999,         900,         "external limit isn't allowed -> calculated internal limit will be used"],
        [1,    True,  D('333.67'), D('278.05'), "using default external limit calculator"],
        [None, True,  D('333.67'), D('278.05'), "using default external limit calculator"]
    )
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_base(session, client, fixed_limit, external_limit, expected_limit, expected_limit_wo_tax, expected_description):
    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, 'RUB', 300)

    client_overdraft = client.overdraft.get((SERVICE_ID, FIRM_ID))
    client_overdraft.fixed_limit = fixed_limit
    session.flush()

    if external_limit:
        client_external_overdraft = mapper.ClientExternalOverdraft(
            client_id=client.id,
            service_id=SERVICE_ID,
            iso_currency='RUB',
            overdraft_limit=D('333.67')
        )
        session.add(client_external_overdraft)
        session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(overdraft_limit=333, overdraft_limit_wo_tax=300, fixed_limit=fixed_limit)
            )
        )
    )

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=expected_limit,
                    overdraft_limit_wo_tax=expected_limit_wo_tax,
                    fixed_limit=fixed_limit,
                    description=expected_description
                ),
            ),
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop()]))
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_external_limit_wo_calculated_limit(session, client):
    client_external_overdraft = mapper.ClientExternalOverdraft(
        client_id=client.id,
        service_id=SERVICE_ID,
        iso_currency='RUB',
        overdraft_limit=111
    )
    session.add(client_external_overdraft)
    session.flush()

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.all_of(
                hamcrest.has_length(1),
                hamcrest.has_entry(
                    (SERVICE_ID, FIRM_ID),
                    hamcrest.has_properties(
                        overdraft_limit=111,
                        overdraft_limit_wo_tax=D('92.5'),
                        description="using default external limit calculator"
                    ),
                ),
            )
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 999, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop(service_id=service_id) for service_id in [SERVICE_ID, SERVICE_ID - 1, SERVICE_ID + 1]]))
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_external_limit_wrong_currency(session, client):
    SERVICE_ID_2 = SERVICE_ID - 1
    SERVICE_ID_3 = SERVICE_ID + 1

    client.set_overdraft_limit(SERVICE_ID,   FIRM_ID, 333, 'RUB')
    client.set_overdraft_limit(SERVICE_ID_2, FIRM_ID, 333, 'RUB')
    client.set_overdraft_limit(SERVICE_ID_3, FIRM_ID, 333, 'RUB')
    client_external_overdraft = mapper.ClientExternalOverdraft(
        client_id=client.id,
        service_id=SERVICE_ID,
        iso_currency='USD',
        overdraft_limit=111
    )
    session.add(client_external_overdraft)
    session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.all_of(
                hamcrest.has_length(3),
                hamcrest.has_entry(
                    (SERVICE_ID, FIRM_ID),
                    hamcrest.has_properties(overdraft_limit=333),
                ),
                hamcrest.has_entry(
                    (SERVICE_ID_2, FIRM_ID),
                    hamcrest.has_properties(overdraft_limit=333),
                ),
                hamcrest.has_entry(
                    (SERVICE_ID_3, FIRM_ID),
                    hamcrest.has_properties(overdraft_limit=333),
                ),
            )
        )
    )

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.all_of(
                hamcrest.has_length(3),
                hamcrest.has_entry(
                    (SERVICE_ID, FIRM_ID),
                    hamcrest.has_properties(
                        overdraft_limit=333,
                        description="using default external limit calculator -> imported overdraft currency != current overdraft currency -> limit won't be updated".format(client.id, FIRM_ID)
                    ),
                ),
                hamcrest.has_entry(
                    (SERVICE_ID_2, FIRM_ID),
                    hamcrest.has_properties(
                        overdraft_limit=999,
                        description="external limit isn't allowed -> calculated internal limit will be used"
                    ),
                ),
                hamcrest.has_entry(
                    (SERVICE_ID_3, FIRM_ID),
                    hamcrest.has_properties(
                        overdraft_limit=999,
                        description="external limit isn't allowed -> calculated internal limit will be used"
                    ),
                ),
            )
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 999, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop(service_id=service_id, thresholds=thresholds) for service_id, thresholds in [(SERVICE_ID, {'null': 1}), (SERVICE_ID - 1, None), (SERVICE_ID + 1, None)]]))
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_external_limit_no_currency(session, client):
    SERVICE_ID_2 = SERVICE_ID - 1
    SERVICE_ID_3 = SERVICE_ID + 1

    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, 'RUB')
    client.set_overdraft_limit(SERVICE_ID_2, FIRM_ID, 333, 'RUB')
    client.set_overdraft_limit(SERVICE_ID_3, FIRM_ID, 333, 'RUB')
    client_external_overdraft = mapper.ClientExternalOverdraft(
        client_id=client.id,
        service_id=SERVICE_ID,
        iso_currency='RUB',
        overdraft_limit=111
    )
    session.add(client_external_overdraft)
    session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.all_of(
                hamcrest.has_length(3),
                hamcrest.has_entry(
                    (SERVICE_ID, FIRM_ID),
                    hamcrest.has_properties(overdraft_limit=333),
                ),
                hamcrest.has_entry(
                    (SERVICE_ID_2, FIRM_ID),
                    hamcrest.has_properties(overdraft_limit=333),
                ),
                hamcrest.has_entry(
                    (SERVICE_ID_3, FIRM_ID),
                    hamcrest.has_properties(overdraft_limit=333),
                ),
            )
        )
    )

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.all_of(
                hamcrest.has_length(3),
                hamcrest.has_entry(
                    (SERVICE_ID, FIRM_ID),
                    hamcrest.has_properties(
                        overdraft_limit=333,
                        description="using default external limit calculator -> imported overdraft currency is not allowed for firm -> limit won't be updated".format(client.id, FIRM_ID)
                    ),
                ),
                hamcrest.has_entry(
                    (SERVICE_ID_2, FIRM_ID),
                    hamcrest.has_properties(
                        overdraft_limit=999,
                        description="external limit isn't allowed -> calculated internal limit will be used"
                    )
                ),
                hamcrest.has_entry(
                    (SERVICE_ID_3, FIRM_ID),
                    hamcrest.has_properties(
                        overdraft_limit=999,
                        description="external limit isn't allowed -> calculated internal limit will be used"
                    )
                ),
            )
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 999, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop()]))
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_external_limit_after_ban(session, client):
    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 1, None)
    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 0, None)
    client_external_overdraft = mapper.ClientExternalOverdraft(
        client_id=client.id,
        service_id=SERVICE_ID,
        iso_currency='RUB',
        overdraft_limit=111
    )
    session.add(client_external_overdraft)
    session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                    (SERVICE_ID, FIRM_ID),
                    hamcrest.has_properties(overdraft_limit=0, iso_currency=None)
            )
        )
    )

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                    (SERVICE_ID, FIRM_ID),
                    hamcrest.has_properties(
                        overdraft_limit=111,
                        description='using default external limit calculator'
                    ),
            )
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=VERY_LONG_ERROR))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 999, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop()]))
@pytest.mark.parametrize('fixed_limit', [1, None])
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_checker_fail(session, client, fixed_limit):
    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, None)

    client_overdraft = client.overdraft.get((SERVICE_ID, FIRM_ID))
    client_overdraft.fixed_limit = fixed_limit
    session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(overdraft_limit=333, fixed_limit=fixed_limit)
            ),
        )
    )

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=0,
                    fixed_limit=None,
                    description=VERY_LONG_ERROR
                )
            ),
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 900, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop()]))
@pytest.mark.parametrize(
    'valid_until, expected_limit, expected_limit_wo_tax, expected_description',
    (
        [None,                        D('333.67'), D('278.05'), "using default external limit calculator"],
        [datetime.date(2020, 9, 1),   999,         900,         "external limit isn't allowed -> calculated internal limit will be used"],
        [datetime.date(9999, 10, 20), D('333.67'), D('278.05'), "using default external limit calculator"],
    )
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_external_deadline(session, client, valid_until, expected_limit, expected_limit_wo_tax, expected_description):
    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, 'RUB', 300)

    session.flush()

    client_external_overdraft = mapper.ClientExternalOverdraft(
        client_id=client.id,
        service_id=SERVICE_ID,
        iso_currency='RUB',
        overdraft_limit=D('333.67'),
        valid_until=valid_until,
    )
    session.add(client_external_overdraft)
    session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=333,
                    overdraft_limit_wo_tax=300
                )
            )
        )
    )

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=expected_limit,
                    overdraft_limit_wo_tax=expected_limit_wo_tax,
                    description=expected_description
                )
            )
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.checks.ActCheck.check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 900, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop()]))
@pytest.mark.parametrize(
    'merge_policy, expected_limit, expected_limit_wo_tax, expected_description',
    (
        [None,                                                  D('333.67'), D('278.05'), "using default external limit calculator"],
        [ExternalOverdraftMergePolicy.EXTERNAL_ONLY,            D('333.67'), D('278.05'), "using default external limit calculator"],
        [ExternalOverdraftMergePolicy.TAKE_DEFAULT_IF_POSITIVE, 999,         900,         "merge policy: TAKE_DEFAULT_IF_POSITIVE -> calculated internal limit will be used"],
    )
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_external_merge_with_internal(session, client, merge_policy, expected_limit, expected_limit_wo_tax, expected_description):
    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, 'RUB', 300)

    session.flush()

    client_external_overdraft = mapper.ClientExternalOverdraft(
        client_id=client.id,
        service_id=SERVICE_ID,
        iso_currency='RUB',
        overdraft_limit=D('333.67'),
        merge_policy=merge_policy,
    )
    session.add(client_external_overdraft)
    session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=333,
                    overdraft_limit_wo_tax=300
                )
            )
        )
    )

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=expected_limit,
                    overdraft_limit_wo_tax=expected_limit_wo_tax,
                    description=expected_description
                )
            )
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.checks.ActCheck.check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(0, 0, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop()]))
@pytest.mark.parametrize(
    'merge_policy, expected_limit, expected_limit_wo_tax, expected_description',
    (
        [None,                                                  D('333.67'), D('278.05'), "using default external limit calculator"],
        [ExternalOverdraftMergePolicy.EXTERNAL_ONLY,            D('333.67'), D('278.05'), "using default external limit calculator"],
        [ExternalOverdraftMergePolicy.TAKE_DEFAULT_IF_POSITIVE, D('333.67'), D('278.05'), "merge policy: TAKE_DEFAULT_IF_POSITIVE -> calculated internal limit will be used -> got zero internal limit, external one will be used instead -> using default external limit calculator"],
    )
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_external_merge_without_internal(session, client, merge_policy, expected_limit, expected_limit_wo_tax, expected_description):
    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, 'RUB', 300)

    session.flush()

    client_external_overdraft = mapper.ClientExternalOverdraft(
        client_id=client.id,
        service_id=SERVICE_ID,
        iso_currency='RUB',
        overdraft_limit=D('333.67'),
        merge_policy=merge_policy,
    )
    session.add(client_external_overdraft)
    session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(overdraft_limit=333, overdraft_limit_wo_tax=300)
            )
        )
    )

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=expected_limit,
                    overdraft_limit_wo_tax=expected_limit_wo_tax,
                    description=expected_description
                ),
            ),
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.checks.ActCheck.check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(0, 0, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop(service_id=ServiceId.MARKET, is_agency_allowed=0)]))
@pytest.mark.parametrize(
    'iso_currency, expected_limit, expected_limit_wo_tax, expected_description',
    (
        ['RUB', 10, None, "using MARKET external limit calculator"],
    )
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_external_market(session, client, iso_currency, expected_limit, expected_limit_wo_tax, expected_description):
    client.set_overdraft_limit(ServiceId.MARKET, FIRM_ID, 333, None, None)

    session.flush()

    client_external_overdraft = mapper.ClientExternalOverdraft(
        client_id=client.id,
        service_id=ServiceId.MARKET,
        iso_currency=iso_currency,
        overdraft_limit=D('333.67'),
    )
    session.add(client_external_overdraft)
    session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (ServiceId.MARKET, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=333,
                    overdraft_limit_wo_tax=hamcrest.core.isnone.none(),
                ),
            )
        )
    )

    process_client(client)

    if client.is_agency:
        expected_limit = 0
        expected_limit_wo_tax = 0
        expected_description = "Decline in overdraft for service_id=%s firm_is=%s due reason \"Agency is not allowed to have overdraft with this service and firm params\"" % (ServiceId.MARKET, FIRM_ID)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (ServiceId.MARKET, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=expected_limit,
                    overdraft_limit_wo_tax=expected_limit_wo_tax,
                    iso_currency=hamcrest.core.isnone.none(),
                    description=expected_description
                ),
            ),
        )
    )



@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.checks.ActCheck.check', new=mock.Mock(return_value="failed act"))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 900, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop()]))
@pytest.mark.parametrize(
    'merge_policy, expected_limit, expected_limit_wo_tax, expected_description',
    (
        [None,                                                  D('333.67'), D('278.05'), "using default external limit calculator"],
        [ExternalOverdraftMergePolicy.EXTERNAL_ONLY,            D('333.67'), D('278.05'), "using default external limit calculator"],
        [ExternalOverdraftMergePolicy.TAKE_DEFAULT_IF_POSITIVE, D('333.67'), D('278.05'), "using default external limit calculator"],
    )
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_external_with_act_fail(session, client, merge_policy, expected_limit, expected_limit_wo_tax, expected_description):
    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, 'RUB', 300)

    session.flush()

    client_external_overdraft = mapper.ClientExternalOverdraft(
        client_id=client.id,
        service_id=SERVICE_ID,
        iso_currency='RUB',
        overdraft_limit=D('333.67'),
        merge_policy=merge_policy,
    )
    session.add(client_external_overdraft)
    session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(overdraft_limit=333, overdraft_limit_wo_tax=300)
            )
        )
    )

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=expected_limit,
                    overdraft_limit_wo_tax=expected_limit_wo_tax,
                    description=expected_description
                ),
            ),
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value="failed chain_check"))
@mock.patch('balance.overdraft.checks.ActCheck.check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 900, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop()]))
@pytest.mark.parametrize(
    'merge_policy, expected_limit, expected_limit_wo_tax, expected_description',
    (
        [None,                                                  D(0), D(0), "failed chain_check"],
        [ExternalOverdraftMergePolicy.EXTERNAL_ONLY,            D(0), D(0), "failed chain_check"],
        [ExternalOverdraftMergePolicy.TAKE_DEFAULT_IF_POSITIVE, D(0), D(0), "failed chain_check"],
    )
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_external_with_chain_check_fail(session, client, merge_policy, expected_limit, expected_limit_wo_tax, expected_description):
    client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, 'RUB', 300)

    session.flush()

    client_external_overdraft = mapper.ClientExternalOverdraft(
        client_id=client.id,
        service_id=SERVICE_ID,
        iso_currency='RUB',
        overdraft_limit=D('333.67'),
        merge_policy=merge_policy,
    )
    session.add(client_external_overdraft)
    session.flush()

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(overdraft_limit=333, overdraft_limit_wo_tax=300)
            )
        )
    )

    process_client(client)

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=expected_limit,
                    overdraft_limit_wo_tax=expected_limit_wo_tax,
                    description=expected_description
                ),
            ),
        )
    )


@mock.patch('balance.overdraft.checks.Checker.chain_check', new=mock.Mock(return_value=None))
@mock.patch('balance.overdraft.limit.calculate_limit', new=mock.Mock(return_value=(999, 900, None)))
@mock.patch('balance.mapper.ServiceFirmOverdraftParams.get', new=mock.Mock(return_value=[patch_sfop(only_external=1)]))
@pytest.mark.parametrize(
    'has_prev_overdraft_limit, merge_policy, external_limit, expected_description',
    (
        [True, None,  False, "internal and external limits aren't allowed"],
        [True, ExternalOverdraftMergePolicy.TAKE_DEFAULT_IF_POSITIVE, True, "using default external limit calculator"],
        [False, ExternalOverdraftMergePolicy.EXTERNAL_ONLY, True, "using default external limit calculator"],
        [False, None, False, ""]
    )
)
@pytest.mark.parametrize('client', [0, 1], ids=['client', 'agency'], indirect=True)
def test_only_external_sfo_params(
    session, client, has_prev_overdraft_limit, merge_policy, external_limit, expected_description
):
    if has_prev_overdraft_limit:
        client.set_overdraft_limit(SERVICE_ID, FIRM_ID, 333, 'RUB', 300)
        session.flush()

    if external_limit:
        client_external_overdraft = mapper.ClientExternalOverdraft(
            client_id=client.id,
            service_id=SERVICE_ID,
            iso_currency='RUB',
            overdraft_limit=D('333.67'),
            merge_policy=merge_policy,
        )
        session.add(client_external_overdraft)
        session.flush()

    process_client(client)

    if not (has_prev_overdraft_limit or external_limit):
        hamcrest.assert_that(
            client,
            hamcrest.has_properties(
                overdraft_ban=0,
                overdraft={}
            )
        )
        return

    hamcrest.assert_that(
        client,
        hamcrest.has_properties(
            overdraft_ban=0,
            overdraft=hamcrest.has_entry(
                (SERVICE_ID, FIRM_ID),
                hamcrest.has_properties(
                    overdraft_limit=D(0) if not external_limit else D('333.67'),
                    fixed_limit=None,
                    description=expected_description
                )
            ),
        )
    )
