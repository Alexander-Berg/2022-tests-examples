# -*- coding: utf-8 -*-
import cli


def gen_key(path, curve):
    return cli.openssl([
        'ecparam',
        '-out', path,
        '-name', curve,
        '-genkey',
    ])
