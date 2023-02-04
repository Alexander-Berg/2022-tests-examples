from os import environ
from shutil import rmtree

from dwh.settings.settings_conf import ConfApplier


def test_conf_applier(tmp_path):
    applier = ConfApplier(env_type='dev', base_dir=tmp_path)
    path_conf = tmp_path / 'conf'

    rmtree(path_conf)  # вычистим для проверки автогенерирования файлов
    path_conf.mkdir()  # и снова создадим, но пустую

    environ['MNCLOSE'] = 'hoho'

    environ['DWH_ORA_DUMMY_HOST'] = 'host'
    environ['DWH_ORA_DUMMY_HOST_RO'] = ''   # не означен хост, эту запись пропустим
    environ['DWH_ORA_DUMMY_PWD'] = 'pwd'
    environ['DWH_ORA_DUMMY_USER'] = 'user'
    environ['DWH_ORA_DUMMY_ID'] = 'mu'

    applier.run()

    assert (path_conf / 'dwh-secret.yaml').exists()
    assert (path_conf / 'mnclose.xml').exists()
    assert (path_conf / 'db' / 'db-conn-dummy.cfg.xml').exists()
