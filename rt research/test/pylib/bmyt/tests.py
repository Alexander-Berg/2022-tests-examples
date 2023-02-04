# coding: utf-8

import subprocess
import logging

from yt.wrapper.errors import YtOperationFailedError

import yatest.common

from bm.bmyt import BMYT


def test_perl():
    subprocess.call(['perl', '-v'])


def test_local_yt(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    res_dir = '//home/bmyt'
    yt_client.create('map_node', path=res_dir)

    build_dir = yatest.common.build_path()
    catalogia_spec = {
        'lib': {'type': 'local', 'build_dir': build_dir},
        'dicts': {'type': 'local', 'build_dir': build_dir},
        'perllibs': {'type': 'local', 'dir': '.'},
        'gendicts': [
            {'name': 'generated_dicts', 'type': 'local', 'dir': '.'},
        ],
    }
    bmyt_client = BMYT(yt_client=yt_client, catalogia_spec=catalogia_spec)

    input = '//home/test001.input'
    input_rows = [
        {'id': 1, 'text': 'товары для дома'},
        {'id': 2, 'text': 'светильники'},
    ]
    yt_client.write_table(input, input_rows)
    bm_mapper = {
        "begin": """
            use BaseProject;
            $self->{proj} = BaseProject->new({load_dicts => 1});
        """,
        "mapper": """
            $r->{norm} = $self->{proj}->phrase($r->{text})->norm_phr;
            yield($r);
        """,
        "dst_fields": [
            {'text': str, 'norm': str},
        ],
    }
    output = '//home/test001.output'
    try:
        bmyt_client.run_bm_map(
            bm_mapper,
            input,
            output,
        )
    except YtOperationFailedError as err:
        logging.error('BMYT operation failed!')
        logging.error(err.attributes)
        raise err
    output_rows = list(yt_client.read_table(output))
    assert len(output_rows) == 2
    assert output_rows[0]['norm'] == 'дом товар'
