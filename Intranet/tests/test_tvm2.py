import json
import pytest

from tvmauth.mock import TvmClientPatcher

import intranet.compositor_processors.src.commands as commands_library
from intranet.compositor_processors.src.logic.decorators import nirvana_command
from intranet.compositor_processors.src.logic.tvm2_client import (
    get_service_ticket,
    TVM2Error,
)

from intranet.compositor_processors.src.settings import DIRECTORY_TVM_CLIENT


def test_get_tvm2_ticket_no_secret():

    @nirvana_command
    def test_func_4() -> dict:
        get_service_ticket(DIRECTORY_TVM_CLIENT)
        return {}

    input_data = {'some_arg': 10}
    file_name = 'some_file'
    with open(file_name, 'w') as input_file:
        json.dump(input_data, input_file)

    command = getattr(commands_library, 'test_func_4_command')
    with pytest.raises(TVM2Error):
        with TvmClientPatcher():
            command.callback(file_name)


def test_get_tvm2_ticket_success(test_vcr):

    @nirvana_command
    def test_func_3() -> dict:
        service_ticket = get_service_ticket(DIRECTORY_TVM_CLIENT)
        # в реальной жизни возвращать тикет из функции конечно не нужно
        # а нужно его использовать для запросов в апи
        return {'ticket': service_ticket}

    input_data = {'some_arg': 10}
    file_name = 'some_file'
    output_file_name = 'output_file'
    with open(file_name, 'w') as input_file:
        json.dump(input_data, input_file)

    command = getattr(commands_library, 'test_func_3_command')
    with TvmClientPatcher():
        command.callback(file_name, output_file_name, 'tvm2_secret')

    with open(output_file_name) as output_file:
        json_data = json.load(output_file)

    assert json_data == {
        'some_arg': 10,
        'ticket': 'Some service ticket',
    }
