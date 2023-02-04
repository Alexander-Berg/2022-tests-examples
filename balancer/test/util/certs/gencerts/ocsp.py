# -*- coding: utf-8 -*-
import cli


def gen_ocsp_req(ca_file, issuer, cert, ocsp_req):
    return cli.openssl([
        'ocsp',
        '-CAfile', ca_file,
        '-issuer', issuer,
        '-cert', cert,
        '-reqout', ocsp_req,
    ])


def sign(ca_file, issuer, index, rsigner, rkey, ocsp_req, ocsp_resp):
    return cli.openssl([
        'ocsp',
        '-CAfile', ca_file,
        '-CA', ca_file,
        '-issuer', issuer,
        '-index', index,
        '-rsigner', rsigner,
        '-rkey', rkey,
        '-reqin', ocsp_req,
        '-respout', ocsp_resp,
    ])
