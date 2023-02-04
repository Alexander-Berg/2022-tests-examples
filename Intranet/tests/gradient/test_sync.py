import mock
import pytest
from dateutil import parser

from review.gradient import models, tasks
from review.lib import datetimes
from review.staff import const as staff_const


ENGAGED_FROM = '2022-02-10T19:08:15.690'
ENGAGED_FROM_DT = parser.parse(ENGAGED_FROM)


class ConnectorMock:

    def __init__(self):
        self._umbrellas = self._umbrella_gen()
        self._umbrella_eng = self._umbrella_engagement_gen()
        self._vs_eng = self._vs_engagement_gen()

    @staticmethod
    def _umbrella_gen():
        yield {
            'result': [
                {
                    'goal': 'GOALZ-123',
                    'goal_id': 123,
                    'name': '123',
                    'value_stream': {
                        'name': 'VS 123',
                        'name_en': 'VS 123',
                        'url': 'vs_123',
                        'abc_service_id': 123,
                    },
                },
                {
                    'goal': 'GOALZ-223',
                    'goal_id': 223,
                    'name': '223',
                    'value_stream': {
                        'name': 'VS 123',
                        'name_en': 'VS 123',
                        'url': 'vs_123',
                        'abc_service_id': 123,
                    },
                },
            ],
            'continuation_token': 1,
        }
        yield {
            'result': [
                {
                    'goal': 'GOALZ-777',
                    'goal_id': 777,
                    'name': '777',
                    'value_stream': {
                        'name': 'VS 777',
                        'name_en': 'VS 777',
                        'url': 'vs_777',
                        'abc_service_id': 777,
                    },
                },
                {
                    'goal': 'GOALZ-FAKE',
                    'goal_id': 100500,
                    'name': 'FAKE',
                    'value_stream': {
                        'name': 'VS VFAKE',
                        'name_en': 'VS FAKE',
                        'url': 'vs_FAKE',
                        'abc_service_id': None,
                    },
                },
            ],
        }

    @staticmethod
    def _umbrella_engagement_gen():
        yield {
            'person_123': [
                {
                    'goal': 'GOALZ-123',
                    'engagement': '51.00',
                    'engaged_from': ENGAGED_FROM,
                    'engaged_to': None,
                },
                {
                    'goal': None,
                    'engagement': '48.00',
                    'engaged_from': ENGAGED_FROM,
                    'engaged_to': None,
                },
                {
                    'goal': 'GOALZ-223',
                    'engagement': '1.00',
                    'engaged_from': ENGAGED_FROM,
                    'engaged_to': None,
                },
            ]
        }
        yield {
            'person_777': [
                {
                    'goal': 'GOALZ-777',
                    'engagement': '100.00',
                    'engaged_from': ENGAGED_FROM,
                    'engaged_to': None,
                },
            ]
        }
        yield {
            'person_vs_without_umbrellas': []
        }

    @staticmethod
    def _vs_engagement_gen():
        yield {
            'person_123': [
                {
                    'url': 'anything',
                    'name': '-',
                    'name_en': '-',
                    'abc_service_id': 1233,
                    'service_tags': ['sm_bs'],
                },
                {
                    'url': 'vs_123',
                    'name': 'VS 123',
                    'name_en': 'VS 123',
                    'abc_service_id': 123,
                    'service_tags': ['sm_bs', 'vs'],
                },
                {
                    'url': 'anything_1',
                    'name': '-',
                    'name_en': '-',
                    'abc_service_id': 1234,
                    'service_tags': ['bs_vs'],
                },
            ]
        }
        yield {
            'person_777': [
                {
                    'url': 'anything',
                    'name': '-',
                    'name_en': '-',
                    'abc_service_id': 7771,
                    'service_tags': ['sm_bs'],
                },
                {
                    'url': 'vs_777',
                    'name': 'VS 777',
                    'name_en': 'VS 777',
                    'abc_service_id': 777,
                    'service_tags': ['sm_bs', 'vs'],
                },
                {
                    'url': 'anything_1',
                    'name': '-',
                    'name_en': '-',
                    'abc_service_id': 7772,
                    'service_tags': ['bs_vs'],
                },
            ]
        }
        yield {
            'person_vs_only': [
                {
                    'url': 'anything',
                    'name': '-',
                    'name_en': '-',
                    'abc_service_id': 7771,
                    'service_tags': ['sm_bs'],
                },
                {
                    'url': 'vs_123',
                    'name': 'VS 123',
                    'name_en': 'VS 123',
                    'abc_service_id': 123,
                    'service_tags': ['sm_bs', 'vs'],
                },
                {
                    'url': 'anything_1',
                    'name': '-',
                    'name_en': '-',
                    'abc_service_id': 7772,
                    'service_tags': ['bs_vs'],
                },
            ]
        }
        yield {
            'person_vs_without_umbrellas': [
                {
                    'url': 'no_umbrella',
                    'name': 'no_umbrella',
                    'name_en': '-',
                    'abc_service_id': 100500,
                    'service_tags': ['sm_bs', 'vs'],
                },
                {
                    'url': 'vs_123',
                    'name': 'VS 123',
                    'name_en': 'VS 123',
                    'abc_service_id': 123,
                    'service_tags': ['sm_bs', 'vs'],
                },
            ]
        }

    def get(self, resource, *args, **kwargs):
        if resource == staff_const.UMBRELLAS:
            resp = next(self._umbrellas)
            return resp
        assert 1, 'Wrong resourse passed'

    def post(self, resource, *args, **kwargs):
        if resource == staff_const.UMBRELLAS_ENGAGEMENT:
            resp = next(self._umbrella_eng)
            return resp
        elif resource == staff_const.VS_ENGAGEMENT:
            resp = next(self._vs_eng)
            return resp
        assert 1, 'Wrong resourse passed'


