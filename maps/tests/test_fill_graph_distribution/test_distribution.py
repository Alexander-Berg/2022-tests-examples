import difflib
from unittest import mock
import os
import shutil
import tarfile
import time

from yatest.common import test_source_path
from maps.garden.sdk.core import Version
from maps.garden.sdk import test_utils as garden_test_utils
from maps.garden.libs.stylerepo.test_utils import \
    test_utils as stylerepo_test_utils

from maps.garden.modules.carparks.lib import configs, distribution, \
    fill_graph as carparks

from maps.garden.modules.renderer_map_stable_bundle.lib import (
    graph as stable_map_bundle_graph,
    bundle_info as stable_map_bundle_info)
from maps.garden.modules.renderer_navi_stable_bundle.lib import (
    graph as stable_navi_bundle_graph,
    bundle_info as stable_navi_bundle_info)
from maps.garden.modules.stylerepo_map_design_src.lib import (
    graph as testing_map_bundle_graph,
    bundle_info as testing_map_bundle_info)
from maps.garden.modules.stylerepo_navi_design_src.lib import (
    graph as testing_navi_bundle_graph,
    bundle_info as testing_navi_bundle_info)
from maps.garden.modules.stylerepo_experiments_src.lib import \
    graph as experiments_graph

TEST_REGIONS = [('cis1', 'yandex')]

DATA_VERSION = '20200419-214500'


def fill_mms_dump(path):
    with open(os.path.join(path, 'file1.mms'), 'w') as f:
        f.write('file1.mms contents')

    subdir = os.path.join(path, 'subdir')
    os.makedirs(subdir)
    with open(os.path.join(subdir, 'file2.mms'), 'w') as f:
        f.write('subdir/file2.mms contents')


def create_mms_dump(cook):
    for config in configs.DUMP_CONFIGS:
        version = Version()
        resource = cook.create_input_resource(
            config.dump_dir_resource_name()
        )

        resource.version = version
        resource.load_environment_settings(cook.environment_settings)

        fill_mms_dump(resource.path())

        resource.logged_commit()


def create_input_resources(cook):
    create_mms_dump(cook)

    cook.create_build_params_resource(
        properties={'release_name': '20.01.22-1'})

    stylerepo_test_utils.create_design_resources(
        cook,
        stable_map_bundle_info.get_map_bundle_info(),
        stable_navi_bundle_info.get_navi_bundle_info(),
        testing_map_bundle_info.get_map_design_bundle_info(),
        testing_navi_bundle_info.get_navi_design_bundle_info())


def list_directory(dir):
    assert os.path.exists(dir), f'Dir not found: {dir}'
    file_list = []
    for root, dirs, files in os.walk(dir):
        # we also remove 'dir' prefix so that paths are relative to dir
        file_list.extend([(f'{root}/{file}')[len(dir):] for file in files])
    return sorted(file_list)


ECSTATIC_PACKAGE = 'maps.garden.sdk.ecstatic'
YT_PACKAGE = 'maps.garden.sdk.yt'


class TestCarparksDistribution():
    def test_renderer_config(self, environment_settings):
        self.run_config(configs.RENDERER_CONFIG, environment_settings)

    def test_handler_config(self, environment_settings):
        self.run_config(configs.HANDLER_CONFIG, environment_settings)

    @mock.patch(
        YT_PACKAGE + '.UploadToYtTask.__call__',
        new=mock.MagicMock())
    @mock.patch(
        YT_PACKAGE + '.YtResource.calculate_size',
        new=mock.MagicMock())
    @mock.patch(
        YT_PACKAGE + '.YtFileResource.physically_exists')
    @mock.patch(
        ECSTATIC_PACKAGE + '.resources.DatasetResource.physically_exists')
    @mock.patch(
        ECSTATIC_PACKAGE + '.tasks.UploadDatasetTask.__call__',
        new=mock.MagicMock())
    @mock.patch(
        ECSTATIC_PACKAGE + '.tasks.MoveTask.__call__',
        new=mock.MagicMock())
    @mock.patch(
        ECSTATIC_PACKAGE + '.tasks.WaitReadyTask.__call__',
        new=mock.MagicMock())
    @mock.patch(
        ECSTATIC_PACKAGE + '.tasks.ActivateTask.__call__',
        new=mock.MagicMock())
    def run_config(self, config, environment_settings, yt_physically_exists,
                   ecstatic_physically_exists):
        yt_physically_exists.return_value = True
        ecstatic_physically_exists.return_value = True

        configs.DUMP_CONFIGS[:] = [config]

        distribution.make_data_version = lambda: DATA_VERSION

        # mock without tracking calls
        time.sleep = lambda x: None

        self.run_build(config, environment_settings)

    def run_build(self, config, environment_settings):
        cook = garden_test_utils.GraphCook(environment_settings)
        carparks.fill_graph_data(cook.input_builder(), TEST_REGIONS)

        stable_map_bundle_graph.fill_graph(cook.input_builder(), TEST_REGIONS)
        stable_navi_bundle_graph.fill_graph(cook.input_builder(), TEST_REGIONS)
        testing_map_bundle_graph.fill_graph(cook.input_builder(), TEST_REGIONS)
        testing_navi_bundle_graph.fill_graph(cook.input_builder(), TEST_REGIONS)
        experiments_graph.fill_graph(cook.input_builder(), TEST_REGIONS)

        carparks.fill_graph_distribution(cook.target_builder())

        create_input_resources(cook)
        result_resources = garden_test_utils.execute(cook)

        self.check_results(config, result_resources)

    def check_results(self, config, resources):
        for resource in resources.values():
            resource.ensure_available()
            if 'data_version' in resource.properties:
                assert resource.properties['data_version'] == DATA_VERSION

        generated_root = resources[config.dataset_dir_resource_name()].path()

        expected_root = test_source_path(
            'expected_dist_{}'.format(config.consumer()))

        if os.getenv('YA_MAPS_CANONIZE_TESTS'):
            if os.path.exists(expected_root):
                shutil.rmtree(expected_root)
            shutil.copytree(generated_root, expected_root)

        generated_files = list_directory(generated_root)
        expected_files = list_directory(expected_root)

        self.assert_sets_equal(expected_files,
                               generated_files,
                               'Wrong set of files generated')
        for file in expected_files:
            generated_path = f'{generated_root}/{file}'
            expected_path = f'{expected_root}/{file}'

            if file.split('.')[-1] == 'tar':
                self.compare_tars(generated_path, expected_path)
                continue

            with open(generated_path, 'r') as f:
                generated = f.readlines()
            with open(expected_path, 'r') as f:
                expected = f.readlines()

            self.assert_sets_equal(expected,
                                   generated,
                                   f'File {file} differs')

    def compare_tars(self, generated_path, expected_path):
        with tarfile.open(generated_path, 'r') as exp_tar:
            with tarfile.open(expected_path, 'r') as gen_tar:
                assert set(exp_tar.getnames()) == set(gen_tar.getnames())

                for file in exp_tar.getnames():
                    self.assert_sets_equal(
                        exp_tar.extractfile(file).readlines(),
                        gen_tar.extractfile(file).readlines(),
                        f'File {file} from {generated_path} and {expected_path} differs')

    def assert_sets_equal(self, a, b, message):
        assert a == b,\
            message + '\n- Expected\n+ Generated\n? Comments\n'\
            + '\n'.join(difflib.ndiff(a, b))
