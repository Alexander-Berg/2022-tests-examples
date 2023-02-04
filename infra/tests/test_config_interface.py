# encoding: utf-8


def test_config_interface_generates_simple_clickhouse_driver_client_parameters(config_interface):
    """
        Checks that 'ConfigInterface' method
        '._generate_clickhouse_client_parameters'
        Parameters are generated from 'default' connection and credentials
        in 'config_example1.py' file.
        'default' connection and credentials are very simple:
          - only one host to choose from
          - no password
          - no SSL certificate used for verification
    """
    # noinspection PyProtectedMember
    params_generated = config_interface._generate_clickhouse_client_parameters(connection_name="default",
                                                                               credentials_name="default")
    assert params_generated["hosts"] == {
        "in_my_dc": [],
        "in_other_dcs": [
            {
                "fqdn": "localhost",
                "port": 9440,
                "datacenter_name": "vla"
            }
        ]
    }
    assert params_generated["user"] == "tentacles"
    assert params_generated["database"] == "tentacles_local"


def test_get_configured_harvesters_list(config_interface):
    configured_harvesters_list_generated = config_interface.get_configured_harvesters()
    configured_harvesters_list_expected = [
        "dummy", "juggler",
        "nanny_state_dumper", "hq", "resource_maker",
        "yp_lite_switcher", "yp_lite_pod_count_tuner",
        "yp_lite_reallocator", "clickhouse_dumper",
        "clickhouse_dropper", "clickhouse_optimizer",
        "yp_lite_pods_tracker"
    ]
    assert configured_harvesters_list_generated == configured_harvesters_list_expected


def test_get_harvester_config(config_interface):
    harvester_config_generated = config_interface.get_harvester_config("dummy")
    harvester_config_expected = {
        "common_parameters": {
            "source_url": "http://localhost/",
            "source_url_token_secret_name": "DUMMY_LOCALHOST_TOKEN",
        },
        "common_settings": {
            "update_interval_sec": 60,
            "rotate_snapshots_older_than_sec": 180,
            "chunk_size": 0,
            "data_list_path": "values_dummy",
        },
        "arguments": {
            "dummy_foo": {
                "query": "foo",
            },
            "dummy_bar": {
                "query": "bar",
            },
            "dummy_baz": {
                "query": "baz",
            },
        },
    }
    assert harvester_config_generated == harvester_config_expected


def test_get_yp_client_config(config_interface):
    config_generated = config_interface.get_yp_client_config("FAKE")
    assert config_generated == {
        "address": "fake.example.com:8090",
        "config": {
            "token": "yp_token",
            "request_timeout": 42
        }
    }


def test_get_yp_lite_pods_manager_config(config_interface):
    config_generated = config_interface.get_yp_lite_pods_manager_config()
    assert type(config_generated["clickhouse_client_parameters"]) == dict
