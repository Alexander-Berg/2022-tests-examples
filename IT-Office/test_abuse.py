import pytest
import paramiko
import source.abuse as abuse
from source.tests.data_abuse import *
from source.tests.fixtures_abuse import *


@pytest.mark.parametrize('item', test_data_field)
def test_get_ticket_field(item):
    assert abuse.get_ticket_field(
        item['tag_start'], item['tag_end'], item['string']) == item['result']


@pytest.mark.parametrize('log', test_data_str_parser)
def test_log_str_parser(log):
    assert isinstance(abuse.log_str_parser(log), list)

#TODO Need fix this tests
#def test_check_abuse(check_abuse):
#    assert abuse.start_check_abuse() == test_true_result