@pytest.fixture
def db_state(
    main_product_builder,
    umbrella_builder,
    umbrella_person_builder,
    main_product_person_builder,
    person_builder,
):
    future_date = datetimes.shifted(datetimes.now(), days=1)
    past_date = datetimes.shifted(datetimes.now(), days=-10)

    main_product_123 = main_product_builder(
        name='name_to_change',
        abc_service_id=123,
    )
    main_product_old = main_product_builder(
        name='main_product_old',
        abc_service_id=435,
    )
    umbrella_123 = umbrella_builder(
        name='name_to_change',
        issue_key='GOALZ-123',
        main_product=main_product_123,
    )
    umbrella_223 = umbrella_builder(
        name='223',
        issue_key='GOALZ-223',
        main_product=main_product_123,
    )
    umbrella_old = umbrella_builder(
        name='OLD',
        issue_key='GOALZ-OLD',
        main_product=main_product_old,
    )

    person_123 = person_builder(login='person_123')
    person_777 = person_builder(login='person_777')
    person_vs_only = person_builder(login='person_vs_only')
    person_vs_without_umbrellas = person_builder(
        login='person_vs_without_umbrellas',
    )

    umbrella_eng_to_change_date_to = umbrella_person_builder(
        person=person_123,
        engagement=60,
        umbrella=umbrella_123,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=future_date,
    )
    umbrella_eng_not_active = umbrella_person_builder(
        person=person_123,
        engagement=60,
        umbrella=umbrella_123,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=past_date,
    )
    umbrella_eng_to_left_asis = umbrella_person_builder(
        person=person_123,
        engagement=1,
        umbrella=umbrella_223,
        engaged_from=ENGAGED_FROM_DT,
    )
    umbrella_eng_to_finish = umbrella_person_builder(
        person=person_123,
        engagement=100,
        umbrella=umbrella_223,
        engaged_from=datetimes.shifted(ENGAGED_FROM_DT, days=-1),
    )

    main_product_eng_to_finish = main_product_person_builder(
        person=person_123,
        main_product=main_product_old,
        engaged_from=ENGAGED_FROM_DT,
    )
    main_product_eng_to_left_asis = main_product_person_builder(
        person=person_vs_only,
        main_product=main_product_123,
        engaged_from=ENGAGED_FROM_DT,
    )
    main_product_eng_not_active = main_product_person_builder(
        person=person_vs_only,
        main_product=main_product_123,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=past_date,
    )

    return {
        'future_date': future_date,
        'past_date': past_date,
        'umbrella_123': umbrella_123,
        'umbrella_223': umbrella_223,
        'umbrella_old': umbrella_old,
        'main_product_123': main_product_123,
        'main_product_old': main_product_old,
        'person_123': person_123,
        'person_vs_only': person_vs_only,
        'person_777': person_777,
        'person_vs_without_umbrellas': person_vs_without_umbrellas,
        'umbrella_eng_to_change_date_to': umbrella_eng_to_change_date_to,
        'umbrella_eng_not_active': umbrella_eng_not_active,
        'umbrella_eng_to_left_asis': umbrella_eng_to_left_asis,
        'umbrella_eng_to_finish': umbrella_eng_to_finish,
        'main_product_eng_to_finish': main_product_eng_to_finish,
        'main_product_eng_to_left_asis': main_product_eng_to_left_asis,
        'main_product_eng_not_active': main_product_eng_not_active,
    }


