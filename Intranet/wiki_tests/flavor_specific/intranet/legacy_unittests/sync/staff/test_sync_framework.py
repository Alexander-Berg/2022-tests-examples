from mock import MagicMock, patch

from wiki.intranet.models import Department, GroupMembership
from wiki.sync.staff.mapping.field_mapping import FieldMapping
from wiki.sync.staff.mapping.models import DepartmentMapper
from wiki.sync.staff.mapping.repository_mapper import RepositoryMapper
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.flavor_specific.intranet.legacy_unittests.sync.staff.stubs import (
    NEOFELIS_DEPT_ID,
    NEOFELIS_ID,
    NEOFELIS_PARENT_DEPT_ID,
    DepartmentMockMapper,
    StaffMockMapper,
    mock_registry,
    parent_dept,
    GroupMembershipMockMapper,
    gm_factory,
    GROUP_ID,
    GROUPB_ID,
    GroupMockMapper,
    grp_admins,
    grp_neadmins,
)


class StaffMapperTest(BaseTestCase):
    def test_mapping(self):
        remote = {
            'id': 42,
            'cast': 'input',
            'department_group': {'department': {'id': 42}},
            'cars': [
                {'model': 'lamborgini'},
                {'model': 'bmw'},
            ],
            '_meta': {'modified_at': '123'},
        }

        cast_fn = MagicMock(return_value='output')

        class MockMapper(RepositoryMapper):
            pk_mappings = [FieldMapping('id', 'id')]

            field_mapping = [
                FieldMapping('department_id', 'department_group.department.id'),
                FieldMapping('car', 'cars[1].model'),
                FieldMapping('foo', 'bar', default=1000),
                FieldMapping('cast', 'cast', cast_fn),
            ]

        fields = MockMapper.convert_remote_to_model_fields(remote)

        self.assertEqual(fields['department_id'], remote['department_group']['department']['id'])
        self.assertEqual(fields['car'], remote['cars'][1]['model'])
        self.assertEqual(fields['id'], remote['id'])
        self.assertEqual(fields['foo'], 1000)
        self.assertEqual(fields['cast'], 'output')
        cast_fn.assert_called_once_with('input')

    def test_model_creation_and_update(self):
        fields = DepartmentMapper.convert_remote_to_model_fields(parent_dept)
        DepartmentMapper.create_local_model(fields)
        DepartmentMapper.create_local_model(fields, exist_ok=True)

        dept = Department.objects.get(pk=fields['id'])
        assert dept.name_en == fields['name_en']

        parent_dept['department']['name']['full']['en'] = 'new name'
        fields = DepartmentMapper.convert_remote_to_model_fields(parent_dept)
        DepartmentMapper.update_local_model(dept, fields=fields)

        dept.refresh_from_db()
        assert dept.name_en == 'new name'

    def test_node_resolve(self):
        """
        Staff Neofelis зависит от Department, который зависит от parent Department
        """
        sync_node = StaffMockMapper.get_node(NEOFELIS_ID)
        StaffMockMapper.find_missing_related_models(sync_node, mock_registry)

        dep_1 = sync_node.missing_dependencies[0]
        dep_2 = sync_node.missing_dependencies[0].missing_dependencies[0]

        self.assertEqual(dep_1.mapper, DepartmentMockMapper)
        self.assertEqual(dep_1.raw['department']['id'], NEOFELIS_DEPT_ID)

        self.assertEqual(dep_2.mapper, DepartmentMockMapper)
        self.assertEqual(dep_2.raw['department']['id'], NEOFELIS_PARENT_DEPT_ID)

        self.assertEqual(len(dep_2.missing_dependencies), 0)

    def test_gm_resolve(self):
        """
        GroupMembershipMapper должен создать запись если пары staff_id x group_id не существует
        и должен вызвать обновление, если уже существует
        """

        mapper = GroupMockMapper()
        mapper.process_batch([], mock_registry)
        mapper.process_batch([grp_admins, grp_neadmins], mock_registry)

        mapper = GroupMembershipMockMapper()
        mapper.process_batch(
            [gm_factory(GROUP_ID, NEOFELIS_ID, 1), gm_factory(GROUPB_ID, NEOFELIS_ID, 2)], mock_registry
        )

        mdl = GroupMembership.objects.get(staff_id=NEOFELIS_ID, group_id=GROUP_ID)
        mdl2 = GroupMembership.objects.get(staff_id=NEOFELIS_ID, group_id=GROUPB_ID)

        assert mdl.group.name == 'Admins'
        assert mdl.staff.work_email == 'neofelis@yandex-team.ru'

        assert mdl2.group.name == 'neAdmins'
        assert mdl2.group.id == GROUPB_ID
        assert mdl2.staff.work_email == 'neofelis@yandex-team.ru'

        mm = MagicMock()
        mm.return_value = (True, ['some_field'])

        with patch.object(GroupMembershipMockMapper, 'update_local_model', mm):
            mapper.process_batch(
                [gm_factory(GROUP_ID, NEOFELIS_ID, 1), gm_factory(GROUPB_ID, NEOFELIS_ID, 2)], mock_registry
            )
            assert mm.call_count == 2
