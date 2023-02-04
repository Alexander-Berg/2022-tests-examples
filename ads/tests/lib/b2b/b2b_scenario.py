import logging

from ads.bsyeti.big_rt.py_test_lib import helpers as test_lib_helpers

from . import helpers, utils

log = logging.getLogger(__name__)
logging.basicConfig()


def get_profiles(stand, problems_interval_seconds, problems_max_count, trim_data, resharder_enabled=True, upload_to_yt=False):

    if resharder_enabled:
        with utils.measure_time("resharding"):
            with helpers.launch_resharder(stand) as resharder_process:
                helpers.process_resharding(stand, resharder_process, trim_data=trim_data)

        with utils.measure_time("freezing queues"):
            helpers.freeze_queues(stand)
    else:
        with utils.measure_time("preparing queues"):
            helpers.prepare_queues(stand)
        with utils.measure_time("pushing grut data"):
            helpers.prepate_grut(stand)

    with helpers.launch_caesar(
        stand,
        problems_interval_seconds=problems_interval_seconds,
        problems_max_count=problems_max_count,
    ) as caesar_process:
        for priority, workers in helpers.get_ordered_worker_batches(stand):
            workers = list(workers)
            log.info("processing %s with priority %s", workers, priority)
            with utils.measure_time("caesar {}".format([w.name for w in workers])):
                helpers.unfreeze_queues(stand, workers)
                helpers.wait_for_caesar([stand.queues[w.input_queue] for w in workers], process=caesar_process)
                helpers.freeze_queues(stand, workers)

        with utils.measure_time("read profiles"):
            profiles = helpers.read_profiles(stand, upload_to_yt)
        try:
            test_lib_helpers.retry(test_lib_helpers.log_sensors)(stand.caesar_port)
        except Exception as e:
            log.exception(e)
    return profiles
