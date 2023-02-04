# -*- coding: utf-8 -*-
from balance.xmlizer import getxmlizer
import tests.object_builder as ob
import pytest


DATETIME_FORMAT = '%Y-%m-%dT%H:%M:%S'


@pytest.fixture
def person(session):
    return ob.PersonBuilder.construct(session)


def test_show_update_by_update_dt_if_extprops_param_was_updated(session, person):
    old_value = 'old_value'
    new_value = 'new_value'

    person.local_city = old_value
    session.flush()

    person.local_city = new_value
    session.flush()

    xml_res = getxmlizer(person).xmlize_detailed()
    node_local_city = xml_res.find('local-city')

    # check that value was changed
    assert node_local_city.text == new_value

    assert node_local_city.attrib['update-dt'] == person.extprops_collection_Person_local_city[0].update_dt.strftime(
        DATETIME_FORMAT
    )
    assert node_local_city.attrib['update-by'] == person.extprops_collection_Person_local_city[0].passport.gecos


def test_show_update_dt_update_by_if_attribute_value_was_updated(session, person):
    delivery_type_old = 1
    delivery_type_new = 2
    person.delivery_type = delivery_type_old
    session.flush()

    person.delivery_type = delivery_type_new
    session.flush()

    xml_res = getxmlizer(person).xmlize_detailed()
    node_delivery_type = xml_res.find('delivery-type')

    # check that value was changed
    assert int(node_delivery_type.text) == delivery_type_new

    for attribute in person.attributes:
        if attribute.code == 'DELIVERY_TYPE':
            assert node_delivery_type.attrib['update-dt'] == attribute.update_dt.strftime(DATETIME_FORMAT)
            assert node_delivery_type.attrib['update-by'] == attribute.passport.gecos
            break
    else:
        assert False
