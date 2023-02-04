# -*- coding: utf-8 -*-
import collections
from abc import ABCMeta, abstractmethod
from balancer.test.util.stdlib.doc_inherit import doc_inherit


class WrongHeadersTypeException(Exception):
    pass


class AbstractHeaders(object):
    __metaclass__ = ABCMeta

    @abstractmethod
    def get_names(self):
        """Get all header names
        :rtype: list of str
        """
        raise NotImplementedError()

    @abstractmethod
    def get_all(self, name):
        """Get all values of specified header

        :param str name: header name

        :rtype: list of str
        """
        raise NotImplementedError()

    def __getitem__(self, key):
        """Alias for get_all method
        """
        return self.get_all(key)

    @abstractmethod
    def __contains__(self, item):
        raise NotImplementedError()

    def get_one(self, name, default=None):
        """
        Get value of specified header.
        If there is more than one value, then exception is raised.

        :param str name: header name

        :return: header value or default
        """
        values = self.get_all(name)
        if values:
            assert len(values) == 1, 'Header "%s" appears more than once (%s)' % (name, str(values))
            return values[0]
        else:
            return default


def iter_headers(headers):
    if headers is None:
        return []
    elif isinstance(headers, dict):
        return headers.iteritems()
    elif isinstance(headers, collections.Iterable):
        return headers
    else:
        raise WrongHeadersTypeException(
            'headers must be an iterable of pairs (name, value), not {}'.format(type(headers))
        )


class Headers(AbstractHeaders):
    def __init__(self, headers):
        super(Headers, self).__init__()
        self.__headers = dict()
        for name, value in iter_headers(headers):
            name = name.lower()
            if name not in self.__headers:
                self.__headers[name] = list()
            if isinstance(value, collections.Iterable) and not isinstance(value, basestring):
                self.__headers[name].extend(list(value))
            else:
                self.__headers[name].append(value)

    def get_names(self):
        return self.__headers.keys()

    def __contains__(self, item):
        return item.lower() in self.__headers

    @doc_inherit
    def get_all(self, name):
        name = name.lower()
        if name in self:
            return self.__headers[name]
        else:
            return list()

    def __eq__(self, other):
        return \
            isinstance(other, Headers) and \
            self.__headers == other.__headers  # pylint: disable=protected-access

    def __ne__(self, other):
        return not self == other

    def __repr__(self):
        result = list()
        for name in self.get_names():
            for value in self.get_all(name):
                result.append('%s: %s\r\n' % (name, value))
        return ''.join(result)


class HeaderField(object):
    def __init__(self, name, value):
        super(HeaderField, self).__init__()
        self._name = name
        self._value = value

    @property
    def name(self):
        return self._name

    @property
    def value(self):
        return self._value

    def __repr__(self):
        return '{}: {}'.format(self.name, self.value)


class RawHeaders(AbstractHeaders):
    def __init__(self, headers):
        super(RawHeaders, self).__init__()
        self.__headers = list()
        self.__names = list()
        self.__filled = True
        for field in iter_headers(headers):
            if not isinstance(field, HeaderField):
                field = HeaderField(field[0], field[1])
            if field.name is not None:
                low_name = field.name.lower()
                if low_name not in self.__names:
                    self.__names.append(low_name)
            else:
                self.__filled = False
            self.__headers.append(field)

    @property
    def headers(self):
        return self.__headers

    def get_fields(self, name):
        result = list()
        for field in self.__headers:
            if field.name == name:
                result.append(field)
        return result

    def get_one_field(self, name):
        result = self.get_fields(name)
        assert len(result) == 1
        return result[0]

    def get_names(self):
        return self.__names

    def __contains__(self, item):
        return item.lower() in self.__names

    def items(self):
        return [(field.name, field.value) for field in self.__headers]

    # TODO: raise exceptions if not filled
    @doc_inherit
    def get_all(self, name):
        name = name.lower()
        result = list()
        for field in self.__headers:
            if name == field.name.lower():
                result.append(field.value)
        return result

    def __repr__(self):
        return ''.join(['%s: %s\r\n' % (name, value) for name, value in self.items()])
