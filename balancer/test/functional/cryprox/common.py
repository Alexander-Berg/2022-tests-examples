import json

from configs import Config


BACKEND_REQ = '/cry/SLA3ou955/4acce7GfG7M/ScFdnA3e/5yu/3q2Hvb0Dt/uzzQH/WKSa4cib/OhMfYTeMCz/ONlQt/vgVeNFBjM/w3JmDrGadPYY4'
STATIC_REQ = '/cry/SLA3ou903/4acce7GfG7M/ScFdnAmb/p6x/0qDN_74Pt/OGwBn/KASLNKz6/-4MMUTZsCw/JclNr/fYbf-tQms/ttZGPnG6p8YQ'

CLIENT_HEADERS = {
    'x-req-id': 'client_req_id',
    'accept-encoding': 'client_accept_encoding',
    'content-encoding': 'client_content_encoding',
    'cookie': 'client_cookie1=1; client_cookie2=2',
    'set_cookie': 'client_set_cookie1=1',
    'content-type': 'client_content_type',
    'content-security-policy' : 'client_csp',
    'content-security-policy-report-only' : 'client_csp_report_only',
}

CLIENT_HEADERS_CYCLE = dict(CLIENT_HEADERS)
CLIENT_HEADERS_CYCLE['x-aab-proxy'] = '1'

CLIENT_HEADERS_CRY = dict(CLIENT_HEADERS)
CLIENT_HEADERS_CRY['cookie'] = 'client_cookie1=1; cryprox=1; client_cookie2=2'

CLIENT_HEADERS_CRY_CYCLE = dict(CLIENT_HEADERS_CRY)
CLIENT_HEADERS_CRY_CYCLE['x-aab-proxy'] = '1'

BACKEND_HEADERS = {
    'x-req-id': 'backend_req_id',
    'accept-encoding': 'backend_accept_encoding',
    'content-encoding': 'backend_content_encoding',
    'cookie': 'backend_cookie',
    'set_cookie': 'backend_set_cookie1=1',
    'content-type': 'backend_content_type',
    'content-security-policy' : 'backend_csp',
    'content-security-policy-report-only' : 'backend_csp_report_only',
}

CRYPROX_HEADERS = {
    'x-req-id': 'cryprox_req_id',
    'accept-encoding': 'cryprox_accept_encoding',
    'content-encoding': 'cryprox_content_encoding',
    'cookie': 'cryprox_cookie',
    'set_cookie': 'cryprox_set_cookie1=1',
    'content-type': 'cryprox_content_type',
    'content-security-policy' : 'cryprox_csp',
    'content-security-policy-report-only' : 'cryprox_csp_report_only',
}

CLIENT_RESP_HEADERS = dict(BACKEND_HEADERS)
CLIENT_RESP_HEADERS.update({
    'content-encoding': 'cryprox_content_encoding',
    'content-security-policy' : 'cryprox_csp',
    'content-security-policy-report-only' : 'cryprox_csp_report_only',
})

CRYPROX_CRYPT_HEADERS = {
    'x-req-id': 'client_req_id',
    'accept-encoding': 'client_accept_encoding',
    'cookie': 'client_cookie1=1; client_cookie2=2',
    'content-encoding': 'backend_content_encoding',
    'content-type': 'backend_content_type',
    'content-security-policy' : 'backend_csp',
    'content-security-policy-report-only' : 'backend_csp_report_only',
}

CRYPROX_CRYPT_HEADERS_CRY = dict(CRYPROX_CRYPT_HEADERS)
CRYPROX_CRYPT_HEADERS_CRY['cookie'] = 'client_cookie1=1; cryprox=1; client_cookie2=2'

CONFIG1 = {
    'crypt_secret_key': 'duoYujaikieng9airah4Aexai4yek4qu',
    'crypt_preffixes': '/cry/',
    'crypt_enable_trailing_slash': False,
    'backend_url_re': r'backend\.local/.*?',
}


def start_balancer(ctx, disable_cryprox=False, cryprox_backend_timeout=None, service_backend_timeout=None):
    secrets_file = ctx.manager.fs.create_file('secrets_file')
    ctx.manager.fs.rewrite('secrets_file', json.dumps(CONFIG1))

    disable_file = None
    if disable_cryprox:
        disable_file = ctx.manager.fs.create_file('disable_file')

    ctx.start_balancer(Config(
        partner_token='token', secrets_file=secrets_file, disable_file=disable_file,
        cryprox_backend_timeout=cryprox_backend_timeout, service_backend_timeout=service_backend_timeout,
    ))
