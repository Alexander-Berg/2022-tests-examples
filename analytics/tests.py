import datetime
import requests
import json
import logging
from utils import log_request

#################### STAT #####################

def stat_get_report_data(tokens, project, path, cgi={}):
    url = 'https://upload.stat.yandex-team.ru/_api/statreport/json/{}/{}'.format(project, path)
    headers = {
        'Content-Type': 'application/json',
        'Authorization': 'OAuth {}'.format(tokens['stat'])
    }
    r = requests.get(url, headers=headers, json=cgi)
    log_request(r)
    if r.status_code != 200:
        return []
    return r.json().get('values', [])


def stat_get_tree_node_values(tokens, project, path, node_id, tree_level, cgi={}):
    values = stat_get_report_data(tokens, project, path, cgi)
    result = []
    for elem in values:
        tree_path = elem['tree_field'].split('\t')
        if len(tree_path) <= tree_level:
            continue
        cur_node = tree_path[tree_level]
        if cur_node == node_id:
            result.append(elem)

    return result


def test_stat_get_report_tree_last_point(tokens, project, path, node_id, tree_level, field_name, cgi={}):
    values = stat_get_tree_node_values(tokens, project, path, node_id, tree_level, cgi)
    if len(values) == 0:
        return "ERROR"
    values.sort(key=lambda x: x['fielddate'], reverse=True)
    last_elem = values[0]
    return str(last_elem.get(field_name, "ERROR"))


################## METRICS ##################

def metrics_get_metric_values(tokens, metric_name, cron_id, host_id, filt, last_days=7):
    cur_datetime = datetime.datetime.now()
    time_delta = datetime.timedelta(days=last_days)
    start_datetime = cur_datetime - time_delta

    params = {
        'config': [
            {
                'cronId': cron_id,
                'filter': {
                    'filter': filt,
                    'metric': metric_name,
                    'system': host_id
                }
            }
        ],
        'endDate': datetime.datetime.isoformat(cur_datetime),
        'pointsAggregateType': 'CRON_EXPRESSION',
        'startDate': datetime.datetime.isoformat(start_datetime)
    }
    url = "https://metrics.yandex-team.ru/api-history/graph/lines?limit=1500&withoutComments=true&withoutProperties=true"
    headers = {
        'Content-Type': 'application/json',
        'Authorization': 'OAuth {}'.format(tokens['metrics'])
    }
    r = requests.post(url, headers=headers, data=json.dumps(params))
    log_request(r)
    if r.status_code != 200:
        return []
    return r.json()[0]['points']


def test_metrics_metric_confidence(tokens, metric_name, cron_id, host_id, filt):
    metric_values = metrics_get_metric_values(tokens, metric_name, cron_id, host_id, filt)
    if len(metric_values) < 2:
        status = "ERROR"
    elif metric_values[-2]['confidence'] is None:
        status = "ERROR"
    else:
        status = "OK" if metric_values[-2]['confidence'] else "BAD"
    return status


def test_metrics_get_metric_value(tokens, metric_name, cron_id, host_id, filt):
    metric_values = metrics_get_metric_values(tokens, metric_name, cron_id, host_id, filt)
    if len(metric_values) < 2:
        status = "ERROR"
    elif metric_values[-2]['value'] is None:
        status = "ERROR"
    else:
        status = str(metric_values[-2]['value'])
    return status


def metrics_get_basket_queries(tokens, basket_id):
    url = 'https://metrics-qgaas.metrics.yandex-team.ru/api/basket/{}/query'.format(basket_id)
    headers = {
        'Content-Type': 'application/json',
        'Authorization': 'OAuth {}'.format(tokens['metrics'])
    }
    r = requests.get(url, headers=headers)
    log_request(r)
    if r.status_code != 200:
        return []
    return r.json()


def test_metrics_basket_size(tokens, basket_id):
    return str(len(metrics_get_basket_queries(tokens, basket_id)))


#################### HITMAN ###################

def hitman_get_hp_storage_info(tokens, hit_type):
    url = 'https://asdb.hitman.yandex-team.ru/api/honeypots/v1/statistic/hit_type/{}'.format(hit_type)
    headers = {
        'Authorization': 'OAuth {}'.format(tokens['hitman'])
    }
    r = requests.get(url, headers=headers)
    log_request(r)
    if r.status_code != 200:
        return {}
    return r.json()


def test_hitman_hp_last_create(tokens, hit_type):
    info = hitman_get_hp_storage_info(tokens, hit_type)
    if 'lastGenCreateMs' not in info:
        return "ERROR"
    return str(info['lastGenCreateMs'])


def test_hitman_hp_last_update(tokens, hit_type):
    info = hitman_get_hp_storage_info(tokens, hit_type)
    if 'lastWrittenMs' not in info:
        return "ERROR"
    return str(info['lastWrittenMs'])


#################### TESTS MAPPING ######################

test_functions = {
    'metrics': {
        'confidence_checker': test_metrics_metric_confidence,
        'metric_value': test_metrics_get_metric_value,
        'basket_size': test_metrics_basket_size
    },
    'stat': {
        'report_tree_value': test_stat_get_report_tree_last_point
    },
    'hitman': {
        'hp_last_create': test_hitman_hp_last_create,
        'hp_last_update': test_hitman_hp_last_update
    }
}
