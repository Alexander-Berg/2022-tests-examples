import mock
import pytest

from wiki.sync.connect.models import Organization
from wiki.sync.connect.tasks.helpers import (
    ForcedSyncData,
    import_org_data_by_dir_org_id,
    _import_dir_organization_unsafe,
)

pytestmark = [
    pytest.mark.django_db,
]

path = 'wiki.sync.connect.tasks.helpers'


class TestForcedSyncSetCloudId:
    def test_ensure_cloud_id(self, api_url, client, wiki_users, organizations, groups):
        dir_id = 1000001
        cloud_id = 'zxc'

        d = {
            'id': dir_id,
            'label': 'text',
            'name': 'masstermax org',
            'language': 'ru',
            'subscription_plan': 'free',
            'is_blocked': False,
            'cloud_org_id': cloud_id,
            'admin_id': '15',
            'organization_type': 'org',
        }

        with (
            mock.patch(f'{path}.dir_client.get_organization') as m,
            mock.patch(f'{path}.import_dir_organization_mp_safe') as m2,
            mock.patch('wiki.sync.connect.utils.import_org_data'),
            mock.patch('wiki.sync.connect.utils.organization_imported.send'),
            mock.patch('wiki.sync.connect.utils.ChangeEvent.save'),
            mock.patch('wiki.sync.connect.utils.ChangeEvent.objects.select_for_update'),
        ):
            m.return_value = d
            m2.side_effect = _import_dir_organization_unsafe

            data = ForcedSyncData(
                dir_org_id=dir_id,
                cloud_org_id=cloud_id,
                user_uid='',
                user_cloud_uid='',
                user_iam_token='',
            )
            import_org_data_by_dir_org_id(data)
            assert m.called
            assert m2.called
            assert (Organization.objects.get(dir_id=dir_id)).cloud_id == cloud_id
