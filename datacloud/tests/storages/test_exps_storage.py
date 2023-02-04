# -*- coding: utf-8 -*-
import os
import json
import murmurhash
from datacloud.dev_utils.yt import yt_utils
from datacloud.ml_utils.benchmark_v2.exps_storage import YtExpsStorage


def test_yt_exps_storage():
    yt_client = yt_utils.get_yt_client(os.environ["YT_PROXY"])

    storage_path = '//benchmarks/exps_storage'

    yes = YtExpsStorage(table_path=storage_path, yt_client=yt_client)
    yes._yt_table.create_table()
    assert yt_client.exists(storage_path)

    config = {
        'features': ['f1', 'f2', 'f3'],
        'model': 'LogisticRegression',
        'dataset': 'XPROD-1091'
    }
    expected_key = murmurhash.hash64(json.dumps(config, sort_keys=True))

    assert yes._make_exp_key(config=config) == expected_key
    assert yes.read_by_config(config=config) is None
    assert yes.read(key=expected_key) is None

    metrics = {'auc': 0.5, 'std': 0.}

    yes.add_experiment(config=config, metrics=metrics)
