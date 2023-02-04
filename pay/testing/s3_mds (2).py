from paysys.sre.tools.monitorings.lib.checks.mds import s3_mds
from paysys.sre.tools.monitorings.lib.util.helpers import merge

STRUST_SERVICE_ID = 740
TRUST_MONGO_BACKUPS_ID = 33235

host = "trust.test.s3_mds"

children = []


def checks():
    return merge(
        s3_mds("testing", "strust", STRUST_SERVICE_ID),
        s3_mds("testing", "trustmongodbbackups", TRUST_MONGO_BACKUPS_ID),
    )
