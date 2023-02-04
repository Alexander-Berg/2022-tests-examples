# -*- coding: utf8 -*-
import yatest.common
import os


# Соглашения:
# В терминах yatest существуют:
# - yatest.common.source_path(): корень аркадии
# - yatest.common.build_path(): директория с собранными артефактами. Структура соответствует аркадии
# - os.getcwd(): рабочая директория и по совместительству таргет загрузки sandbox зависимостей теста
# В терминах bmtest:
# - fs_root: корень структуры с бинарниками необходимыми для теста(perl, so-шки)
# - merged_root: корень аркадии с собранными артефактами


def make_fs_root():
    dst = os.getcwd()
    fs_root = os.path.join(dst, 'fs_root')
    os.mkdir(fs_root)

    # ToDo: extract perllibs.tgz
    yatest.common.execute(
        ['tar', '-xf', 'perl.tgz', '-C', fs_root],
        check_exit_code=True
    )

    return fs_root


def make_merged_root():
    dst = os.getcwd()
    merged_root = os.path.join(dst, 'merged_root')
    os.mkdir(merged_root)

    # Copy sources
    src_root = yatest.common.source_path()
    for dir_name, _, file_list in os.walk(src_root + '/rt-research', followlinks=True):
        copy_path = os.path.abspath(os.path.join(merged_root, os.path.relpath(dir_name, src_root)))  # abspath to create root
        os.mkdir(copy_path)
        for fname in file_list:
            os.symlink(os.path.join(dir_name, fname), os.path.join(copy_path, fname))

    # Copy build artifacts
    build_root = yatest.common.build_path()
    for dir_name, _, file_list in os.walk(build_root + '/rt-research', followlinks=True):
        copy_path = os.path.abspath(os.path.join(merged_root, os.path.relpath(dir_name, build_root)))  # abspath to create root
        for fname in file_list:
            dst = os.path.join(copy_path, fname)
            if os.path.isdir(copy_path) and not os.path.isfile(dst):
                os.symlink(os.path.join(dir_name, fname), dst)

    for bm_dir_name in ('gen-dicts', 'work'):
        bm_dir_arch = bm_dir_name+'.tgz'
        if os.path.isfile(bm_dir_arch):
            bm_dir_path = '{}/rt-research/broadmatching/{}'.format(merged_root, bm_dir_name)
            os.mkdir(bm_dir_path)
            yatest.common.execute(
                ['tar', '-xf', bm_dir_arch, '-C', bm_dir_path],
                check_exit_code=True
            )

    return merged_root


def list_bm_unittests(prefix):
    ut_list = []
    src_root = yatest.common.source_path('rt-research/broadmatching/scripts/tests/unit/'+prefix)
    for dir_name, _, file_list in os.walk(src_root, followlinks=True):
        for file_name in file_list:
            file_path = os.path.join(dir_name, file_name)
            if os.path.isfile(file_path) and os.access(file_path, os.X_OK):
                test_name = ''.join(file_path[len(src_root)+1:].split('.')[:-1])
                ut_list.append(test_name)
    return ut_list


def run_bm_unittests(fs_root, merged_root, suite, name):
    yatest.common.execute(
        ['{}/rt-research/broadmatching/bin/bm-unittests'.format(merged_root), '{}/{}'.format(suite, name)],
        env=get_env(fs_root),
        check_exit_code=True
    )


def get_env(fs_root):
    perl5lib = [
        '{}/etc/perl'.format(fs_root),
        '{}/usr/local/lib/perl/5.14.2'.format(fs_root),
        '{}/usr/lib/perl5'.format(fs_root),
        '{}/usr/share/perl5'.format(fs_root),
        '{}/usr/lib/perl/5.14'.format(fs_root),
        '{}/usr/share/perl/5.14'.format(fs_root),
    ]
    return {
        'LD_LIBRARY_PATH': '{}/usr/lib'.format(fs_root),
        'PERL5LIB': ':'.join(perl5lib),
        'PATH': '{}:/bin'.format(fs_root),
        'BM_CURR_HOST': 'bmgen-dev01e.yandex.ru',
    }
