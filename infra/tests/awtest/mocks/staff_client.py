class StaffMockClient(object):
    @staticmethod
    def get_staff_id_for_abc_role(role_slug):
        return '{}_id'.format(role_slug)

    @staticmethod
    def get_groups_by_ids(group_ids, *_, **__):
        return {g_id: {'url': 'testing'} for g_id in group_ids}

    @staticmethod
    def get_group_members(role_slug):
        return ['{}_{}'.format(role_slug, u_id) for u_id in range(10)]

    @staticmethod
    def get_abc_roles(abc_service_id):
        return [{"role_scope": "development", "id": 268088}, {"role_scope": "cert", "id": 268093}]
