
def test_handle_timeout(run_bg_task):
    run_bg_task('handle_timedout')


def test_cleanup(run_bg_task):
    run_bg_task('cleanup')


def test_stats(run_bg_task):
    run_bg_task('send_stats')
