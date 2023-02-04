from common import factories

from django.conf import settings
from plan.services.models import Service


def test_base_non_leaf():
    non_leaf = factories.ServiceFactory(is_base=True)
    leaf = factories.ServiceFactory(parent=non_leaf, is_base=True)
    regular = factories.ServiceFactory(parent=leaf)

    assert non_leaf.is_base_non_leaf()
    assert not leaf.is_base_non_leaf()
    assert not regular.is_base_non_leaf()

    assert set(Service.objects.base()) == {non_leaf, leaf}
    assert set(Service.objects.base_non_leaf()) == {non_leaf}


def test_change_is_base():
    base_tag = factories.ServiceTagFactory(slug=settings.BASE_TAG_SLUG)
    service = factories.ServiceFactory()

    service.is_base = True
    service.save()

    assert service.tags.filter(pk=base_tag.pk).exists()

    service.is_base = False
    service.save()

    assert not service.tags.filter(pk=base_tag.pk).exists()
