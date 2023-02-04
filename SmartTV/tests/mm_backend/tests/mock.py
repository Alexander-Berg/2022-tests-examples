class VhApiEmptyResponseMock:
    @staticmethod
    def get_channels(_):
        return []


class VhApiMock:
    channels = (
        ('Романтика', 100003),
        ('AIVA', 1949),
        ('Семейные комедии', 1541518787)
    )

    @staticmethod
    def get_channels(_):
        return VhApiMock.channels
