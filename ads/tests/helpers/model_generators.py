# coding=utf-8

import random
import time
import datetime
import string

from ads.watchman.timeline.api.lib.modules.events import models, dao, schemas, errors, enum_types, resource_manager
from ads.watchman.timeline.api.lib.calendar import models as calendar_models

from ads.watchman.timeline.api.lib import config


class HumanFriendlyEventGenerator(object):
    DESCRIPTION_POOLS = {
        models.EventType.holiday: [
            [u"мирный", u"", u"международный", u"всероссийский"],
            [u"день", u"праздник", u"юбилей"],
            [u"святого", u"", u"большого", u"великого"],
            [u"студента", u"снега", u"радиотехника", u"хлеба", u"ковра"]
        ],
        models.EventType.fiasco: [
            [u"Упал", u"Сломался", u"Выкатили", u"Заработал", u"Перестал работать", u"Активировали"],
            [u"новый", u"улучшеный", u"экспериментальный", u""],
            [u"движок,", u"фикс,", u"поведенческий,", u"эксперимент,", u"поиск,", u"бродматч,", u"релиз поиска,",
             u"ваншот,", u"лейбл,"],
            [u"который", u"который внезапно"],
            [u"стал вырезать нашу рекламу на серпе.", u"стал терять запросы.", u"выключил рекламу.",
             u"терял логи."],
            [u"Потеряли"],
            [u"10%", u"1%", u"100 1000", u"80 000", u"250 000", u"5%", u"7%", u"25%"],
            [u"фишек.", u"кликов.", u"денег.", u"показов."]
        ]
    }

    OWNER_POOL = [
        [u"Бандерлог", u"Боромир", u"Баклажан", u"Буерак", u"Бадминтон"],
        [u"Джоникэш", u"Корвалол", u"Кабачок", u"Комбикорм", u"Коленвал"]
    ]

    @staticmethod
    def _generate_phrase(pool):
        phrase = []
        for word_list in pool:
            phrase.append(random.choice(word_list))
        phrase = " ".join(phrase).replace("  ", " ")    # Word scipped with "" token that gives us two spaces
        return phrase

    def __init__(self, session):
        super(HumanFriendlyEventGenerator, self).__init__()

        db_dao = dao.SqlDao(session)

        self._enum_dict = {
            enum_type: [item.name for item in enum_type.dao_getter(db_dao)]
            for enum_type in list(enum_types.TimelineDBEnums)
        }

        print self._enum_dict

    def generate_event(self):
        enum_items = {
            enum_type.name: random.choice(items) for enum_type, items in self._enum_dict.iteritems()
        }

        owners = [self._generate_phrase(self.OWNER_POOL) for _ in xrange(random.randint(0, 3))]
        event = TestModelGenerator.create_event(event_id=None, owners=owners, **enum_items)
        event.description.title = self._generate_phrase(self.DESCRIPTION_POOLS[event.event_type])
        return event


class TestModelGenerator(object):
    MIN_TIME = datetime.datetime(2016, 1, 1)
    MAX_TIME = datetime.datetime(2019, 1, 1)

    def __init__(self):
        self._id_counter = 0

    @staticmethod
    def generate_metrics(event_type):
        if event_type == models.EventType.fiasco:
            cost = random.randint(0, 100000) if random.random() < 0.5 else None
            cost_percents = random.randint(-100, 100) if random.random() < 0.5 else None
            clicks = random.randint(0, 100000) if random.random() < 0.5 else None
            clicks_percents = random.randint(-100, 100) if random.random() < 0.5 else None
            shows = random.randint(0, 100000) if random.random() < 0.5 else None
            shows_percents = random.randint(-100, 100) if random.random() < 0.5 else None
            return models.FiascoMetrics(cost=cost, cost_percents=cost_percents, clicks=clicks,
                                        clicks_percents=clicks_percents, shows=shows, shows_percents=shows_percents)
        elif event_type == models.EventType.holiday:
            return models.HolidayMetrics()
        elif event_type == models.EventType.bad_log:
            return models.BadLogMetrics()
        else:
            raise ValueError("Not supported event type {}".format(event_type.name))

    @staticmethod
    def get_schema_class(event_type):
        if event_type == models.EventType.fiasco:
            return schemas.FiascoEventSchema
        elif event_type == models.EventType.holiday:
            return schemas.HolidayEventSchema
        elif event_type == models.EventType.bad_log:
            return schemas.BadLogEventSchema
        else:
            raise ValueError("Not supported event type {}".format(event_type.name))

    @classmethod
    def create_event(cls, event_id=None, start_time=None, end_time=None, geo_type=None, page_type=None, ticket=None,
                     source_type=None, product_type=None, duration_type=None, event_type=None, owners=None):
        if start_time is None:
            start_time = cls.get_random_time()

        if end_time is None:
            end_time = start_time + datetime.timedelta(hours=random.randint(0, 100))

        duration_type = duration_type or random.choice(list(models.DurationType))
        event_type = event_type or random.choice(list(models.EventType))
        metrics = cls.generate_metrics(event_type)
        r_manager = resource_manager.ResourceManager()
        geo_type = geo_type or random.choice(r_manager.get_names('geo_type'))
        page_type = page_type or random.choice(r_manager.get_names('page_type'))
        source_type = source_type or random.choice(r_manager.get_names('source_type'))
        product_type = product_type or random.sample(r_manager.get_names('product_type'), 2)

        owners = owners or [cls._random_string() for _ in xrange(random.randint(0, 5))]
        description = models.Description(
            title=cls._random_string(),
            key_ticket=cls.or_none(cls._random_url()),
            images=[cls._random_url(), cls._random_url()]
        )

        metrics = metrics or cls.generate_metrics(event_type)
        ticket = ticket or cls._random_string()

        return models.Event(event_id=event_id,
                            start_time=start_time,
                            end_time=end_time,
                            event_type=event_type,
                            source_type=source_type,
                            description=description,
                            owners=owners,
                            duration_type=duration_type,
                            page_type=page_type,
                            geo_type=geo_type,
                            product_type=product_type,
                            metrics=metrics,
                            ticket=ticket)

    @classmethod
    def create_enum_item(cls, name=None, description=None):
        if name is None:
            name = cls._random_string()

        if description is None:
            description = cls._random_string()

        return models.EnumItem(
            name=name,
            description=description
        )

    @classmethod
    def or_none(cls, value, probability=0.1):
        return value if random.random() > probability else None

    @classmethod
    def create_tree_enum_node(cls, name=None, description=None, parent_name=None):
        if name is None:
            name = cls._random_string()
        if description is None:
            description = cls._random_string()

        return models.TreeEnumItem(
            name=name,
            description=description,
            parent_name=parent_name
        )

    @classmethod
    def create_geo_type(cls, name=None, description=None, parent_name=None, geo_id=None):

        if description is None:
            description = cls._random_string()

        r_manager = resource_manager.ResourceManager()
        name = name or random.choice(r_manager.get_names('geo_type'))

        return models.GeoType(
            name=name,
            description=description,
            parent_name=parent_name,
            geo_id=geo_id
        )

    @classmethod
    def _random_string(cls):
        return u''.join([random.choice(string.ascii_letters + string.digits) for _ in xrange(32)])

    @classmethod
    def _random_url(cls):
        return u'http://{}.ru'.format(cls._random_string())

    @classmethod
    def get_random_time(cls):
        return datetime.datetime.fromtimestamp(random.randint(
            int(time.mktime(cls.MIN_TIME.timetuple())),
            int(time.mktime(cls.MAX_TIME.timetuple())))
        )

    @classmethod
    def get_random_date(cls):
        return cls.get_random_time().date()

    @classmethod
    def create_holidays_params(cls, start_day=None, end_day=None, geo_type=None, out_mode=None):
        geo_type = geo_type or cls.create_geo_type()
        if start_day is None:
            start_day = cls.get_random_date()

        if end_day is None:
            end_day = start_day + datetime.timedelta(days=random.randint(0, 100))

        return calendar_models.HolidayParams(
            start_day=start_day,
            end_day=end_day,
            geo_type=geo_type)


