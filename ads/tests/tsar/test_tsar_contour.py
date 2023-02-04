import yatest
from ads.quality.adv_machine.tsar.cm_robot.tests.common import TsarRobot, upload_models_to_yt


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
        tsar_models_config = {
            './dssm_data/rsya_ctr50.appl.doc': '//home/advquality/tsar/tsar_models/dssm/rsya_ctr50.appl.doc',
            './dssm_data/identity50.proj': '//home/advquality/tsar/tsar_models/dssm/identity50.proj',
            './dssm_data/banner_BCPrGG_prod.appl': '//home/advquality/tsar/tsar_models/dssm/banner_BCPrGG_prod.appl',
            './ffm_data/robdrynkin_bsdev_75076_prod_v3': '//home/advquality/tsar/tsar_models/ffm/robdrynkin_bsdev_75076_prod_v3',
            './pytorch_data/dump': '//home/advquality/tsar/tsar_models/pytorch/dump',
            './search_dssm/assessor.appl': '//home/advquality/tsar/tsar_models/dssm/search/assessor.appl'
        }

        yt_client = local_robot.yt.yt_client
        upload_models_to_yt(yt_client, tsar_models_config)

        # since we want this test to be medium, we a not allowed to add huge models any more
        # So, in terms of economy, we reuse as many models as it is possible in this test

        yt_client.copy(
            '//home/advquality/tsar/tsar_models/dssm/rsya_ctr50.appl.doc',
            '//home/advquality/tsar/tsar_models/dssm/jruziev_tsar_searchBC1.appl'
        )

        local_robot.cm.call_target_async('tsar.run_all', timer=15 * 60)
        local_robot.wait_targets(['tsar.run_all', 'tsar.stream_enable'])

        local_robot.check_finish()
