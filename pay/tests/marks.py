import pytest

from pay.integration.conf import settings

mark_selenium = pytest.mark.skipif(not settings.SELENIUM_TESTS_ENABLED, reason='selenium disabled')
