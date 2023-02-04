import operator
import re


class methodcalled_checker:
    def __init__(self, name):
        self.name = name

    def __eq__(self, other):
        method_name = re.match(r'^operator\.methodcaller\(\'([\w]+)\'\)$', repr(other)).group(1)
        return isinstance(other, operator.methodcaller) and self.name == method_name


class isinstance_checker:
    def __init__(self, cls):
        self.cls = cls

    def __eq__(self, other):
        return isinstance(other, self.cls)
