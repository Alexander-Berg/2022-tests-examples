import json
import pytest
import uuid

from constance.test import override_config

from django.core.files import File
from django.test import override_settings
from unittest.mock import patch

from intranet.femida.src.celery_app import NoRetry
from intranet.femida.src.publications.choices import (
    PUBLICATION_FACETS,
    PUBLICATION_LANGUAGES,
    VANADIUM_BATCH_STATUSES,
    VANADIUM_TASK_STATUSES,
    PUBLICATION_PRO_LEVELS,
    PUBLICATION_EMPLOYMENT_TYPES,
)
from intranet.femida.src.publications.models import (
    Publication,
    PublicationFacet,
    PublicationSuggest,
)
from intranet.femida.src.publications.tasks import (
    take_result_table_from_vanadium,
    process_vanadium_result_table,
    fill_publication_facet_table,
    update_suggests_priority,
    update_publications_search_vectors,
)
from intranet.femida.src.utils.vanadium import VanadiumError
from intranet.femida.src.vacancies.choices import VACANCY_PRO_LEVELS

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises, ctx_combine, AnyOrderList


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('field_name, n_calls', (
    ('title', 2),
    ('short_summary', 2),
    ('description', 1),
))
@patch('intranet.femida.src.publications.models.update_og_image')
def test_publication_og_image_update(mocked_update_og_image, field_name, n_calls):
    publication = f.PublicationFactory()
    setattr(publication, field_name, 'new_value')
    publication.save()
    # Всегда один раз вызывается при создании объекта
    assert mocked_update_og_image.call_count == n_calls


@pytest.mark.parametrize('vanadium_answer, exception', (
    ({'batch-status': VANADIUM_BATCH_STATUSES.progress}, VanadiumError),
    ({'batch-status': VANADIUM_BATCH_STATUSES.failed, 'batch-error-info': 'error'}, NoRetry),
    ({'batch-status': VANADIUM_BATCH_STATUSES.succeeded, 'batch-output-table-path': 'path'}, None),
))
def test_take_result_table_from_vanadium(vanadium_answer, exception):
    ctx_managers = ctx_combine(
        patch(
            'intranet.femida.src.publications.tasks.VanadiumAPI.check_batch_status',
            return_value=vanadium_answer,
        ),
        pytest.raises(exception) if exception else assert_not_raises(),
    )
    result = None if exception else 'path'
    with ctx_managers:
        table_path = take_result_table_from_vanadium({
            'batch-uuid': '123e4567-e89b-12d3-a456-426655440000',
            'yt-cluster': 'hahn',
            'queue-name': 'fast',
            'batch-name': 'femida123',
        })
        assert table_path == result


@pytest.mark.parametrize('table, exception', (
    ([{'status': VANADIUM_TASK_STATUSES.failure, 'error-type': 'error'}], NoRetry),
    ([{'status': VANADIUM_TASK_STATUSES.success, 'screenshot-variance': 0}], NoRetry),
    (
        [{
            'status': VANADIUM_TASK_STATUSES.success,
            'screenshot-variance': 100,
            'url': '/publications/123e4567-e89b-12d3-a456-426655440000/og/',
            'meta': {'publication_id': 1234567},
            'screenshot': 'https://vanadium.s3.yandex.net/123.png',
        }],
        None,
    ),
    (
        [
            {
                'status': VANADIUM_TASK_STATUSES.success,
                'screenshot-variance': 100,
                'url': '/publications/123e4567-e89b-12d3-a456-426655440000/og/',
                'meta': {'publication_id': 1234567},
                'screenshot': 'https://vanadium.s3.yandex.net/123.png',
            },
            {'status': VANADIUM_TASK_STATUSES.failure, 'error-type': 'error'},
        ],
        NoRetry,
    )
))
def test_process_vanadium_result_table(django_assert_num_queries, table, exception):
    num_queries = 0 if exception else 2
    f.PublicationFactory(id=1234567, uuid=uuid.UUID('123e4567-e89b-12d3-a456-426655440000'))
    ctx_managers = ctx_combine(
        patch(
            'intranet.femida.src.publications.tasks._get_processed_screenshot_data_from_yt',
            return_value=table,
        ),
        patch(
            'intranet.femida.src.publications.tasks.download',
            return_value=File(file=b''),
        ),
        pytest.raises(exception) if exception else assert_not_raises(),
        django_assert_num_queries(num_queries),
    )
    with ctx_managers:
        process_vanadium_result_table('path')


