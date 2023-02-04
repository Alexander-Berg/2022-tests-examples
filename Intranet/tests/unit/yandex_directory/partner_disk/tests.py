# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    equal_to,
    calling,
    raises,
)
from testutils import (
    TestCase,
    mocked_requests,
    assert_called_once,
    assert_not_called,
)
from unittest.mock import (
    patch,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    ServiceModel,
    enable_service,
    UserServiceLicenseExternalData,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationServiceModel,
    ResourceRelationModel,
)
from intranet.yandex_directory.src.yandex_directory.connect_services.partner_disk.client import PartnerDiskError
from intranet.yandex_directory.src.yandex_directory.connect_services.partner_disk.tasks import (
    DeleteSpacePartnerDiskTask,
    AddSpacePartnerDiskTask,
)
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShards


class TestPartnerDiskClient(TestCase):

    def setUp(self):
        super(TestPartnerDiskClient, self).setUp()
        tvm.tickets['partner_disk'] = 'disk-tvm-ticket'
        self.product_id = 'yandex_directory_1tb'
        self.uid = 77777

    def test_delete_disk_space(self):
        url = '{host}/v1/disk/partners/yandex_directory/services/123'.format(
            host=app.config['PARTNER_DISK_HOST'],
            pr_id=self.product_id,
        )
        with mocked_requests() as requests:
            requests.delete.return_value.status_code = 204
            app.partner_disk.delete_disk_space(self.uid, '123')

            assert_called_once(
                requests.delete,
                timeout=0.5,
                url=url,
                headers={
                    'X-Uid': str(self.uid),
                    'X-Ya-Service-Ticket': 'disk-tvm-ticket',
                },
            )

        # 404 код обрабатывается без ошибок
        with mocked_requests() as requests:
            requests.delete.return_value.status_code = 404
            app.partner_disk.delete_disk_space(self.uid, '123')

    def test_add_disk_space(self):
        url = '{host}/v1/disk/partners/yandex_directory/services?product_id={pr_id}'.format(
            host=app.config['PARTNER_DISK_HOST'],
            pr_id=self.product_id,
        )
        with mocked_requests() as requests:
            requests.get.return_value.status_code = 200
            requests.post.return_value.status_code = 201
            app.partner_disk.add_disk_space(self.uid)

            assert_called_once(
                requests.post,
                timeout=60,
                url=url,
                headers={
                    'X-Uid': str(self.uid),
                    'X-Ya-Service-Ticket': 'disk-tvm-ticket',
                },
            )

        # если место уже было выдано, но не пытаемся выдать его снова
        with mocked_requests() as requests:
            requests.get.return_value.json.return_value = {
                'items': [{'product_id': 'intranet.yandex_directory.src.yandex_directory_1tb',
                            'service_id': '08dcfa11490ca9bcd2629ab57bdb5d53'}],
                'partner': 'yandex_directory'
            }
            assert_not_called(requests.post)


