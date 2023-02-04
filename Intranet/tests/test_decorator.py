import pytest
import json

from intranet.compositor_processors.src.logic.utils import (
    validate_annotations,
    parse_input,
    get_func_params,
)

from intranet.compositor_processors.src.logic.decorators import nirvana_command

import intranet.compositor_processors.src.commands as commands_library


def test_validate_annotations_fail_return_annotation():

    def test_func(some_arg: int) -> str:
        pass

    with pytest.raises(TypeError):
        validate_annotations(test_func)


def test_validate_annotations_fail_no_return_annotation():

    def test_func(some_arg: int):
        pass

    with pytest.raises(TypeError):
        validate_annotations(test_func)


def test_validate_annotations_fail_arg_annotation():
    def test_func(some_arg) -> None:
        pass

    with pytest.raises(SyntaxError):
        validate_annotations(test_func)


def test_validate_annotations_success():
    def test_func(some_arg: int) -> None:
        pass

    validate_annotations(test_func)


def test_parse_input_success():
    def test_func(some_arg: int) -> None:
        pass

    func_params = get_func_params(test_func)
    json_input = {'some_arg': 12, 'some_another': 'test'}
    parse_input(json_input, func_params)


def test_parse_input_success_default():
    def test_func(some_arg: int = 10) -> None:
        pass

    func_params = get_func_params(test_func)
    json_input = {'some_another': 'test'}
    parse_input(json_input, func_params)


def test_parse_input_fail_no_arg():
    def test_func(some_arg: int) -> None:
        pass

    func_params = get_func_params(test_func)
    json_input = {'some_another': 'test'}
    with pytest.raises(ValueError):
        parse_input(json_input, func_params)


def test_parse_input_fail_wrong_type():
    def test_func(some_arg: int) -> None:
        pass

    func_params = get_func_params(test_func)
    json_input = {'some_arg': 'test'}
    with pytest.raises(ValueError):
        parse_input(json_input, func_params)


def test_nirvana_command_success():
    @nirvana_command
    def test_func(some_arg: int) -> dict:
        return {'org_id': some_arg, 'test': 'some'}

    input_data = {'some_arg': 10}
    file_name = 'some_file'
    output_file_name = 'output_file'
    with open(file_name, 'w') as input_file:
        json.dump(input_data, input_file)

    command = getattr(commands_library, 'test_func_command')
    command.callback(file_name, output_file_name)

    with open(output_file_name) as output_file:
        json_data = json.load(output_file)

    assert json_data == {
        'some_arg': 10,
        'test': 'some',
        'org_id': 10,
    }

    # проверим что исходную функцию так же можно вызывать
    result = test_func(42)
    assert result == {'org_id': 42, 'test': 'some'}


def test_nirvana_command_fail_no_output():
    @nirvana_command
    def test_func_0(some_arg: int) -> dict:
        return {'org_id': some_arg, 'test': 'some'}

    input_data = {'some_arg': 10}
    file_name = 'some_file'
    with open(file_name, 'w') as input_file:
        json.dump(input_data, input_file)

    command = getattr(commands_library, 'test_func_0_command')
    with pytest.raises(RuntimeError):
        command.callback(file_name)


def test_nirvana_command_fail_exception():
    @nirvana_command
    def test_func_2(some_arg: int) -> None:
        raise KeyError('something went wrong')

    input_data = {'some_arg': 10}
    file_name = 'some_file'
    with open(file_name, 'w') as input_file:
        json.dump(input_data, input_file)

    command = getattr(commands_library, 'test_func_2_command')
    with pytest.raises(KeyError):
        command.callback(file_name)


def test_nirvana_command_no_needed_arg():
    @nirvana_command
    def test_func_1(some_arg: int) -> None:
        raise KeyError('something went wrong')

    input_data = {'some_other_arg': 10}
    file_name = 'some_file'
    with open(file_name, 'w') as input_file:
        json.dump(input_data, input_file)

    command = getattr(commands_library, 'test_func_1_command')
    with pytest.raises(ValueError):
        command.callback(file_name)

    with pytest.raises(KeyError):
        test_func_1(42)
