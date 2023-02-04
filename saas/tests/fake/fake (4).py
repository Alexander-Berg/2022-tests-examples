# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import random

from faker import Faker
from faker.providers import BaseProvider

from saas.library.python.common_functions import GEO_LOCATIONS


fake = Faker()


class Provider(BaseProvider):
    # noinspection PyMethodMayBeStatic
    def gencfg_group_name(self, geo='', base_name='', prefix_geo=False):
        group_base_name = base_name if base_name else '_'.join(fake.words(3)).upper()
        if not geo:
            geo = random.choice(tuple(GEO_LOCATIONS))
        if prefix_geo or random.getrandbits(2) > 0:
            return '{}_{}'.format(geo, group_base_name)
        else:
            return '{}_{}'.format(group_base_name, geo)

    # noinspection PyMethodMayBeStatic
    def gencfg_tag_name(self, nanny_format=False):
        release = fake.random.randint(80, 999)
        revision = fake.random.randint(1, 9999)
        format_str = r'tags\/stable-{}-r{}' if nanny_format else 'stable-{}-r{}'
        return format_str.format(release, revision)
