import json

import logging

import os

import subprocess as sp

import yt.wrapper as yt

import yatest
from ads.quality.adv_machine.tsar.cm_robot.tests.common import TsarRobot, upload_models_to_yt


MAE_THRESHOLD = 1e-5


def make_old_banner_transport_config(
        adv_tsar_config,
        yt_proxy,
        ffm_model_path,
        dssm_model_path,
        dssm_projector_path,
        pytorch_model_path,
        search_bc_dssm_model,
        dst_table):
    config = {}
    config['Cluster'] = yt_proxy
    config['AdvMachineExportTable'] = adv_tsar_config['FilteredJoinedRsyaBannersTable']
    config['OutputTable'] = dst_table
    config['ScoringDataSizePerJobGB'] = 1
    config['ScoringMapperMemoryLimitGB'] = 20
    config['MetricTable'] = '//home/advquality/tsar/tsar_contour/BannerVectorsValidationTest'
    config['MetricsTolerance'] = 0.1,
    config['GenocideResultTableName'] = '//home/advquality/tsar/tsar_contour/GenocideResults'
    config['GenocideTable'] = '//home/advquality/tsar/tsar_contour/genocide_results_test'
    config['YtPool'] = 'adv-machine'
    config['ModelFolder'] = 'LEGACY_FIELD'

    models = []

    # ffm
    models.append(adv_tsar_config['FFMModel'])
    models[-1]['ModelPath'] = ffm_model_path

    # dssm
    models.append(adv_tsar_config['TsarDssmModel'])
    models[-1]['ModelPath'] = dssm_model_path
    models[-1]['ProjectorPath'] = dssm_projector_path

    # torch
    models.append(adv_tsar_config['TsarPytorchModel'])
    models[-1]['ModelPath'] = pytorch_model_path

    # bc search dssm
    for dssm_model in adv_tsar_config['DssmModels']:
        if dssm_model.get('Type') == 'TsarSearchDssm':
            models.append(dssm_model.copy())
            models[-1]['ModelPath'] = search_bc_dssm_model

    config['Models'] = models

    config['AcceptableDataWeightDeviation'] = 0.3
    config['VersionsPrefix'] = '//home/advquality/tsar/tsar_contour/versions'
    config['TargetDomainTableName'] = '//home/yabs/dict/TargetDomain'
    config['ApplyGenocide'] = False
    config['FilterNonActiveBanners'] = False

    return config


def prepare_testing_workspace(yt_client, yt_proxy, adv_tsar_config_path, am_make_dump_binary):
    with open(adv_tsar_config_path) as f:
        adv_tsar_config = json.load(f)

    ffm_local_path = './ffm_data/robdrynkin_bsdev_75076_prod_v3'
    dssm_local_path = './dssm_data/rsya_ctr50.appl.doc'
    dssm_projector_local_path = './dssm_data/identity50.proj'
    pytorch_local_path = './pytorch_data/BannerNamespaces'
    search_bc_model_local_path = './dssm_data/jruziev_tsar_searchBC1.appl'
    adv_torch_prepared_dump_path = './dump'

    sp.check_call(
        '{} make-pytorch-dump --model-dir {} -o {}'.format(os.path.join('./', am_make_dump_binary), pytorch_local_path, adv_torch_prepared_dump_path),
        shell=True
    )

    tsar_models_config = {
        dssm_local_path: '//home/advquality/tsar/tsar_models/dssm/rsya_ctr50.appl.doc',
        dssm_projector_local_path: '//home/advquality/tsar/tsar_models/dssm/identity50.proj',
        './dssm_data/banner_BCPrGG_prod.appl': '//home/advquality/tsar/tsar_models/dssm/banner_BCPrGG_prod.appl',
        search_bc_model_local_path: '//home/advquality/tsar/tsar_models/dssm/jruziev_tsar_searchBC1.appl',
        ffm_local_path: '//home/advquality/tsar/tsar_models/ffm/robdrynkin_bsdev_75076_prod_v3',
        adv_torch_prepared_dump_path: '//home/advquality/tsar/tsar_models/pytorch/dump',
        './search_dssm/assessor.appl': '//home/advquality/tsar/tsar_models/dssm/search/assessor.appl'
    }

    upload_models_to_yt(yt_client, tsar_models_config)

    yt_client.create('map_node', '//home/advquality/tsar/tsar_contour/versions', recursive=True)

    old_transport_config = make_old_banner_transport_config(
        adv_tsar_config=adv_tsar_config,
        yt_proxy=yt_proxy,
        ffm_model_path=ffm_local_path,
        dssm_model_path=dssm_local_path,
        dssm_projector_path=dssm_projector_local_path,
        pytorch_model_path=pytorch_local_path,
        search_bc_dssm_model=search_bc_model_local_path,
        dst_table='//home/advquality/tsar/tsar_contour/BannerTransportResult'
    )

    with open('tsar_transport.json', 'w') as f:
        json.dump(old_transport_config, f)

    return tsar_models_config


