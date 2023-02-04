# coding: utf-8


from idm.core.plugins.dumb import Plugin as DumbPlugin, BasePlugin
from idm.permissions.hooks import Hooks
from idm.users.constants.user import USER_TYPES


class SimplePlugin(DumbPlugin):
    def get_info(self, **kwargs):
        """Роли, запрошенные у системы"""
        return {
            'code': 0,
            'roles': {
                'slug': 'role',
                'name': 'Роль',
                'values': {
                    'admin': 'Админ',
                    'manager': 'Менеджер',
                    'poweruser': 'Могучий Пользователь',
                    'superuser': 'Супер Пользователь',
                },
            },
            'fields': [
                {'slug': 'login', 'name': 'Доп поле', 'required': False},
                {'slug': 'passport-login', 'name': 'Паспортный логин', 'required': False},
            ],
        }


class YaPlugin(SimplePlugin):
    """Плагин, идентичный Simple плагину. Нужен для функционирования фикстуры other_system."""
    pass


class ComplexPlugin(DumbPlugin):
    """Плагин, идентичный тестовому, но с другим деревом ролей"""

    def get_info(self):
        tree = {
            'roles': {
                'slug': 'project',
                'name': 'Проект',
                'firewall-declaration': 'test-project',
                'values': {
                    'subs': {
                        'name': 'Подписки',
                        'roles': {
                            'slug': 'role',
                            'name': 'роль',
                            'values': {
                                'developer': 'Разработчик',
                                'manager': {
                                    'name': 'Менеджер',
                                    'help': 'Управленец',
                                },
                            },
                        },
                    },
                    'rules': {
                        'name': 'IDM',
                        'firewall-declaration': 'test-project-rules',
                        'roles': {
                            'slug': 'role',
                            'name': 'роль',
                            'values': {
                                'admin': 'Админ',
                                'auditor': {
                                    'name': 'Аудитор',
                                    'firewall-declaration': 'test-rules-auditor',
                                },
                                'invisic': {
                                    'name': 'невидимка',
                                    'visibility': False,
                                },
                            },
                        },
                    },
                },
            },
            'fields': [
                {
                    'slug': 'field_1',
                    'name': 'Поле 1',
                    'required': True
                },
                {
                    'slug': 'field_2',
                    'name': 'Поле 2',
                    'required': False
                },
                {
                    'slug': 'passport-login',
                    'name': 'Паспортный логин',
                    'required': True
                },
            ],
        }
        return tree


