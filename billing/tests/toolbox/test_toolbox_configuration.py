from bcl.toolbox.configuration import Configurable, dump_xml_settings


def test_configurable():

    class My(Configurable):
        pass

    settings = My.get_settings('RobotSetting', 'bcl', ['Email'])
    assert settings == ['robot-bcl@yandex-team.ru']


def test_dump():

    dumped = dump_xml_settings()

    setting = dumped['']['robot__bcl']['password']
    assert setting.is_secret
    assert setting.component == 'robot'
    assert setting.realm == ''
    assert setting.name == 'password'
    assert setting.value == '*****'
    assert setting.associate == ''
    assert setting.env_def == 'BCL_ROBOT__BCL__PASSWORD = *****'

    setting = dumped['alfa']['http__ya']['login']
    assert not setting.is_secret
    assert setting.component == 'http'
    assert setting.realm == 'ya'
    assert setting.name == 'login'
    assert setting.value == '643223'
    assert setting.associate == 'alfa'
    assert setting.env_def == 'BCL_HTTP__ALFA_YA__LOGIN = 643223'
