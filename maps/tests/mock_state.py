from omniORB.any import to_any, from_any
import Yandex, Yandex__POA

class State(Yandex__POA.State):
    def __init__(self, **kwargs):
        self.__data = kwargs.copy()

    def clearState(self):
        self.__data.clear ()

    def setParam(self, param, value):
        self.__data[param] = value
        return 1

    def getParam(self, param):
        return self.__data.get(param, None)

    def getAllParams(self):
        return [Yandex.State.Param(Name=name, Value=value) for name, value in self.__data.iteritems()]

