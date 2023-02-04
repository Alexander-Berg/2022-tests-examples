import rstr


class PersonalRole:
    IDM_SLUG = 'maps-auto-head-unit-task-management'
    IDM_ROLE_USER = 'user'

    def __init__(self, login=None, role=None, path=None, fields={}):
        self.login = login or rstr.letters(5, 20)
        self.role = role or {PersonalRole.IDM_SLUG: PersonalRole.IDM_ROLE_USER}
        self.path = path or '/{}/{}'.format(PersonalRole.IDM_SLUG, PersonalRole.IDM_ROLE_USER)
        self.fields = fields

    def __eq__(self, obj):
        return isinstance(obj, PersonalRole) and \
            self.__dict__ == obj.__dict__

    def __str__(self):
        return (f"PersonalRole(login={self.login}, role={self.role}, "
                f"path={self.path}, fields={self.fields})")

    @staticmethod
    def roles_from_json(json):
        assert json['code'] == 0, json
        roles = []
        for user in json['users']:
            login = user['login']
            for role in user['roles']:
                slug, role_kind = list(role.items())[0]
                path = '/{}/{}'.format(slug, role_kind)
                personal_role = PersonalRole(login=login, role=role, path=path)
                roles.append(personal_role)
        return roles
