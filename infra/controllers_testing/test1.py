import infra.callisto.controllers.user.callisto.yt_observers as callisto_observers
import infra.callisto.controllers.build.task as task


def test_nightly_task_creation():
    t = callisto_observers.BuildCallistoIncrementalShard('0-123', '20171123-000321', 'abcdef', '//non-existent/yt/root', '1503327491')
    t_restored = task.Task.from_json(t.json())
    assert t.produce_config() == t_restored.produce_config()
