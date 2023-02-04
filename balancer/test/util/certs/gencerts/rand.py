# -*- coding: utf-8 -*-
import cli


def gen_rand(length, path, base64=False):
    cmd = [
        'rand',
        length,
        '-out', path,
    ]
    if base64:
        cmd.append('-base64')
    return cli.openssl(cmd)
