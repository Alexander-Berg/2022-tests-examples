from bcl.toolbox.tasks import task, get_registered_tasks, RegisteredTask


def test_basic():

    @task(hour='3-18', minute=-10, weekday='1-4')
    def testtask(**task_kwargs):
        a = 1
        return 100

    target_task = get_registered_tasks()['testtask']  # type: RegisteredTask

    assert target_task.func() == 100
    assert target_task.func_wrapper
    testtask()
