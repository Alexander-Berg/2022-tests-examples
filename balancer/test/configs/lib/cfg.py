import json
from yatest.common import source_path


def process_config(config, f, path=""):
    if isinstance(config, dict):
        for k, v in config.iteritems():
            new_patch = '{}/{}'.format(path, k)
            f(k, v, new_patch)
            process_config(v, f, new_patch)
    elif isinstance(config, list):
        for v in config:
            process_config(v, f, path)


def process_module(name, f):
    def do_process_module(k, v, path):
        if k == name and isinstance(v, dict):
            f(v)
    return do_process_module


def process_balancer(f):
    def do_process_module(k, v, path):
        if k == 'balancer2' and isinstance(v, dict):
            for k2, v2 in v.iteritems():
                if k in ('rr', 'weighted2', 'active', 'dynamic', 'hashing', 'dynamic_hashing', 'sd') and isinstance(v2, dict):
                    f(v2)
    return do_process_module


def parse_duration(duration_str):
    if duration_str.endswith('ms'):
        return float(duration_str[:-2]) / 1000.0
    elif duration_str.endswith('s'):
        return float(duration_str[:-1])
    raise ValueError('"{}" is not a valid duration'.format(duration_str))


SANE_TIMEOUT = 180.0
ANTIROBOTS_SANE_TIMEOUT = 0.2


def process_proxy_module(max_timeout):
    def do_process_module(k, v, path):
        if k in ('proxy', 'proxy_options') and isinstance(v, dict):
            assert 'backend_timeout' in v
            assert 'connect_timeout' in v
            backend_timeout = parse_duration(v['backend_timeout'])
            assert backend_timeout <= max_timeout
            connect_timeout = parse_duration(v['connect_timeout'])
            assert connect_timeout <= max_timeout
    return do_process_module


def process_antirobot_module():
    m = {
        'antirobot': 'checker',
        'geobase': 'geo',
        'exp_getter': 'uaas',
    }
    def do_process_module(k, v, path):
        if k in m and isinstance(v, dict) and m[k] in v:
            if not (k == 'antirobot' and 'captcha' in path):
                process_config(v[m[k]], process_proxy_module(ANTIROBOTS_SANE_TIMEOUT), path)
    return do_process_module


def sane_timeouts(balancer_parsed_config):
    process_config(balancer_parsed_config, process_proxy_module(SANE_TIMEOUT))
    process_config(balancer_parsed_config, process_antirobot_module())


def check_non_zero_weights(rr):
    found_non_zero_weight = False
    for section, params in rr.iteritems():
        if not isinstance(params, dict):
            continue
        if params['weight'] > 0:
            found_non_zero_weight = True
            break
    assert found_non_zero_weight


def all_weights_non_zero(balancer_parsed_config):
    process_config(balancer_parsed_config, process_balancer(check_non_zero_weights))


SANE_ATTEMPTS = 2
SANE_CONNECTION_ATTEMPTS = 6


def check_sane_attempts(section, value, path):
    if not isinstance(value, dict):
        return
    assert value.get('attempts', 1) <= SANE_ATTEMPTS
    assert value.get('connection_attempts', 1) <= SANE_CONNECTION_ATTEMPTS


def sane_attempts_count(balancer_parsed_config):
    process_config(balancer_parsed_config, check_sane_attempts)


def service_alerts(balancer_parsed_config, balancer_name):
    services = set()
    services_from_alerts = set()

    def add_service(v):
        if v.get('id') == "upstream-info":
            assert 'fields' in v
            assert 'upstream' in v['fields']
            name = v['fields']['upstream']
            services.add(name)

    process_config(balancer_parsed_config, process_module('meta', add_service))

    alerts_file = source_path('search/spi-tools/alerts-providers/l7_{}/alerts.json'.format(balancer_name))
    alerts = json.load(open(alerts_file))

    for i in alerts['verticals']:
        alert_name = i['template_params'].get('name')
        if not alert_name:
            alert_name = i['template_params'].get('alert_name')

        for name in alert_name.split('+'):
            if name:
                services_from_alerts.add(name)

    services -= services_from_alerts
    if services:
        raise AssertionError((
            'Services without alerts: {}. '
            'Add them to https://a.yandex-team.ru/arc/trunk/arcadia/'
            'search/spi-tools/alerts-providers/l7_{}/alerts.json'
        ).format(','.join(sorted(services)), balancer_name))
