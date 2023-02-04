env_example1 = {
    "ENV_SECRET_NAME": "ENV_SECRET_VALUE",
    "MONGODB_PASSWORD": "mongodb_qwerty_custom",
    "CLICKHOUSE_PASSWORD": "clickhouse_qwerty_custom",
    "DUMMY_LOCALHOST_TOKEN": "dummy_localhost_token",
    "NANNY_OAUTH_TOKEN": "nanny_oauth_token",
    "YP_TOKEN": "yp_token",
}

cli_args_example1 = {
    "CONFIG_FILE": "/tmp/example.conf",
    "MONGO_DB_NAME": None,
    "CH_DB_NAME": None,
    "SECRETS_DIRECTORY": None,
    "MY_DATACENTER_NAME": "man",
}

config_file_example1 = """
---

# Secrets settings
secrets:
  secrets_directory: "/tmp"


# Miscellaneous settings
misc:
  my_datacenter_name: Null
  env_name: "testing"
  url: "tentacles-tests.y-t.ru"


# Storages connection and credentials
storage:

  # MongoDb
  mongodb:
    connection:
      default:
        hosts:
          - fqdn: "server1.example.com"
            port: 27017
          - fqdn: "server2.example.com"
            port: 27017
          - fqdn: "server3.example.com"
            port: 27017
        replicaset: "example_replicaset_name"
        ssl_ca_cert_path: Null
        database_name: "custom_database_name"
    credentials:
      default:
        username: "tentacles_custom_username"
        auth_source: "custom_auth_source_database"
        password_secret_name: MONGODB_PASSWORD

  # ClickHouse
  clickhouse:
    connection:
      default:
        hosts:
          - fqdn: "localhost"
            port: 9440
            datacenter_name: "vla"
        ssl_ca_cert_path: Null
        database_name: "tentacles_local"
        native_port: 9440
        connect_timeout: 2
        read_timeout: 20
    credentials:
      default:
        username: "tentacles"
        password_secret_name: CLICKHOUSE_PASSWORD

api:

  server_config:
    port: 80
    listen_queue: 1024
    processes: 1
    threads: 1
    logfile: /logs/uwsgi.log
    log_maxsize: 4194304  # 4M
    clickhouse:
      connection: "default"
      credentials: "default"

  gui_config:
    yasm_panel_all_metrics: "https://yasm.yandex-team.ru/template/panel/template-example/service_key={0}{1}/"


worker:
  http:
    port: 8000
  debug:
    sock_path:

# Harvesters settings
harvesters:

  results_storage:
    mongodb:
      connection: "default"
      credentials: "default"
      locks_collection_name: "harvesters_locks"
      pymongo_additional_params:
        connect: True
        appname: "harvester"

  harvesters:
    dummy:
      common_parameters:
        source_url: "http://localhost/"
        source_url_token_secret_name: "DUMMY_LOCALHOST_TOKEN"
      common_settings:
        chunk_size: 0
        data_list_path: "values_dummy"
        update_interval_sec: 60
        rotate_snapshots_older_than_sec: 180
      arguments:
        dummy_foo:
            query: "foo"
        dummy_bar:
            query: "bar"
        dummy_baz:
            query: "baz"
    juggler:
      common_parameters:
        complete_events_url: "http://juggler-api.search.yandex.net/api/events/complete_events"
        complete_events_url_essential_params:
          do: 1
          format: "json"
        timeout: 15
      common_settings:
        chunk_size: 0
        data_list_path: Null
        update_interval_sec: 60
        rotate_snapshots_older_than_sec: 300
      arguments:
        some_juggler_hostname__timestamp-age:
          host_name: "some_juggler_hostname"
          service_name: "timestamp-age"
        some_juggler_hostname__UNREACHABLE:
          host_name: "some_juggler_hostname"
          service_name: "UNREACHABLE"
    nanny_state_dumper:
      common_parameters:
        token_secret_name: "NANNY_OAUTH_TOKEN"
        nanny_api: "https://nanny.yandex-team.ru/"
        nanny_current_state_url_template: "v2/services/{}/current_state/"
        nanny_snapshot_resources_url_template: "v2/services/{}/runtime_attrs/"
        nanny_active_revision_id_url_template: "v2/services/_helpers/get_active_revision_id/{}/"
        timeout: 30
      common_settings:
        chunk_size: 0
        data_list_path: Null
        update_interval_sec: 60
        rotate_snapshots_older_than_sec: 900
      arguments:
        rtc_sla_tentacles_testing_sas:
        rtc_sla_tentacles_testing_yp_lite_daemonset:
    hq:
      common_parameters:
        instances_url_prefix: "/rpc/instances/"
        hq_located_api:
          sas: "http://hq.sas-swat.yandex-team.ru/"
          msk: "http://hq.msk-swat.yandex-team.ru/"
          vla: "http://hq.vla-swat.yandex-team.ru/"
          man: "http://hq.man-swat.yandex-team.ru/"
      common_settings:
        chunk_size: 5000
        data_list_path: "values"
        update_interval_sec: 60
        rotate_snapshots_older_than_sec: 300
      arguments:
        hq_sas:
          location: "sas"
          nanny_service_name: "rtc_sla_tentacles_testing_gencfg"
          timeout: 30
        hq_man:
          location: "sas"
          nanny_service_name: "rtc_sla_tentacles_testing_yp_lite"
          timeout: 30
        hq_daemonset_man:
          location: "sas"
          nanny_service_name: "rtc_sla_tentacles_testing_yp_lite_daemonset"
          timeout: 30
    resource_maker:
      common_parameters: {}
      common_settings:
        chunk_size: 0
        data_list_path: Null
        update_interval_sec: 300
        rotate_snapshots_older_than_sec: 7200
      arguments:
        default:
          keep_copies: 10
          storage_dir: /tmp/resource_maker/
    yp_lite_switcher:
      common_parameters:
      common_settings:
        chunk_size: 0
        data_list_path: Null
        update_interval_sec: 1800
        rotate_snapshots_older_than_sec: 7200
      arguments:
        rtc_sla_tentacles_testing_gencfg:
        rtc_sla_tentacles_testing_yp_lite:
        rtc_sla_tentacles_testing_yp_lite_daemonset:
    yp_lite_pod_count_tuner:
      common_parameters:
      common_settings:
        chunk_size: 0
        data_list_path: Null
        update_interval_sec: 60
        rotate_snapshots_older_than_sec: 900
      arguments:
        rtc_sla_tentacles_testing_yp_lite_daemonset:
    yp_lite_reallocator:
      common_parameters: {}
      common_settings:
        chunk_size: 0
        data_list_path: Null
        update_interval_sec: 60
        rotate_snapshots_older_than_sec: 900
      arguments:
        rtc_sla_tentacles_testing_yp_lite:
    clickhouse_dumper:
      common_parameters:
      common_settings:
        chunk_size: 0
        data_list_path: "values"
        update_interval_sec: 60
        rotate_snapshots_older_than_sec: 180
      arguments:
        clickhouse_dumper:
    clickhouse_dropper:
      common_parameters:
      common_settings:
        chunk_size: 0
        data_list_path: Null
        update_interval_sec: 3600
        rotate_snapshots_older_than_sec: 7200
      arguments:
        clickhouse_dropper:
    clickhouse_optimizer:
      common_parameters:
      common_settings:
        chunk_size: 0
        data_list_path: Null
        update_interval_sec: 900
        rotate_snapshots_older_than_sec: 2700
      arguments:
        clickhouse_optimizer:
    yp_lite_pods_tracker:
      common_parameters:
      common_settings:
        chunk_size: 0
        data_list_path: Null
        update_interval_sec: 60
        rotate_snapshots_older_than_sec: 900
      arguments:
        rtc_sla_tentacles_testing_yp_lite_daemonset:
          yp_cluster: FAKE
          restrict_nodes_to_specified_in_podset: True


# Nanny clients settings
nanny:
  nanny_oauth_token_secret_name: "NANNY_OAUTH_TOKEN"


# YP client settings
yp:
  yp_token_secret_name: "YP_TOKEN"
  masters:
    FAKE: fake.example.com:8090
  client_config_kwargs:
    request_timeout: 42


# YP Lite pods manager settings
yp_lite_pods_manager:
  clickhouse:
    connection: "default"
    credentials: "default"
  new_pods_allocation_batch_size: 1
  pods_without_nodes_removal_batch_size: 1


# Tentacles groups settings.
tentacles_groups:

  rtc_sla_tentacles_testing_gencfg:
    location: sas
    gencfg_master_group: SAS_RUNTIME
    availability_settings:
      monitoring: {}
    reallocation_settings:
    redeployment_settings:
      resource_maker: default
      cooldown_after_redeployment_min: 1
      monitoring:
        redeployment_stalled_after_min: 15
        last_redeployment_maximum_age_min: 30

  rtc_sla_tentacles_testing_yp_lite:
    location: man
    yp_cluster: MAN
    availability_settings:
      monitoring: {}
    reallocation_settings:
      degrade_params:
        max_unavailable_pods: 1
        min_update_delay_seconds: 15
      cooldown_after_reallocation_min: 3
      monitoring:
        reallocation_stalled_after_min: 15
        last_reallocation_maximum_age_min: 30
    redeployment_settings:
      resource_maker: default
      cooldown_after_redeployment_min: 1
      update_nanny_instances_with_allocated_pods: True
      monitoring:
        redeployment_stalled_after_min: 15
        last_redeployment_maximum_age_min: 45

  rtc_sla_tentacles_testing_yp_lite_daemonset:
    location: vla
    yp_cluster: VLA
    availability_settings:
      monitoring:
        min_percent_of_available_tentacles: 42
    reallocation_settings:
    redeployment_settings:
      resource_maker: default
      cooldown_after_redeployment_min: 1
      update_nanny_instances_with_allocated_pods: True
      monitoring:
        redeployment_stalled_after_min: 15
        last_redeployment_maximum_age_min: 45
        min_percent_of_tentacles_with_fresh_resource: 42


# Incidents settings
incidents:

  incidents_collection_name: "incidents"

  assignees:
    - team_a
    - team_b
    - team_c
"""