def clear_yt_space(yt_client, models_config):
    for _, yt_path in models_config.items():
        if yt_client.exists(yt_path):
            yt_client.remove(yt_path)


def test_tsar_contour(links, yt_stuff, yql_api):
    work_path = yatest.common.work_path()
    output_path = yatest.common.output_path()
    resources_path = yatest.common.work_path()

    with TsarRobot(
            links,
            work_path,
            output_path,
            resources_path,
            yt_stuff=yt_stuff,
            local_yql_api=yql_api) as local_robot:
        am_make_dump_binary = 'am_make_dump'
        am_old_banner_transport_binary = 'banner_transport'
        am_tsar_diff_checker_binary = 'am_tsar_diff_checker'

        yt_client = local_robot.yt.yt_client

        tsar_models = prepare_testing_workspace(yt_client, local_robot.yt.get_proxy(), './bin/tsar/scripts/tsar_config.json', am_make_dump_binary)

        local_robot.cm.call_target_async('tsar.run_all', timer=15 * 60)
        local_robot.wait_targets(['tsar.run_all', 'tsar.stream_enable'])

        local_robot.check_finish()

        clear_yt_space(yt_client, tsar_models)

        sp.check_call(
            '{} --config {}'.format(os.path.join('./', am_old_banner_transport_binary), 'tsar_transport.json'),
            shell=True
        )

        with open('./bin/tsar/scripts/tsar_config.json') as adv_t_c, open('tsar_transport.json') as tsar_t_c:
            adv_tsar_config = json.load(adv_t_c)
            old_tsar_config = json.load(tsar_t_c)

        diff_table_name = '//home/advquality/tsar/tsar_contour/DiffTable'

        sp.check_call(
            '{} --tsar-transport {} --adv-transport {} --output-diff-table {} -s {}'.format(
                os.path.join('./', am_tsar_diff_checker_binary),
                old_tsar_config['OutputTable'],
                adv_tsar_config['FinalTable'],
                diff_table_name,
                local_robot.yt.get_proxy()
            ),
            shell=True
        )

        row_count = int(yt_client.get(yt.ypath_join(diff_table_name, '@row_count')))

        logging.info('Got {} rows in diff table'.format(row_count))

        diff_row_count = 0
        avg_mae = 0.0
        avg_mse = 0.0

        has_mae_outlier = False

        for i, r in enumerate(yt_client.read_table(diff_table_name, format='json')):
            if r['NFlappingCoordinates'] > 0:
                logging.error('Got diff in row# %d! Vector version: %d; mae: %f; mse: %f; n_outlier_coords: %d' % (i + 1, r['Version'], r['MAE'], r['MSE'], r['NFlappingCoordinates']))
                diff_row_count += 1
            if r['MAE'] > MAE_THRESHOLD:
                logging.error('Row #%d has too big absolute difference' % (i+1))
                has_mae_outlier = True
            avg_mae += r['MAE']
            avg_mse += r['MSE']

        logging.info('Rows total: %d' % row_count)
        logging.info('Avg MAE: %f' % avg_mae)
        logging.info('Avg MSE: %f' % avg_mse)
        logging.info('Rows with flapping coords: %d' % diff_row_count)

        if has_mae_outlier:
            raise Exception('Check failed')