class EventsStorage(object):
    EVENTS = [
        {
            u"id": 1,
            u"start_time": u"2017-11-13T18:00:04",
            u"end_time": u"2017-11-13T19:13:04",
            u"event_type": u"fiasco",
            u"source_type": u"FIASCO",
            u"ticket": u"BSDEV-12345",
            u"description": {
                u"title": u"I am broken"
            },
            u"owners": [
                u"ilariia", u"npytincev"
            ],
            u"duration_type": u"persistent",
            u"page_type": u"Yandex search",
            u"geo_type": u"Russia",
            u"product_type": u"direct",
            u"metrics": {
                u"effect_stats": {
                    u"cost": 100000
                }
            }
        }
    ]

    @classmethod
    def get_existed_event_ids(cls):
        return [event[u"id"] for event in cls.EVENTS]

    @classmethod
    def get_event_by_id(cls, event_id):
        return next(event for event in cls.EVENTS if event[u"id"] == event_id)

    @classmethod
    def get_not_existed_event_id(cls):
        return max(event[u"id"] for event in cls.EVENTS) + 1


class TestDao(dao.IDao):
    COUNTERS = {"put_event": 0, "put_holiday": 0, "put_fiasco": 0}

    def get_events(self, event_filter):
        # TODO: simplify
        if event_filter.event_type_list is None:
            yield TestModelGenerator.create_event(start_time=event_filter.start_time)
            yield TestModelGenerator.create_event(start_time=event_filter.end_time)
        else:
            for event_type in event_filter.event_type_list:
                yield TestModelGenerator.create_event(
                    start_time=event_filter.start_time,
                    event_type=event_type
                )
                yield TestModelGenerator.create_event(
                    start_time=event_filter.end_time,
                    event_type=event_type
                )

    def get_page_types(self):
        pass

    def get_source_types(self):
        pass

    def get_product_types(self):
        pass

    @staticmethod
    def get_geo_types():
        return [TestModelGenerator.create_geo_type(geo_id=i) for i in xrange(3)]

    def __init__(self, *args):
        pass

    def get_event_by_id(self, event_id):
        try:
            return schemas.EventWithIdSchema().load(EventsStorage.get_event_by_id(event_id)).data
        except Exception:
            raise errors.TimelineError(u"Bad")

    def put_event(self, event):
        """
        In real code it must be creation of other object, not muting current
        """
        event.event_id = EventsStorage.get_not_existed_event_id()
        self.COUNTERS["put_event"] += 1
        return event

    def put_fiasco(self, event):
        self.put_event(event)
        self.COUNTERS["put_fiasco"] += 1

    def put_holiday(self, event):
        self.put_event(event)
        self.COUNTERS["put_holiday"] += 1


def get_db_session_mock():
    return u"some_session"


class TestingConfig(config.Config):
    TESTING = True
    DAO_CLASS = TestDao
    DEPLOY_VERSION = "12345"
    DEPLOY_DESCRIPTION = "test description"

    def __init__(self):
        self.DAO_INIT = get_db_session_mock
