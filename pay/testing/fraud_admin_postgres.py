from paysys.sre.tools.monitorings.lib.checks.postgres import postgres

host = "trust.test.fraud-admin.postgres"

children = []


def checks():
    return postgres(
        cluster_id='285fe0dd-a5af-42f9-bee7-d25155aac224',
        db_name='fraud_admin_test', tags=["trust_postgres"]
    )
