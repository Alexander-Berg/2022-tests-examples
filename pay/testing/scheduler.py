from paysys.sre.tools.monitorings.configs.trust.base import scheduler, utils
from paysys.sre.tools.monitorings.lib.util.helpers import empty_children

host = "trust.test.scheduler"
children = empty_children

solomon_cluster = "scheduler_test"

queue_size_thresholds = {
    "default": utils.thresholds(500, 1000),
    # BINDING_NOTIFY
    "test:bs:BINDING_NOTIFY": utils.thresholds(1500, 2000),
    "test:bs_ng:BINDING_NOTIFY": utils.thresholds(1500, 2000),
    "test:tpay_shard0:BINDING_NOTIFY": utils.thresholds(1500, 2000),
    "test:tpay_shard1:BINDING_NOTIFY": utils.thresholds(1500, 2000),
    # PAYMENT_AUTO_ACTION
    "test:bs:PAYMENT_AUTO_ACTION": utils.thresholds(50000, 100000),
    "test:bs_ng:PAYMENT_AUTO_ACTION": utils.thresholds(50000, 100000),
    "test:tpay_shard0:PAYMENT_AUTO_ACTION": utils.thresholds(50000, 100000),
    "test:tpay_shard1:PAYMENT_AUTO_ACTION": utils.thresholds(50000, 100000),
    # TRUST_RETRY
    "test:bs:TRUST_RETRY": utils.thresholds(10000, 20000),
    "test:bs_ng:TRUST_RETRY": utils.thresholds(10000, 20000),
    "test:tpay_shard0:TRUST_RETRY": utils.thresholds(10000, 20000),
    "test:tpay_shard1:TRUST_RETRY": utils.thresholds(10000, 20000),
}

expired_thresholds = {
    "default": utils.thresholds(10, 20),
}


def checks():
    return scheduler.get_checks(
        host, "trust.test", solomon_cluster, queue_size_thresholds, expired_thresholds
    )
