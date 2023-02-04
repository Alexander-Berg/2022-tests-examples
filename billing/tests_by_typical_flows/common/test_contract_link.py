# coding: utf-8

import pytest
from datetime import timedelta as shift
from apikeys import apikeys_defaults as defaults
from apikeys.tests_by_typical_flows import typical_flows as flow
from btestlib import utils

__author__ = 'kostya-karpus'

SPEECHKIT_CLOUD_CLIENT = ('speechkitcloud', 'apikeys_speechkitcloud_client')
SPEECHKIT_CLOUD_SILVER = ('speechkitcloud', 'apikeys_speechkitcloud_partner_silver')

MAPS = ('apimaps', 'apikeys_apimaps_1000_yearprepay_2017')
CITY = ('city', 'apikeys_city_100k_yearprepay_2017')
STATICMAPS = ('staticmaps', 'apikeys_city_25k_yearprepay_2017')


@pytest.mark.parametrize('tariff', [
    # MAPS,
    SPEECHKIT_CLOUD_SILVER,
    # CITY,
    # STATICMAPS,
], ids=lambda x: '{}-{}'.format(*x))
@pytest.mark.parametrize('scenario',
                         [
                             # # example with all dates
                             # {'description': u'example',
                             #  'dates': {'FINISH_DT': '2018-02-20T00:00:00',
                             #            'DT': '2016-11-22T00:00:00',
                             #            'IS_BOOKED': '2016-11-22T00:00:00',
                             #            'IS_FAXED': '2016-11-22T00:00:00',
                             #            'SERVICE_START_DT': '2016-11-22T00:00:00',
                             #            'IS_SIGNED': None,
                             #            'IS_SUSPENDED': '2016-11-22T00:00:00',
                             #            'IS_CANCELLED': '2016-11-22T00:00:00'},
                             #  'contract_activated': False},

                             {'description': u'is_signed',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT,
                                        'IS_SIGNED': defaults.BASE_DT},
                              'contract_activated': True},

                             {'description': u'is_faxed',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT,
                                        'IS_FAXED': defaults.BASE_DT,
                                        'IS_SIGNED': None},
                              'contract_activated': True},

                             {'description': u'is_booked',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT,
                                        'IS_FAXED': defaults.BASE_DT,
                                        'IS_BOOKED': defaults.BASE_DT,
                                        'IS_SIGNED': None},
                              'contract_activated': True},

                             {'description': u'service_start_dt = today',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT,
                                        'SERVICE_START_DT': defaults.BASE_DT,
                                        'IS_SIGNED': defaults.BASE_DT},
                              'contract_activated': True},

                             {'description': u'unsigned',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT,
                                        'IS_SIGNED': None},
                              'contract_activated': False},

                             {'description': u'service_start_dt > today',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT,
                                        'SERVICE_START_DT': defaults.BASE_DT + shift(days=1),
                                        'IS_SIGNED': defaults.BASE_DT},
                              'contract_activated': False},

                             {'description': u'dt > today',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT + shift(days=1)},
                              'contract_activated': False},

                             {'description': u'finish_dt > today',
                              'dates': {'FINISH_DT': defaults.BASE_DT - shift(days=1),
                                        'DT': defaults.BASE_DT - shift(days=30),
                                        'IS_SIGNED': defaults.BASE_DT - shift(days=30)},
                              'contract_activated': False},
                         ],
                         ids=lambda x: x['description'])
@pytest.mark.good
@pytest.mark.smoke
def test_contract(tariff, scenario, db_connection):
    scenario = utils.aDict(scenario)
    service_id, scenario.tariff = tariff
    flow.Contract.general_contract(scenario, db_connection, service_id)


@pytest.mark.xfail(reason='APIKEYS-464')
@pytest.mark.parametrize('tariff', [
    SPEECHKIT_CLOUD_CLIENT,
    SPEECHKIT_CLOUD_SILVER,
    CITY,
    MAPS,
    STATICMAPS,
],
                         ids=lambda x: '{}-{}'.format(*x))
@pytest.mark.parametrize('scenario',
                         [
                             {'description': u'is_cancelled',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT - shift(days=30),
                                        'IS_SIGNED': defaults.BASE_DT - shift(days=30),
                                        },
                              'stop_type': 'IS_CANCELLED'
                              },

                             {'description': u'is_suspended',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT - shift(days=30),
                                        'IS_SIGNED': defaults.BASE_DT - shift(days=30),
                                        },
                              'stop_type': 'IS_SUSPENDED'
                              },
                         ],
                         ids=lambda x: x['description'])
def test_stopped_contract(tariff, scenario, db_connection):
    current_scenario = utils.aDict(scenario)
    service_id, current_scenario.tariff = tariff
    current_scenario.contract_activated = True
    flow.Contract.stopped_contract(current_scenario, db_connection, service_id)


@pytest.mark.parametrize('tariff', [SPEECHKIT_CLOUD_CLIENT], ids=lambda x: '{}-{}'.format(*x))
@pytest.mark.parametrize('scenario',
                         [
                             {'description': u'terminated by collateral',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT - shift(days=365),
                                        'IS_SIGNED': None,
                                        'IS_BOOKED': defaults.BASE_DT,
                                        'IS_FAXED': defaults.BASE_DT
                                        },
                              'contract_activated': True,
                              'collateral_contract': {
                                  '_type': 90,
                                  'tariff': None
                              }},

                             {'description': u'change tariff',
                              'dates': {'FINISH_DT': defaults.BASE_DT + shift(days=365),
                                        'DT': defaults.BASE_DT,
                                        'IS_SIGNED': None,
                                        'IS_BOOKED': defaults.BASE_DT,
                                        'IS_FAXED': defaults.BASE_DT
                                        },
                              'contract_activated': True,
                              'collateral_contract': {
                                  '_type': 1043,
                                  'tariff': SPEECHKIT_CLOUD_SILVER[1]
                              }},

                         ],
                         ids=lambda x: x['description'])
def test_collateral_contract(tariff, scenario, db_connection):
    scenario = utils.aDict(scenario)
    service_id, scenario.tariff = tariff
    flow.Contract.collateral_contract(scenario, db_connection, service_id)


@pytest.mark.parametrize('tariff', [
    SPEECHKIT_CLOUD_CLIENT,
    SPEECHKIT_CLOUD_SILVER,
    CITY,
    MAPS,
    STATICMAPS,
],
                         ids=lambda x: '{}-{}'.format(*x))
@pytest.mark.parametrize('scenario',
                         [
                             {'description': u'new after expiry',
                              'dates_old1': {'FINISH_DT': defaults.BASE_DT + shift(days=1),
                                             'DT': defaults.BASE_DT - shift(days=30),
                                             'IS_SIGNED': defaults.BASE_DT - shift(days=30),
                                             },
                              'dates_old2': {'FINISH_DT': defaults.BASE_DT - shift(days=1),
                                             'DT': defaults.BASE_DT - shift(days=30),
                                             'IS_SIGNED': defaults.BASE_DT - shift(days=30),
                                             },
                              'dates_new': {'FINISH_DT': defaults.BASE_DT + shift(days=30),
                                            'DT': defaults.BASE_DT,
                                            'IS_SIGNED': defaults.BASE_DT,
                                            },
                              },
                         ],
                         ids=lambda x: x['description'])
def test_new_contract(tariff, scenario, db_connection):
    scenario = utils.aDict(scenario)
    service_id, scenario.tariff = tariff
    flow.Contract.change_contract(scenario, db_connection, service_id)
