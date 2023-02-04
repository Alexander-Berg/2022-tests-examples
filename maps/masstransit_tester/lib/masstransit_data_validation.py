from maps.garden.libs.masstransit_resources.common import MASSTRANSIT_DATA_ON_YT_RESOURCE
from maps.garden.sdk.core import DataValidationWarning, Demands, Creates, Task
from maps.garden.sdk.extensions import mutagen
from maps.garden.sdk.resources import PythonResource
from maps.garden.sdk.utils import MB, GB
from maps.garden.sdk.sandbox import upload_report_to_sandbox
from maps.garden.sdk.yt.utils import get_yt_settings, get_server_settings, get_garden_prefix
from yt.wrapper.ypath import ypath_join
from maps.pylibs.utils.lib.filesystem import temporary_directory
from maps.pylibs.utils.lib.threadpool import run_parallel
from maps.garden.sdk.yt import geobase

from maps.masstransit.tools.compare_routers.lib.ammo import read_masstransit_data_validation_config

from .wrapper import validate_data

import functools
import logging
import os
import shutil


MASSTRANSIT_DATA_YT_PATH = "masstransit_data/{release}"
MASSTRANSIT_ROUTES_VALIDATION_CONFIG = 'masstransit_tester/config.json'


def download_yt_file(reader, local_path):
    with open(local_path, "wb") as out:
        shutil.copyfileobj(reader, out, length=10*MB)


def make_download_functions_for_dir(client, yt_path, local_path):
    os.makedirs(local_path)
    files = client.list(yt_path)
    logging.info("Fetching files %s from %s to %s", repr(files), yt_path, local_path)
    funcs = []
    for file_name in files:
        yt_file_path = ypath_join(yt_path, file_name)
        local_file_path = os.path.join(local_path, file_name)
        # NOTE: We use functools.partial because a lambda would capture the entire lexical scope
        # instead of variables values, and all functions would end up with the same values of the variables.
        # NOTE: It's important to create file readers here and not inside download_yt_file.
        # Creating readers concurrently with the same client didn't work for some reason.
        funcs.append(functools.partial(download_yt_file, client.read_file(yt_file_path), local_file_path))
    return funcs


def get_deployed_dataset_name(yt_client, prefix):
    symlink_path = ypath_join(prefix, MASSTRANSIT_DATA_YT_PATH.format(release="latest"))
    return yt_client.get(ypath_join(symlink_path, "@key"))


class ValidateMasstransitDataTask(Task):
    def load_environment_settings(self, environment_settings):
        self._environment_settings = environment_settings
        self._yt_config = environment_settings["yt_servers"]["hahn"]
        self._yt_prefix = get_garden_prefix(get_server_settings(
            get_yt_settings(environment_settings),
            server="hahn"))

    def __call__(self, masstransit_data_on_yt, validation_result):
        yt_client = masstransit_data_on_yt.get_yt_client()

        os.environ["masstransit-geobase.geodataPath"] = geobase.get_geobase6(self._yt_config)
        tzdata_path = geobase.get_tzdata_zones_bin(self._environment_settings)
        os.environ["masstransit-geobase.tzdataPath"] = tzdata_path
        os.environ["GEOBASE_TZ_PATH"] = tzdata_path

        with temporary_directory() as tmpdir:
            masstransit_data_to_validate = os.path.join(tmpdir, "masstransit-data-to-validate")
            released_masstransit_data = os.path.join(tmpdir, "released-masstransit-data")

            released_version = get_deployed_dataset_name(yt_client, self._yt_prefix)
            released_masstransit_data_on_yt = ypath_join(self._yt_prefix, MASSTRANSIT_DATA_YT_PATH.format(release=released_version))

            logging.info("Datasets to validate are %s", masstransit_data_on_yt.path)
            logging.info("Released datasets are %s", released_masstransit_data_on_yt)
            run_parallel(
                make_download_functions_for_dir(yt_client, masstransit_data_on_yt.path, masstransit_data_to_validate) +
                make_download_functions_for_dir(yt_client, released_masstransit_data_on_yt, released_masstransit_data)
            )
            logging.info("Successfully fetched datasets")

            validation_config = read_masstransit_data_validation_config(MASSTRANSIT_ROUTES_VALIDATION_CONFIG)
            validation_result.value, report = validate_data(masstransit_data_to_validate, released_masstransit_data, validation_config)

            logging.info('Uploading report to the sandbox')
            sb_report_link = upload_report_to_sandbox(report, "masstransit_tester", self._environment_settings)
            logging.info(f'The report is available at the url: {sb_report_link}')

            if not validation_result.value:
                raise DataValidationWarning(f"Masstransit data validation failed. Detailed report: {sb_report_link}")
            logging.info('Done')

    def predict_consumption(self, demands, creates):
        # TODO: Compute memory requirements based on the input resources.
        # It might require https://st.yandex-team.ru/MAPSGARDEN-14189.
        #
        # We request this amount of memory for two datasets in tmpfs (~20 gb)
        # and two Masstransit instance (~40 gb).
        # TODO: It's possible to reduce this requirement: we can download and use each dataset one by one.
        # In this case we would only need tmpfs for one dataset at a time.
        # So memory requirements can be reduced further after YT-6856 is done.
        return {"cpu": 4, "ram": 60 * GB, "tmpfs": True}


@mutagen.propagate_properties("shipping_date")
def fill_graph(graph_builder, regions=None):
    graph_builder.add_resource(
        PythonResource("masstransit_data_validation_completed")
    )
    graph_builder.add_task(
        Demands(masstransit_data_on_yt=MASSTRANSIT_DATA_ON_YT_RESOURCE),
        Creates(validation_result="masstransit_data_validation_completed"),
        ValidateMasstransitDataTask())
