# -*- coding: utf-8 -*-
from abc import ABCMeta, abstractproperty


class WrongDataTypeException(Exception):
    pass


class AbstractData(object):
    __metaclass__ = ABCMeta

    def __init__(self):
        super(AbstractData, self).__init__()
        self.__raw = None

    @property
    def raw(self):
        """
        Full message body.

        :rtype: str
        """
        return self.__raw

    def _set_raw(self, value):
        self.__raw = value

    @abstractproperty
    def content(self):
        """
        Message content (not full body). If data is chunked, chunks will be joined.

        :rtype: str
        """
        raise NotImplementedError()


class EmptyData(AbstractData):
    def __new__(cls):
        if not hasattr(cls, '_instance'):
            cls._instance = super(EmptyData, cls).__new__(cls)
        return cls._instance

    @property
    def content(self):
        return ''

    def __repr__(self):
        return ''


class PlainData(AbstractData):
    def __init__(self, data):
        if not isinstance(data, str):
            raise WrongDataTypeException('expected raw string')
        super(PlainData, self).__init__()
        self.__data = data

    @property
    def content(self):
        return self.__data

    def __repr__(self):
        return self.__data


class Chunk(object):
    def __init__(self, length, data):
        super(Chunk, self).__init__()
        self.__length = length
        self.__data = data

    def __len__(self):
        return self.length

    def __str__(self):
        return self.data

    def __repr__(self):
        return '%X\r\n%s\r\n' % (self.__length, self.__data)

    @property
    def length(self):
        return self.__length

    @property
    def data(self):
        return self.__data


class ChunkedData(AbstractData):
    def __init__(self, chunks):
        if not isinstance(chunks, list):
            raise WrongDataTypeException('expected list of chunks')
        super(ChunkedData, self).__init__()
        self.__chunks = [self.__build_chunk(chunk) for chunk in chunks]

    @staticmethod
    def __build_chunk(data):
        if isinstance(data, Chunk):
            return data
        else:
            return Chunk(len(data), data)

    @property
    def chunks(self):
        return self.__chunks

    @property
    def content(self):
        return ''.join([chunk.data for chunk in self.__chunks])

    def __repr__(self):
        return ''.join([repr(chunk) for chunk in self.__chunks])
