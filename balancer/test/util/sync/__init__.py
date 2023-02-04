# -*- coding: utf-8 -*-
import os
import sys
import ctypes
import contextlib
import time

from Queue import Empty, Full
from collections import deque
import multiprocessing as m
import threading
from balancer.test.util.stdlib import named
from balancer.test.util.stdlib.multirun import Multirun

from multiprocessing import managers
from multiprocessing.queues import Queue as mpq


class _InnerQueue(deque):
    def qsize(self):
        return len(self)

    def empty(self):
        return len(self) == 0

    def full(self):
        return len(self) == self.maxlen

    def put(self, obj, *_):
        if self.full():
            raise Full()
        self.append(obj)

    def get(self, *_):
        if self.empty():
            raise Empty()
        return self.popleft()


# Workaround for qsize for OS X
class QueueWrapper(mpq):
    def __init__(self, *args, **kwargs):
        super(QueueWrapper, self).__init__(*args, **kwargs)
        self.size = Counter(0)

    def put(self, *args, **kwargs):
        self.size.inc()
        super(QueueWrapper, self).put(*args, **kwargs)

    def get(self, *args, **kwargs):
        self.size.dec()
        return super(QueueWrapper, self).get(*args, **kwargs)

    def qsize(self):
        return self.size.value

    def empty(self):
        return not self.qsize()

    def clear(self):
        while not self.empty():
            self.get()


class Queue(object):
    def __init__(self, timeout, maxsize=0):
        super(Queue, self).__init__()
        self.__timeout = timeout
        if sys.platform == "darwin":
            self.__sync_queue = QueueWrapper(maxsize=maxsize)
        else:
            self.__sync_queue = m.Queue(maxsize=maxsize)
        self.__is_closed = False
        if maxsize <= 0:
            maxlen = None
        else:
            maxlen = maxsize
        self.__buffer_queue = _InnerQueue(maxlen=maxlen)

    def qsize(self):
        return self.__queue.qsize()

    def empty(self):
        if not self.__is_closed:  # __sync_queue.empty() can return invalid state BALANCER-1400
            return self.__sync_queue.qsize() == 0
        else:
            return self.__buffer_queue.empty()

    def full(self):
        return self.__queue.full()

    def put(self, obj, block=True, timeout=None):
        if timeout is None:
            timeout = self.__timeout
        return self.__queue.put(obj, block, timeout)

    def get(self, block=True, timeout=None):
        if timeout is None:
            timeout = self.__timeout
        return self.__queue.get(block, timeout)

    def close(self):
        if not self.__is_closed:
            self.__sync_queue.close()
            self.__is_closed = True

    def finish(self):
        if not self.__is_closed:
            while self.__sync_queue.qsize() > 0:
                obj = self.__sync_queue.get(block=True, timeout=self.__timeout)
                self.__buffer_queue.append(obj)
            self.close()

    @property
    def __queue(self):
        if not self.__is_closed:
            return self.__sync_queue
        else:
            return self.__buffer_queue


class Counter(object):
    def __init__(self, default=0):
        super(Counter, self).__init__()
        self.__default = default
        self.__counter = m.Value(ctypes.c_int, default)

    def inc(self):
        with self.__counter.get_lock():
            result = self.__counter.value
            self.__counter.value += 1
            return result

    def dec(self):
        with self.__counter.get_lock():
            result = self.__counter.value
            self.__counter.value -= 1
            return result

    @property
    def value(self):
        return self.__counter.value

    def reset(self):
        self.__counter.value = self.__default


class SharedValue(named.Named):
    __PROXY_TYPES = {
        managers.Value: managers.ValueProxy,
        threading.Lock().__class__: managers.AcquirerProxy,
        list: managers.ListProxy,
    }

    def __init__(self, value_func, proxy_type=None):
        super(SharedValue, self).__init__()
        self.__value_func = value_func
        # A dirty hack. Maybe there is a better way to determine proxytype
        if proxy_type is None:
            value_type = self.get_value().__class__
            proxy_type = self.__PROXY_TYPES[value_type]
        self.__proxy_type = proxy_type

    def get_value(self):
        return self.__value_func()

    @property
    def proxy_type(self):
        return self.__proxy_type


class SharedStateMetaClass(named.NameResolverMetaClass):
    def __init__(cls, class_name, bases, class_dict):
        for name, attr in class_dict.iteritems():
            if isinstance(attr, SharedValue):
                cls.add_shared_value(attr)
        return super(SharedStateMetaClass, cls).__init__(class_name, bases, class_dict)


@contextlib.contextmanager
def run_in_dir(dir_name):
    cwd = os.getcwd()
    os.chdir(dir_name)
    yield
    os.chdir(cwd)


def _gen_chdir_proxytype(base_proxy, addr_dir):
    class _ChdirProxy(base_proxy):
        def _callmethod(self, methodname, args=None, kwds=None):
            if args is None:
                args = ()
            if kwds is None:
                kwds = {}
            with run_in_dir(addr_dir):
                return super(_ChdirProxy, self)._callmethod(methodname, args, kwds)

    return _ChdirProxy


