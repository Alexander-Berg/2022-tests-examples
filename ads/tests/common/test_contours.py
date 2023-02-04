from ads.quality.adv_machine.cm_robot.tests.common import create_table_with_schema


def run_and_check_main_contour(local_robot):
    local_robot.cm.check_call_target("main_attempt.init_run", timeout=15 * 60)
    create_table_with_schema(local_robot.yt, '//home/advquality/adv_machine/period_stats/latest/slice_features.counter_offer', schema=[
        {"name": "CounterOfferID", "type": "uint64", "sort_order": "ascending"}
    ])

    create_table_with_schema(local_robot.yt, '//home/advquality/adv_machine/fresh_slices/counter_fresh_slices', schema=[
        {"name": "CounterID", "type": "uint64", "sort_order": "ascending"},
    ])
    create_table_with_schema(local_robot.yt, '//home/advquality/adv_machine/fresh_slices/counter_offer_fresh_slices', schema=[
        {"name": "CounterOfferID", "type": "uint64", "sort_order": "ascending"},
    ])
    create_table_with_schema(local_robot.yt, '//home/advquality/adv_machine/mobile_info/app_installs_by_banner', schema=[
        {"name": "BannerID", "type": "uint64", "sort_order": "ascending"},
        {"name": "InstallsCount", "type": "uint64"},
        {"name": "InstallsDiff", "type": "int64"}
    ])

    local_robot.yt.create_dir('//home/advquality/adv_machine/linear_models')
    with open('BroadPhraseNorm.dict') as f:
        local_robot.yt.yt_client.write_file('//home/advquality/adv_machine/linear_models/BroadPhraseNorm.dict', f)

    local_robot.cm.call_target_async("main_attempt.link_to_latest", timer=15 * 60)
    local_robot.wait_targets(["main_attempt.link_to_latest", "main.run_all"])

    local_robot.check_finish()

    print('Statistics from 4mr_beta:', local_robot.yt.get("//home/advquality/adv_machine/robot/latest/STREAMS.MERGED_FINAL.4mr_beta/@_stages_info"))


def run_and_check_build_contour(local_robot):
    local_robot.cm.call_target_async("build.produce_indices_and_minus_words", timer=15 * 60)
    local_robot.wait_targets(["build.produce_indices_and_minus_words"])

    local_robot.check_finish()
