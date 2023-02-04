from django_idm_api.utils import get_hooks

from django.test import TestCase

from lms.courses.tests.factories import CourseTeamFactory
from lms.idm.constants import GROUPS_RULE_NAME, SUPERUSER_RULE_NAME, TEAMS_RULE_NAME
from lms.users.tests.factories import GroupFactory

from .factories import GroupFirewallRuleFactory, TeamFirewallRuleFactory


def build_info(groups=None, teams=None):
    return {
        'code': 0,
        'roles': {
            'slug': 'role',
            'name': {
                'ru': 'Тип',
                'en': 'Type'
            },
            'values': {
                'superuser': {
                    'name': {
                        'ru': 'Суперпользователь',
                        'en': 'Superuser',
                    },
                    'firewall-declaration': SUPERUSER_RULE_NAME,
                },
                'groups': {
                    'name': {
                        'ru': 'Участник группы',
                        'en': 'Group member'
                    },
                    'firewall-declaration': GROUPS_RULE_NAME,
                    'roles': {
                        'slug': 'group',
                        'name': {
                            'ru': 'Группа',
                            'en': 'Group',
                        },
                        'values': groups or {},
                    }
                },
                'teams': {
                    'name': {
                        'ru': 'Участник команды',
                        'en': 'Team member',
                    },
                    'firewall-declaration': TEAMS_RULE_NAME,
                    'roles': {
                        'slug': 'team',
                        'name': {
                            'ru': 'Команда',
                            'en': 'Team',
                        },
                        'values': teams or {},
                    }
                }
            }
        }
    }


class IDMRoleHooksHookTestCase(TestCase):
    def setUp(self) -> None:
        self.hooks = get_hooks()

    def test_info(self):
        groups = GroupFactory.create_batch(3)
        teams = CourseTeamFactory.create_batch(5)

        group_values = {
            obj.pk: {'name': obj.name}
            for obj in groups
        }

        team_values = {
            obj.pk: {'name': obj.name}
            for obj in teams
        }

        with self.assertNumQueries(2):
            info = self.hooks.info()

        expected = build_info(groups=group_values, teams=team_values)
        self.assertEqual(info, expected)

    def test_info_with_group_rule(self):
        group = GroupFactory()
        group_rule = GroupFirewallRuleFactory(group=group)

        group_values = {
            obj.pk: {
                'name': obj.name,
                'firewall-declaration': group_rule.rule.slug,
            }
            for obj in [group]
        }

        with self.assertNumQueries(2):
            info = self.hooks.info()

        expected = build_info(groups=group_values)
        self.assertEqual(info, expected)

    def test_info_with_team_rule(self):
        team = CourseTeamFactory()
        team_rule = TeamFirewallRuleFactory(team=team)

        team_values = {
            obj.pk: {
                'name': obj.name,
                'firewall-declaration': team_rule.rule.slug,
            }
            for obj in [team]
        }

        with self.assertNumQueries(2):
            info = self.hooks.info()

        expected = build_info(teams=team_values)
        self.assertEqual(info, expected)