class TestPartnerDiskTasks(TestCase):

    def setUp(self):
        super(TestPartnerDiskTasks, self).setUp()
        tvm.tickets['partner_disk'] = 'disk-tvm-ticket'
        self.uid = self.create_user()['id']
        self.org_id = self.organization['id']
        self.disk = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='disk',
            name='disk',
            robot_required=False,
            paid_by_license=True,
        )
        UpdateServicesInShards().try_run()
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.org_id,
            self.disk['slug'],
        )

        self.resource_id = OrganizationServiceModel(self.main_connection).filter(
            org_id=self.org_id,
            service_id=self.disk['id']
        ).scalar('resource_id')[0]

    def test_DeleteSpacePartnerDiskTask(self):
        UserServiceLicenseExternalData(self.main_connection).create(
            org_id=self.org_id,
            user_id=self.uid,
            service_id=self.disk['id'],
            external_data={'service_id': '1234'}
        )
        DeleteSpacePartnerDiskTask(self.main_connection).delay(
            org_id=self.org_id,
            uid=self.uid,
            resource_id=self.resource_id,
            author_id=self.admin_uid,
        )
        with patch.object(app, 'partner_disk') as mocked_disk:
            self.process_tasks()
            mocked_disk.delete_disk_space.assert_called_once_with(self.uid, '1234')

        assert_that(
            UserServiceLicenseExternalData(self.main_connection).filter(
                user_id=self.uid
            ).count(),
            equal_to(0)
        )

        # если в базе есть запись, то в диск не ходим и место не удаляем
        self.create_licenses_for_service(self.disk['id'], user_ids=[self.uid])
        DeleteSpacePartnerDiskTask(self.main_connection).delay(
            org_id=self.org_id,
            uid=self.uid,
            resource_id=self.resource_id,
            author_id=self.admin_uid,
        )
        with patch.object(app, 'partner_disk') as mocked_disk:
            self.process_tasks()
            assert_not_called(mocked_disk.delete_disk_space)

    def test_DeleteSpacePartnerDiskTask_rollback(self):
        # если не получилось удалить место, то возвращаем лицензию в базу
        assert_that(
            ResourceRelationModel(self.main_connection).filter(
                org_id=self.org_id,
                user_id=self.uid,
                resource_id=self.resource_id,
            ).count(),
            equal_to(0),
        )

        UserServiceLicenseExternalData(self.main_connection).create(
            org_id=self.org_id,
            user_id=self.uid,
            service_id=self.disk['id'],
            external_data={'service_id': '1234'}
        )

        DeleteSpacePartnerDiskTask(self.main_connection).delay(
            org_id=self.org_id,
            uid=self.uid,
            resource_id=self.resource_id,
            author_id=self.admin_uid,
        )
        with patch.object(app, 'partner_disk') as mocked_disk:
            mocked_disk.delete_disk_space.side_effect = PartnerDiskError()
            self.process_tasks()

        assert_that(
            ResourceRelationModel(self.main_connection).filter(
                org_id=self.org_id,
                user_id=self.uid,
                resource_id=self.resource_id,
            ).count(),
            equal_to(1),
        )

        assert_that(
            UserServiceLicenseExternalData(self.main_connection).filter(
                user_id=self.uid
            ).count(),
            equal_to(1)
        )

    def test_AddSpacePartnerDiskTask(self):
        # если лицензии нет, то не ходим в диск и не выдаем место
        AddSpacePartnerDiskTask(self.main_connection).delay(
            org_id=self.org_id,
            uid=self.uid,
            resource_id=self.resource_id,
            author_id=self.admin_uid,
        )
        with patch.object(app, 'partner_disk') as mocked_disk:
            self.process_tasks()
            assert_not_called(mocked_disk.add_disk_space)

        self.create_licenses_for_service(self.disk['id'], user_ids=[self.uid])
        AddSpacePartnerDiskTask(self.main_connection).delay(
            org_id=self.org_id,
            uid=self.uid,
            resource_id=self.resource_id,
            author_id=self.admin_uid,
        )
        with patch.object(app, 'partner_disk') as mocked_disk:
            mocked_disk.add_disk_space.return_value = {'product_id': 'intranet.yandex_directory.src.yandex_directory_1tb',
                                                       'service_id': 'dea2b5b08ec438d96b60357a87f078df'}
            self.process_tasks()
            mocked_disk.add_disk_space.assert_called_once_with(self.uid)

        assert_that(
            UserServiceLicenseExternalData(self.main_connection).filter(
                user_id=self.uid
            ).one()['external_data']['service_id'],
            equal_to('dea2b5b08ec438d96b60357a87f078df')
        )

    def test_AddSpacePartnerDiskTask_rollback(self):
        # если не получилось выдать место, то удаляем лицензию из базы
        self.create_licenses_for_service(self.disk['id'], user_ids=[self.uid])
        assert_that(
            ResourceRelationModel(self.main_connection).filter(
                org_id=self.org_id,
                user_id=self.uid,
                resource_id=self.resource_id,
            ).count(),
            equal_to(1),
        )

        AddSpacePartnerDiskTask(self.main_connection).delay(
            org_id=self.org_id,
            uid=self.uid,
            resource_id=self.resource_id,
            author_id=self.admin_uid,
        )
        with patch.object(app, 'partner_disk') as mocked_disk:
            mocked_disk.add_disk_space.side_effect = PartnerDiskError()
            self.process_tasks()

        assert_that(
            ResourceRelationModel(self.main_connection).filter(
                org_id=self.org_id,
                user_id=self.uid,
                resource_id=self.resource_id,
            ).count(),
            equal_to(0),
        )
