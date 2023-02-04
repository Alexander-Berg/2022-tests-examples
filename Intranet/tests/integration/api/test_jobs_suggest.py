import pytest

from unittest.mock import patch, Mock

from django.db import connection
from django.urls.base import reverse
from waffle.testutils import override_switch

from intranet.femida.src.publications.choices import JOBS_SUGGEST_TYPES, PUBLICATION_FACETS
from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import ctx_combine


pytestmark = pytest.mark.django_db


NAME_RU = 'Технический Читатель'
NAME_EN = 'Technical Reader'
SLUG = 'technical-reader'

LIMIT = 10


@pytest.fixture(autouse=True, scope='module')
def create_extensions(module_db):
    connection.cursor().execute('CREATE EXTENSION fuzzystrmatch')


@pytest.mark.parametrize('language, query, name, facet', (
    ('ru', 'чит', NAME_RU, JOBS_SUGGEST_TYPES.professions),
    ('en', 'rea', NAME_EN, JOBS_SUGGEST_TYPES.public_professions),
))
def test_jobs_facet_suggest(tvm_jobs_client, language, query, name, facet):
    f.PublicProfessionFactory(
        name_ru=NAME_RU,
        name_en=NAME_EN,
        slug=SLUG,
        professions=[
            f.ProfessionFactory(
                name=NAME_RU,
                name_en=NAME_EN,
                slug=SLUG,
            ),
        ],
    )
    f.PublicationFacetFactory(
        facet=PUBLICATION_FACETS.professions,
        value=SLUG,
        lang=language,
        publication_ids=[1],
    )
    f.PublicationFacetFactory(
        facet=PUBLICATION_FACETS.public_professions,
        value=SLUG,
        lang=language,
        publication_ids=[1],
    )

    # Для теста сортировки
    for i in range(LIMIT):
        f.PublicProfessionFactory(
            name_ru=f'{i}-{NAME_RU}',
            name_en=f'{i}-{NAME_EN}',
            professions=[
                f.ProfessionFactory(
                    name=f'{i}-{NAME_RU}',
                    name_en=f'{i}-{NAME_EN}',
                ),
            ],
        )

    url = reverse('private-api:jobs_suggest:suggest')
    patch_lang = patch(
        target='intranet.femida.src.api.jobs.suggest.suggest.get_publications_lang',
        new=Mock(return_value=language),
    )

    with patch_lang:
        response = tvm_jobs_client.get(
            url,
            {'query': query, 'limit': LIMIT, 'suggest_type': facet},
        )
    assert response.status_code == 200
    result = response.json()
    assert len(result['results']) == LIMIT
    suggest = result['results'][0]
    assert suggest['text'] == name
    assert len(suggest['facets']) == 1
    suggest_filter = suggest['facets'][0]
    assert suggest_filter['facet'] == facet
    assert suggest_filter['value'] == SLUG


@pytest.mark.parametrize('language, query, name, distance', (
    ('ru', 'технический', NAME_RU, 1),
    ('ru', 'техничефффф', NAME_RU, 5),
    ('en', 'technical', NAME_EN, 1),
    ('en', 'technmmmm', NAME_EN, 5),
))
@override_switch('enable_jobs_db_suggest', True)
def test_jobs_db_suggest(tvm_jobs_client, language, query, name, distance):
    facets = [f.PublicationFacetFactory() for _ in range(4)]
    disfigured_name = name.replace(name[0], '*', 1)
    correct_order = [
        f.PublicationSuggestFactory(
            lang=language,
            text=name,
            facets=[facets[0], facets[1]],
        ),
        f.PublicationSuggestFactory(
            lang=language,
            text=disfigured_name,
            priority=2,
            facets=[facets[0], facets[3]],
        ),
        f.PublicationSuggestFactory(
            lang=language,
            text=disfigured_name,
            facets=[facets[1], facets[2]],
            priority=1,
        ),
    ]
    for i in range(LIMIT):
        f.PublicationSuggestFactory(
            lang=language,
            text=disfigured_name,
        )

    url = reverse('private-api:jobs_suggest:suggest')
    patch_lang = patch(
        target='intranet.femida.src.api.jobs.suggest.suggest.get_publications_lang',
        new=Mock(return_value=language),
    )
    patch_distance = patch(
        target='intranet.femida.src.api.jobs.suggest.suggest.DBSuggest._levenshtein_distance',
        new=distance
    )
    with ctx_combine(patch_lang, patch_distance):
        response = tvm_jobs_client.get(
            path=url,
            data={
                'query': query,
                'limit': LIMIT,
                'suggest_type': JOBS_SUGGEST_TYPES.db_suggest,
            },
        )
    assert response.status_code == 200
    result = response.json()
    assert len(result['results']) == LIMIT
    for i, correct_suggest in enumerate(correct_order):
        result_suggest = result['results'][i]
        for facet in result_suggest['facets']:
            correct_suggest.facets.all().filter(facet=facet['facet'], value=facet['value']).get()