class _ChdirManager(m.managers.BaseManager):
    def __init__(self, address, authkey):
        self.__unix_socket = isinstance(address, str)
        if self.__unix_socket:
            addr_dir, addr_file = os.path.split(os.path.abspath(address))
            self.__addr_dir = addr_dir
            address = addr_file
        super(_ChdirManager, self).__init__(address, authkey)

    @property
    def address(self):
        address = super(_ChdirManager, self).address
        if self.__unix_socket:
            return os.path.join(self.__addr_dir, address)
        else:
            return address

    @contextlib.contextmanager
    def __run_in_dir_unix(self):
        if self.__unix_socket:
            with run_in_dir(self.__addr_dir):
                yield
        else:
            yield

    def start(self):
        with self.__run_in_dir_unix():
            super(_ChdirManager, self).start()

    def connect(self):
        with self.__run_in_dir_unix():
            super(_ChdirManager, self).connect()

    def shutdown(self):
        with self.__run_in_dir_unix():
            super(_ChdirManager, self).shutdown()

    def finish(self):
        self.shutdown()

    def register(self, name, func=None, proxytype=None):
        proxytype = _gen_chdir_proxytype(proxytype, self.__addr_dir)
        super(_ChdirManager, self).register(name, func, proxytype=proxytype)
        inner = getattr(self, name)

        def wrapper():
            with run_in_dir(self.__addr_dir):
                return inner()
        setattr(self, name, wrapper)


class SharedState(object):
    __metaclass__ = SharedStateMetaClass

    __SHARED_VALUES = list()

    AUTHKEY = 'Scorpions'

    @classmethod
    def add_shared_value(cls, value):
        cls.__SHARED_VALUES.append(value)

    @staticmethod
    def __server_register(server_class, name, value, proxy_type):
        server_class.register(name, lambda: value, proxytype=proxy_type)

    @classmethod
    def create_server(cls, address, authkey=None):
        if authkey is None:
            authkey = cls.AUTHKEY

        class SharedStateManager(_ChdirManager):
            pass

        server = SharedStateManager(address=address, authkey=authkey)

        for value in cls.__SHARED_VALUES:
            cls.__server_register(server, value.name, value.get_value(), value.proxy_type)

        return server

    @staticmethod
    def __client_register(server_class, name, proxy_type):
        server_class.register(name, proxytype=proxy_type)

    @classmethod
    def create_client(cls, address, authkey=None):
        if authkey is None:
            authkey = cls.AUTHKEY

        class SharedStateManager(_ChdirManager):
            pass

        client = SharedStateManager(address=address, authkey=authkey)

        for value in cls.__SHARED_VALUES:
            cls.__client_register(client, value.name, value.proxy_type)

        return client


class SharedManager(object):
    def __init__(self, resource_manager, fs_manager):
        super(SharedManager, self).__init__()
        self.__fs_manager = fs_manager
        self.__resource_manager = resource_manager

    def start(self, manager_class):
        # using relative path because max path length for AF_UNIX is 108
        # for more details see linux/un.h
        abs_path = self.__fs_manager.create_file('shared_unix_socket')
        self.__fs_manager.remove(abs_path)
        address = os.path.relpath(abs_path)
        manager = manager_class.create_server(address=address)
        self.__resource_manager.register(manager)
        manager.start()
        return manager


class NoRereadError(Exception):
    """Exception raised if no reread happens.
    """

    def __init__(self, message):
        self.message = message


class RereadsWatcher(object):
    def __init__(self, ctx, filepath):
        self.ctx = ctx
        self.filepath = filepath
        self.prev_count_of_reads = 0

    def __count_of_reads(self):
        result = self.ctx.call_json_event('dump_shared_files')

        count = None
        for worker_result in result:
            for fname, curr_id in worker_result[0]['file-readers'].iteritems():
                if fname == self.filepath and (count is None or count > curr_id):
                    count = curr_id

        return count

    def wait_reread(self):
        curr = 0
        success = False
        for run in Multirun():
            curr = self.__count_of_reads()
            if curr is not None and self.prev_count_of_reads < curr:
                success = True
                break
        if success:
            self.prev_count_of_reads = curr
        else:
            raise NoRereadError("no reread happened when it's expected")


class CheckersWatcher(object):
    def __init__(self, ctx, filepath):
        self.ctx = ctx
        self.filepath = filepath

    def __is_all_workers_think_that_file_exists(self):
        result = self.ctx.call_json_event('dump_shared_files')

        count = 0
        workers = 0
        for worker_result in result:
            for fname, exists in worker_result[0]['file-checkers'].iteritems():
                if fname == self.filepath:
                    workers += 1
                    if exists:
                        count += 1

        if count == 0:
            return False
        elif count == workers:
            return True
        else:
            return None

    def wait_checker(self, is_exists):
        curr = None
        while curr != is_exists:
            curr = self.__is_all_workers_think_that_file_exists()
            time.sleep(0.5)
