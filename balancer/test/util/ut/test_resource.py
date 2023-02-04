# -*- coding: utf-8 -*-
import pytest
import balancer.test.util.resource as resource


class ConcreteError(RuntimeError):
    pass


class ConcreteResource(resource.AbstractResource):
    def __init__(self, throw_in_finish=False):
        super(ConcreteResource, self).__init__()
        self.finish_call_count = 0
        self.__throw_in_finish = throw_in_finish

    def _finish(self):
        self.finish_call_count += 1
        if self.__throw_in_finish:
            raise ConcreteError('just checking')


def test_resource_finish():
    res = ConcreteResource(throw_in_finish=False)
    assert res.finish_call_count == 0
    for i in xrange(3):
        res.finish()
        assert res.finish_call_count == 1


def test_resource_finish_throw():
    res = ConcreteResource(throw_in_finish=True)
    assert res.finish_call_count == 0

    with pytest.raises(ConcreteError):
        res.finish()
    assert res.finish_call_count == 1

    for i in xrange(3):
        res.finish()
        assert res.finish_call_count == 1


@pytest.mark.parametrize('throw_in_finish', [False, True], ids=['throw', 'nothrow'])
def test_resource_set_finished(throw_in_finish):
    res = ConcreteResource(throw_in_finish=throw_in_finish)
    assert res.finish_call_count == 0
    res.set_finished()
    for i in xrange(3):
        res.finish()
        assert res.finish_call_count == 0