@pytest.mark.parametrize('lang', (PUBLICATION_LANGUAGES.ru, PUBLICATION_LANGUAGES.en))
@override_settings(JOBS_FORBIDDEN_PROFESSIONS=[], CITY_HOMEWORKER_ID=100500)
def test_fill_publication_facet_table(settings, lang):
    profession1 = f.ProfessionFactory()
    profession2 = f.ProfessionFactory()
    profession3 = f.ProfessionFactory()

    public_profession1 = f.PublicProfessionFactory(professions=[profession1])
    public_profession2 = f.PublicProfessionFactory(professions=[profession2])
    public_profession3 = f.PublicProfessionFactory(professions=[profession3])

    city1 = f.CityFactory()
    city2 = f.CityFactory(id=settings.CITY_HOMEWORKER_ID)
    city3 = f.CityFactory()

    skill1 = f.SkillFactory(is_public=True)
    skill2 = f.SkillFactory(is_public=True)
    skill3 = f.SkillFactory(is_public=True)

    vacancy1 = f.VacancyFactory(
        profession=profession1,
        pro_level_min=VACANCY_PRO_LEVELS.intern,
        pro_level_max=VACANCY_PRO_LEVELS.senior,
    )
    vacancy2 = f.VacancyFactory(
        profession=profession2,
        pro_level_min=VACANCY_PRO_LEVELS.middle,
        pro_level_max=VACANCY_PRO_LEVELS.lead,
    )
    vacancy3 = f.VacancyFactory(profession=profession2)

    f.VacancyCityFactory(vacancy=vacancy1, city=city1)
    f.VacancyCityFactory(vacancy=vacancy2, city=city2)
    f.VacancyCityFactory(vacancy=vacancy3, city=city2)

    f.VacancySkillFactory(vacancy=vacancy1, skill=skill1)
    f.VacancySkillFactory(vacancy=vacancy2, skill=skill2)
    f.VacancySkillFactory(vacancy=vacancy3, skill=skill2)

    pub_service1 = f.PublicServiceFactory()
    pub_service2 = f.PublicServiceFactory()
    pub_service3 = f.PublicServiceFactory()

    pub1 = f.PublishedExternalPublicationFactory(
        vacancy=vacancy1,
        public_service=pub_service1,
        lang=lang,
        is_chief=True,
    )
    pub2 = f.PublishedExternalPublicationFactory(
        vacancy=vacancy2,
        public_service=pub_service2,
        lang=lang,
        is_chief=False,
    )
    pub3 = f.PublishedExternalPublicationFactory(
        vacancy=vacancy3,
        public_service=pub_service2,
        lang=lang,
        is_chief=False,
    )

    # id публикаций, для сортировки и чтобы не вылезти за 100 символов по длине строки
    p1, p2, p3 = sorted((pub1.id, pub2.id, pub3.id))

    expected_filters_lang_data = {
        (PUBLICATION_FACETS.cities, city1.slug, lang, (p1,)),
        (PUBLICATION_FACETS.cities, city2.slug, lang, (p2, p3)),
        (PUBLICATION_FACETS.cities, city3.slug, lang, ()),
        (PUBLICATION_FACETS.pro_levels, PUBLICATION_PRO_LEVELS.junior, lang, (p1, p3)),
        (PUBLICATION_FACETS.pro_levels, PUBLICATION_PRO_LEVELS.middle, lang, (p1, p2, p3)),
        (PUBLICATION_FACETS.pro_levels, PUBLICATION_PRO_LEVELS.senior, lang, (p1, p2, p3)),
        (PUBLICATION_FACETS.pro_levels, PUBLICATION_PRO_LEVELS.chief, lang, (p1,)),
        (PUBLICATION_FACETS.professions, profession1.slug, lang, (p1,)),
        (PUBLICATION_FACETS.professions, profession2.slug, lang, (p2, p3)),
        (PUBLICATION_FACETS.professions, profession3.slug, lang, ()),
        (PUBLICATION_FACETS.public_professions, public_profession1.slug, lang, (p1,)),
        (PUBLICATION_FACETS.public_professions, public_profession2.slug, lang, (p2, p3)),
        (PUBLICATION_FACETS.public_professions, public_profession3.slug, lang, ()),
        (PUBLICATION_FACETS.services, pub_service1.slug, lang, (p1,)),
        (PUBLICATION_FACETS.services, pub_service2.slug, lang, (p2, p3)),
        (PUBLICATION_FACETS.services, pub_service3.slug, lang, ()),
        (PUBLICATION_FACETS.skills, str(skill1.id), lang, (p1,)),
        (PUBLICATION_FACETS.skills, str(skill2.id), lang, (p2, p3)),
        (PUBLICATION_FACETS.skills, str(skill3.id), lang, ()),
        (PUBLICATION_FACETS.employment_types, PUBLICATION_EMPLOYMENT_TYPES.intern, lang, (p1, p3)),
        (PUBLICATION_FACETS.employment_types, PUBLICATION_EMPLOYMENT_TYPES.office, lang, (p1,)),
        (PUBLICATION_FACETS.employment_types, PUBLICATION_EMPLOYMENT_TYPES.remote, lang, (p2, p3)),
    }

    other_languages = set(PUBLICATION_LANGUAGES._db_values) - {lang}
    expected_filters_data = set()
    for item in expected_filters_lang_data:
        expected_filters_data.add(item)
        for other_lang in other_languages:
            expected_filters_data.add((*item[:2], other_lang, ()))

    fill_publication_facet_table()
    result = (
        PublicationFacet.objects
        .values_list(
            'facet',
            'value',
            'lang',
            'publication_ids',
        )
    )
    result_set = {(facet, value, lang, tuple(sorted(ids))) for facet, value, lang, ids in result}

    assert result_set == expected_filters_data


