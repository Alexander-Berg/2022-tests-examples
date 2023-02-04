import mock

from datetime import datetime

from maps.analyzer.pylibs.ecstatic.lib import ecstatic

from maps.analyzer.services.jams_analyzer.tools.jams_uploader.lib import monitoring
from maps.pylibs.monitoring.test.test_case import TestCase


@mock.patch('maps.analyzer.pylibs.ecstatic.lib.ecstatic.now', mock.MagicMock())
class EdgeSpeedsUploaderTest(TestCase):

    def juggler_process(self, log_class):
        def run(log_lines):
            monitoring.juggler_process(log_lines, self.juggler_send, log_class)

        return run

    def juggler_process_realtime(self, log_lines):
        monitoring.juggler_process(log_lines, self.juggler_send, monitoring.LogClass.JAMS)

    def juggler_process_masstransit(self, log_lines):
        monitoring.juggler_process(log_lines, self.juggler_send, monitoring.LogClass.MASSTRANSIT)

    def test_empty_log_gives_coordination_error(self):
        def check_empty_log_gives_coordination_error(_, log_class):
            self.do_logs_processing = self.juggler_process(log_class)
            self.expect_juggler('2;ERROR', "Can't perform the uploader coordination for too long: None")
            self.verify_logs_processing("")

        check_empty_log_gives_coordination_error("", monitoring.LogClass.JAMS)
        check_empty_log_gives_coordination_error("masstransit.", monitoring.LogClass.MASSTRANSIT)

    def test_max_download_time(self):
        def check_max_download_time(_, log_class):
            self.do_logs_processing = self.juggler_process(log_class)
            self.expect_juggler('0;OK', '')
            ecstatic.now.return_value = datetime(2015, 12, 2, hour=15, minute=50, second=40)

            self.verify_logs_processing("""
[2015-12-02 15:49:34] INFO: Download time 1 s, @mon{"download_time": 1.480, "start_time": "20151202T154930"}
[2015-12-02 15:49:35] INFO: @mon {"coordination_start_time": "20151202T154935", "coordination_result": true, "coordination_time": "0.052"}
[2015-12-02 15:50:05] INFO: Download time 2 s, @mon{"download_time": 1.001, "start_time": "20151202T155001"}
[2015-12-02 15:50:07] INFO: @mon {"coordination_start_time": "20151202T155006", "coordination_result": true, "coordination_time": "0.1"}
[2015-12-02 15:50:35] INFO: Download time 3 s, @mon{"download_time": 4.379, "start_time": "20151202T155030"}
                """)

        check_max_download_time("", monitoring.LogClass.JAMS)
        check_max_download_time("masstransit.", monitoring.LogClass.MASSTRANSIT)

    def test_max_upload_time(self):
        def check_max_upload_time(_, log_class):
            self.do_logs_processing = self.juggler_process(log_class)
            # we ignore upload time, but time from coordination to now will be large to produce coordination error
            self.expect_juggler('2;ERROR', "Can't perform the uploader coordination for too long: 72.0")
            ecstatic.now.return_value = datetime(2015, 12, 2, hour=15, minute=48, second=50)

            self.verify_logs_processing("""
[2015-12-02 15:47:33] INFO: Download time 1 s, @mon{"download_time": 1.480, "start_time": "20151202T154731"}
[2015-12-02 15:47:38] INFO: @mon {"coordination_start_time": "20151202T154738", "coordination_result": true, "coordination_time": "0.052"}
[2015-12-02 15:47:39] INFO: Upload time 1 s, @mon{"upload_time": 110.001}
[2015-12-02 15:48:38] INFO: Upload time 2 s, @mon{"upload_time": 110}
[2015-12-02 15:48:49] INFO: Upload time 3 s, @mon{"upload_time": 100.100}
                """)

        check_max_upload_time("", monitoring.LogClass.JAMS)
        check_max_upload_time("masstransit.", monitoring.LogClass.MASSTRANSIT)

    def test_all_metrics_at_once(self):
        def check_all_metrics_at_once(_, log_class):
            self.do_logs_processing = self.juggler_process(log_class)
            self.expect_juggler('2;ERROR', 'max_download_time (value=110 >= threshold=12)')
            ecstatic.now.return_value = datetime(2015, 12, 2, hour=15, minute=48, second=50)

            self.verify_logs_processing("""
[2015-12-02 15:47:35] INFO: Download time 1 s, @mon{"download_time": 100.100}
[2015-12-02 15:47:38] INFO: @mon {"coordination_start_time": "20151202T154738", "coordination_result": true, "coordination_time": "0.052"}
[2015-12-02 15:47:39] INFO: Upload time 3 s, @mon{"upload_time": 4.379}
[2015-12-02 15:48:35] INFO: Download time 4 s, @mon{"download_time": 110.001}
[2015-12-02 15:48:38] INFO: @mon {"coordination_start_time": "20151202T154838", "coordination_result": true, "coordination_time": "0.052"}
[2015-12-02 15:48:40] INFO: Upload time 6 s, @mon{"upload_time": 3.480}
                """)

        check_all_metrics_at_once("", monitoring.LogClass.JAMS)
        check_all_metrics_at_once("masstransit.", monitoring.LogClass.MASSTRANSIT)
