import sys
import inspect

import hamcrest
import yt.wrapper
from types import ModuleType

# populate sys.modules for test_exc_construction
# noinspection PyUnresolvedReferences
from balance import mapper
from balance import exc
from balance.completions_fetcher.configurable_partner_completion import ResourceUnreachableError


class AutoInspectObject(object):
    """
    >>> p = AutoInspectObject('inspect_object')
    >>> p
    AutoInspectObject('''inspect_object''')
    >>> print p.attibute
    inspect_object.attibute
    >>> print p[0]
    inspect_object[0]
    >>> p.id
    AutoInspectObject('''inspect_object.id''')
    >>> p[10](None, xxx=23).id()
    AutoInspectObject('''inspect_object[10](None, **{'xxx': 23}).id(**{})''')
    >>> print list(p)
    []
    >>> int(p)
    -2091605468
    """
    def __init__(self, name = 'root'):
        self.name = name

    def __getattr__(self, attr):
        return self.__class__('%s.%s'%(self.name, attr))

    def __str__(self):
        return '%s'%self.name

    def __repr__(self):
        return "%s('''%s''')"%(self.__class__.__name__, self.name)

    def __iter__(self):
        return iter([])

    def __getitem__(self, item):
        return self.__class__('%s[%r]'%(self.name, item))

    def __call__(self, *a, **kw):
        return self.__class__('%s(%s**%r)'%(self.name, ''.join(repr(itm) + ', ' for itm in a), kw))

    def __int__(self):
        return hash(self.name)


def test_exc_construction():
    parsed_modules = set()
    exception_classes = set()

    def fetch_exceptions(mod):
        if not isinstance(mod, ModuleType) or mod in parsed_modules:
            return

        parsed_modules.add(mod)

        for m in mod.__dict__.values():
            fetch_exceptions(m)

            if isinstance(m, type) and issubclass(m, exc.EXCEPTION):
                exception_classes.add(m)

    for mod_ in sys.modules.values():
        fetch_exceptions(mod_)

    for exception_class in exception_classes:
        args = [
            AutoInspectObject(str(i))
            for i in range(len(inspect.getargspec(exception_class.__init__)[0]) - 1)
        ]
        exception_class(*args)


def test_readable_resource_unreachable_error():
    """
    BALANCE-31926
    """
    exc = ResourceUnreachableError("no-such-table", None)
    hamcrest.assert_that(str(exc), hamcrest.starts_with("Unable to read table"))
    hamcrest.assert_that(str(exc), hamcrest.contains_string("no-such-table"))

    exc = ResourceUnreachableError("no-such-table", ZeroDivisionError())
    hamcrest.assert_that(str(exc), hamcrest.starts_with("Unable to read table"))
    hamcrest.assert_that(str(exc), hamcrest.contains_string("no-such-table"))

    ytexc = yt.wrapper.YtHttpResponseError({"message": "uh oh", "attributes": {"user": "john"}}, None, None, None)
    exc = ResourceUnreachableError("no-such-table", ytexc)
    hamcrest.assert_that(str(exc), hamcrest.starts_with("Unable to read table"))
    hamcrest.assert_that(str(exc), hamcrest.contains_string("no-such-table"))
    hamcrest.assert_that(str(exc), hamcrest.contains_string("uh oh"))
    hamcrest.assert_that(str(exc), hamcrest.contains_string("john"))
