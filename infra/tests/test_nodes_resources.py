import yp_util
from helpers import mock_select_objects

import mock

import io
import json
import sys
import unittest


class TestNodesResources(unittest.TestCase):
    @staticmethod
    def _run_nodes_resources(node_filter="%true", show_free=True):
        argv = [
            "--format", "json",
            "--cluster", "test",
            "--node-filter", node_filter,
            "--segment", "",
            "--no-pretty-units",
        ]
        if show_free:
            argv += [
                "--show-free",
            ]

        old_stdout = sys.stdout
        buf = io.StringIO()

        try:
            sys.stdout = buf
            yp_util.nodes_resources.main(argv)
        finally:
            sys.stdout = old_stdout

        return json.loads(buf.getvalue())

    @mock.patch("yp_util.nodes_resources.build_yp_client")
    def test_simple(self, build_yp_client_mock):
        node_id = "callisto"
        node_segment_id = "jupiter"

        total_resources = dict(
            vcpu=123124,
            memory=1231255,
            net_bw=4855,
            hdd=235235,
            ssd=345435,
            hdd_bw=235235,
            ssd_bw=325436,
            hdd_lvm=123123,
            ssd_lvm=888899,
            hdd_lvm_bw=123222,
            ssd_lvm_bw=578123,
        )

        free_resources = {key: value - 157 for key, value in total_resources.items()}

        mock_select_objects(
            build_yp_client_mock,
            nodes=[
                dict(
                    id=node_id,
                    segment=node_segment_id,
                    total_resources=total_resources,
                    free_resources=free_resources,
                ),
            ],
        )

        for show_free in (True, False):
            res = self._run_nodes_resources(show_free=show_free)
            assert len(res) == 2  # 1 row for 1 node and 1 row for total sum over all nodes.
            row = res[0]

            assert row["host"] == node_id
            assert row["segment"] == node_segment_id

            for key, expected_total in total_resources.items():
                assert row["total_" + key] == expected_total, \
                    "Mismatch on total_{}: expected {}, got {}, row is {}".format(key, expected_total, row["total_" + key], row)
                if show_free:
                    assert row["free_" + key] == free_resources[key], \
                        "Mismatch on free_{}: expected {}, got {}, row is {}".format(key, free_resources[key], row["free_" + key], row)
                else:
                    expected = total_resources[key] - free_resources[key]
                    assert row["used_" + key] == expected, \
                        "Mismatch on used_{}: expected {}, got {}, row is {}".format(key, expected, row["used_" + key], row)