def test_update_suggests_priority():
    # с русским языком в тестовой базе нет нужного конфига "russian_unaccent"
    # если когда-нибудь появится, можно расширить этот тест
    # (плюс test_external_publication_filter_by_text_search)

    pub1 = f.PublishedExternalPublicationFactory(
        title='title first',
        search_vector_en="'titl':1B 'first':2B",
    )
    pub2 = f.PublishedExternalPublicationFactory(
        title='title second',
        search_vector_en="'titl':1B 'second':2B",
    )
    pub3 = f.PublishedExternalPublicationFactory(
        title='title second',
        search_vector_en="'titl':1B 'second':2B",
    )

    facet1_1 = f.PublicationFacetFactory(
        facet=PUBLICATION_FACETS.cities,
        publication_ids=[pub1.id],
    )
    facet1_2 = f.PublicationFacetFactory(
        facet=PUBLICATION_FACETS.cities,
        publication_ids=[pub2.id],
    )
    facet1_3 = f.PublicationFacetFactory(
        facet=PUBLICATION_FACETS.cities,
        publication_ids=[pub3.id],
    )
    facet1_4 = f.PublicationFacetFactory(
        facet=PUBLICATION_FACETS.cities,
        publication_ids=[pub1.id, pub2.id],
    )
    facet2_1 = f.PublicationFacetFactory(
        facet=PUBLICATION_FACETS.pro_levels,
        value=PUBLICATION_PRO_LEVELS.chief,
        publication_ids=[pub2.id],
    )

    # в пустых текстах саджестов нет смысла
    suggest1_1 = f.PublicationSuggestFactory(text='title')
    suggest1_2 = f.PublicationSuggestFactory(text='first')
    suggest1_3 = f.PublicationSuggestFactory(text='second')

    suggest2_1 = f.PublicationSuggestFactory(text='title')
    suggest2_1.facets.add(facet1_1)
    suggest2_2 = f.PublicationSuggestFactory(text='title')
    suggest2_2.facets.add(facet1_2)
    suggest2_3 = f.PublicationSuggestFactory(text='title')
    suggest2_3.facets.add(facet1_3)
    suggest2_4 = f.PublicationSuggestFactory(text='title')
    suggest2_4.facets.add(facet1_4)
    suggest2_5 = f.PublicationSuggestFactory(text='title')
    suggest2_5.facets.add(facet2_1)

    suggest3_1 = f.PublicationSuggestFactory(text='second')
    suggest3_1.facets.add(facet1_4)
    suggest3_2 = f.PublicationSuggestFactory(text='second')
    suggest3_2.facets.add(facet1_3, facet2_1)
    suggest3_3 = f.PublicationSuggestFactory(text='first')
    suggest3_3.facets.add(facet1_2)
    suggest3_4 = f.PublicationSuggestFactory(text='zero')

    expected_suggests_data = {
        (suggest1_1.id, 3), (suggest1_2.id, 1), (suggest1_3.id, 2),
        (suggest2_1.id, 1), (suggest2_2.id, 1), (suggest2_3.id, 1),
        (suggest2_4.id, 2), (suggest2_5.id, 1),
        (suggest3_1.id, 1), (suggest3_2.id, 0), (suggest3_3.id, 0),
        (suggest3_4.id, 0),
    }

    update_suggests_priority()
    result = set(
        PublicationSuggest.objects
        .values_list('id', 'priority')
    )

    assert result == expected_suggests_data


