# -*- coding: utf-8 -*-
import yaml


class AlarmException(BaseException):
    pass

class SeveralAllowedTransitionsException(BaseException):
    pass

class aDict(dict):
    def __getattr__(self, attr):
        return self[attr]

    def __dir__(self):
        return self.keys() + dir(self.__class__)


def load_pipeline(input, _is_file=True):

    def load_yaml(text):
        try:
            return yaml.load(text)
        except yaml.YAMLError as exc:
            print exc

    if _is_file:
        with open(input) as stream:
            return load_yaml(stream)
    else:
        return load_yaml(input)