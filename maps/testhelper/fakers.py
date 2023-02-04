import random


class AvatarFaker:
    URL = 'https://avatars.mds.yandex.net'
    DIGITS = '0123456789'
    HEX_DIGITS = '0123456789abcdef'

    def __init__(self, namespace, seed, group_id_size=6, auto_id_size=36):
        self.namespace = namespace
        self.random = random.Random(seed)
        self.group_id_size = group_id_size
        self.auto_id_size = auto_id_size

    def __call__(self, name=None, size='%s'):
        ns = self.namespace
        gid = self.group_id()
        name = name or self.auto_id()
        return f'{AvatarFaker.URL}/get-{ns}/{gid}/{name}/{size}'

    def group_id(self):
        return self.choices(AvatarFaker.DIGITS, self.group_id_size)

    def auto_id(self):
        return self.choices(AvatarFaker.HEX_DIGITS, self.auto_id_size)

    def choices(self, population, k):
        return ''.join(self.random.choice(population) for _ in range(k))
