#!/usr/bin/env python

import mock
import json
import datetime
from itertools import izip

from infra.netconfig.checks import execution_state


def test_execution_state(tmpdir):
    state_str = """[
    {
        "interface": "eth0",
        "setup_stages": {
            "bootstrap_net": {
                "exception": null,
                "finished": true,
                "message": "Bootstrap network interface eth0",
                "status": "OK",
                "timestamp": "2014-06-22 20:56:44.640905",
                "traceback": null
            },
            "vlans_routes": {
                "exception": null,
                "finished": true,
                "message": "Setup vlans and routes on eth0",
                "status": "OK",
                "timestamp": "2014-06-22 20:57:03.037533",
                "traceback": null
            }
        }
    }
]"""

    state = json.loads(state_str)
    tmpfile = tmpdir.join("test_netconfig_state.json")
    path = str(tmpfile)

    with mock.patch('infra.netconfig.lib.jugglerutil.push_local') as m:
        def ensure_invocation(call_args, need_service, need_status, predicate):
            print call_args
            (status, descr, service, tags), _ = call_args
            assert service == need_service
            assert status == need_status and predicate(descr), "Unexpected event generated: %s - %s" % (status, descr)

        def ensure_events(mock_util, need_list):
            print mock_util.call_args_list
            assert len(mock_util.call_args_list) == len(need_list), "Unexpected number of invocations: %d vs %d" % (len(mock_util.call_args_list), len(need_list))

            for (c, n) in izip(mock_util.call_args_list, need_list):
                ensure_invocation(c, *n)

        # Outdated state

        tmpfile.write(json.dumps(state))
        assert not execution_state.run(path)
        ensure_events(m, [
            ('netconfig_bootstrap', 'CRIT', lambda x: "outdated" in x and "bootstrap_net" in x),
            ('netconfig_vlans_routes', 'CRIT', lambda x: "outdated" in x and "vlans_routes" in x),
            ('netconfig_execution_state_check', 'OK', lambda x: x == ""),
        ])

        # OK state

        m.reset_mock()
        state[0]["setup_stages"]["bootstrap_net"]["timestamp"] = str(datetime.datetime.now())
        state[0]["setup_stages"]["vlans_routes"]["timestamp"] = str(datetime.datetime.now())
        tmpfile.write(json.dumps(state))
        assert execution_state.run(path)
        ensure_events(m, [
            ('netconfig_bootstrap', 'OK', lambda x: x == ""),
            ('netconfig_vlans_routes', 'OK', lambda x: x == ""),
            ('netconfig_execution_state_check', 'OK', lambda x: x == ""),
        ])

        # vlans ERROR

        m.reset_mock()
        state[0]["setup_stages"]["vlans_routes"]["status"] = "ERROR"
        state[0]["setup_stages"]["vlans_routes"]["exception"] = "BadError"
        state[0]["setup_stages"]["vlans_routes"]["traceback"] = "BadTraceback"

        tmpfile.write(json.dumps(state))
        assert not execution_state.run(path)
        ensure_events(m, [
            ('netconfig_bootstrap', 'OK', lambda x: x == ""),
            ('netconfig_vlans_routes', 'CRIT', lambda x: "BadError" in x and "BadTraceback" in x and "vlans_routes" in x),
            ('netconfig_execution_state_check', 'OK', lambda x: x == ""),
        ])

        # vlans hangup

        m.reset_mock()
        state[0]["setup_stages"]["vlans_routes"]["status"] = "INPROGRESS"
        state[0]["setup_stages"]["bootstrap_net"]["timestamp"] = str(
            datetime.datetime.now() - datetime.timedelta(seconds=1000))
        state[0]["setup_stages"]["vlans_routes"]["timestamp"] = str(
            datetime.datetime.now() - datetime.timedelta(seconds=1000))

        tmpfile.write(json.dumps(state))
        assert not execution_state.run(path)
        ensure_events(m, [
            ('netconfig_bootstrap', 'OK', lambda x: x == ""),
            ('netconfig_vlans_routes', 'CRIT', lambda x: "vlans_routes" in x and "hangup" in x),
            ('netconfig_execution_state_check', 'OK', lambda x: x == ""),
        ])

        # bootstrap hangup

        m.reset_mock()
        state[0]["setup_stages"]["bootstrap_net"]["status"] = "INPROGRESS"
        del state[0]["setup_stages"]["vlans_routes"]
        tmpfile.write(json.dumps(state))
        assert not execution_state.run(path)
        ensure_events(m, [
            ('netconfig_bootstrap', 'CRIT', lambda x: "bootstrap_net" in x and "hangup" in x),
            # OK for now (RTCNETWORK-16)
            ('netconfig_vlans_routes', 'OK', lambda x: "vlans_routes" in x and "Missing" in x),
            ('netconfig_execution_state_check', 'OK', lambda x: x == "")
        ])

        # bootstrap ERROR

        m.reset_mock()
        state[0]["setup_stages"]["bootstrap_net"]["status"] = "ERROR"
        state[0]["setup_stages"]["bootstrap_net"]["exception"] = "BadError"
        state[0]["setup_stages"]["bootstrap_net"]["traceback"] = "BadTraceback"

        tmpfile.write(json.dumps(state))
        assert not execution_state.run(path)
        ensure_events(m, [
            ('netconfig_bootstrap', 'CRIT', lambda x: "BadError" in x and "BadTraceback" in x and "bootstrap_net" in x),
            # OK for now (RTCNETWORK-16)
            ('netconfig_vlans_routes', 'OK', lambda x: "vlans_routes" in x and "Missing" in x),
            ('netconfig_execution_state_check', 'OK', lambda x: x == "")
        ])
