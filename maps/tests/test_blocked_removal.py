import threading

from maps.pylibs.utils.lib.common import wait_until

from maps.garden.sdk.resources.python import PythonResource
from maps.garden.sdk.core import Version

from maps.garden.libs_server.resource_storage.exceptions import NotFoundInStorageError


def test_removing_status(mocker, resource_storage):
    event = threading.Event()

    counts = dict(
        check_cnt=0,
        rm_cnt_start=0,
        rm_cnt_end=0,
        not_found=0,
        rm_func_call_cnt=0
    )

    def blocked_remover():
        event.wait()
        counts["rm_func_call_cnt"] += 1

    mocked_remove = mocker.patch.object(
        PythonResource,
        "remove",
        side_effect=blocked_remover,
    )
    res = PythonResource("R1")
    res.version = Version(properties={"field": "value"})

    resource_storage.save(res)
    assert len(resource_storage) == 1

    metas, removing = resource_storage.resources_status([res.key])
    assert len(metas) == 1
    assert len(removing) == 0
    assert res.key in metas

    lock = threading.Lock()

    def do_check(key):
        with lock:
            counts["check_cnt"] += 1

        return True

    def do_remove():
        with lock:
            counts["rm_cnt_start"] += 1

        try:
            resource_storage.remove_by_key(res.key, do_check)
        except NotFoundInStorageError:
            with lock:
                counts["not_found"] += 1

        with lock:
            counts["rm_cnt_end"] += 1

    t1 = threading.Thread(target=do_remove, daemon=True)
    t2 = threading.Thread(target=do_remove, daemon=True)
    t1.start()
    t2.start()

    assert wait_until(lambda: counts["rm_cnt_start"] == 2)
    assert wait_until(lambda: counts["check_cnt"] == 1)
    assert wait_until(lambda: mocked_remove.call_count == 1)

    assert counts["check_cnt"] == 1
    assert counts["rm_cnt_start"] == 2
    assert counts["rm_cnt_end"] == 0
    assert counts["not_found"] == 0

    metas, removing = resource_storage.resources_status([res.key])
    assert len(metas) == 0
    assert len(removing) == 1
    assert removing.pop() == res.key

    event.set()

    assert wait_until(lambda: counts["rm_cnt_end"] == 2)

    assert counts["rm_func_call_cnt"] == 1
    assert counts["check_cnt"] == 2
    assert counts["rm_cnt_start"] == 2
    assert counts["rm_cnt_end"] == 2
    assert counts["not_found"] == 1
    assert len(resource_storage) == 0

    t1.join()
    t2.join()
