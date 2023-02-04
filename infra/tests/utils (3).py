class DummyDict(object):
    def __getitem__(self, item):
        return self

    def __str__(self):
        return 'dict-value-mock'

    def __repr__(self):
        return self.__str__()
