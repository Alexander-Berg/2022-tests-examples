# coding: utf-8

import os
import shutil

import yatest.common

from bmtest.env import make_fs_root, get_env


def binary_python_app_path(binary_name):
    return os.path.join('rt-research', 'broadmatching', 'scripts', binary_name, binary_name)


def run_test(packages, gendict_res, perl_script):
    arc_root = os.getcwd() + '/arc_root'
    os.mkdir(arc_root)
    fs_root = make_fs_root()

    # сейчас почти все либы есть в perl.tgz
    # yatest.common.execute(
    #     ['tar', '-xf', 'perllibs.tgz', '-C', fs_root],
    #     check_exit_code=True,
    # )

    for pkg_tgz in packages:
        yatest.common.execute(
            ['tar', 'xzf', pkg_tgz, '-C', arc_root],
            check_exit_code=True,
        )

    for res in gendict_res:
        yatest.common.execute(
            ['tar', 'xzf', res + '.tgz', '-C', arc_root],
            check_exit_code=True,
        )

    perl_interpreter = fs_root + '/perl'
    perl_lib = arc_root + '/rt-research/broadmatching/scripts/lib'

    for cm_opts_relpath in (
        binary_python_app_path('bannerland_options'),
        binary_python_app_path('irt_common_options'),
    ):
        cm_opts_path = os.path.join(arc_root, cm_opts_relpath)
        os.makedirs(os.path.dirname(cm_opts_path))
        shutil.copy(yatest.common.binary_path(cm_opts_relpath), cm_opts_path)

    yatest.common.execute(
        [perl_interpreter, '-I', perl_lib, perl_script],
        env=get_env(fs_root),
        check_exit_code=True,
    )


def test_bm():
    pkg_dir = yatest.common.build_path() + '/rt-research/broadmatching/ya_packages'
    packages = ['/'.join([pkg_dir, name, name + '.tgz']) for name in ['bm_lib', 'bm_dicts']]
    perl_script = yatest.common.source_path() + '/rt-research/broadmatching/test/pkg/catalogia_test.pl'
    run_test(packages, ['generated_dicts'], perl_script)


def test_bannerland():
    pkg_dir = yatest.common.build_path() + '/rt-research/broadmatching/ya_packages'
    packages = ['/'.join([pkg_dir, name, name + '.tgz']) for name in ['bm_bannerland_lib', 'bm_dicts']]
    bl_res = ['generated_dicts', 'models', 'marketdata_subphraser', 'db_dump_bannerland']
    perl_script = yatest.common.source_path() + '/rt-research/broadmatching/test/pkg/proj_bannerland.pl'
    run_test(packages, bl_res, perl_script)
