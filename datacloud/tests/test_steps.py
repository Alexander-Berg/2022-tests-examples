# -*- coding: utf-8 -*-
from yt.wrapper import ypath_join
from datacloud.dev_utils.data.data_utils import array_fromstring
from datacloud.features.credit_scoring_events.build_config import CSEBuildConfig
from datacloud.features.credit_scoring_events.cse_features import build_vectors

RETRO_ROOT = '//projects/scoring/test_partner/XPROD-000'


def test_build_vectors(yt_client):
    build_config = CSEBuildConfig(is_retro=True, root=RETRO_ROOT, partners=['no-go-partner'])
    build_vectors(build_config, yt_client)

    eid2features = dict()
    features_table = ypath_join(RETRO_ROOT, 'datacloud/aggregates/credit_scoring_events/features_event')
    for row in yt_client.read_table(features_table):
        eid2features[row['external_id']] = list(array_fromstring(row['features']))

    eid2features_expected = {
        '100000100_2017-03-20': [1., 0.],
        '100000237_2017-03-22': [1., 0.],
        '100000248_2017-03-22': [0., 0.],
        '100000486_2017-03-23': [1., 1.],
        '100000488_2017-03-24': [0., 0.],
    }
    assert eid2features == eid2features_expected
