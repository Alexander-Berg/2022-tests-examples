# -*- coding: utf-8 -*-
import cli
import ecparam


class Params(object):
    algorithm = None

    def __init__(self, params):
        super(Params, self).__init__()
        self.__params = params

    @property
    def params(self):
        return self.__params


class RSAParams(Params):
    algorithm = 'RSA'

    def __init__(self, bits=None, pubexp=None):
        super(RSAParams, self).__init__({
            'rsa_keygen_bits': bits,
            'rsa_keygen_pubexp': pubexp,
        })


class ECParams(Params):
    algorithm = 'EC'

    def __init__(self, curve):
        super(ECParams, self).__init__({
            'ec_paramgen_curve': curve,
        })


def gen_key(path, params):
    # for compatibility with openssl 1.0.1 and lower
    if params.algorithm == 'EC':
        return ecparam.gen_key(path, params.params['ec_paramgen_curve'])
    cmd = [
        'genpkey',
        '-algorithm', params.algorithm,
        '-out', path,
    ]
    for name, value in params.params.iteritems():
        if value is not None:
            cmd.extend([
                '-pkeyopt', '{}:{}'.format(name, value),
            ])
    return cli.openssl(cmd)
