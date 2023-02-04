import pytest

from plan.resources.importers import bot
from plan.resources.importers.external_resource_data import ExternalResourceDataError

bot_data = {
    'instance_number': '434167',
    'segment4': 'SRV',
    'segment3': 'SERVERS',
    'segment2': 'XEON5645',
    'segment1': 'SM/SYS6016TNTF/4T3.5/1U/1P',
    'fqdn': 'jenkinsfol.qa.yandex.net',
    'location': 'RU>MOW>FOL>FOL-3>33>24>-',
}

bot_data_with_link = {
    'instance_number': '434167',
    'segment4': 'SRV',
    'segment3': 'SERVERS',
    'segment2': 'XEON5645',
    'segment1': 'SM/SYS6016TNTF/4T3.5/1U/1P',
    'link': 'yandex.ru',
    'fqdn': 'jenkinsfol.qa.yandex.net',
    'location': 'RU>MOW>FOL>FOL-3>33>24>-',
}

broken_bot_data = {
    'segment4': 'SRV',
    'segment3': 'SERVERS',
    'segment1': 'SM/SYS6016TNTF/4T3.5/1U/1P',
}


def test_external_id():
    server = bot.BotServer(bot_data)
    assert server.external_id == '434167'


def test_name():
    server = bot.BotServer(bot_data)
    assert server.type_name == 'SRV.SERVERS'


def test_type_name():
    server = bot.BotServer(bot_data)
    assert server.name == 'XEON5645/SM/SYS6016TNTF/4T3.5/1U/1P'


def test_attributes():
    server = bot.BotServer(bot_data)
    assert server.attributes == {
        'fqdn': 'jenkinsfol.qa.yandex.net',
        'location': 'RU>MOW>FOL>FOL-3>33>24>-'
    }


def test_no_attribute_field():
    server = bot.BotServer(bot_data_with_link)
    assert server.link
    assert 'link' not in server.attributes


def test_init_check():
    with pytest.raises(ExternalResourceDataError):
        bot.BotServer(broken_bot_data)
