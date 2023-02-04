import random

import pytest

from sqlalchemy.exc import IntegrityError
from datetime import datetime, timedelta


from ads.watchman.timeline.api.lib.modules.events import db, dao, errors, models, enum_types, resource_manager
from ads.watchman.timeline.api.lib.common import models as co_models

import ads.watchman.timeline.api.tests.helpers.model_generators as model_generators


class EnumItemTestContext(co_models.DataObject):
    def __init__(self, enum_type, generator, dao_getter_name):
        self.enum_type = enum_type
        self.generator = generator
        self.dao_getter_name = dao_getter_name

PAGE_TYPE_ENUM = EnumItemTestContext(
    enum_type=enum_types.TimelineDBEnums.page_type,
    generator=model_generators.TestModelGenerator.create_tree_enum_node,
    dao_getter_name='get_page_types'
)

GEO_TYPE_ENUM = EnumItemTestContext(
    enum_type=enum_types.TimelineDBEnums.geo_type,
    generator=model_generators.TestModelGenerator.create_geo_type,
    dao_getter_name='get_geo_types'
)

PRODUCT_TYPE_ENUM = EnumItemTestContext(
    enum_type=enum_types.TimelineDBEnums.product_type,
    generator=model_generators.TestModelGenerator.create_tree_enum_node,
    dao_getter_name='get_product_types'
)

SOURCE_TYPE_ENUM = EnumItemTestContext(
    enum_type=enum_types.TimelineDBEnums.source_type,
    generator=model_generators.TestModelGenerator.create_enum_item,
    dao_getter_name='get_source_types'
)

TREE_ENUMS = [PAGE_TYPE_ENUM, GEO_TYPE_ENUM, PRODUCT_TYPE_ENUM]

SIMPLE_ENUMS = [SOURCE_TYPE_ENUM]


ALL_ENUMS = TREE_ENUMS + SIMPLE_ENUMS
FKEY_ENUMS = [PAGE_TYPE_ENUM, GEO_TYPE_ENUM, SOURCE_TYPE_ENUM]


def get_enum_ids(enums):
    return [e.enum_type.dbo_class.__name__ for e in enums]


def add_event_to_db(event_model, session):
    event_dbo = dao.EventDBOConverter.to_dbo(event_model)
    session.add(event_dbo)
    session.commit()
    return event_dbo


def add_enum_item_to_db(enum_model, enum_dbo_class, session):
    enum_dbo = dao.EnumDBOConverter(enum_dbo_class).to_dbo(enum_model)
    session.add(enum_dbo)
    session.commit()
    return enum_dbo


class DBWithEnums(object):
    """
    There is no checks of parentname and geo_id retrieval
    Better way is make explicit creation of every enum type
    """
    def __init__(self, session):
        self.enum_param_names = {}
        r_manager = resource_manager.ResourceManager()
        for enum_test_context in ALL_ENUMS:
            enum_names = r_manager.get_names(enum_test_context.enum_type.name)
            self.enum_param_names[enum_test_context.enum_type.name] = enum_names
            for enum_name in enum_names:
                enum_item = enum_test_context.generator(name=enum_name)
                add_enum_item_to_db(enum_item, enum_test_context.enum_type.dbo_class, session)
        self.enum_param_names['event_type'] = list(models.EventType)
        self.enum_param_names['duration_type'] = list(models.DurationType)

    def generate_event(self, event_id, start_time=None, end_time=None, ticket=None, event_type=None,
                       duration_type=None, geo_type=None, page_type=None, source_type=None, product_type=None):

        geo_type = geo_type or random.choice(self.enum_param_names["geo_type"])
        page_type = page_type or random.choice(self.enum_param_names["page_type"])
        source_type = source_type or random.choice(self.enum_param_names["source_type"])
        product_type = product_type or random.sample(self.enum_param_names["product_type"], 2)

        return model_generators.TestModelGenerator.create_event(event_id=event_id,
                                                                             ticket=ticket,
                                                                             start_time=start_time,
                                                                             end_time=end_time,
                                                                             geo_type=geo_type,
                                                                             duration_type=duration_type,
                                                                             page_type=page_type,
                                                                             source_type=source_type,
                                                                             product_type=product_type,
                                                                             event_type=event_type)


def test_get_event_by_id_returns_valid_event(db_session):
    db_env = DBWithEnums(db_session)

    event = db_env.generate_event(event_id=1)
    add_event_to_db(event, db_session)

    sql_dao = dao.SqlDao(db_session)

    assert sql_dao.get_event_by_id(event.event_id) == event


