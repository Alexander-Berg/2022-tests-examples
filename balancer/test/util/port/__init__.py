# -*- coding: utf-8 -*-


class PortException(Exception):
    pass


class Port(int):
    def __new__(cls, value, _):
        return super(Port, cls).__new__(cls, value)

    def __init__(self, value, port_manager):
        super(Port, self).__init__(value)
        if value < 0 or value > 65535:
            raise PortException('bad port value: {}'.format(value))
        self.__port_manager = port_manager

    def finish(self):
        self.__port_manager.return_port(self)


class StaticPortManager(object):
    def __init__(self, start_port):
        super(StaticPortManager, self).__init__()
        self.__current_port = start_port

    def get_port(self):
        result = self.__current_port
        self.__current_port += 1
        return Port(result, self)

    def return_port(self, _):
        pass


class YatestPortManager(object):
    def __init__(self):
        self.__max_port = 0
        from yatest.common import network
        self.__port_manager = network.PortManager()

    def get_port(self):
        result = self.__get_next_port()
        return result

    def get_port_range(self, count):
        result = self.__port_manager.get_port_range(0, count)
        return [Port(i, self) for i in range(result, result + count)]

    def return_port(self, port):
        self.__port_manager.release_port(port)

    def __get_next_port(self):
        return Port(self.__port_manager.get_port(self.__max_port), self)

    def release(self):
        self.__port_manager.release()
