"""
Фикстуры, которые изменяют каким-либо образом настройки Django
"""
import pytest


@pytest.fixture
def setup_throttling(settings, request):
    """
    Включает в тестах возможность получить ratelimit на запрос,
    использует throttling scope.

    Для этой фикстуры необходима параметризация:

    @pytest.mark.parametrize(
        'setup_throttling',
        [
            ('scope', 'rate'),
            ('course_join', '1/minute'),
        ],
        indirect=('setup_throttling',)  # или просто True
    )
    def test_something(setup_throttling):
        <code>
    """
    assert hasattr(request, 'param'), (
        'This fixture must be parametrized with ("scope", "rate")'
    )

    scope, rate = request.param

    settings.CACHES = {
        'default': {
            'BACKEND': 'django.core.cache.backends.locmem.LocMemCache',
        }
    }
    settings.REST_FRAMEWORK['DEFAULT_THROTTLE_RATES'][scope] = rate
