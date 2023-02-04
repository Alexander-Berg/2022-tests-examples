import logging

from datacloud.ml_utils.dolphin.prepare_cse.path_config import PathConfig
from datacloud.ml_utils.dolphin.prepare_cse.pipeline import (
    CleanConfig,
    step0_copy_inputs
)

fake_cconfig = CleanConfig(
    experiment_name='experimnet',
    path_to_original_cse='//credit_scoring_events',
    aggs_folder='//aggs',
    crypta_folder='//crypta',
    zeros_vs_ones=1.,
    min_retro_date='2000-01-01',
    no_go_partners=['insurance'],
    n_folds=2,
    val_size=1,
    steps=[]
)

fake_path_config = PathConfig(fake_cconfig)

fake_logger = logging.getLogger(__name__)


class TestStep0CopyInputs():
    def test(self, yt_client, yql_client):
        step0_copy_inputs(
            yt_client=yt_client,
            yql_client=yql_client,
            path_config=fake_path_config,
            cconfig=fake_cconfig,
            logger=fake_logger
        )

        assert yt_client.exists('//aggs/id_value_to_cid')
        assert yt_client.exists('//aggs/phone_id_value_to_cid')
        assert yt_client.exists('//aggs/email_id_value_to_cid')
        assert yt_client.exists('//aggs/cid_to_all')
        assert yt_client.exists('//aggs/credit_scoring_events')
