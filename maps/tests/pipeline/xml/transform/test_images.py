from maps_adv.export.lib.core.enum import ImageType
from maps_adv.export.lib.pipeline.xml.transform.images import TemplateImageFields


def test_expected_result_with_exists_image_type(config):
    namespace = config.AVATARS_NAMESPACE
    avatar = dict(
        image_name="imagename",
        group_id="groupid",
        alias_template="alias_template_value",
    )

    result = TemplateImageFields(
        images={ImageType.PIN: avatar}, anchor="anchor_value", size="size_value"
    ).generate("PinName", ImageType.PIN)

    assert result == dict(
        stylePinName=f"{namespace}--groupid--imagename--alias_template_value",
        anchorPinName="anchor_value",
        sizePinName="size_value",
    )


def test_expected_result_without_exists_image_type():
    result = TemplateImageFields(
        images={}, anchor="anchor_value", size="size_value"
    ).generate("PinName", ImageType.PIN)

    assert result == dict()
