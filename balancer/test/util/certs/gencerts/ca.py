# -*- coding: utf-8 -*-
import cli


def __format_datetime(dt):
    return dt.strftime('%y%m%d%H%M%SZ')


def sign(csr, cert, config, start_date=None, end_date=None, days=None, extensions=None):
    cmd = [
        'ca',
        '-in', csr,
        '-out', cert,
        '-config', config,
    ]
    if start_date is not None:
        cmd.extend(['-startdate', __format_datetime(start_date)])
    if end_date is not None:
        cmd.extend(['-enddate', __format_datetime(end_date)])
    if days is not None:
        cmd.extend(['-days', days])
    if extensions:
        cmd.extend(['-extensions', extensions])
    return cli.openssl(cmd, text='y\ny\n')


def gen_crl(config, crl):
    return cli.openssl([
        'ca',
        '-gencrl',
        '-config', config,
        '-out', crl,
    ])


def revoke(config, cert):
    return cli.openssl([
        'ca',
        '-config', config,
        '-revoke', cert,
    ])
