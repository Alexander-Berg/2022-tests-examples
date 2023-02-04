# coding: utf-8


from hamcrest import (
    assert_that,
    empty,
    contains,
    has_entries,
    all_of,
    instance_of,
)


from testutils import (
    TestCase,
    get_auth_headers,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.models import OrganizationModel, ImageModel
from sqlalchemy.orm import Session
from unittest.mock import patch, ANY


class TestOrganizationsViewCase(TestCase):
    def test_organizations_for_unknown_user_give_empty_list(self):
        # Если пользователь не найден в базе, то считаем, что у него просто нет ни одной организации
        # и отдаём пустой список
#        with patch.object(app.components, 'meta_session', Session(self.meta_connection)):
        headers = get_auth_headers(as_uid=100500)
        response = self.get_json('/proxy/organizations/', headers=headers)
        assert_that(
            response,
            has_entries(
                total=0,
                result=empty(),
                links=empty(),
            )
        )

    def test_organizations_success_case(self):
        # Если пользователь не найден в базе, то считаем, что у него просто нет ни одной организации
        # и отдаём пустой список
        #with patch.object(app.components, 'meta_session', Session(self.meta_connection)):
        org_id = self.organization['id']
        OrganizationModel(self.main_connection).update_user_count(org_id=org_id)
        self.process_tasks()

        response = self.get_json('/proxy/organizations/')
        domain = self.organization_domain

        assert_that(
            response,
            has_entries(
                total=1,
                result=contains(
                    has_entries(
                        id=org_id,
                        admin_id=self.admin_uid,
                        registrar_id=ANY,
                        user_count=1,
                        domains=has_entries(
                            master=domain,
                            owned=[domain],
                            all=[domain],
                        )
                    )
                )
            )
        )

    def test_organizations_success_with_logo(self):
        org_id = self.organization['id']
        OrganizationModel(self.main_connection).update_user_count(org_id=org_id)
        path = '/get-connect/5201/some-img/orig'
        image = ImageModel(self.main_connection).create(
            org_id,
            meta={
                'sizes': {
                    'orig': {
                        'height': 960,
                        'path': path,
                        'width': 640
                    }
                }
            }
        )
        OrganizationModel(self.main_connection).update(
            update_data={'logo_id': image['id']},
            filter_data={'id': org_id},
        )
        response = self.get_json('/proxy/organizations/')
        assert_that(
            response,
            has_entries(
                total=1,
                result=contains(
                    has_entries(
                        id=org_id,
                        logo='https://avatars.mds.yandex.net{}'.format(path),
                    )
                )
            )
        )
