import jinja2
import logging
import os

from contextlib import contextmanager

from yatest.common import output_path, source_path, work_path, binary_path

from ads.bsyeti.big_rt.py_test_lib import helpers
from ads.bsyeti.caesar.tests.lib.b2b.utils import QUEUE_CONSUMER
from .parser import to_snake_case


def create_config(stand, yt_cluster, port, model_service_port):
    parameters = {
        "port": port,
        "model_service_port": model_service_port,
        "table": stand.table,
        "consuming_system_main_path": stand.table_path_factory(stand.table, "consuming_system"),
        "consumer": QUEUE_CONSUMER,
        "input_queue": stand.queues["input"],
        "watcher_queue": stand.queues["watcher"],
        "yt_cluster": yt_cluster.primary.get_proxy_address(),
        "logdir": output_path("caesar_worker_logs/" + stand.test_id),
        "state_table_path": stand.tables[stand.table],
        # add resource folders here
        "multik_package_folder": work_path("multik_package_folder/data"),
        "multik_package_folder_exp": work_path("multik_package_folder_exp/data"),
        "caesar_bases_folder": work_path("caesar-bases"),
        "turbo_shop_id_to_counter_id_folder": work_path("turbo_shop_id_2_counter_id"),
        "turbo_original_urls_base_file": work_path("caesar-turbo-original-urls-base"),
        "currency_dicts_folder": work_path("currency_dicts"),
        "geodata_6_file": work_path("geodata6.bin"),
        "cat_engine_idfs_folder": work_path("cat_engine_idfs"),
        "bigb_ab_experiments_config_file": work_path("bigb_ab_production_config.json"),
        "autobudget_default_config_file": work_path("bidder-config-package/resource/config/default.conf"),
        "autobudget_preprod_config_file": work_path("bidder-config-package/resource/config/preprod.conf"),
        "dssm_model_content_markup_folder": work_path("web-model-dt-300.dssm"),
        "content_markup_folder": work_path("brand_safety_web"),
        "autobudget_experiment_config_file": work_path("bidder-config-package/resource/config/bidder_experiment_settings.conf"),
        "autobudget_experiment_preprod_config_file": work_path("bidder-config-package/resource/config/bidder_experiment_settings_preprod.conf"),
    }

    config_args = {}
    for table in stand.extra_profiles:
        config_args["%s_table" % to_snake_case(table)] = stand.tables[table]
    for k, v in stand.extra_config_args.items():
        config_args[k] = v

    parameters.update(config_args)
    return render_template(parameters)


def render_template(parameters):
    with open(source_path("ads/bsyeti/caesar/tests/ft/common/config.json.j2")) as fd:
        return jinja2.Template(fd.read()).render(parameters)


class Caesar(helpers.BulliedProcess):
    def __init__(self, config_path, port, now):
        assert now
        env = os.environ.copy()
        env["Y_TEST_FIXED_TIME"] = now.isoformat()
        super(Caesar, self).__init__(
            launch_cmd=[
                binary_path("ads/bsyeti/caesar/bin/caesar_worker/caesar_worker"),
                "--config-json",
                config_path,
            ],
            env=env,
        )
        self.port = port

    def get_sensors(self):
        return helpers.get_raw_sensors(self.port)


@contextmanager
def run_caesar(stand, yt_cluster, port, model_service_port):
    json = create_config(stand, yt_cluster, port, model_service_port)
    path = helpers.make_json_file(json, name_template="caesar_config_{json_hash}.json")

    caesar = Caesar(path, port, stand.now)

    with caesar:
        try:
            yield caesar
        finally:
            try:
                helpers.log_sensors(caesar.port)
            except Exception as e:
                logging.exception(e)
