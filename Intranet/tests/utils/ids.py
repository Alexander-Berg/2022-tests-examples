class FakeIdsCollection(list):

    @property
    def total(self):
        return len(self)