def test_put_event_stores_event_in_db(db_session):
    db_env = DBWithEnums(db_session)

    event = db_env.generate_event(event_id=1)
    sql_dao = dao.SqlDao(db_session)
    sql_dao.put_event(event)

    restored_event_dbo = db_session.query(db.EventDBO).filter_by(id=event.event_id).first()
    restored_event = dao.EventDBOConverter().to_model(restored_event_dbo)

    assert restored_event.metrics.__dict__ == event.metrics.__dict__

    assert event == restored_event


def test_put_event_returns_event_reference_with_id(db_session):
    db_env = DBWithEnums(db_session)

    event = db_env.generate_event(event_id=1)
    sql_dao = dao.SqlDao(db_session)
    assert models.EventReference(event.event_id) == sql_dao.put_event(event)


def test_put_event_without_id_returns_reference_with_id(db_session):
    db_env = DBWithEnums(db_session)

    event = db_env.generate_event(event_id=None)
    sql_dao = dao.SqlDao(db_session)
    event_ref = sql_dao.put_event(event)

    event_id = db_session.query(db.EventDBO).first().id
    assert models.EventReference(event_id) == event_ref


def test_dao_raise_error_when_put_two_event_with_same_id(db_session):
    db_env = DBWithEnums(db_session)
    event = db_env.generate_event(event_id=1)
    sql_dao = dao.SqlDao(db_session)
    sql_dao.put_event(event)

    with pytest.raises(errors.TimelineError):
        sql_dao.put_event(event)
    db_session.rollback()


def test_db_raise_error_when_add_two_events_with_same_id(db_session):
    db_env = DBWithEnums(db_session)
    event = db_env.generate_event(event_id=1)
    add_event_to_db(event, db_session)

    with pytest.raises(IntegrityError):
        add_event_to_db(event, db_session)
    db_session.rollback()


def test_db_raise_error_when_add_two_fiasco_events_with_same_ticket(db_session):
    # TODO: make test more explicit
    db_env = DBWithEnums(db_session)
    event = db_env.generate_event(event_id=1, ticket="test", event_type=models.EventType.fiasco)

    add_event_to_db(event, db_session)

    with pytest.raises(IntegrityError):
        event.event_id = 2
        add_event_to_db(event, db_session)
    db_session.rollback()


def test_db_can_add_two_not_fiasco_events_with_ticket(db_session):
    # TODO: make test more explicit and general
    db_env = DBWithEnums(db_session)
    event = db_env.generate_event(event_id=1)
    event.ticket = "test"
    event.event_type = models.EventType.holiday

    add_event_to_db(event, db_session)

    event.start_time -= timedelta(10)

    try:
        event.event_id = 2
        add_event_to_db(event, db_session)
    except IntegrityError:
        pytest.fail("Integrity error on holiday events with same tickets")
        db_session.rollback()


def test_adding_geo_type_with_same_geo_id_raises_exception(db_session):
    geo_type = model_generators.TestModelGenerator.create_geo_type(name=u"geo_1", geo_id=1)
    add_enum_item_to_db(geo_type, db.GeoTypeDBO, db_session)

    geo_type = model_generators.TestModelGenerator.create_geo_type(name=u"geo_2", geo_id=1)
    with pytest.raises(IntegrityError):
        add_enum_item_to_db(geo_type, db.GeoTypeDBO, db_session)

    db_session.rollback()


def test_raise_exception_get_on_nonexistent_event(db_session):
    sql_dao = dao.SqlDao(db_session)
    with pytest.raises(errors.TimelineError):
        sql_dao.get_event_by_id(1)
    db_session.rollback()


def test_dao_get_events_returns_all_events_if_filters_not_set(db_session):
    db_env = DBWithEnums(db_session)

    expected_events = set()
    for i in xrange(10):
        event = db_env.generate_event(event_id=i)
        add_event_to_db(event, db_session)
        expected_events.add(event)

    sql_dao = dao.SqlDao(db_session)
    assert expected_events == set(sql_dao.get_events(models.Filter()))


def test_dao_get_events_start_time_filter(db_session):
    # ToDo: generalize time filtering tests
    db_env = DBWithEnums(db_session)
    start_time = datetime(2017, 1, 1)

    not_expected_events = [
        db_env.generate_event(
            event_id=1,
            start_time=start_time - timedelta(days=25),
            end_time=start_time - timedelta(days=20)
        )
    ]

    expected_events = [
        db_env.generate_event(event_id=2, start_time=start_time - timedelta(10), end_time=start_time),
        db_env.generate_event(event_id=3, start_time=start_time,  end_time=start_time + timedelta(20)),
        db_env.generate_event(event_id=4, start_time=start_time + timedelta(10),  end_time=start_time + timedelta(20))
    ]

    for e in not_expected_events + expected_events:
        add_event_to_db(e, db_session)

    sql_dao = dao.SqlDao(db_session)

    assert set(expected_events) == set(sql_dao.get_events(models.Filter(start_time=start_time)))


