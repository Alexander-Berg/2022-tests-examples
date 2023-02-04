from paysys.sre.tools.monitorings.configs.trust.base import trust_mongo
from paysys.sre.tools.monitorings.lib.util.helpers import merge

host = "trust.test.mongo"
children = ["trust-mongo-test"]

split_by_dc = True


def checks():
    return merge(
        trust_mongo.get_checks(
            children, "trust-test", 3, host=host, split_by_dc=split_by_dc
        ),
        trust_mongo.spinlock_delay(host, 27101, "mongo_test", "trust.test", 180, 300),
    )
