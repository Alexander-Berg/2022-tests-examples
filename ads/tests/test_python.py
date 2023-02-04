#!/usr/bin/env python
import getpass

from yabs.notifier import auto
from yabs.notifier import notify

def x(a, b, c=5, **kwargs):
    print a + b * c


class Obj(object):
    def __init__(self, x):
        self.x = x

    def method(self, x, **kwargs):
        return x + kwargs.get("y", 0)


class Hello(object):

    @notify
    def __call__(self):
        pass

    @staticmethod
    @notify
    def static_method_Success():
        pass

    @staticmethod
    @notify
    def static_method_called_from_class_Success():
        pass

    @notify
    def simple_call_Success(self):
        pass

    @notify
    def simple_call_with_parameters_Success(self, a):
        pass

    @notify
    def simple_call_with_parameters_less_params_Fail(self, a):
        pass

    @notify
    def simple_call_with_parameters_more_params_Fail(self, a):
        pass

    @notify(getpass.getuser())
    def call_and_meta_Success(self):
        pass

    @notify(getpass.getuser())
    def call_with_parameters_and_meta_Success(self, a, b):
        pass

    @notify(getpass.getuser(), mail=getpass.getuser(), xmpp=getpass.getuser())
    def call_with_parameters_and_meta_and_kwargs_Success(self, a, b):
        pass

    @notify(getpass.getuser())
    def call_with_parameter_and_meta_pass_more_params_Fail(self, a):
        pass

    @notify(getpass.getuser())
    def call_with_parameter_and_meta_pass_less_params_Fail(self, a):
        pass


@notify
def simple_call_Success():
    pass


@notify
def simple_call_with_parameters_Success(a):
    pass


@notify
def simple_call_with_parameters_less_params_Fail(a):
    pass


@notify
def simple_call_with_parameters_more_params_Fail(a):
    pass


@notify(getpass.getuser())
def call_and_meta_Success():
    pass


@notify(getpass.getuser())
def call_with_parameters_and_meta_Success(a, b):
    pass

@notify(getpass.getuser(), mail=getpass.getuser(), xmpp=getpass.getuser())
def call_with_parameters_and_meta_and_kwargs_Success(a, b):
    pass

@notify(getpass.getuser())
def call_with_parameter_and_meta_pass_more_params_Fail(a):
    pass


@notify(getpass.getuser())
def call_with_parameter_and_meta_pass_less_params_Fail(a):
    pass


def func(x=0, y=1, z=2, t=3, *args, **kwargs):
    return x + y + z + t


def func_2(x, y, z, a=5, b=6, *args, **kwargs):
    return x + y + z + a + b

if __name__ == "__main__":
    auto.init()
    # print "My Script"
    # notify(x)(1, 2, 3, d=7)
    # notify(x)(1, 2, 3)
    # notify(x)(1, 2, d=7)
    # notify(Obj.method)(Obj(5), 1)
    # a = Obj(5)
    # notify(a.method)(1, y=2)
    # notify([getpass.getuser(), 'nonexistaccount'])(x)(1, 2, 3, d=7)
    # a = Hello()
    # a()
    # a.static_method_Success()
    # Hello.static_method_called_from_class_Success()
    # a.simple_call_Success()
    # a.simple_call_with_parameters_Success("x")
    # a.call_and_meta_Success()
    # a.call_with_parameters_and_meta_Success("1", "2")
    # a.call_with_parameters_and_meta_and_kwargs_Success("1", "2")

    # # a.call_with_parameter_and_meta_pass_more_params_Fail("1", "2")
    # # a.call_with_parameter_and_meta_pass_less_params_Fail()
    # # a.simple_call_with_parameters_less_params_Fail()
    # # a.simple_call_with_parameters_more_params_Fail(1, 2, 3)

    # simple_call_Success()
    # simple_call_with_parameters_Success("x")
    # call_and_meta_Success()
    # call_with_parameters_and_meta_Success(1, 2)
    # call_with_parameters_and_meta_and_kwargs_Success(1, 2)
    # # call_with_parameter_and_meta_pass_more_params_Fail("1", "2")
    # # call_with_parameter_and_meta_pass_less_params_Fail()
    # # simple_call_with_parameters_less_params_Fail()
    # # simple_call_with_parameters_more_params_Fail(1, 2, 3)

    # notify(func)(111, z=5)
    # notify(func)(111, z=5, e=6)
    # notify(func)(111, z=5, y=7)
    # notify(func)(111, 2, 3, 4, 5, 6, e=5)
    # notify(func)(111, 2, 3, 4, 5, 6, 7, e=5)
    # notify(func_2)(1, 2, 3)
    # notify(func_2)(1, a=7, b=8, y=1, z=2)
    # notify(func_2)(1, 2, 3, 4, 5, 6, 7)
