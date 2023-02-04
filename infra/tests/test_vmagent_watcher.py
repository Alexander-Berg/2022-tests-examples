import mock
import datetime
from infra.qyp.vmagent_monitoring.src import vmagent_watcher
from infra.qyp.vmagent_monitoring.src import helpers


def test_process():
    vmagent_watcher.VmagentWatcher.YP_CLUSTERS = {"SAS": "random_address"}
    watcher = vmagent_watcher.VmagentWatcher()
    helpers.get_client = mock.Mock(return_value=None)
    make_ts_now_time = helpers.make_ts(datetime.datetime.now())
    make_ts_old_time = helpers.make_ts(datetime.datetime.now() -
                                       datetime.timedelta(seconds=2*(helpers.GOOD_UPD_TIME/(10**3))))
    pod_list = [
        vmagent_watcher.helpers.Vm("SAS", "sas_pod1", "ACTIVE", make_ts_now_time, "node_sas"),  # good
        vmagent_watcher.helpers.Vm("SAS", "sas_pod2", "ACTIVE", make_ts_old_time, "node_sas"),  # bad_time
        vmagent_watcher.helpers.Vm("SAS", "sas_pod3", "BAD_STATUS", make_ts_now_time, "node_sas"),  # bad_status
        vmagent_watcher.helpers.Vm("SAS", "sas_pod4", "BAD_STATUS", make_ts_now_time, "node_sas"),  # bad_status
    ]

    def get_pod_list(*args):
        return pod_list

    pod_list_patch = mock.patch("infra.qyp.vmagent_monitoring.src.helpers.get_pods", get_pod_list)
    pod_list_patch.start()
    watcher._process()
    pod_list[3].state = "NEW_BAD_STATUS"
    watcher._process()
    broken_vms_list = [
        vmagent_watcher.helpers.Vm("SAS", "sas_pod2", "ACTIVE", make_ts_old_time, "node_sas"),  # bad_time
        vmagent_watcher.helpers.Vm("SAS", "sas_pod3", "BAD_STATUS", make_ts_now_time, "node_sas"),  # bad_status
    ]
    bad_status_list = [
        vmagent_watcher.helpers.Vm("SAS", "sas_pod3", "BAD_STATUS", make_ts_now_time, "node_sas"),  # bad_status
        vmagent_watcher.helpers.Vm("SAS", "sas_pod4", "NEW_BAD_STATUS", make_ts_now_time, "node_sas"),  # bad_status
    ]
    broken_pods_from_watcher = [x.pod_id for x in watcher.broken_pods["SAS"]["list_with_broken_vms"]]
    broken_pods_from_watcher.sort()
    broken_pods = [x.pod_id for x in broken_vms_list]
    broken_pods.sort()
    bad_status_pods_from_watcher = [x.pod_id for x in watcher.prev_bad_state_vms]
    bad_status_pods_from_watcher.sort()
    bad_status_pods = [x.pod_id for x in bad_status_list]
    bad_status_pods.sort()
    assert broken_pods_from_watcher == broken_pods
    assert bad_status_pods_from_watcher == bad_status_pods
    pod_list_patch.stop()
