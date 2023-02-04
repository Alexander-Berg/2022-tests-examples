# -*- coding: utf-8 -*-
from abc import ABCMeta, abstractmethod

from balancer.test.util import sync


class AbstractState(object):
    def __init__(self):
        super(AbstractState, self).__init__()
        self.__accepted = sync.Counter(0)
        self.__conn_addrs = sync.Queue(10)

    @property
    def accepted(self):
        """Counter of incoming connections

        :rtype: Counter
        """
        return self.__accepted

    @property
    def conn_addrs(self):
        return self.__conn_addrs

    def finish(self):
        self.__conn_addrs.finish()


class AbstractServerHandler(object):
    __metaclass__ = ABCMeta

    @abstractmethod
    def handle(self):
        raise NotImplementedError()

    def finish(self):
        pass


class AbstractServerHandlerFactory(object):
    __metaclass__ = ABCMeta

    def __init__(self, state):
        super(AbstractServerHandlerFactory, self).__init__()
        self.__state = state

    def __call__(self, sock, *_):
        handler = self.create_handler(sock)
        try:
            handler.handle()
        finally:
            handler.finish()

    @abstractmethod
    def create_handler(self, sock):
        raise NotImplementedError()

    @property
    def state(self):
        return self.__state


class RequestInfo(object):
    def __init__(self, raw_request, start_time, fin_time):
        super(RequestInfo, self).__init__()
        self.__raw_request = raw_request
        self.__request = raw_request.to_request()
        self.__start_time = start_time
        self.__fin_time = fin_time

    @property
    def request(self):
        """
        :returns: request object
        :rtype: HTTPRequest
        """
        return self.__request

    @property
    def raw_request(self):
        """
        :returns: raw request object
        :rtype: RawHTTPRequest
        """
        return self.__raw_request

    @property
    def start_time(self):
        """
        :returns: request start time
        :rtype: datetime
        """
        return self.__start_time

    @property
    def fin_time(self):
        """
        :returns: request finish time
        :rtype: datetime
        """
        return self.__fin_time

    @property
    def duration(self):
        """
        :returns: request duration
        :rtype: timedelta
        """
        return self.fin_time - self.start_time


class StreamInfo(object):
    def __init__(self, start_time, fin_time):
        super(StreamInfo, self).__init__()
        self.__start_time = start_time
        self.__fin_time = fin_time

    @property
    def start_time(self):
        """
        :returns: handle start time
        :rtype: datetime
        """
        return self.__start_time

    @property
    def fin_time(self):
        """
        :returns: handle finish time
        :rtype: datetime
        """
        return self.__fin_time

    @property
    def duration(self):
        """
        :returns: handle duration
        :rtype: timedelta
        """
        return self.fin_time - self.start_time


class State(AbstractState):
    QUEUE_TIMEOUT = 10

    def __init__(self, config):
        super(State, self).__init__()
        self.__config = config
        self.__requests = sync.Queue(config.queue_timeout)
        self.__streams = sync.Queue(config.queue_timeout)
        self.__read_errors = sync.Queue(config.queue_timeout)

    def get_request(self):
        """
        :returns: next request from requests queue
        :rtype: HTTPRequest
        """
        return self.requests.get().request

    def get_raw_request(self):
        """
        :returns: next raw request from requests queue
        :rtype: RawHTTPRequest
        """
        return self.requests.get().raw_request

    @property
    def requests(self):
        """Requests, received by server

        :returns: queue of :class:`.RequestInfo` objects
        :rtype: Queue
        """
        return self.__requests

    @property
    def streams(self):
        """
        Streams, handled by server

        :returns: queue if :class:`.StreamInfo` objects
        :rtype: Queue
        """
        return self.__streams

    @property
    def read_errors(self):
        """Errors, happened while reading requests

        :returns: queue of :class:`.HTTPReadError`
        :rtype: Queue
        """
        return self.__read_errors

    @property
    def config(self):
        """
        Backend config
        """
        return self.__config

    def finish(self):
        self.__requests.finish()
        self.__streams.finish()
        self.__read_errors.finish()

        super(State, self).finish()


class HandlerConfigException(Exception):
    pass


class ConfigServerHandler(AbstractServerHandler):
    __metaclass__ = ABCMeta

    def __init__(self, state, sock, config):
        super(ConfigServerHandler, self).__init__()
        self.__state = state
        # FIXME socket timeout is None
        self.__sock = sock
        self.__sock.set_timeout(None)
        self.__config = config

    @abstractmethod
    def handle(self):
        raise NotImplementedError()

    @property
    def sock(self):
        """
        :rtype: SocketStream
        """
        return self.__sock

    @property
    def state(self):
        """Server shared state"""
        return self.__state

    @property
    def config(self):
        """Handler config"""
        return self.__config


class Config(object):
    HANDLER_TYPE = None
    STATE_TYPE = State

    def __init__(self, queue_timeout=State.QUEUE_TIMEOUT):
        super(Config, self).__init__()
        self.queue_timeout = queue_timeout


class ConfigServerHandlerFactory(AbstractServerHandlerFactory):
    def __init__(self, state, config):
        super(ConfigServerHandlerFactory, self).__init__(state)
        self.__config = config
        self.__handler_type = config.HANDLER_TYPE

    def create_handler(self, sock):
        return self.__handler_type(self.state, sock, self.__config)

    @property
    def name(self):
        return '%s_%s' % (self.__class__.__name__, self.__config.__class__.__name__)
