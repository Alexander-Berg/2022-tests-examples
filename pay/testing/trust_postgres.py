from paysys.sre.tools.monitorings.lib.checks.postgres import postgres
from paysys.sre.tools.monitorings.lib.util.helpers import merge

host = "trust.test.postgres"

children = []

tags = ["trust_postgres"]
sentry_tags = ["trust_sentry_postgres"]


def checks():
    return merge(
        postgres(
            cluster_id="9f6cfab1-d00b-4110-bce5-92140faaf51c",
            db_name="trust_payments_testing",
            users=[
                "trust_directory_app",
                "trust_export_ng_app",
                "trust_export_xg_app",
                "trust_gateway",
                "trust_notify_ng_app",
                "trust_notify_xg_app",
                "trust_payments_app",
                "trust_payments_xg_app",
                "trust_worker_ng_app",
                "trust_worker_app",
            ],
            tags=tags
        ),
        postgres(cluster_id="mdblf66idkpvbajttqc8", db_name="trust_receipts_test", tags=tags),
        postgres(
            db_name="trust_payments_testing",
            cluster_name="trust_payments_testing_shard1",
            cluster_id="mdblf66idkpvbajttqc8",
            users=[
                "trust_export",
                "trust_notify",
                "trust_payments_app",
                "trust_payments",
                "trust_utility",
                "trust_worker_app",
            ],
            tags=tags
        ),
        postgres(
            cluster_name="trust_gomer_test",
            cluster_id="mdbvq2se5iabt8q7gvh5",
            db_name="trust_gomer_test",
            conn_limit=100,
            tags=tags
        ),
        postgres(
            cluster_name="trust_metadata_test",
            cluster_id="mdb5cma14llkorqdlm67",
            db_name="trust_metadata_test",
            conn_limit=100,
            tags=tags
        ),
        postgres(
            cluster_name="trust_sentry_test",
            cluster_id="mdb0uv84tn9rii5j4g58",
            db_name="trust_sentry_test",
            conn_limit=100,
            query_time_limit=30,  # ms
            cpu_wait_limit=4.0,  # don't check them
            disk_space_warn_threshold=60,
            disk_space_crit_threshold=80,
            tags=sentry_tags,
        ),
        postgres(
            cluster_name="trust_utility_test",
            cluster_id="mdbgad4gj15spo2gbtvu",
            db_name="trust_utility_test",
            conn_limit=100,
            disk_space_warn_threshold=60,
            disk_space_crit_threshold=80,
            tags=tags,
        ),
    )
