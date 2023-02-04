from staff.lib.tasks import LockedTask


def test_locked_task_passes_all_args_to_get_context():
    class TestLockedTask(LockedTask):
        some_positional_arg = None
        some_keyword_arg = None

        def locked_run(self, *args, **kwargs):
            pass

        def get_log_context(self, some_positional_arg, some_keyword_arg=None, **args):
            TestLockedTask.some_positional_arg = some_positional_arg
            TestLockedTask.some_keyword_arg = some_keyword_arg
            return {}

    task = TestLockedTask()
    task.apply(args=['test_positional_arg'], kwargs={'some_keyword_arg': 'test_keyword_arg'})

    assert task.some_positional_arg == 'test_positional_arg'
    assert task.some_keyword_arg == 'test_keyword_arg'
