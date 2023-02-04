# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    calling,
    raises,
    equal_to,
    contains,
    contains_inanyorder,
    empty,
)

from testutils import (
    TestCase,
    create_organization,
)
from intranet.yandex_directory.src.yandex_directory.common.exceptions import FeatureNotFound
from intranet.yandex_directory.src.yandex_directory.core.models import OrganizationModel
from intranet.yandex_directory.src.yandex_directory.core.features import (
    DOMAIN_AUTO_HANDOVER,
    CHANGE_ORGANIZATION_OWNER,
)
from intranet.yandex_directory.src.yandex_directory.core.features.utils import (
    is_feature_enabled,
    get_organization_features_info,
    set_feature_value_for_organization,
    get_features_to_enable,
)


class TestGetFeatureValue(TestCase):
    def test_feature_for_organization(self):
        feature_id, feature_slug = self.create_feature(enabled_default=True)
        self.set_feature_value_for_organization(feature_slug, enabled=False)
        enabled = is_feature_enabled(self.meta_connection, self.organization['id'], feature_slug)
        assert not enabled

    def test_feature_enabled_default(self):
        feature_id, feature_slug = self.create_feature(enabled_default=True)
        enabled = is_feature_enabled(self.meta_connection, self.organization['id'], feature_slug)
        assert enabled

    def test_feature_not_found(self):
        assert_that(
            calling(is_feature_enabled).with_args(
                self.meta_connection,
                self.organization['id'],
                'no-feature',
            ),
            raises(FeatureNotFound)
        )


class TestGetOrganizationFeatures(TestCase):
    def test_feature_for_organization(self):
        # получаем список фич для организации

        # фичи уже добавлены для всех тестов
        features = get_organization_features_info(self.meta_connection, self.organization['id'])
        assert_that(
            features,
            equal_to(
                {
                    'pdd-proxy-to-connect': {
                        'enabled': False,
                        'default': False,
                        'description': 'Switches traffic of PDD proxy to Connect'
                    },
                    'domain-auto-handover': {
                        'enabled': True,
                        'default': False,
                        'description': 'Auto domain handover'
                    },
                    'can-work-without-owned-domains': {
                        'enabled': True,
                        'default': False,
                        'description': 'Ability to add departments, groups and invite outer users to organizations without owned domains'
                    },
                    'change-organization-owner': {
                        'enabled': False,
                        'default': False,
                        'description': 'Organization owner can transfer ownership to other user'
                    },
                    'no-doregistration': {
                        'enabled': False,
                        'default': False,
                        'description': 'Accept EULA automatically for every new user (needed for some portals).'
                    },
                    'no-more-pdd': {
                        'enabled': True,
                        'default': False,
                        'description': 'Dont use PDD API for the organization.'
                    },
                    'multiorg': {
                        'enabled': True,
                        'default': False,
                        'description': 'User in many organizations'
                    },
                    'use_cloud_proxy': {
                        'enabled': False,
                        'default': False,
                        'description': 'Use proxy to ya.org'
                    },
                    'use_domenator': {
                        'enabled': False,
                        'default': False,
                        'description': 'Use domenator to work with organization domains',
                    },
                    'disable_maillists_check': {
                        'enabled': False,
                        'default': False,
                        'description': 'Disable work CheckMaillistsTask',
                    },
                    'sso_available': {
                        'enabled': False,
                        'default': False,
                        'description': 'Available SSO for organization',
                    },
                 }
            )
        )

    def test_multiorg_feature_for_cloud_organization(self):
        org = self.create_organization(organization_type='cloud', preset='cloud')
        features = get_organization_features_info(self.meta_connection, org['id'])
        expected = {
            'enabled': True,
            'default': False,
            'description': 'User in many organizations'
        }
        assert features['multiorg'] == expected


class TestSetFeatureValueForOrganization(TestCase):
    def test_create_new_feature_for_org(self):
        # Включаем фичу для организации, у которой ещё нет такой фичи
        org_id = create_organization(
            self.meta_connection,
            self.main_connection,
            label='new_org',
        )['organization']['id']

        assert_that(
            is_feature_enabled(
                self.meta_connection,
                org_id,
                CHANGE_ORGANIZATION_OWNER,
            ),
            equal_to(False)
        )
        set_feature_value_for_organization(
            self.meta_connection,
            org_id,
            CHANGE_ORGANIZATION_OWNER,
            True,
        )
        assert_that(
            is_feature_enabled(
                self.meta_connection,
                org_id,
                CHANGE_ORGANIZATION_OWNER,
            ),
            equal_to(True)
        )

    def test_update_feature_for_org(self):
        # Выключаем фичу для организации, у которой фича уже есть
        assert_that(
            is_feature_enabled(
                self.meta_connection,
                self.organization['id'],
                DOMAIN_AUTO_HANDOVER,
            ),
            equal_to(True)
        )
        set_feature_value_for_organization(
            self.meta_connection,
            self.organization['id'],
            DOMAIN_AUTO_HANDOVER,
            False,
        )
        assert_that(
            is_feature_enabled(
                self.meta_connection,
                self.organization['id'],
                DOMAIN_AUTO_HANDOVER,
            ),
            equal_to(False)
        )


class TestGetFeaturesToEnable(TestCase):
    def test_get_features_to_enable_success(self):
        """Проверим, что для обычной организации функция отдаёт пустой список.
        """
        features = get_features_to_enable(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
        )
        assert_that(
            features,
            contains_inanyorder('no-more-pdd', 'can-work-without-owned-domains', 'multiorg'),
        )

    def test_ya_organization_for_adminka_or_pdd(self):
        """Проверим, что для обычной организации функция отдаёт пустой список.
        Если организация пришла из источников pdd_new_promo или adminka,
        то для неё мы должны включать фичу ya_organization.
        """
        for source in ('adminka', 'pdd_new_promo'):
            # Притворимся, что организация из нужного источника
            OrganizationModel(self.main_connection) \
                .filter(id=self.organization['id']) \
                .update(source=source)
            features = get_features_to_enable(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
            )
            assert_that(
                features,
                contains_inanyorder('no-more-pdd', 'can-work-without-owned-domains', 'multiorg'),
            )
