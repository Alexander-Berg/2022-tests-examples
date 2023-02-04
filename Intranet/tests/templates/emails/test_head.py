import pytest
from django.utils.translation import override

from idm.tests.templates.utils import (LANG_CODES, generate_named_user,
                                       render_template)

pytestmark = pytest.mark.parametrize('lang', LANG_CODES)


def test_head(lang: str):
    user = generate_named_user()
    with override(lang):
        assert render_template('emails/head.txt', {'user': user}) == \
               {'ru': 'Добрый день, %s!', 'en': 'Good day, %s!'}[lang] % user.get_short_name(lang)