@override_config(JOBS_SEARCH_PUBLICATION_SYNONYM_FIELDS='["title", "sphere_name_en"]')
@override_config(JOBS_FTS_PUBLICATION_FIELDS_PRIORITIES_RU='{}')
@override_config(
    JOBS_FTS_PUBLICATION_FIELDS_PRIORITIES_EN=json.dumps({
        'title': 'A',
        'service_name_en': 'B',
        'cities_en': 'B',
        'skills': 'B',
        'prof_name_en': 'B',
        'sphere_name_en': 'B',
        'short_summary': 'C',
        'description': 'C',
        'duties': 'D',
        'key_qualifications': 'D',
        'additional_requirements': 'D'
    })
)
@patch('intranet.femida.src.utils.begemot.SynonymAPI.get_synonyms')
def test_update_publications_search_vectors(patched_get_synonyms):
    patched_get_synonyms.return_value = {'title': ['antonym'], 'impossible': ['synonym']}

    # В тестовой базе нет нужного конфига "russian_unaccent", тестируем на английском

    profession = f.ProfessionFactory(name_en='prof_name_en')
    sphere = f.ProfessionalSphereFactory(name_en='impossible sphere')
    city = f.CityFactory(name_en='city_name_en')
    skill = f.SkillFactory(name='skill_name')
    vacancy = f.VacancyFactory(profession=profession, professional_sphere=sphere)

    f.VacancyCityFactory(vacancy=vacancy, city=city)
    f.VacancySkillFactory(vacancy=vacancy, skill=skill)

    pub_service = f.PublicServiceFactory(
        name_ru='service_name_ru',
        name_en='service_name_en',
    )

    publication = f.PublishedExternalPublicationFactory(
        vacancy=vacancy,
        public_service=pub_service,
        title='test title',
        short_summary='short_summary',
        description='description',
        duties='duties',
        key_qualifications='key_qualifications',
        additional_requirements='additional_requirements'
    )

    expected_search_vectors_data = {
        'search_vector_en': (
            "'antonym':3A 'synonym':15B 'citi':7B 'descript':18C 'duti':19 'en':6B,9B,12B "
            "'imposs':13B 'key':20 'name':5B,8B,11B 'prof':10B 'qualif':21 'servic':4B 'short':16C "
            "'sphere':14B 'summari':17C 'test':1A 'titl':2A 'addit':22 'requir':23 "
        ),
    }

    update_publications_search_vectors(id=publication.id)
    patched_get_synonyms.assert_called_once()

    pub = Publication.objects.get(id=publication.id)
    for field, expected_search_vector in expected_search_vectors_data.items():
        vector = getattr(pub, field)

        assert AnyOrderList(vector.split()) == expected_search_vector.split(), vector
