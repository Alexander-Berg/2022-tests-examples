# -*- coding: utf-8 -*-


class FopException(Exception):
    def __init__(self, value):
        self.value = value

    def __str__(self):
        return repr(self.value)


class ServerFopException(FopException):
    pass


class RenderFopException(FopException):
    pass


class TemplateNotFoundFopException(RenderFopException):
    pass


class SubprocessFopException(FopException):
    pass
