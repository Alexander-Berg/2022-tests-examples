# -*- coding: utf-8 -*-
class Options(object):
    def __init__(self, obj, default_parent=None):
        super(Options, self).__init__()
        self.__obj = obj
        parent_obj = self.__get_obj_attr('parent_options', default_parent)
        if parent_obj is not None:
            self.__parent = Options(parent_obj)
        else:
            self.__parent = None

    def __get_obj_attr(self, name, default=None):
        if isinstance(self.__obj, dict):
            return self.__obj.get(name, default)
        else:
            return getattr(self.__obj, name, default)

    def get(self, name, default=None):
        result = self.__get_obj_attr(name)
        if result is None and self.__parent is not None:
            result = self.__parent.get(name, default)
        if result is None:
            result = default
        return result

    def __getitem__(self, name):
        result = self.get(name)
        if result is None:
            raise KeyError(name)
        return result

    def __getattr__(self, name):
        result = self.get(name)
        if result is None:
            raise AttributeError(name)
        return result