def test_sync(db_state):
    conn_path = 'review.gradient.tasks.StaffConnector'
    tasks.settings.GRADIENT_SYNC_CHUNK_SIZE = 1
    get_persons_path = 'review.gradient.tasks._get_persons_from_db'
    person_123 = db_state['person_123']
    person_777 = db_state['person_777']
    person_vs_only = db_state['person_vs_only']
    person_vs_without_umbrellas = db_state['person_vs_without_umbrellas']
    persons_mock = {
        it.login: it.id
        for it in (
            person_123,
            person_777,
            person_vs_only,
            person_vs_without_umbrellas,
        )
    }

    now = datetimes.now()
    with mock.patch('review.lib.datetimes.now', return_value=now):
        with mock.patch(get_persons_path, return_value=persons_mock):
            with mock.patch(conn_path, return_value=ConnectorMock()):
                tasks.sync_vs_and_umbrellas()

    main_product_123 = db_state['main_product_123']
    main_product_123.refresh_from_db()
    assert main_product_123.name == 'VS 123'

    main_product_777_q = models.MainProduct.objects.filter(
        name='VS 777',
        abc_service_id=777,
    )
    assert main_product_777_q.count() == 1
    main_product_777 = main_product_777_q.get()

    main_product_old_q = models.MainProduct.objects.filter(
        name=db_state['main_product_old'].name,
        abc_service_id=db_state['main_product_old'].abc_service_id,
        id=db_state['main_product_old'].id,
    )
    assert main_product_old_q.count() == 1
    main_product_old = main_product_old_q.get()

    main_product_no_umbrella_q = models.MainProduct.objects.filter(
        name='no_umbrella',
        abc_service_id=100500,
    )
    assert main_product_no_umbrella_q.count() == 1
    main_product_no_umbrella = main_product_no_umbrella_q.get()

    assert models.MainProduct.objects.count() == 4

    umbrella_123 = db_state['umbrella_123']
    umbrella_223 = db_state['umbrella_223']

    umbrella_123.refresh_from_db()
    umbrella_223.refresh_from_db()

    assert db_state['umbrella_123'].issue_key == 'GOALZ-123'
    assert db_state['umbrella_123'].name == '123'
    assert db_state['umbrella_123'].main_product == main_product_123

    assert db_state['umbrella_223'].issue_key == 'GOALZ-223'
    assert db_state['umbrella_223'].name == '223'
    assert db_state['umbrella_223'].main_product == main_product_123

    umbrella_old = db_state['umbrella_old']
    umbrella_old_refreshed = models.Umbrella.objects.get(id=umbrella_old.id)
    assert umbrella_old_refreshed.issue_key == umbrella_old.issue_key
    assert umbrella_old_refreshed.name == umbrella_old.name
    assert umbrella_old_refreshed.main_product == umbrella_old.main_product

    umbrerlla_none_q = models.Umbrella.objects.filter(main_product_id=None)
    assert umbrerlla_none_q.count() == 1
    umbrerlla_none = umbrerlla_none_q.get()

    umbrella_777_q = models.Umbrella.objects.filter(
        issue_key='GOALZ-777',
        name='777',
        main_product=main_product_777,
    )
    assert umbrella_777_q.count() == 1
    umbrella_777 = umbrella_777_q.get()

    assert models.Umbrella.objects.count() == 5

    assert models.UmbrellaPerson.objects.filter(
        person=person_123,
        umbrella=umbrella_123,
        engagement=51,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=None,
    ).count() == 1
    assert models.UmbrellaPerson.objects.filter(
        person=person_123,
        umbrella=umbrerlla_none,
        engagement=48,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=None,
    ).count() == 1
    assert models.UmbrellaPerson.objects.filter(
        person=person_123,
        umbrella=umbrella_123,
        engagement=60,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=db_state['past_date'],
    ).count() == 1
    assert models.UmbrellaPerson.objects.filter(
        person=person_123,
        umbrella=umbrella_223,
        engagement=1,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=None,
    ).count() == 1
    assert models.UmbrellaPerson.objects.filter(
        person=person_123,
        engagement=100,
        umbrella=umbrella_223,
        engaged_from=datetimes.shifted(ENGAGED_FROM_DT, days=-1),
        engaged_to=ENGAGED_FROM_DT,
    ).count() == 1
    assert models.UmbrellaPerson.objects.filter(
        person=person_777,
        engagement=100,
        umbrella=umbrella_777,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=None,
    ).count() == 1

    assert models.UmbrellaPerson.objects.count() == 6

    assert models.MainProductPerson.objects.filter(
        person=person_123,
        main_product=main_product_old,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=now,
    ).count() == 1
    assert models.MainProductPerson.objects.filter(
        person=person_123,
        main_product=main_product_123,
        engaged_from=now,
        engaged_to=None,
    ).count() == 1
    assert models.MainProductPerson.objects.filter(
        person=person_vs_only,
        main_product=main_product_123,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=None,
    ).count() == 1
    assert models.MainProductPerson.objects.filter(
        person=person_vs_only,
        main_product=main_product_123,
        engaged_from=ENGAGED_FROM_DT,
        engaged_to=db_state['past_date'],
    ).count() == 1
    assert models.MainProductPerson.objects.filter(
        person=person_777,
        main_product=main_product_777,
        engaged_from=now,
        engaged_to=None,
    ).count() == 1
    assert models.MainProductPerson.objects.filter(
        person=person_vs_without_umbrellas,
        main_product=main_product_no_umbrella,
        engaged_from=now,
        engaged_to=None,
    ).count() == 1

    assert models.MainProductPerson.objects.count() == 6
