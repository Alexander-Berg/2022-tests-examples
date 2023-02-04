import numpy as np
from yt.wrapper import ypath_join
from datacloud.ml_utils.junk.benchmark_v2.main import Experiment
from datacloud.dev_utils.logging.logger import get_basic_logger


ROOT = '//projects/scoring/test_partner/XPROD-000'


def generate_data(yt_client):
    n_sample = 10000
    random_seed = 42

    operators = ['TL2', 'MTS', 'BEE']
    regions = ['MSK', 'SPB', 'RZN']
    features_prod_count = 10

    np.random.seed(random_seed)

    target_data = [
        {'external_id': str(i), 'target': str(v)}
        for i, v in enumerate(np.random.binomial(1, 0.5, size=n_sample))
    ]

    operator_region_data = [
        {'external_id': str(i), 'operator': operator, 'region': region}
        for i, (operator, region) in enumerate(zip(
            np.random.choice(operators, size=n_sample),
            np.random.choice(regions, size=n_sample),
        ))
    ]

    features_prod = [
        {'external_id': str(i),
         'features': np.random.randn(features_prod_count).astype('<f4').tostring()}
        for i in range(n_sample)
        if i % 2 == 0
    ]

    catboost_predictions = [
        {'external_id': str(i), 'catboost_predictions': v}
        for i, v in enumerate(np.random.rand(n_sample))
        if i % 2 == 0
    ]

    yt_client.mkdir(ypath_join(ROOT, 'raw_data'), recursive=True)

    yt_client.write_table(ypath_join(ROOT, 'raw_data/glued'), target_data)
    yt_client.write_table(ypath_join(ROOT, 'operator_region'), operator_region_data)
    yt_client.write_table(ypath_join(ROOT, 'features_prod'), features_prod)
    yt_client.write_table(ypath_join(ROOT, 'predictions_catboost'), catboost_predictions)


def test_experiment(yt_client):
    generate_data(yt_client)
    logger = get_basic_logger(__name__, '%(asctime)s %(message)s')

    features= [
        {
            "table": "features_prod",
            "transformer": {
                "class": "DecodeAndFillNanToMeanAndHit",
            }
        },
        {
            "table": "operator_region",
            "transformer": {
                "class": "OneHotTransformer",
                "params": {
                    "columns": ["operator", "region"]
                }
            }
        },
        {
            "table": "predictions_catboost",
            "transformer": {
                "class": "SelectColumnAndLogitAndFillNanToMeanAndHit",
                "params": {
                    "column": "catboost_predictions"
                }
            }
        }
    ]

    logistic = {
        "class": "LogisticRegression",
        "params": {
            "C": 0.46,
            "solver": "lbfgs",
            "max_iter": 1000,
        }
    }
    catboost = {
        "class": "CatBoostClassifier",
        "params": {
            "iterations": 10,
            "verbose": 0,
        },
    }

    for model, use_tqdm, log in [
        (logistic, False, None),
        (logistic, True, None),
        (catboost, False, None),
        (catboost, False, logger),
    ]:
        config = {
            "features": features,
            "model": model,
            "click_stream_tables": ["features_prod"],
        }
        experiment = Experiment.from_json(config, ROOT, yt_client=yt_client, use_tqdm=use_tqdm, logger=log)
        experiment.run_experiment()

        assert abs(np.array(experiment.aucs).mean() - 0.5) < 0.02
        assert abs(np.array(experiment.aucs_on_cs).mean() - 0.5) < 0.02
