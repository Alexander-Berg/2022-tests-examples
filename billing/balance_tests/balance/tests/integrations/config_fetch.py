import copy
import json
from balance.tests.integrations.test_data import Tests, TestsExtraContext
from balance.tests.integrations.utils import get_row_by_configuration_cc, get_all_configs
from btestlib import reporter

log = reporter.logger()


def create_config(integration_cc, configuration_cc, thirdparty_processing, contracts, tests, tests_extra_context):
    return {
        'service': {
            'integration_cc': integration_cc,
            'configuration_cc': configuration_cc,
        },
        'thirdparty_processing': thirdparty_processing,
        'contracts': contracts,
        'tests': tests,
        'tests_extra_context': tests_extra_context
    }


def get_configs(row):
    if not row:
        log.debug('No such config')
        return None

    scheme = json.loads(row['scheme'])
    if 'tests' not in scheme:
        log.debug('no tests in %s' % row)
        return None

    contracts = scheme['contracts']
    tests = Tests(scheme.get('tests'))
    tests_extra_context = TestsExtraContext(scheme.get('tests_extra_context'))
    payment_tests, act_tests = tests.split_payments_and_acts()
    thirdparty_processing = scheme.get('thirdparty_processing')

    configuration_cc = row['cc']
    integration_cc = row['integration_cc']

    for i in range(len(contracts)):
        if 'person' not in contracts[i]:
            contracts[i]["person"] = {
                "type": {
                    "mandatory": ["ph"],
                    # TODO: remove
                }
            }
            log.info('config %s has not person type' % row['cc'])
        else:
            log.info('config %s has person type' % row)

    config_for_payments = create_config(integration_cc, configuration_cc,
                                        thirdparty_processing, contracts, payment_tests, tests_extra_context)
    config_for_acts = create_config(integration_cc, configuration_cc,
                                    thirdparty_processing, contracts, act_tests, tests_extra_context)

    return config_for_payments, config_for_acts


def get_service_ids(config):
    service_ids = []
    for contract in config['contracts']:
        if 'services' not in contract:
            continue
        service_ids.extend(contract['services'].get('mandatory', []))
        service_ids.extend(contract['services'].get('optional', []))
    return service_ids


def get_config_and_id_list(metafunc):
    acts_only = metafunc.config.getoption('acts')
    payments_only = metafunc.config.getoption('payments')
    configuration_cc = metafunc.config.getoption('config')
    mock_trust = metafunc.config.getoption('mock_trust')
    config_name = metafunc.fixturenames[1]

    if config_name == 'payment_config':
        payments_only = True
    else:
        acts_only = True

    if configuration_cc:
        rows = [get_row_by_configuration_cc(configuration_cc)]
    else:
        rows = get_all_configs()

    config_list, ids = [], []
    for row in rows:
        config_for_payment_tests, config_for_act_tests = get_configs(row)
        for config, name in [(config_for_payment_tests, 'payments'), (config_for_act_tests, 'acts')]:
            if not config['tests'].tests:
                continue
            if (name == 'payments' and acts_only) or (name == 'acts' and payments_only):
                continue
            service_ids = get_service_ids(config)
            for service_id in service_ids:
                for test_id, test_case in enumerate(config['tests'].tests):
                    config_with_service_id_and_test = copy.deepcopy(config)
                    config_with_service_id_and_test['service_id'] = service_id
                    config_with_service_id_and_test['tests'].tests = [test_case]
                    config_list.append(config_with_service_id_and_test)
                    ids.append("{}_{}_{}_test_{}".format(row['cc'], name, service_id, test_id))

    return config_name, config_list, ids, mock_trust
