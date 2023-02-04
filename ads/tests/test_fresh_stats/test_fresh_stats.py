import yatest
from ads.quality.adv_machine.cm_robot.tests.common import AdvMachineRobot, create_table_with_schema


def test_group_export_info(links, yt_stuff):
    work_path = yatest.common.work_path()
    output_path = yatest.common.output_path()
    resources_path = yatest.common.work_path()

    with AdvMachineRobot(links, work_path, output_path, resources_path, yt_stuff=yt_stuff) as local_robot:
        create_table_with_schema(local_robot.yt, '//home/yabs/stat/AdvmCounterOfferStatChevent', schema=[
            {"name": "CounterOfferID", "type": "uint64", "sort_order": "ascending"},
            {"name": "CounterID", "type": "int64", "sort_order": "ascending"}
        ])
        create_table_with_schema(local_robot.yt, '//home/yabs/stat/AdvmCounterOfferStatVisit', schema=[
            {"name": "CounterOfferID", "type": "uint64", "sort_order": "ascending"},
            {"name": "CounterID", "type": "int64", "sort_order": "ascending"}
        ])
        create_table_with_schema(local_robot.yt, '//home/yabs/stat/AdvmCounterSelectTypeStatChevent', schema=[
            {"name": "CounterID", "type": "int64", "sort_order": "ascending"},
            {"name": "SelectType", "type": "int64", "sort_order": "ascending"},
            {"name": "CounterOfferID", "type": "uint64", "sort_order": "ascending"}
        ])
        create_table_with_schema(local_robot.yt, '//home/yabs/stat/AdvmCounterSelectTypeStatVisit', schema=[
            {"name": "CounterID", "type": "int64", "sort_order": "ascending"},
            {"name": "SelectType", "type": "int64", "sort_order": "ascending"},
            {"name": "CounterOfferID", "type": "uint64", "sort_order": "ascending"}
        ])
        local_robot.cm.call_target_async("fresh_stats.prepare_fresh_stats", timer=15 * 60)
        local_robot.wait_targets(["fresh_stats.prepare_fresh_stats"])

        local_robot.check_finish()
