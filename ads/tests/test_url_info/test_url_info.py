import yatest
from ads.quality.adv_machine.cm_robot.tests.common import AdvMachineRobot


def test_url_info_contour(links, yt_stuff):
    work_path = yatest.common.work_path()
    output_path = yatest.common.output_path()
    resources_path = yatest.common.work_path()

    with AdvMachineRobot(links, work_path, output_path, resources_path, yt_stuff=yt_stuff) as local_robot:
        local_robot.cm.mark_success("url_info.prepare_sovetnik_urls")  # TODO: Test this!

        local_robot.cm.call_target_async("url_info.extract_titles", timer=15 * 60)
        local_robot.wait_targets(["url_info.extract_titles"])

        local_robot.cm.call_target_async("url_info.run_all", timer=15 * 60)
        local_robot.wait_targets(["url_info.run_all"])

        local_robot.cm.call_target_async("banner_url.run_all", timer=15 * 60)
        local_robot.wait_targets(["banner_url.run_all"])

        local_robot.cm.call_target_async("banner_url.stream_enable", timer=15 * 60)
        local_robot.wait_targets(["banner_url.stream_enable"])

        local_robot.check_finish()
