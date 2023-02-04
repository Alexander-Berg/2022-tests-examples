class AnyOrderList:
    """
    Мок для тестов, чтобы сравнивать два списка на равенство.
    Примеры:
    >>> AnyOrderList([1, 2, 3]) == [2, 1, 3]  # True
    >>> AnyOrderList([1, 2, 3]) == [1, 2]  # False
    >>> AnyOrderList([1, 1, 2]) == [1, 2, 2]  # False
    """

    def __init__(self, value):
        assert isinstance(value, list)
        self.value = value

    def __eq__(self, other):
        if not isinstance(other, list):
            return False
        return sorted(self.value) == sorted(other)

    def __repr__(self):
        return f'AnyOrderList(value={self.value})'
