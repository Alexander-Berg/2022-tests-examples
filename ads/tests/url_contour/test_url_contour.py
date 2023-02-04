import yatest
from ads.quality.adv_machine.content_merger.cm_robot.tests.common import ContentMergerRobot


def test_url_info(links, yt_stuff):
    work_path = yatest.common.work_path()
    output_path = yatest.common.output_path()
    resources_path = yatest.common.work_path()

    with ContentMergerRobot(links, work_path, output_path, resources_path, yt_stuff=yt_stuff) as local_robot:
        local_robot.cm.call_target_async('urls.direct_tier_extract', timer=15 * 60)
        local_robot.wait_targets(['urls.direct_tier_extract'])

        local_robot.check_finish()
