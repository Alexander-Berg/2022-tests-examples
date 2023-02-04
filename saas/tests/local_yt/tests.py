from test_common import run_test
from run_yt import run_yt


ytpull_test_timeout = 300


def do_test(name, params, timeout, metrics):
    run_yt()
    run_test(name, params, name, timeout, metrics=metrics)


def testTEST_MR_FETCH_SIMPLE_UNIT_C(metrics):
    test_name = 'TestMRFetchSimple'
    test_pars = '-g $CONF_PATH/cluster/cluster_1be_internal.cfg -d $TEST_DATA_PATH'
    timeout = ytpull_test_timeout
    do_test(test_name, test_pars, timeout, metrics)


def testTEST_YT_PULL_SIMPLE_UNIT_C(metrics):
    test_name = 'TestYtPullSimple'
    test_pars = '-g $CONF_PATH/cluster/cluster_1be_internal.cfg -s 1 -d $TEST_DATA_PATH'
    timeout = ytpull_test_timeout
    do_test(test_name, test_pars, timeout, metrics)


def testTEST_YT_PULL_SIMPLE_RESTART_UNIT_C(metrics):
    test_name = 'TestYtPullSimpleRestart'
    test_pars = '-g $CONF_PATH/cluster/cluster_1be_internal.cfg -s 1 -d $TEST_DATA_PATH'
    timeout = ytpull_test_timeout
    do_test(test_name, test_pars, timeout, metrics)


def testTEST_YT_PULL_TWO_CHUNKS_UNIT_C(metrics):
    test_name = 'TestYtPullTwoChunks'
    test_pars = '-g $CONF_PATH/cluster/cluster_1be_internal.cfg -s 1 -d $TEST_DATA_PATH'
    timeout = ytpull_test_timeout
    do_test(test_name, test_pars, timeout, metrics)


def testTEST_YT_PULL_TWO_CHUNKS_PAUSE_UNIT_C(metrics):
    test_name = 'TestYtPullTwoChunksPause'
    test_pars = '-g $CONF_PATH/cluster/cluster_1be_internal.cfg -s 1 -d $TEST_DATA_PATH'
    timeout = ytpull_test_timeout
    do_test(test_name, test_pars, timeout, metrics)


def testTEST_YT_PULL_SHARD_BOUNDARIES_UNIT_C(metrics):
    test_name = 'TestYtPullShardBoundaries'
    test_pars = '-g $CONF_PATH/cluster/cluster_1be_internal.cfg -s 1 -d $TEST_DATA_PATH'
    timeout = ytpull_test_timeout
    do_test(test_name, test_pars, timeout, metrics)


def testTEST_YT_PULL_BACKUP_FROM_ONE_CHUNK_UNIT_C(metrics):
    test_name = 'TestYtPullBackupFromOneChunk'
    test_pars = '-g $CONF_PATH/cluster/cluster_1be_internal.cfg -s 1 -d $TEST_DATA_PATH'
    timeout = ytpull_test_timeout
    do_test(test_name, test_pars, timeout, metrics)


def testTEST_YT_PULL_BACKUP_FROM_SCRATCH_UNIT_C(metrics):
    test_name = 'TestYtPullBackupFromScratch'
    test_pars = '-g $CONF_PATH/cluster/cluster_1be_internal.cfg -s 1 -d $TEST_DATA_PATH'
    timeout = ytpull_test_timeout
    do_test(test_name, test_pars, timeout, metrics)


def testTEST_YT_PULL_MAX_DOC_AGE_UNIT_C(metrics):
    test_name = 'TestYtPullMaxDocAge'
    test_pars = '-g $CONF_PATH/cluster/cluster_1be_internal.cfg -s 1 -d $TEST_DATA_PATH'
    timeout = ytpull_test_timeout
    do_test(test_name, test_pars, timeout, metrics)
