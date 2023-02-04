import pytest

from kelvin.subjects.models import Subject, Theme


@pytest.fixture
def subject_model():
    """
    Учебный предмет
    """
    return Subject.objects.create(
        slug='math',
        name=u'Высшая математика',
    )


@pytest.fixture
def theme_model(subject_model):
    """
    Тема
    """
    return Theme.objects.create(
        name=u'Обыкновенные дифференциальные уравнения',
        code=u'1112',
        subject=subject_model,
    )
