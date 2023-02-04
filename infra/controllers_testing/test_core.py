import pytest

import infra.callisto.controllers.sdk as sdk
import infra.callisto.controllers.core.links as links
import infra.callisto.controllers.core.cache as cache


def test_register_on_init():
    a = sdk.Controller()
    assert set(links.all_ctrls()) == {a}
    b = sdk.Controller()
    assert set(links.all_ctrls()) == {a, b}


def test_links():
    a = sdk.Controller()
    b = sdk.Controller()
    c = sdk.Controller()
    d = sdk.Controller()
    a.register(b, c)
    b.register(d)
    assert set(a.children) == {b, c}
    assert set(b.children) == {d}


def test_topological_sort():
    a = sdk.Controller()
    b = sdk.Controller()
    d = sdk.Controller()
    a.register(b)
    b.register(d)

    assert links.topological_sort(a) == [d, b, a]

    b.register(a)  # add loop a <-> b
    with pytest.raises(AssertionError):
        links.topological_sort(a)


def test_ctrl_cache():
    class A(sdk.Controller):
        def __init__(self):
            super(A, self).__init__()
            self.value = 0

        def notifications(self):
            return [
                sdk.notify.TextNotification(str(self.value), sdk.notify.NotifyLevels.INFO)
            ]

        def json_view(self):
            if self.value < 3:
                return self.value
            else:
                raise RuntimeError()

    a = A()
    cache.init_cache([a])
    assert cache.notifications(a) == [], 'should be empty before first iteration'
    assert cache.json_view(a) is None, 'should be None before first iteration'

    a.value = 1
    cache.update_ctrl_cache(a)
    assert len(cache.notifications(a)) == 1
    assert cache.notifications(a)[0].message == '1'
    assert cache.json_view(a) == 1

    a.value = 2
    cache.update_ctrl_cache(a)
    assert len(cache.notifications(a)) == 1, 'still one notification'
    assert cache.notifications(a)[0].message == '2', 'message with text `1` should be replaced by new'
    assert cache.json_view(a) == 2

    a.value = 3
    cache.update_ctrl_cache(a)
    assert len(cache.notifications(a)) == 2, '`3` + error about evaluating json_view'
    assert cache.notifications(a)[0].message == '3' or cache.notifications(a)[1].message == '3'
    assert cache.json_view(a) == 2, 'should keep prev evaluated value'
