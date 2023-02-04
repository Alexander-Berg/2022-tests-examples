import pytest

from django.utils import translation

from intranet.femida.src.api.offers.forms import CitizenshipChoiceField
from intranet.femida.src.offers.citizenship_choices import CITIZENSHIP_TRANSLATIONS_RU, CITIZENSHIP_TRANSLATIONS_EN


@pytest.mark.parametrize('lang, translations', [
    ('en-en', CITIZENSHIP_TRANSLATIONS_EN),
    ('ru-ru', CITIZENSHIP_TRANSLATIONS_RU)
])
def test_citizenship_field_preserves_choices_order(lang, translations):
    ideal_data = [label for value, label in translations.items() if value != 'ZZ']
    translation.activate(lang)
    cf = CitizenshipChoiceField()
    result = cf.structure_as_dict(prefix='', name='', state=None, base_initial={}, base_data={})['choices']
    assert result[0]['value'] == ''
    assert result[-1]['value'] == 'ZZ'
    assert [x['label'] for x in result[1:-1]] == ideal_data
