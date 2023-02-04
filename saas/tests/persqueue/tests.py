from test_common import run_test
import logbroker


def do_test(name, params, timeout, metrics, topic_count):
    with logbroker.TestManager(topic_count=topic_count):
        run_test(name, params, name, timeout, metrics=metrics)


def testTEST_PERSQUEUE_SIMPLE_UNIT_C(metrics):
    test_name = 'TestPersQueueSimple'
    test_pars = '-g $CONF_PATH/cluster/cluster_pq_internal.cfg -k off -d $TEST_DATA_PATH'
    timeout = 240
    topic_count = 2
    do_test(test_name, test_pars, timeout, metrics, topic_count)
