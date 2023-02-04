# -*- coding: utf-8 -*-
import base64
import os
import re
import logging
import textwrap

from balancer.test.util.process import call


TICKET_KEY_SIZE = 48
TICKET_ID_SIZE = 16

BEGIN_CERT = '-----BEGIN CERTIFICATE-----'
END_CERT = '-----END CERTIFICATE-----'
CERT = re.compile(r'%s\s+(.+?)\s+%s' % (BEGIN_CERT, END_CERT), re.DOTALL)
SERVER_CERT = re.compile(r'Server certificate\s+%s\s+(.+?)\s+%s' % (BEGIN_CERT, END_CERT), re.DOTALL)
VERIFY_CODE = re.compile(r'Verify return code: (\d+) \((.+)\)')

SESSION_ID = re.compile(r'Session-ID\s*:\s*(\w+)')
OCSP_CERT = re.compile(r'(?<=OCSP Response Data:\n).*?Signature Algorithm: *\S*\n(.*?)(?=\nCertificate:)',
                       re.DOTALL)
TICKET = re.compile(r'TLS session ticket:\n(.*?)\n^$', re.DOTALL | re.MULTILINE)
TICKET_ID = re.compile(r'0000 - ((?:[0-9a-f]{2}[ -]){16})')
TICKET_TTL = re.compile(r'TLS session ticket lifetime hint: (\d+) \(seconds\)')

CIPHER = re.compile(r'Cipher\s+:\s+([\w-]+)')

ADVERTISED_PROTOCOLS = re.compile(r'Protocols advertised by server:\s+(.+?)\n')
ALPN_PROTOCOL = re.compile(r'ALPN protocol:\s+(.+?)\n')

PROTOCOL = re.compile(r'Protocol\s+:\s+(\S+)')

HELLO_PREFIX = 12
RANDOM_LENGTH = 64
CLIENT_HELLO = re.compile(r'>>>(?:.*?)ClientHello\n(.*?)<<<', re.DOTALL)
SERVER_HELLO = re.compile(r'<<<(?:.*?)ServerHello\n(.*?)<<<', re.DOTALL)

MASTER_KEY = re.compile(r'Master-Key\s*:\s*(\w+)')


def _match_groups(text, regexp, groups_count):
    result = regexp.search(text)
    if result is None:
        return None
    groups = result.groups()
    if len(groups) != groups_count:
        return None
    return groups


def _match(text, regexp):
    groups = _match_groups(text, regexp, 1)
    if groups is None:
        return None
    text = textwrap.dedent(groups[0])
    return text


def ocsp_response(resp_file):
    logger = logging.getLogger()  # FIXME
    cert_info = call(['openssl', 'ocsp', '-respin', resp_file, '-text'], logger).stdout
    return _match(cert_info, OCSP_CERT)


def ticket_key(ticket_file):
    if os.path.getsize(ticket_file) == TICKET_KEY_SIZE:
        with open(ticket_file) as file_:
            return file_.read(TICKET_ID_SIZE)
    else:
        with open(ticket_file) as file_:
            ticket_str = file_.readlines()[1]
        return base64.b64decode(ticket_str)[:TICKET_ID_SIZE]


def cert(cert_file):
    with open(cert_file) as file_:
        return _match(file_.read(), CERT)


class SSLHandshakeInfo(object):
    def __init__(self, handshake_info):
        super(SSLHandshakeInfo, self).__init__()
        self.handshake_info = handshake_info
        verify_result = _match_groups(self.handshake_info, VERIFY_CODE, 2)
        if verify_result is not None:
            self.verify_code = int(verify_result[0])
            self.verify_status = verify_result[1]
            self.verified = self.verify_code == 0
        else:
            self.verify_code = None
            self.verify_status = None
            self.verified = None
        self.reused = 'Reused' in self.handshake_info
        self.server_cert = self.__match(SERVER_CERT)
        self.session_id = self.__match(SESSION_ID)
        self.cipher = self.__match(CIPHER)
        self.protocol = self.__match(PROTOCOL)
        self.client_random = self.__get_random(CLIENT_HELLO)
        self.server_random = self.__get_random(SERVER_HELLO)
        self.master_key = self.__match(MASTER_KEY)

        self.has_ocsp = 'OCSP' in self.handshake_info and 'OCSP response: no response sent' not in self.handshake_info
        if self.has_ocsp:
            self.ocsp_data = self.__match(OCSP_CERT)
            self.ocsp_verified = 'OCSP Response Status: successful (0x0)' in self.handshake_info
        else:
            self.ocsp_data = None
            self.ocsp_verified = None

        self.secure_renegotiation = 'Secure Renegotiation IS supported' in self.handshake_info

        self.has_ticket = 'TLS session ticket' in self.handshake_info
        if self.has_ticket:
            self.ticket_data = self.__match(TICKET)
            self.ticket_ttl = self.__match(TICKET_TTL)
        else:
            self.ticket_data = None
            self.ticket_ttl = None
        if self.ticket_data:
            ticket_id_str = _match(self.ticket_data, TICKET_ID).replace('-', ' ')
            ticket_id_bytes = [chr(int(x, 16)) for x in ticket_id_str.split()]
            self.ticket_id = ''.join(ticket_id_bytes)
        else:
            self.ticket_id = None

        advert_protos = self.__match(ADVERTISED_PROTOCOLS)
        if advert_protos:
            self.advertised_protocols = advert_protos.split(', ')
        else:
            self.advertised_protocols = None
        alpn_protos = self.__match(ALPN_PROTOCOL)
        if alpn_protos:
            self.alpn_protocols = alpn_protos.split(', ')
        else:
            self.alpn_protocols = None

    def __match(self, regexp):
        return _match(self.handshake_info, regexp)

    def __get_random(self, regexp):
        res = self.__match(regexp)
        if res is not None:
            hex_dump = ''.join(res.split())
            rnd = hex_dump[HELLO_PREFIX:HELLO_PREFIX + RANDOM_LENGTH]
            return rnd.upper()
        else:
            return None