class TestPlugin(DumbPlugin):
    def get_info(self, project=None, role=None, **kwargs):
        """Возвращает дерево ролей от системы"""
        dct = {
            'code': 0,
            'roles': {
                'slug': 'project',
                'name': 'Проект',
                'firewall-declaration': 'test1-fire',
                'values': {
                    'proj1': {
                        'name': 'Проект 1',
                        'help': 'Проект для связи с общественностью',
                        'firewall-declaration': 'test1-fire-proj1',
                        'roles': {
                            'slug': 'role',
                            'name': 'Роль',
                            'values': {
                                'admin': 'Админ',
                                'manager': {
                                    'name': 'Менеджер',
                                    'help': 'Самый главный менеджер для тестов',
                                    'firewall-declaration': 'test1-fire-proj1-manager',
                                },
                                'doc': 'Тех писатель',
                            }
                        }
                    },
                    'proj2': {
                        'name': 'Проект 2',
                        'roles': {
                            'slug': 'role',
                            'name': 'Роль',
                            'values': {
                                'wizard': 'Кудесник',
                                'invisible_role': {
                                    'name': 'Невидимка!',
                                    'visibility': False,
                                }
                            }
                        }
                    },
                    'proj3': {
                        'name': 'Проект 3',
                        'roles': {
                            'slug': 'a_subproject',
                            'name': 'Альфа-подпроект',
                            'values': {
                                'subproj1': {
                                    'name': 'Подпроект 1',
                                    'roles': {
                                        'slug': 'role',
                                        'name': 'Роль',
                                        'values': {
                                            'admin': 'Админ',
                                            'manager': 'Менеджер',
                                            'developer': 'Разработчик',
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            'fields': [
                {
                    'slug': 'passport-login',
                    'name': 'Паспортный логин',
                    'required': True,
                },
                {
                    'slug': 'something_else',
                    'name': 'Новое что-то',
                    'required': False,
                }
            ]
        }

        return dct


class ADSystemPlugin(DumbPlugin):
    def get_info(self, **kwargs):
        return {
            'code': 0,
            'roles': {
                'slug': 'type',
                'name': 'тип роли',
                'values': {
                    'global': {
                        'unique_id': 'global',
                        'name': 'global',
                        'roles': {
                            'slug': 'role',
                            'name': 'роль',
                            'values': {
                                'system_group_relation': {
                                    'name': {'en': 'system-group relation', 'ru': 'связь системы с группой'},
                                    'unique_id': 'system_group_relation',
                                    'fields': [
                                        {'is_required': True,
                                         'name': {'ru': 'слаг системы', 'en': 'system slug'},
                                         'slug': 'system'},
                                        {'is_required': True, 'name': {'ru': 'DN группы', 'en': 'group DN'},
                                         'slug': 'group_dn'}]}}}},

                    'roles_in_groups': {
                        'unique_id': 'roles_in_groups',
                        'name': 'roles_in_groups',
                        'roles': {
                            'slug': 'ad_group',
                            'name': 'AD-группа',
                            'values': {
                                'OU=group1': {
                                    'name': 'OU=group1',
                                    'unique_id': 'OU=group1',
                                    'roles': {
                                        'slug': 'group_roles',
                                        'name': {'ru': 'Роли группы', 'en': 'Group roles'},
                                        'values': {
                                            'member': {
                                                'unique_id': 'OU=group1::member',
                                                'name': {'ru': 'Участник', 'en': 'Member'}
                                            },
                                            'responsible': {
                                                'unique_id': 'OU=group1::responsible',
                                                'name': {'ru': 'Ответственный', 'en': 'Responsible'}
                                            }
                                        }
                                    }
                                },
                                'OU=group2': {
                                    'name': 'OU=group2',
                                    'unique_id': 'OU=group2',
                                    'roles': {
                                        'slug': 'group_roles',
                                        'name': {'ru': 'Роли группы', 'en': 'Group roles'},
                                        'values': {
                                            'member': {
                                                'unique_id': 'OU=group2::member',
                                                'name': {'ru': 'Участник', 'en': 'Member'}},
                                            'responsible': {
                                                'unique_id': 'OU=group2::responsible',
                                                'name': {'ru': 'Ответственный', 'en': 'Responsible'}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


class SelfPlugin(BasePlugin):
    """Плагин, который изображает IDM как систему.
    Отличается от Generic-плагина тем, что ему не нужен"""

    def __init__(self, system):
        super(SelfPlugin, self).__init__(system)
        self.hooks = Hooks()

    def get_info(self, *args, **kwargs):
        return self.hooks.info()

    def add_role(self, role_data, fields_data, username=None, group_id=None, **kwargs):
        subject_type = kwargs.get('subject_type') or USER_TYPES.USER
        return self.hooks.add_role(login=username, role=role_data, fields=fields_data, subject_type=subject_type)

    def remove_role(self, role_data, system_specific, username=None, is_fired=None, group_id=None, **kwargs):
        subject_type = kwargs.get('subject_type') or USER_TYPES.USER
        return self.hooks.remove_role(
            login=username,
            role=role_data,
            data=system_specific,
            is_fired=is_fired,
            subject_type=subject_type,
        )


class InfoDatabasePlugin(DumbPlugin):
    def get_info(self):
        return self.system.roles_info.roles
