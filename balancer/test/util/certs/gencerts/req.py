# -*- coding: utf-8 -*-
import cli


def gen_req(key, csr, subj):
    cli.openssl([
        'req',
        '-new',
        '-nodes',
        '-key', key,
        '-out', csr,
        '-subj', subj,
    ])


def gen_self_signed_cert(key, cert, subj, digest, days, config):
    cli.openssl([
        'req',
        '-x509',
        '-new',
        '-nodes',
        '-key', key,
        '-out', cert,
        '-subj', subj,
        '-{}'.format(digest),
        '-days', days,
        '-config', config,
    ])
