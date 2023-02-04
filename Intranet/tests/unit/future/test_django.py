import django
import pytest

from tests.utils.assertions import assert_not_raises


def test_empty_field_list_filter():
    """
    Если тест падает, потому что версия django теперь 3+,
    нужно перейти с класса ok.future.django.admin.EmptyFieldListFilter
    на django.contrib.admin.filters.EmptyFieldListFilter
    и удалить наш класс и тест
    """
    raises = assert_not_raises if django.VERSION[0] < 3 else pytest.raises
    with raises(ImportError):
        from ok.future.django.admin import EmptyFieldListFilter  # noqa
