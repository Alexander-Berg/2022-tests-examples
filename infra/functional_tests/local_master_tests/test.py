import yatest.common
import test_with_local_yp

import infra.yp_service_discovery.functional_tests.local_master_tests.scenario.scenario as scenario


def test():
    test_with_local_yp.run_test(
        yatest.common.output_path(),
        scenario,
        yp_instance_options={
            'yp_master_config': {
                'watch_manager': {
                    'query_selector_enabled_per_type': {
                        'pod': True
                    },
                    'state_per_type_per_log': {
                        'pod': {
                            'yp_lite_watch_log': 'query_store',
                            'deploy_watch_log': 'query_store',
                            'service_controller_watch_log': 'query_store',
                            'service_discovery_watch_log': 'query_store',
                            'service_discovery_with_boxes_watch_log': 'query_store',
                            'service_discovery_node_id_watch_log': 'query_store',
                        }
                    }
                }
            }
        }
    )
