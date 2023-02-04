from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.predef.handler.server.http import SimpleConfig


def test_antirobot_wrapper_headers_in_response(wrap_ctx):
    """
    antirobot wrapper не удаляет заголовки X-Antirobot-Ban-Source-Ip и X-ForwardToUser-Y.
    """
    headers = ['X-Antirobot-Ban-Source-Ip', 'X-ForwardToUser-Y']
    wrap_ctx.start_antirobot_backend(SimpleConfig(http.response.ok(
        headers=[(header, 'qwerty') for header in headers]
    )))
    wrap_ctx.start_antirobot_balancer()

    response = wrap_ctx.perform_request(http.request.get())
    for header in headers:
        asserts.header_value(response, header, 'qwerty')
