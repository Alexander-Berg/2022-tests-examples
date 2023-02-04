import sys  # noqa

import yatest.common


CONF_PATH = yatest.common.test_source_path('data/test_labels.conf')


def test_labels_in_config(agent):
    data = agent.read(params={'project': 'solomon', 'service': 'test'})
    assert 'commonLabels' in data

    common_labels = data['commonLabels']
    assert common_labels['key1'] == 'value1'
    assert common_labels['key2'] == 'value2'
    assert common_labels['host'] == 'test-host'
