import factory

from intranet.table_flow.src.users import models as user_models
from intranet.table_flow.src.rules import models as rule_models


class User(factory.DjangoModelFactory):
    username = factory.Sequence(lambda b: f'user_{b}')
    staff_id = factory.Sequence(lambda n: n + 1)

    class Meta:
        model = 'users.User'


class Rule(factory.DjangoModelFactory):
    slug = factory.Sequence(lambda n: f'rule{n}')
    json = factory.LazyAttribute(lambda _: {'in_fields': {}, 'out_fields': {}, 'cases': []})

    class Meta:
        model = 'rules.Rule'
