import json
import requests


def remove_ts_from_data(data):
    for metric in data['sensors']:
        if 'timeseries' in metric:
            for value_obj in metric['timeseries']:
                value_obj.pop('ts', None)
        elif 'ts' in metric:
            metric.pop('ts', None)


def are_metrics_equal(data, expected):
    def dict_key(d):
        return tuple(sorted(d.items()))

    remove_ts_from_data(data)

    data_metrics = {dict_key(metric['labels']): metric
                        for metric in data['sensors']}

    for expected_metric in expected['sensors']:
        metric_labels = dict_key(expected_metric['labels'])

        assert metric_labels in data_metrics
        data_metric = data_metrics[metric_labels]

        if expected_metric != data_metric:
            return False

        data_metrics.pop(metric_labels, None)

    return True


def send_metrics(endpoint, metrics):
    requests.post(endpoint, data=json.dumps(metrics), headers={'Content-Type': 'application/json'})
