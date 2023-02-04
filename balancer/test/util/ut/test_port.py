# -*- coding: utf-8 -*-
import pytest
import balancer.test.util.port as p


@pytest.mark.parametrize('value', [0, 1, 111, 65535])
def test_port(value):
    manager = None
    port = p.Port(value, manager)
    assert port == value
    assert isinstance(port, int)


@pytest.mark.parametrize('value', [-1, 65536, 100500])
def test_bad_port_value(value):
    manager = None
    with pytest.raises(p.PortException):
        p.Port(value, manager)


def test_return_port():
    class Manager(object):
        def __init__(self):
            super(Manager, self).__init__()
            self.returned_port = None

        def return_port(self, port):
            self.returned_port = port

    manager = Manager()
    value = 8080
    port = p.Port(value, manager)
    port.finish()
    assert manager.returned_port == value


def test_static_port_manager():
    start_port = 8080
    port_manager = p.StaticPortManager(start_port)

    for i in xrange(10):
        port = port_manager.get_port()
        assert port == start_port + i

    port.finish()
    next_port = port_manager.get_port()
    assert next_port == port + 1


@pytest.yield_fixture(scope='module')
def yatest_port_manager():
    port_manager = p.YatestPortManager()
    yield port_manager
    port_manager.release()


def test_yatest_port_manager(yatest_port_manager):
    ports = yatest_port_manager.get_port_range(3)

    ports[0].finish()
    next_port = ports[1]
    assert next_port == ports[0] + 1