def test_dao_get_events_end_time_filter(db_session):
    db_env = DBWithEnums(db_session)
    end_time = datetime(2017, 1, 1)

    not_expected_events = [
        db_env.generate_event(
            event_id=1,
            start_time=end_time + timedelta(days=20),
            end_time=end_time + timedelta(days=25)
        )
    ]

    expected_events = [
        db_env.generate_event(event_id=2, start_time=end_time - timedelta(10), end_time=end_time),
        db_env.generate_event(event_id=3, start_time=end_time,  end_time=end_time + timedelta(20)),
        db_env.generate_event(event_id=4, start_time=end_time - timedelta(20),  end_time=end_time - timedelta(10))
    ]

    for e in not_expected_events + expected_events:
        add_event_to_db(e, db_session)

    sql_dao = dao.SqlDao(db_session)

    assert set(expected_events) == set(sql_dao.get_events(models.Filter(end_time=end_time)))


def test_dao_get_events_start_and_end_time_filter(db_session):
    db_env = DBWithEnums(db_session)

    start_time = datetime(2017, 1, 1)
    end_time = datetime(2017, 1, 10)

    not_expected_events = [
        db_env.generate_event(
            event_id=1,
            start_time=end_time + timedelta(days=10),
            end_time=end_time + timedelta(days=25)
        ),
        db_env.generate_event(
            event_id=2,
            start_time=start_time - timedelta(days=25),
            end_time=start_time - timedelta(days=10)
        )
    ]

    expected_events = [
        db_env.generate_event(event_id=3, start_time=start_time - timedelta(10), end_time=start_time),
        db_env.generate_event(event_id=4, start_time=end_time, end_time=end_time + timedelta(10)),
        db_env.generate_event(event_id=5, start_time=start_time + timedelta(1), end_time=end_time - timedelta(1)),
        db_env.generate_event(event_id=6, start_time=start_time - timedelta(1), end_time=end_time + timedelta(1)),

    ]

    for e in not_expected_events + expected_events:
        add_event_to_db(e, db_session)

    sql_dao = dao.SqlDao(db_session)

    assert set(expected_events) == set(sql_dao.get_events(models.Filter(start_time=start_time, end_time=end_time)))


ENUM_TYPES = ["page_type", "event_type", "source_type", "product_type", "page_type", "geo_type", "duration_type"]


@pytest.mark.parametrize("enum_type", ENUM_TYPES, ids=ENUM_TYPES)
def test_dao_get_events_enum_type_filter(enum_type, db_session):
    db_env = DBWithEnums(db_session)

    events = [db_env.generate_event(event_id=i) for i in xrange(20)]
    sql_dao = dao.SqlDao(db_session)

    for e in events:
        sql_dao.put_event(e)

    list_of_created_enum_type = random.sample(db_env.enum_param_names[enum_type], 2)

    # Magic for merge checks of db-enum and code-enum attributes
    # TODO: remove this magic
    def type_magic(value):
        return value if isinstance(value, frozenset) else [value]

    expected_events = [e for e in events if set(type_magic(getattr(e, enum_type))) & set(list_of_created_enum_type)]
    list_of_created_enum_name = [getattr(e, 'name', e) for e in list_of_created_enum_type]
    fltr = models.Filter(**{u"{}_list".format(enum_type): list_of_created_enum_name})
    assert set(expected_events) == set(sql_dao.get_events(fltr))


@pytest.mark.parametrize("conflicting_event_params, dao_put_method",
                         [
                             (
                                 dict(start_time=datetime(2017, 1, 1),
                                      geo_type=resource_manager.ResourceManager().get_names(enum_types.TimelineDBEnums.geo_type.name)[0],
                                      event_type=models.EventType.holiday),
                                 dao.SqlDao.put_holiday
                             ),
                             (
                                 dict(ticket='same_old_ticket', event_type=models.EventType.fiasco),
                                 dao.SqlDao.put_fiasco
                             ),
                         ], ids=["put_holiday", "put_fiasco"])
def test_upsert_happens_when_update_event_on_conflict(conflicting_event_params, dao_put_method, db_session):
    db_env = DBWithEnums(db_session)
    event = db_env.generate_event(event_id=None, **conflicting_event_params)

    sql_dao = dao.SqlDao(db_session)
    event.event_id = dao_put_method(sql_dao, event).event_id

    assert event == sql_dao.get_event_by_id(event.event_id)

    other_event = db_env.generate_event(event_id=event.event_id,
                                        end_time=event.end_time + timedelta(1),
                                        **conflicting_event_params)

    assert other_event != event

    dao_put_method(sql_dao, other_event)
    assert other_event == sql_dao.get_event_by_id(event.event_id)


