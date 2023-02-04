# coding=utf-8

import pytest

from butils.application.logger import get_logger
from balance import mapper, constants
from balance.utils.sql_alchemy import is_attribute_loaded

from tests import object_builder as ob

log = get_logger()


@pytest.mark.parametrize(
    [
        'builder_class'
    ],
    [
        (ob.ClientBuilder, ),
        (ob.PersonBuilder, ),
    ]
)
@pytest.mark.parametrize(
    # exists = True - insert object, expect BY_FILE in exportable
    # exists = False - do not insert object, do not expect BY_FILE in exportable
    [
        'exists'
    ],
    [
        (True, ),
        (False, ),
    ]
)
def test_object_is_in_db(builder_class, exists, session):
    obj = builder_class.construct(session)
    if exists:
        obj.enqueue('BY_FILE', force=True)
    session.flush()
    session.expire(obj)
    session.clear_cache()
    log.info('Session cache has been cleared')
    assert ('BY_FILE' in obj.exportable) is exists
    # BALANCE-31798 regression check
    assert not is_attribute_loaded(obj, 'exports_rel')
    log.info('Second check, cache must be used')
    assert ('BY_FILE' in obj.exportable) is exists


@pytest.mark.parametrize(
    # exists = True - insert object, expect BY_FILE in exportable
    # exists = False - do not insert object, do not expect BY_FILE in exportable
    [
        'exists'
    ],
    [
        (True, ),
        (False, ),
    ]
)
def test_exports_relation_is_loaded(session, exists):
    client = ob.ClientBuilder.construct(session)
    if exists:
        client.enqueue('BY_FILE', force=True)
        session.flush()
    session.clear_cache()
    assert is_attribute_loaded(client, 'exports_rel')
    assert ('BY_FILE' in client.exportable) is exists


@pytest.mark.parametrize(
    # is_by_file_exportable = True - enqueue object, expect BY_FILE in exportable
    # is_by_file_exportable = False - do not enqueue object, do not expect BY_FILE in exportable
    [
        'is_by_file_exportable'
    ],
    [
        (True, ),
        (False, ),
    ]
)
def test_client_does_not_has_id(is_by_file_exportable):
    client = mapper.Client()
    if is_by_file_exportable:
        client.enqueue('BY_FILE', force=True)
    assert client.id is None
    assert ('BY_FILE' in client.exportable) is is_by_file_exportable


@pytest.mark.parametrize(
    # is_by_file_exportable = True - enqueue object, expect BY_FILE in exportable
    # is_by_file_exportable = False - do not enqueue object, do not expect BY_FILE in exportable
    [
        'is_by_file_exportable'
    ],
    [
        (True, ),
        (False, ),
    ]
)
def test_person_does_not_has_id(is_by_file_exportable, session):
    client = ob.ClientBuilder.construct(session)
    person = mapper.Person(client, constants.PersonCategoryCodes.russia_resident_individual)

    if is_by_file_exportable:
        person.enqueue('BY_FILE', force=True)

        # We need to manually invalidate cache after enqueue, because there is no logic,
        # that would do it. It is not a problem, because there are no such use cases
        # in the project for this export type.
        session.clear_cache()

    assert person.id is None
    assert ('BY_FILE' in person.exportable) is is_by_file_exportable
