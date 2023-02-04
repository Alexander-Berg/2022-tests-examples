from helpers import mock_select_objects
import yp_util

import mock

import io
import json
import random
import sys
import unittest


class TestAccountExplain(unittest.TestCase):
    @staticmethod
    def _run_account_explain(account_id, recursive=False, sort_by=None, cluster="test", output_format="json"):
        argv = [account_id, "--format", output_format, "--cluster", cluster, "--abc-token", "foo"]
        if recursive:
            argv += ["--recursive"]
        if sort_by:
            argv += ["--sort-by", ",".join(sort_by)]

        old_stdout = sys.stdout
        buf = io.StringIO()

        try:
            sys.stdout = buf
            yp_util.account_explain.main(argv)
        finally:
            sys.stdout = old_stdout

        if output_format == "json":
            return json.loads(buf.getvalue())
        else:
            # tabular mode
            return buf.getvalue()

    @staticmethod
    def _mock_abc_client(abc_client_mock, account_children=None, slug_to_abc_id=None):
        account_children = account_children or dict()
        slug_to_abc_id = slug_to_abc_id or dict()

        abc_client_mock.AbcClient.FQDN = "foo.yandex-team.ru"

        def get_abc_id(account_id):
            prefix = "abc:service:"
            assert account_id.startswith(prefix)
            return int(account_id[len(prefix):])

        service_id_to_children = {
            get_abc_id(account_id): [get_abc_id(child) for child in children]
            for account_id, children in account_children.items()
        }

        def list_service_children(service_id):
            service_ids = service_id_to_children.get(service_id, [])
            return {service_id: dict(slug="<mock>") for service_id in service_ids}

        client = abc_client_mock.AbcClient()
        client.list_service_children.side_effect = list_service_children
        client.get_service_slug_by_id.side_effect = lambda account_id: "<mock>"
        client.get_service_id_by_slug.side_effect = lambda slug: slug_to_abc_id.get(slug)

    @mock.patch("yp_util.account_explain.abc_client")
    @mock.patch("yp_util.account_explain.build_yp_client")
    def test_simple(self, build_yp_client_mock, abc_client_mock):
        account_id = "lolo_account"
        node_segment_id = "pepperoni"

        account_usage_per_segment = {
            node_segment_id: dict(
                vcpu=123124,
                memory=1231255,
                net_bw=4855,
                hdd=235235,
                ssd=345435,
                hdd_bw=235235,
                ssd_bw=325436,
                ipv4=343,
            )
        }

        account_limits_per_segment = {
            node_segment_id: dict(
                vcpu=223124,
                memory=2231255,
                net_bw=5855,
                hdd=335235,
                ssd=445435,
                hdd_bw=335235,
                ssd_bw=425436,
                ipv4=443,
            )
        }

        pod_set_1_usage = {key: 100 for key in account_usage_per_segment[node_segment_id]}
        pod_set_2_usage = {key: value - 100 for key, value in account_usage_per_segment[node_segment_id].items()}

        mock_select_objects(
            build_yp_client_mock,
            accounts=[
                dict(
                    id=account_id,
                    usage_per_segment=account_usage_per_segment,
                    limits_per_segment=account_limits_per_segment,
                )
            ],
            pod_sets=[
                dict(
                    id="1",
                    account_id=account_id,
                    node_segment_id=node_segment_id,
                    usage=pod_set_1_usage,
                ),
                dict(
                    id="2",
                    account_id=account_id,
                    node_segment_id=node_segment_id,
                    usage=pod_set_2_usage,
                ),
            ],
        )

        result = self._run_account_explain(account_id)

        for expected, got in (
            (account_limits_per_segment[node_segment_id], result["accounts_limits"]),
            (account_usage_per_segment[node_segment_id], result["accounts_usages"]),
        ):
            assert len(got) == 1
            res = got[0]

            assert res["account_id"] == account_id
            assert res["segment"] == node_segment_id

            for key, value in expected.items():
                assert res[key] == value

        pod_sets_usages = sorted(result["pod_sets_usages"], key=lambda x: x["vcpu"])

        assert len(pod_sets_usages) == 2

        for expected, got in (
            (pod_set_1_usage, pod_sets_usages[0]),
            (pod_set_2_usage, pod_sets_usages[1]),
        ):
            for key, value in expected.items():
                assert got[key] == value, "Expected {}, got {}".format(expected, got)

        # Finally, very simple "tabular" mode test
        result_tabular = self._run_account_explain(account_id, cluster="test,test", output_format="tabular")
        assert "Cluster `test`" in result_tabular
        assert " total " in result_tabular

    @mock.patch("yp_util.account_explain.abc_client")
    @mock.patch("yp_util.account_explain.build_yp_client")
    def test_recursive(self, build_yp_client_mock, abc_client_mock):
        parent_account_id = "abc:service:1"
        first_account_id = "abc:service:100"
        second_account_id = "abc:service:200"
        node_segment_id = "nondefault"

        mock_select_objects(
            build_yp_client_mock,
            accounts=[
                dict(
                    id=parent_account_id,
                    usage_per_segment={},
                    limits_per_segment={},
                ),
                dict(
                    id=first_account_id,
                    usage_per_segment={
                        node_segment_id: dict(vcpu=100),
                    },
                    limits_per_segment={
                        node_segment_id: dict(vcpu=1000),
                    },
                ),
                dict(
                    id=second_account_id,
                    usage_per_segment={
                        node_segment_id: dict(vcpu=200),
                    },
                    limits_per_segment={
                        node_segment_id: dict(vcpu=2000),
                    },
                ),
            ],
            pod_sets=[
                dict(
                    id="1",
                    account_id=first_account_id,
                    node_segment_id=node_segment_id,
                    usage=dict(vcpu=100),
                ),
                dict(
                    id="2",
                    account_id=second_account_id,
                    node_segment_id=node_segment_id,
                    usage=dict(vcpu=200),
                ),
            ],
        )

        self._mock_abc_client(
            abc_client_mock,
            account_children={
                parent_account_id: [first_account_id, second_account_id],
            },
        )

        result = self._run_account_explain(parent_account_id, recursive=True)
        assert len(result["accounts_limits"]) == len(result["accounts_usages"]) == 2
        assert len(result["pod_sets_usages"]) == 2

        limits_per_account = {r["account_id"]: r for r in result["accounts_limits"]}
        usages_per_account = {r["account_id"]: r for r in result["accounts_usages"]}

        assert usages_per_account[first_account_id]["vcpu"] == 100
        assert limits_per_account[first_account_id]["vcpu"] == 1000
        assert usages_per_account[second_account_id]["vcpu"] == 200
        assert limits_per_account[second_account_id]["vcpu"] == 2000

        usages_per_pod_set = {r["pod_set_id"]: r for r in result["pod_sets_usages"]}

        assert usages_per_pod_set["1"]["vcpu"] == 100
        assert usages_per_pod_set["2"]["vcpu"] == 200

    @mock.patch("yp_util.account_explain.abc_client")
    @mock.patch("yp_util.account_explain.build_yp_client")
    def test_slug(self, build_yp_client_mock, abc_client_mock):
        account_id = "abc:service:123"
        node_segment_id = "lolwut"

        mock_select_objects(
            build_yp_client_mock,
            accounts=[
                dict(
                    id=account_id,
                    usage_per_segment={
                        node_segment_id: dict(memory=10000, vcpu=30000),
                    },
                    limits_per_segment={
                        node_segment_id: dict(memory=15000, vcpu=50000),
                    },
                ),
            ],
            pod_sets=[
                dict(
                    id="1",
                    account_id=account_id,
                    node_segment_id=node_segment_id,
                    usage=dict(memory=10000, vcpu=30000),
                ),
            ],
        )

        self._mock_abc_client(abc_client_mock, slug_to_abc_id={"hehe": 123})

        result = self._run_account_explain("abc:slug:hehe")
        assert len(result["accounts_limits"]) == len(result["accounts_usages"]) == 1
        assert len(result["pod_sets_usages"]) == 1

        account_limits = result["accounts_limits"][0]
        account_usages = result["accounts_usages"][0]
        pod_set_usages = result["pod_sets_usages"][0]

        assert account_limits["account_id"] == account_id
        assert account_limits["vcpu"] == 50000
        assert account_limits["memory"] == 15000

        assert account_usages["account_id"] == account_id
        assert account_usages["vcpu"] == 30000
        assert account_usages["memory"] == 10000

        assert pod_set_usages["pod_set_id"] == "1"
        assert pod_set_usages["vcpu"] == 30000
        assert pod_set_usages["memory"] == 10000

    @mock.patch("yp_util.account_explain.abc_client")
    @mock.patch("yp_util.account_explain.build_yp_client")
    def test_sort(self, build_yp_client_mock, abc_client_mock):
        account_id = "foobar"
        node_segment_id = "trololo"

        pod_sets = [
            dict(
                id=str(i),
                account_id=account_id,
                node_segment_id=node_segment_id,
                usage=dict(vcpu=(i // 5) * 100, memory=(i + 1) * 100),
            )
            for i in range(10)
        ]
        random.shuffle(pod_sets)

        mock_select_objects(
            build_yp_client_mock,
            accounts=[
                dict(
                    id=account_id,
                    usage_per_segment={
                        node_segment_id: dict(memory=5500, vcpu=1500),
                    },
                    limits_per_segment={
                        node_segment_id: dict(memory=5500, vcpu=1500),
                    },
                ),
            ],
            pod_sets=pod_sets,
        )

        result = self._run_account_explain(account_id, sort_by=["vcpu", "memory"])
        assert len(result["accounts_limits"]) == len(result["accounts_usages"]) == 1
        assert len(result["pod_sets_usages"]) == 10

        for i, usage in enumerate(result["pod_sets_usages"]):
            k = 9 - i
            assert usage["pod_set_id"] == str(k)
            assert usage["vcpu"] == (k // 5) * 100
            assert usage["memory"] == (k + 1) * 100
