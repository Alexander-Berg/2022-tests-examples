import urllib.parse


class HasArg(object):
    def __init__(self, arg, value):
        self._arg = arg
        self._value = value

    def __eq__(self, rhs):
        (_, url) = rhs.split(' ', 1)
        parsed_url = urllib.parse.urlsplit(url)
        params = urllib.parse.parse_qsl(parsed_url.query)
        return (self._arg, self._value) in params

    def __repr__(self):
        return '...&{self._arg}={self._value}&...'.format(**locals())


class HasNoArg(object):
    def __init__(self, arg):
        self._arg = arg

    def __eq__(self, rhs):
        print(rhs)
        (_, url) = rhs.split(' ', 1)
        parsed_url = urllib.parse.urlsplit(url)
        params = dict(urllib.parse.parse_qsl(parsed_url.query))
        return self._arg not in params

    def __repr__(self):
        return 'Arg "{self._arg}" is missing'.format(**locals())


class UrlIs(object):
    def __init__(self, url):
        self._url = urllib.parse.urlsplit(url)
        self._params = frozenset(urllib.parse.parse_qsl(self._url.query))

    def __eq__(self, rhs):
        (_, url) = rhs.split(' ', 1)
        parsed_url = urllib.parse.urlsplit(url)
        params = frozenset(urllib.parse.parse_qsl(parsed_url.query))
        return self._url.path == parsed_url.path and self._params == params

    def __repr__(self):
        return '{}'.format(self._url)


class UrlPathIs(object):
    def __init__(self, url_path):
        self._url_path = url_path

    def __eq__(self, rhs):
        (_, url) = rhs.split(' ', 1)
        parsed_url = urllib.parse.urlsplit(url)
        return self._url_path == parsed_url.path

    def __repr__(self):
        return self._url_path


class MethodIs(object):
    def __init__(self, method):
        self._method = method

    def __eq__(self, rhs):
        (method, _) = rhs.split(' ', 1)
        return self._method == method

    def __repr__(self):
        return self._method


class And(object):
    def __init__(self, *args):
        self._args = args

    def __eq__(self, rhs):
        for arg in self._args:
            if not arg == rhs:
                return False
        return True

    def __repr__(self):
        return ' and '.join(['{}'.format(arg) for arg in self._args])


class Not(object):
    def __init__(self, value):
        self._value = value

    def __eq__(self, rhs):
        return not (self._value == rhs)

    def __repr__(self):
        return 'not {self._value}'.format(**locals())


class Anything(object):
    def __eq__(self, rhs):
        return True

    def __repr__(self):
        return '<anything>'


class ContainsKeyValue(object):
    def __init__(self, key, value):
        self._key = key
        self._value = value

    def __eq__(self, rhs):
        return (self._key, self._value) in rhs.items()

    def __repr__(self):
        return '...{self._key}: {self._value}...'.format(**locals())


class ContainsKey(object):
    def __init__(self, key):
        self._key = key

    def __eq__(self, rhs):
        return self._key in rhs

    def __repr__(self):
        return '...{self._key}: *...'.format(**locals())


class HasDummyServiceTicket(ContainsKeyValue):
    SERVICE_TICKET_HEADER = 'x-ya-service-ticket'

    def __init__(self, service):
        super(HasDummyServiceTicket, self).__init__(
            HasDummyServiceTicket.SERVICE_TICKET_HEADER,
            'dummy-ticket-' + service)


class HasDummyUserTicket(ContainsKeyValue):
    USER_TICKET_HEADER = 'x-ya-user-ticket'

    def __init__(self, authorization):
        super(HasDummyUserTicket, self).__init__(
            HasDummyUserTicket.USER_TICKET_HEADER,
            'dummy-ticket-' + authorization)
