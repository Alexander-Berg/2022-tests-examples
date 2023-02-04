# module not called dbswitch.py so that it doesn't break tests that rely on actual DB


class DummyDbPool(object):
    __db = {}

    def __init__(self, db):
        self.__db = db
        self.__expect = None

    def __getitem__(self, key):
        return self

    def _expect(self, table):
        self.__expect = table

    def sql(self, *args, **kwargs):
        return self.__db[self.__expect]

    def getRootMaster(self, *args, **kwargs):
        return self

    def getRootSlave(self, *args, **kwargs):
        return self

    def connect(self, *args, **kwargs):
        return self

    def close(self, *args, **kwargs):
        return True

    def execute(self, *args, **kwargs):
        return self.__db[self.__expect]

    def contextual_connect(self):
        return self

    def begin(self):
        return self

    def rollback(self):
        pass
