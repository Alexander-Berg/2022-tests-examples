import helpers
import logging
import local_yp
import pytest

from .conftest import create_user


TEST_ZONE_NAME = 'test-zone.yandex.net'


@pytest.mark.usefixtures("yp_env")
class TestBalancingRequestsOnYpClusters(object):
    YP_MASTER_CONFIG = {
        "authentication_manager": {
            "cypress_token_authenticator": {"root_path": "//yp/private/tokens"},
        },
    }

    def test_balancing_requests_to_yp_clusters(self, yp_env):
        yp_instances = yp_env
        yp_instances = {cluster: yp_instances[cluster] for cluster in list(yp_instances.keys())[:3]}
        assert len(yp_instances) == 3
        clusters = list(yp_instances.keys())
        yt_clients = {cluster: yp_instances[cluster].create_yt_client() for cluster in clusters}
        yp_clients = {cluster: yp_instances[cluster].create_client() for cluster in clusters}

        for cluster, yt_client in yt_clients.items():
            create_user(yp_instances[cluster], "bridge", grant_permissions=[("dns_record_set", "create")])
            yt_client.set("//yp/private/tokens/secret", "bridge")

        bridge, client = helpers.create_bridge(yp_instances, zones_config={
            TEST_ZONE_NAME: {
                'Clusters': clusters,
                'MaxReplicaAgeForLookup': '1ms',
                'WriteToChangelist': True,
                'MaxNumberChangesInRecordSetPerMinute': 0,
            },
        })

        banned = set([])
        left = set(clusters)

        for _ in range(len(clusters)):
            logging.info(f"Allowed clusters: {', '.join(left)}. Banned clusters: {', '.join(banned)}")
            record_sets = helpers.generate_record_sets(1)
            _, resp = helpers.add_records(client, record_sets,
                                          check_response={'clusters': left})

            used_cluster = resp.responses[0].update.cluster

            logging.info(yp_clients[used_cluster].get_object("dns_record_set", record_sets[0]['meta']['id'], selectors=['/meta', '/spec']))

            logging.info(f"Ban bridge user in {used_cluster} cluster")
            yp_clients[used_cluster].update_object(
                "user", "bridge", set_updates=[{"path": "/spec/banned", "value": True}]
            )
            local_yp.sync_access_control(yp_instances[used_cluster])

            logging.info(yp_clients[used_cluster].get_object("user", "bridge", selectors=['/meta', '/spec']))

            left.remove(used_cluster)
            banned.add(used_cluster)

        bridge.stop()
