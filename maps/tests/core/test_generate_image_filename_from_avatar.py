import pytest

from maps_adv.export.lib.core.utils import generate_image_filename_from_avatar


def test_generates_expected_image_filename_with_alias(config):
    namespace = config.AVATARS_NAMESPACE
    avatar = dict(image_name="img", group_id="123456798", alias="alias")

    name = generate_image_filename_from_avatar(avatar)

    assert name == f"{namespace}--123456798--img--alias"


@pytest.mark.parametrize(
    ["alias_template", "expected_alias"],
    [
        ["alias_template", "alias_template"],
        ["alias_template_{zoom}", "alias_template"],
        ["alias_template{zoom}", "alias_template"],
        ["alias_template_zoom", "alias_template_zoom"],
        ["alias_template_{size}", "alias_template"],
        ["alias_template{size}", "alias_template"],
        ["alias_template_size", "alias_template_size"],
    ],
)
def test_generates_expected_image_filename_with_alias_template(
    alias_template, expected_alias, config
):
    namespace = config.AVATARS_NAMESPACE
    avatar = dict(image_name="img", group_id="123456798", alias_template=alias_template)

    name = generate_image_filename_from_avatar(avatar)

    assert name == f"{namespace}--123456798--img--{expected_alias}"


def test_alias_field_has_more_priority_than_alias_template_field(config):
    namespace = config.AVATARS_NAMESPACE
    avatar = dict(
        image_name="img",
        group_id="123456798",
        alias="alias",
        alias_template="alias_template",
    )

    name = generate_image_filename_from_avatar(avatar)

    assert name == f"{namespace}--123456798--img--alias"
