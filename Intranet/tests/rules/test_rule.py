import pytest

from intranet.table_flow.src.rules.models import Rule, RuleUser
from intranet.table_flow.src.rules.admin import RuleAdmin
from intranet.table_flow.src.users.models import User


@pytest.mark.django_db
def test_create_rule():
    Rule.objects.create(slug='a', file='/some/file', json='{}')
    assert Rule.objects.get()


@pytest.mark.django_db
def test_access_to_rules():
    ruleA = Rule.objects.create(slug='a', json={})
    ruleB = Rule.objects.create(slug='b', json={})
    user = User.objects.create(staff_id=0, username='0')
    RuleUser.objects.create(rule=ruleA, user=user)
    filtered_rules = RuleAdmin._filter_rules_by_user(Rule.objects.all(), user)
    assert ruleA in filtered_rules and ruleB not in filtered_rules

    admin = User.objects.create(staff_id=1, is_superuser=True, username='1')
    filtered_rules = RuleAdmin._filter_rules_by_user(Rule.objects.all(), admin)
    assert ruleA in filtered_rules and ruleB in filtered_rules
