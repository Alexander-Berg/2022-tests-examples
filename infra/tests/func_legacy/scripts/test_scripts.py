# coding=utf-8
import os

from utils import must_start_instancectl, must_stop_instancectl
from utils import wait_condition_is_true


def test_scripts(cwd, ctl, patch_loop_conf, ctl_environment, request):
    """
        Запускаем ctl с тремя демонами у каждого из которых по скрипту.
        Чекаем, что install работает 1 раз, stop работает 1 раз.
        Рестарт запускается систематически.
    """

    def check_file_and_its_content(file_name, exists=True, lines_number_checker=None):
        """
            Проверяет файл - существует ли он, не существует, сколько в нём строк.
            Например:

                check_file_and_content('file', exists=False) - проверяет, что файл не сущетсвует

                check_file_and_content('file', lines_number_checker=lamda x: x <= 2) - проверяет, что файл сущетсвует
                    и в нём 2 или меньше строк

            :param file_name:
            :param exists: должен ли существовать файл
            :param lines_number_checker: функция проверки числа строк: если ок, должна возвращать True;
                False в противном случае. Срабатывает, если файл должен существовать и существует
            :return: True, если все проверки прошли; False в противном случае
            :rtype: bool
        """
        file_path = os.path.join(cwd.strpath, file_name)
        if not exists:
            # проверяем, что файл не существует
            return not os.path.exists(file_path)
        else:
            # файл не существует, сразу возвращаем False
            if not os.path.exists(file_path):
                return False

        if lines_number_checker is not None:
            # нужно проверить, сколько строк, тут мы уже уверены, что файл существует
            with open(file_path) as fd:
                if not lines_number_checker(len(fd.readlines())):
                    return False
        # по умолчанию возвращаем True - всё ок
        return True

    p = must_start_instancectl(ctl, request, ctl_environment)
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=9, check_timeout=1,
        file_name='test_restart_script_result.txt', lines_number_checker=lambda x: x >= 2
    ), 'Not enough restarts'
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_stop_script_result.txt', exists=False
    ), 'Stop script exists'
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_install_script_result.txt', lines_number_checker=lambda x: x == 1
    )

    must_stop_instancectl(ctl, process=p)

    for script in ['install', 'stop', 'restart']:
        for line in cwd.join('test_{}_script_result.txt'.format(script)).readlines():
            assert 'this is test_{}_script script'.format(script) == line.strip()

    assert wait_condition_is_true(
        check_file_and_its_content, timeout=5, check_timeout=1,
        file_name='test_restart_script_result.txt', lines_number_checker=lambda x: x >= 2
    )
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_stop_script_result.txt', lines_number_checker=lambda x: x == 1
    )
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_install_script_result.txt', lines_number_checker=lambda x: x == 1
    )

    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_restart_script.txt', lines_number_checker=lambda x: x >= 2
    )
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_stop_script.txt', lines_number_checker=lambda x: x == 1
    )
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_install_script.txt', lines_number_checker=lambda x: x >= 2
    )

    p = must_start_instancectl(ctl, request, ctl_environment)

    assert wait_condition_is_true(
        check_file_and_its_content, timeout=9, check_timeout=1,
        file_name='test_restart_script_result.txt', lines_number_checker=lambda x: x >= 2
    )
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_stop_script_result.txt', lines_number_checker=lambda x: x == 1
    )
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_install_script_result.txt', lines_number_checker=lambda x: x == 2
    )

    must_stop_instancectl(ctl, process=p)

    for script in ['install', 'stop', 'restart']:
        for line in cwd.join('test_{}_script_result.txt'.format(script)).readlines():
            assert 'this is test_{}_script script'.format(script) == line.strip()

    assert wait_condition_is_true(
        check_file_and_its_content, timeout=9, check_timeout=1,
        file_name='test_restart_script_result.txt', lines_number_checker=lambda x: x >= 2
    )
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_stop_script_result.txt', lines_number_checker=lambda x: x == 2
    )
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_install_script_result.txt', lines_number_checker=lambda x: x == 2
    )

    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_restart_script.txt', lines_number_checker=lambda x: x >= 2
    )
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_stop_script.txt', lines_number_checker=lambda x: x == 2
    )
    assert wait_condition_is_true(
        check_file_and_its_content, timeout=2, check_timeout=1,
        file_name='test_install_script.txt', lines_number_checker=lambda x: x >= 2
    )

