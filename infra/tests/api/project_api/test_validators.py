import pytest

import walle.views.helpers.validators as validators
from walle.clients.cms import CmsApiVersion
from walle.errors import BadRequestError
from walle.projects import CmsSettings

CMS_URL = "http://project.yandex-team.ru/cms"
BOT_PROJECT_ID = 1000009
TVM_APP_ID = 100500
OTHER_TVM_APP_ID = 1050


class TestCheckCmsTvmAppIdRequirements:
    @pytest.fixture
    def mock_service(self, mp, mock_get_planner_id_by_bot_project_id, mock_abc_get_service_slug):
        mock_get_planner_id_by_bot_project_id()
        mock_abc_get_service_slug()

    @pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
    def test_tvm_app_id_must_be_set_if_supported(self, walle_test, api_version):
        settings = CmsSettings(
            url=CMS_URL,
            max_busy_hosts=None,
            api_version=api_version,
            tvm_app_id=None,
            temporary_unreachable_enabled=False,
        )
        with pytest.raises(BadRequestError) as exc:
            validators.check_cms_tvm_app_id_requirements(settings, BOT_PROJECT_ID)
        expected_msg = (
            "CMS TVM app id must be set for non-default CMSes. If your CMS does not support TVM "
            + "yet, please contact Wall-e administrators"
        )
        assert str(exc.value) == expected_msg

    @pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
    def test_tvm_app_id_doesnt_match_service(self, walle_test, api_version, mock_service, mock_service_tvm_app_ids):
        settings = CmsSettings(
            url=CMS_URL,
            max_busy_hosts=None,
            api_version=api_version,
            tvm_app_id=TVM_APP_ID,
            temporary_unreachable_enabled=False,
        )
        mock_service_tvm_app_ids([OTHER_TVM_APP_ID])

        with pytest.raises(BadRequestError) as exc:
            validators.check_cms_tvm_app_id_requirements(settings, BOT_PROJECT_ID)
        assert str(exc.value) == "CMS TVM app id {} is not registered in ABC service some_service".format(TVM_APP_ID)

    @pytest.mark.parametrize("api_version", CmsApiVersion.ALL_CMS_API)
    def test_tvm_app_id_matches_service(self, walle_test, api_version, mock_service, mock_service_tvm_app_ids):
        settings = CmsSettings(
            url=CMS_URL,
            max_busy_hosts=None,
            api_version=api_version,
            tvm_app_id=TVM_APP_ID,
            temporary_unreachable_enabled=False,
        )
        mock_service_tvm_app_ids([TVM_APP_ID])

        validators.check_cms_tvm_app_id_requirements(settings, BOT_PROJECT_ID)


class TestHostShortNameTemplateValidator:
    def test_index_is_required(self):
        with pytest.raises(BadRequestError):
            validators.validated_host_shortname_template("no-index-here")

    def test_three_allowed_placeholders_allowed(self):
        validators.validated_host_shortname_template("prefix-{index:04d}{bucket}{location}-suffix")

    def test_index_with_modifier_allowed_when_without_bucket(self):
        validators.validated_host_shortname_template("prefix-{index:02d}-{location}-suffix")

    def test_index_with_modifier_allowed_when_with_bucket(self):
        validators.validated_host_shortname_template("prefix-{index:02d}{bucket}{location}-suffix")

    def test_index_without_modifier_not_allowed_when_with_bucket(self):
        with pytest.raises(BadRequestError):
            validators.validated_host_shortname_template("prefix-{index}{bucket}{location}-suffix")

    def test_index_without_modifier_allowed_when_without_bucket(self):
        validators.validated_host_shortname_template("prefix-{index}-{location}-suffix")

    def test_only_three_known_placeholders_allowed(self):
        with pytest.raises(BadRequestError):
            validators.validated_host_shortname_template("{prefix}-{index:04}{bucket}{location}-{suffix}")

    def test_only_shortnames_allowed(self):
        with pytest.raises(BadRequestError):
            # this name template has a dot in in which makes it an fqdn template, not shortname.
            validators.validated_host_shortname_template("{location}{bucket}-{index:04}.suffix")