def test_insert_happens_when_insert_for_multiple_geo_types(db_session):
    db_env = DBWithEnums(db_session)
    sql_dao = dao.SqlDao(db_session)
    date = datetime(2017, 1, 1)

    for geo_type_name in db_env.enum_param_names["geo_type"]:
        event = db_env.generate_event(event_id=None, start_time=date, event_type=models.EventType.holiday, geo_type=geo_type_name)
        event.event_id = sql_dao.put_holiday(event).event_id
        assert event == sql_dao.get_event_by_id(event.event_id)

    assert len(sql_dao.get_events(models.Filter())) == len(db_env.enum_param_names["geo_type"])


@pytest.mark.parametrize("enum_test_context", FKEY_ENUMS, ids=get_enum_ids(FKEY_ENUMS))
def test_event_slice_updates_on_renaming(enum_test_context, db_session):
    """
    :type enum_test_context: EnumItemTestContext
    :param db_session:
    :return:
    """
    db_env = DBWithEnums(db_session)

    old_enum_name = db_env.enum_param_names[enum_test_context.enum_type.name][0] + u"_old"
    new_enum_name = db_env.enum_param_names[enum_test_context.enum_type.name][0] + u"_new"

    enum_item = enum_test_context.generator(old_enum_name)
    enum_dbo = add_enum_item_to_db(enum_item, enum_test_context.enum_type.dbo_class, db_session)

    event = db_env.generate_event(event_id=None, **{enum_test_context.enum_type.name: enum_item.name})
    event_dbo = add_event_to_db(event, db_session)

    sql_dao = dao.SqlDao(db_session)
    assert getattr(sql_dao.get_event_by_id(event_dbo.id), enum_test_context.enum_type.name) == old_enum_name

    enum_dbo.name = new_enum_name
    db_session.commit()
    assert getattr(sql_dao.get_event_by_id(event_dbo.id), enum_test_context.enum_type.name) == new_enum_name


@pytest.mark.parametrize("enum_test_context", ALL_ENUMS, ids=get_enum_ids(ALL_ENUMS))
def test_get_type_methods_return_valid_models(enum_test_context, db_session):
    """
    :type enum_test_context: EnumItemTestContext
    :param db_session:
    :return:
    """
    expected_enum_items = set()

    for i in xrange(10):
        test_enum = enum_test_context.generator(name=u"test_enum_{}".format(i))
        add_enum_item_to_db(test_enum, enum_test_context.enum_type.dbo_class, db_session)
        expected_enum_items.add(test_enum)

    sql_dao = dao.SqlDao(db_session)
    restored_enum_items = set(getattr(sql_dao, enum_test_context.dao_getter_name)())

    assert expected_enum_items == restored_enum_items


@pytest.mark.parametrize("enum_test_context", ALL_ENUMS, ids=get_enum_ids(ALL_ENUMS))
def test_adding_two_enums_with_same_name_raises_error(enum_test_context, db_session):
    """
    :type enum_test_context: EnumItemTestContext
    :param db_session:
    :return:
    """
    test_enum = enum_test_context.generator(name=u"test_enum")
    add_enum_item_to_db(test_enum, enum_test_context.enum_type.dbo_class, db_session)

    with pytest.raises(IntegrityError):
        add_enum_item_to_db(test_enum, enum_test_context.enum_type.dbo_class, db_session)

    db_session.rollback()


@pytest.mark.parametrize("enum_test_context", TREE_ENUMS, ids=get_enum_ids(TREE_ENUMS))
def test_can_add_and_get_tree_enum_with_parent_name(enum_test_context, db_session):
    """
    :type enum_test_context: EnumItemTestContext
    :param db_session:
    :return:
    """
    first_enum = enum_test_context.generator(name=u"test_enum_1")
    add_enum_item_to_db(first_enum, enum_test_context.enum_type.dbo_class, db_session)

    second_enum = enum_test_context.generator(name="test_enum_2", parent_name=first_enum.name)
    add_enum_item_to_db(second_enum, enum_test_context.enum_type.dbo_class, db_session)

    sql_dao = dao.SqlDao(db_session)
    restored_enum_items = set(getattr(sql_dao, enum_test_context.dao_getter_name)())
    assert {first_enum, second_enum} == restored_enum_items


@pytest.mark.parametrize("enum_test_context", TREE_ENUMS, ids=get_enum_ids(TREE_ENUMS))
def test_raise_error_on_add_tree_enum_item_with_nonexistent_parent_name(enum_test_context, db_session):
    """
    :type enum_test_context: EnumItemTestContext
    :param db_session:
    :return:
    """
    test_enum = enum_test_context.generator(name=u"test_enum", parent_name=u"any_name")

    with pytest.raises(IntegrityError):
        add_enum_item_to_db(test_enum, enum_test_context.enum_type.dbo_class, db_session)

    db_session.rollback()
