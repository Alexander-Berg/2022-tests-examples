from google.protobuf import json_format
import yaml

from infra.orly.lib import sched
from infra.orly.proto import orly_pb2

TEST_RULE_SPEC = """
duration: '1200s'
maxAllowed: 2
schedule:
    enableDays:
    - day: 'MON'
      begin: '04:00'
      end: '23:00'
    - day: 'TUE'
      begin: '04:00'
      end: '23:00'
    - day: 'WED'
      begin: '04:00'
      end: '23:00'
    - day: 'THU'
      begin: '04:00'
      end: '23:00'
    - day: 'FRI'
      begin: '04:00'
      end: '23:00'
    - day: 'SAT'
      begin: '04:00'
      end: '23:00'
    - day: 'SUN'
      begin: '04:00'
      end: '23:00'
    - day: 'MON'
      begin: '00:00'
      end: '04:00'
      maxAllowed: 1
    - day: 'TUE'
      begin: '00:00'
      end: '04:00'
      maxAllowed: 1
    - day: 'WED'
      begin: '00:00'
      end: '04:00'
      maxAllowed: 1
    - day: 'THU'
      begin: '00:00'
      end: '04:00'
      maxAllowed: 1
    - day: 'FRI'
      begin: '00:00'
      end: '04:00'
      maxAllowed: 1
    - day: 'SAT'
      begin: '00:00'
      end: '04:00'
      maxAllowed: 1
    - day: 'SUN'
      begin: '00:00'
      end: '04:00'
      maxAllowed: 1
    - day: 'MON'
      begin: '23:00'
      end: '23:59'
      maxAllowed: 1
    - day: 'TUE'
      begin: '23:00'
      end: '23:59'
      maxAllowed: 1
    - day: 'WED'
      begin: '23:00'
      end: '23:59'
      maxAllowed: 1
    - day: 'THU'
      begin: '23:00'
      end: '23:59'
      maxAllowed: 1
    - day: 'FRI'
      begin: '23:00'
      end: '23:59'
      maxAllowed: 1
    - day: 'SAT'
      begin: '23:00'
      end: '23:59'
      maxAllowed: 1
    - day: 'SUN'
      begin: '23:00'
      end: '23:59'
      maxAllowed: 1
"""


def test_full_rule():
    d = yaml.load(TEST_RULE_SPEC, Loader=yaml.SafeLoader)
    r = orly_pb2.Rule()
    json_format.ParseDict(d, r.spec)
    s = sched.make_schedule(r)
    # Monday 8:44
    p = s.days[0].get_policy(524)
    assert p.max_allowed == 2


def test_schedule():
    rule = orly_pb2.Rule()
    op = orly_pb2.Operation()
    assert sched.make_schedule(rule).get_policy().add(rule, op)
    mon = rule.spec.schedule.enable_days.add()
    mon.day = orly_pb2.DayRule.MON
    mon.begin = '12:00'
    mon.end = '19:00'
    mon.max_allowed = 10
    schedule = sched.make_schedule(rule)
    # Check forbidden
    assert schedule.days[orly_pb2.DayRule.MON].get_policy(0).add(rule, op)
    # Check ok
    assert schedule.days[orly_pb2.DayRule.MON].get_policy(720).add(rule, op) is None, 'Should not be'
    assert rule.status.in_progress == 1
    assert op.status.in_progress.status == 'True'


def test_max_allowed_policy():
    p = sched.MaxAllowedPolicy(0)
    r = orly_pb2.Rule()
    op = orly_pb2.Operation()
    # No operations allowed by policy
    assert p.add(r, op) == 'no operations allowed at the moment'
    assert r.status.in_progress == 0
    # Limit reached
    p.max_allowed = 1
    r.status.in_progress = 1
    assert p.add(r, op) == 'too many operations in progress'
    assert r.status.in_progress == 1
    # Good case
    p.max_allowed = 2
    assert p.add(r, op) is None
    assert r.status.in_progress == 2
    assert op.status.in_progress.status == 'True'
