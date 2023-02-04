import pytest

from kelvin.news.models import News


@pytest.fixture
def news(content_manager):
    """
    Создает пустую новость
    """
    news = News.objects.create(
        title='News 1',
        summary='Summary 1',
        content='Content 1',
        status=News.Status.DRAFT,
        owner=content_manager,
    )
    return news
