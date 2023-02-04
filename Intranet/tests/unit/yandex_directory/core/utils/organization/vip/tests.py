# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    has_entries,
    contains_inanyorder,
    empty,
    has_item,
    has_items,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
    trial_status)

from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShards
from testutils import (
    TestCase,
    create_organization,
    override_settings,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationModel,
    ServiceModel,
    OrganizationServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.organization import (
    vip_reason,
    organization_type,
    subscription_plan,
)

from intranet.yandex_directory.src.yandex_directory.core.utils.organization.vip import update_vip_reasons_for_all_orgs


class Test_update_vip_reasons(TestCase):

    def setUp(self, *args, **kwargs):
        super(Test_update_vip_reasons, self).setUp(*args, **kwargs)

        service = ServiceModel(self.meta_connection).create(
            slug='paid-service-slug',
            name='Name',
            robot_required=False,
            paid_by_license=True,
            trial_period_months=1
        )
        UpdateServicesInShards().try_run()

        self.organization_paid_and_trial_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='o1'
        )['organization']['id']
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization_paid_and_trial_id,
            service['slug'],
        )
        OrganizationModel(self.main_connection).update_one(
            self.organization_paid_and_trial_id,
            {
                'subscription_plan': subscription_plan.paid,
            },
        )

        self.organization_paid_service_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='o2'
        )['organization']['id']
        org_service = enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization_paid_service_id,
            service['slug'],
        )
        OrganizationServiceModel(self.main_connection).update(
            update_data={
                'trial_status': trial_status.expired,
                'enabled': True
            },
            filter_data={
                'org_id': self.organization_paid_service_id,
                'service_id': org_service['id'],
            }
        )
        user = self.create_user(org_id=self.organization_paid_service_id)
        self.create_licenses_for_service(
            service['id'],
            user_ids=[user['id']],
            org_id=self.organization_paid_service_id
        )

        self.organization_partner_and_whitelist_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='o3'
        )['organization']['id']
        OrganizationModel(self.main_connection).change_organization_type(
            self.organization_partner_and_whitelist_id,
            organization_type.partner_organization,
            partner_id=self.partner['id'],
        )
        OrganizationModel(self.main_connection).update_vip_reasons(
            self.organization_partner_and_whitelist_id, [vip_reason.whitelist]
        )

        self.organization_many_users_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='o4'
        )['organization']['id']
        for _ in range(10):
            self.create_user(
                org_id=self.organization_many_users_id
            )
        OrganizationModel(self.main_connection).update_user_count(self.organization_many_users_id)

        self.organization_no_vip_reasons_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='o5'
        )['organization']['id']
        OrganizationModel(self.main_connection). \
            filter(id=self.organization_no_vip_reasons_id). \
            update(vip=[vip_reason.many_users, vip_reason.whitelist])

    @override_settings(VIP_MANY_USERS_COUNT=5)
    def test_me(self):
        update_vip_reasons_for_all_orgs(self.main_connection)
        self.process_tasks()

        assert_that(
            OrganizationModel(self.main_connection).fields('id', 'vip', 'user_count').all(),
            has_items(
                has_entries(
                    id=self.organization_many_users_id,
                    vip=has_item(vip_reason.many_users)
                ),
                has_entries(
                    id=self.organization_partner_and_whitelist_id,
                    vip=contains_inanyorder(vip_reason.partner, vip_reason.whitelist)
                ),
                has_entries(
                    id=self.organization_paid_and_trial_id,
                    vip=contains_inanyorder(vip_reason.paid, vip_reason.trial_service)
                ),
                has_entries(
                    id=self.organization_paid_service_id,
                    vip=contains_inanyorder(vip_reason.paid)
                ),
                has_entries(
                    id=self.organization_no_vip_reasons_id,
                    vip=contains_inanyorder(vip_reason.whitelist)
                )
            )
        )
