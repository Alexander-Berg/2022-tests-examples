import six


class AbcMockClient(object):
    @staticmethod
    def list_services(*args, **kwargs):
        return {'results': [{u'id': 1872}]}

    @staticmethod
    def get_service(*args, **kwargs):
        return {u'id': 1872}

    @staticmethod
    def get_service_slug(service_id):
        return six.text_type(u'{}_slug'.format(service_id))

    @staticmethod
    def list_service_member_role_slugs(**__):
        return [u'test_svc_role']

    @staticmethod
    def get_service_member_roles(**__):
        return [{u'code': u'l3_responsible'}]
