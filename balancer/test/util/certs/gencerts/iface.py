# -*- coding: utf-8 -*-
import fs
import genpkey
import req
import ca
import os
import run_import_hook
import jinja2
import rand
import ocsp


class OCSPResponder(object):
    def sign(self, ocsp_req, ocsp_resp):
        raise NotImplementedError()


class CA(object):
    @property
    def cert(self):
        raise NotImplementedError()

    @property
    def ocsp(self):
        return None

    def sign(self, csr, cert, extensions=None):
        raise NotImplementedError()


class OCSPGenerationException(Exception):
    pass


class Issuer(fs.DirHolder):
    def __init__(self, root_dir, ca, key_params, subj, start_date=None, end_date=None, days=None, extensions=None):
        super(Issuer, self).__init__(root_dir)
        self.__ca = ca
        self.__key = self.__gen_key(key_params)
        self.__req = self.__gen_req(subj)
        self.__cert = self.__gen_cert(start_date, end_date, days, extensions)
        self.__ocsp = dict()

    @property
    def key(self):
        return self.__key

    @property
    def req(self):
        return self.__req

    @property
    def cert(self):
        return self.__cert

    def gen_ocsp(self, name):
        if self.__ca.ocsp is None:
            raise OCSPGenerationException('no ocsp responder configured')
        ocsp_req = self._path('{}.req.der'.format(name))
        ocsp.gen_ocsp_req(
            ca_file=self.__ca.cert,
            issuer=self.__ca.cert,
            cert=self.cert,
            ocsp_req=ocsp_req,
        )
        ocsp_resp = self._path('{}.der'.format(name))
        self.__ca.ocsp.sign(ocsp_req, ocsp_resp)
        return ocsp_resp

    def revoke(self):
        self.__ca.revoke(self.cert)

    def __gen_key(self, params):
        path = self._path('private.key')
        genpkey.gen_key(path, params)
        return path

    def __gen_req(self, subj):
        path = self._path('cert_req.csr')
        req.gen_req(self.__key, path, subj)
        return path

    def __gen_cert(self, start_date, end_date, days, extensions):
        path = self._path('public.crt')
        self.__ca.sign(self.__req, path, start_date, end_date, days, extensions)
        return path


class LocalResponder(OCSPResponder, fs.DirHolder):
    def __init__(self, root_dir, ca, key_params, subj):
        super(LocalResponder, self).__init__(root_dir)
        self.__ca = ca
        self.__issuer = Issuer(self._path('ocsp'), ca, key_params, subj, extensions='v3_OCSP')

    @property
    def key(self):
        return self.__issuer.key

    @property
    def cert(self):
        return self.__issuer.cert

    def sign(self, ocsp_req, ocsp_resp):
        ocsp.sign(
            ca_file=self.__ca.cert,
            issuer=self.__ca.cert,
            index=self.__ca.index,
            rsigner=self.cert,
            rkey=self.key,
            ocsp_req=ocsp_req,
            ocsp_resp=ocsp_resp,
        )


class LocalRootCA(CA, fs.DirHolder):
    DAYS = 365242

    def __init__(self, root_dir, key_params, subj, digest, ocsp_key_params=None, ocsp_subj=None):
        super(LocalRootCA, self).__init__(root_dir)

        os.makedirs(self._path('new_certs'))

        self.__serial = self._path('serial')
        with open(self.__serial, 'w') as f:
            f.write('01')

        self.__index = self._path('index.txt')
        with open(self.__index, 'w'):
            pass

        os.makedirs(self._path('crl'))
        self.__crl_number = self._path('crlnumber')
        with open(self.__crl_number, 'w') as f:
            f.write('01')

        self.__config = self.__gen_config(digest)
        self.__key = self.__gen_key(key_params)
        self.__cert = self.__gen_self_cert(subj, digest)
        self.__crl = self.__gen_crl()

        if ocsp_key_params is not None and ocsp_subj is not None:
            self.__ocsp = LocalResponder(self.root_dir, self, ocsp_key_params, ocsp_subj)
        else:
            self.__ocsp = None

    @property
    def ocsp(self):
        return self.__ocsp

    @property
    def key(self):
        return self.__key

    @property
    def cert(self):
        return self.__cert

    @property
    def index(self):
        return self.__index

    @property
    def crl(self):
        return self.__crl

    def sign(self, csr, cert, start_date=None, end_date=None, days=None, extensions=None):
        ca.sign(
            csr=csr,
            cert=cert,
            config=self.__config,
            start_date=start_date,
            end_date=end_date,
            days=days,
            extensions=extensions,
        )

    def revoke(self, cert):
        ca.revoke(self.__config, cert)
        self.__gen_crl()

    def __gen_config(self, digest):
        path = self._path('openssl.cnf')
        template = jinja2.Template(run_import_hook.importer.get_data('openssl.cnf'))
        with open(path, 'w') as f:
            f.write(template.render(root_dir=self.root_dir, digest=digest))
        return path

    def __gen_key(self, params):
        dir_path = self._path('private')
        os.makedirs(dir_path)
        path = self._path('private', 'root_ca.key')
        genpkey.gen_key(path, params)
        return path

    def __gen_self_cert(self, subj, digest):
        path = self._path('root_ca.crt')
        req.gen_self_signed_cert(self.__key, path, subj, digest, self.DAYS, self.__config)
        return path

    def __gen_crl(self):
        path = self._path('crl.pem')
        ca.gen_crl(self.__config, path)
        return path


class TicketGenerator(fs.DirHolder):
    __PEM_PATTERN = '''\
-----BEGIN SESSION TICKET KEY-----
{}\
-----END SESSION TICKET KEY-----
'''

    def __init__(self, root_dir):
        super(TicketGenerator, self).__init__(root_dir)

    def gen_raw(self, length, name):
        path = self._path('{}.raw'.format(name))
        rand.gen_rand(length, path)
        return path

    def gen_pem(self, length, name):
        path = self._path('{}.pem'.format(name))
        rand.gen_rand(length, path, base64=True)
        with open(path, 'r') as f:
            data = f.read()
        with open(path, 'w') as f:
            f.write(self.__PEM_PATTERN.format(data))
        return path
