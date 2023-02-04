import os

import xmltodict
import pytest
import yatest


def get_xml_dir():
    return yatest.common.source_path('intranet/vconf/tests/cdr/messages')


def load_xml(name):
    with open(os.path.join(get_xml_dir(), name), 'r') as f:
        xml = f.read()
    return xml


def parse_message(message):
    return xmltodict.parse(message)


@pytest.fixture()
def messages():
    messages = []
    for file_name in os.listdir(get_xml_dir()):
        messages.append(load_xml(file_name))
    return messages


@pytest.fixture()
def sip_participant_join_record():
    xml = load_xml('sip_call_leg_start')
    return parse_message(xml)['records']['record']


@pytest.fixture()
def cms_participant_join_record():
    xml = load_xml('cms_call_leg_start')
    return parse_message(xml)['records']['record']
