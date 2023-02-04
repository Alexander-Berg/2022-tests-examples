# -*- coding: utf-8 -*-
class NameException(Exception):
    pass


class Named(object):
    def __init__(self):
        super(Named, self).__init__()
        self.__name = None

    @property
    def name(self):
        if self.__name is None:
            raise NameException('name has not been set')
        return self.__name

    @name.setter
    def name(self, value):
        if self.__name is not None:
            raise NameException('name has already been set to "{}"'.format(self.__name))
        self.__name = value


class NameResolverMetaClass(type):
    def __new__(cls, class_name, bases, class_dict):
        for name, attr in class_dict.iteritems():
            if isinstance(attr, Named):
                attr.name = name
        return super(NameResolverMetaClass, cls).__new__(cls, class_name, bases, class_dict)
