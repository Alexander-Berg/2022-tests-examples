# -*- coding: utf-8 -*-

import mock
import requests
import pycurl

from butils.http_message import HttpResponse
from butils import wrap_pycurl
from butils.post import http_call, HttpCallException, _get_ssl_validate


class TestHttpCall(object):
    @mock.patch("requests.request")
    def test_get_form(self, mock_requests_request):
        mock_resp = requests.Response()
        mock_resp.status_code = 200
        mock_resp.reason = "OK"
        mock_resp.headers = {"Content-Type": "text/plain", "Content-Length": "8"}
        mock_resp._content = b"SUCCESS\n"

        mock_requests_request.return_value = mock_resp

        should_verify = _get_ssl_validate()
        r = http_call(
            method="GET",
            url="https://somehost:1234/path/",
            params={"login": "vasya", "password": "qwer ty"},
            driver="requests",
        )
        mock_requests_request.assert_called_once_with(
            "GET",
            "https://somehost:1234/path/?login=vasya&password=qwer+ty",
            data=b"",
            headers={"Accept": "*/*", "Host": "somehost"},
            allow_redirects=True,
            cert=None,
            verify=should_verify,
            timeout=None,
        )

        assert isinstance(r, HttpResponse)
        assert r.status_code == 200
        assert r.reason == "OK"
        assert r.ok
        assert r.content == b"SUCCESS\n"
        assert sorted(r.headers.items()) == [
            ("Content-Length", "8"),
            ("Content-Type", "text/plain"),
        ]

    @mock.patch("requests.request")
    def test_requests_json(self, mock_requests_request):
        mock_resp = requests.Response()
        mock_resp.status_code = 201
        mock_resp.reason = "Created"
        mock_resp.headers = {"Content-Type": "application/json", "Content-Length": "18"}
        mock_resp._content = b'{"a": 1, "b": "2"}'

        mock_requests_request.return_value = mock_resp

        should_verify = _get_ssl_validate()
        r = http_call(
            method="POST",
            url="https://somehost:1234/path/",
            json={"top": {"nested1": 123, "nested2": True, "nested3": "x"}},
            driver="requests",
        )
        mock_requests_request.assert_called_once_with(
            "POST",
            "https://somehost:1234/path/",
            data=b'{"top": {"nested1": 123, "nested2": true, "nested3": "x"}}',
            headers={
                "Accept": "*/*",
                "Content-Length": "58",
                "Content-Type": "application/json",
                "Host": "somehost",
            },
            allow_redirects=True,
            cert=None,
            verify=should_verify,
            timeout=None,
        )

        assert isinstance(r, HttpResponse)
        assert r.status_code == 201
        assert r.reason == "Created"
        assert r.ok
        assert r.content == b'{"a": 1, "b": "2"}'
        assert sorted(r.headers.items()) == [
            ("Content-Length", "18"),
            ("Content-Type", "application/json"),
        ]
        assert r.json() == {"a": 1, "b": "2"}

    @mock.patch("requests.request")
    def test_requests_basic(self, mock_requests_request):
        mock_resp = requests.Response()
        mock_resp.status_code = 200
        mock_resp.reason = "O.K."
        mock_resp.headers = {"Content-Type": "application/xml", "Content-Length": "12"}
        mock_resp._content = b"<resp-body/>"

        mock_requests_request.return_value = mock_resp

        r = http_call(
            method="POST",
            url="https://somehost:1234/path/",
            data=[("param1", "value1"), ("p2", "v 2")],
            headers={"X-My-Header": "some header"},
            timeout=(3.5, 5),
            cert=("cert", "key"),
            verify=True,
            allow_redirects=False,
            driver="requests",
        )
        mock_requests_request.assert_called_once_with(
            "POST",
            "https://somehost:1234/path/",
            data=b"param1=value1&p2=v+2",
            headers={
                "Accept": "*/*",
                "Content-Length": "20",
                "Content-Type": "application/x-www-form-urlencoded",
                "Host": "somehost",
                "X-My-Header": "some header",
            },
            timeout=(3.5, 5),
            cert=("cert", "key"),
            verify=True,
            allow_redirects=False,
        )

        assert isinstance(r, HttpResponse)
        assert r.status_code == 200
        assert r.reason == "O.K."
        assert r.content == b"<resp-body/>"
        assert sorted(r.headers.items()) == [
            ("Content-Length", "12"),
            ("Content-Type", "application/xml"),
        ]

    @mock.patch.object(wrap_pycurl.Curl, "setopt")
    @mock.patch.object(wrap_pycurl.CurlMulti, "perform")
    def test_pycurl_basic(self, mock_pycurl_multi_perform, mock_pycurl_setopt):
        mock_pycurl_multi_perform.return_value = (0, 0)

        http_resp = [None]

        def make_http_resp(*args, **kwargs):
            http_resp[0] = HttpResponse(*args, **kwargs)
            return http_resp[0]

        with mock.patch("butils.post.HttpResponse", new=make_http_resp):
            r = http_call(
                method="POST",
                url="https://somehost:1234/path/",
                data=[("param1", "value1"), ("p2", "v 2")],
                headers={"X-My-Header": "some header"},
                timeout=(3.5, 5),
                cert=("cert", "key"),
                verify=True,
                allow_redirects=False,
                driver="pycurl",
            )

        toy_resp = http_resp[0]
        toy_resp.put_header_line(b"HTTP/1.0 200 O.K.\n")
        toy_resp.put_header_line(b"Content-Type: application/xml\n")
        toy_resp.put_header_line(b"Content-Length: 12\n")
        toy_resp.put_header_line(b"\n")
        toy_resp.put_body_piece(b"<resp-body/>")

        raw_headers = [
            "Accept: */*",
            "Content-Length: 20",
            "Content-Type: application/x-www-form-urlencoded",
            "Host: somehost",
            "X-My-Header: some header",
        ]
        mock_pycurl_setopt.assert_any_call(pycurl.CUSTOMREQUEST, "POST")
        mock_pycurl_setopt.assert_any_call(pycurl.URL, "https://somehost:1234/path/")
        mock_pycurl_setopt.assert_any_call(pycurl.HTTPHEADER, raw_headers)
        mock_pycurl_setopt.assert_any_call(pycurl.POSTFIELDS, b"param1=value1&p2=v+2")
        mock_pycurl_setopt.assert_any_call(pycurl.SSL_VERIFYHOST, 2)
        mock_pycurl_setopt.assert_any_call(pycurl.SSL_VERIFYPEER, 1)
        mock_pycurl_setopt.assert_any_call(pycurl.SSLCERT, "cert")
        mock_pycurl_setopt.assert_any_call(pycurl.SSLKEY, "key")
        mock_pycurl_setopt.assert_any_call(pycurl.TIMEOUT_MS, 5000)
        mock_pycurl_setopt.assert_any_call(pycurl.CONNECTTIMEOUT_MS, 3500)
        mock_pycurl_setopt.assert_any_call(pycurl.FOLLOWLOCATION, 0)

        assert isinstance(r, HttpResponse)
        assert r.status_code == 200
        assert r.reason == "O.K."
        assert r.content == b"<resp-body/>"
        assert sorted(r.headers.items()) == [
            ("Content-Length", "12"),
            ("Content-Type", "application/xml"),
        ]

    @mock.patch("requests.request")
    def test_requests_couldnt_resolve(self, mock_requests_request):
        mock_requests_request.side_effect = Exception(
            "Failed to establish a new connection: [Errno -2] Name or service not known"
        )
        try:
            http_call(method="GET", url="https://localtost/", driver="requests")
            ex = None
        except Exception as e:
            ex = e

        assert isinstance(ex, requests.exceptions.ConnectionError)
        assert "[Errno -2] Name or service not known" in str(ex)
        assert isinstance(ex, HttpCallException)
        assert ex.error_code == "couldnt_resolve_host"

    @mock.patch.object(wrap_pycurl.CurlMulti, "perform")
    def test_pycurl_couldnt_resolve(self, mock_pycurl_multi_perform):
        mock_pycurl_multi_perform.side_effect = pycurl.error(
            pycurl.E_COULDNT_RESOLVE_HOST, "Could not resolve host: localtost"
        )
        try:
            http_call(method="GET", url="https://localtost/", driver="pycurl")
            ex = None
        except Exception as e:
            ex = e

        assert isinstance(ex, requests.exceptions.ConnectionError)
        assert "Could not resolve host: localtost" in str(ex)
        assert isinstance(ex, HttpCallException)
        assert ex.error_code == "couldnt_resolve_host"

    @mock.patch("requests.request")
    def test_requests_conn_timeout(self, mock_requests_request):
        mock_requests_request.side_effect = Exception(
            "Connection to localhost timed out."
        )
        try:
            http_call(
                method="GET",
                url="https://localhost/",
                timeout=(0.01, 3),
                driver="requests",
            )
            ex = None
        except Exception as e:
            ex = e

        assert isinstance(ex, requests.exceptions.ConnectTimeout)
        assert "Connection to " in str(ex) and " timed out" in str(ex)
        assert isinstance(ex, HttpCallException)
        assert ex.error_code == "couldnt_connect_timeout"

    @mock.patch.object(wrap_pycurl.CurlMulti, "perform")
    def test_pycurl_conn_timeout(self, mock_pycurl_multi_perform):
        mock_pycurl_multi_perform.side_effect = pycurl.error(
            pycurl.E_OPERATION_TIMEDOUT, "Connection timed out after 1440 milliseconds"
        )
        try:
            http_call(
                method="GET",
                url="https://localhost/",
                timeout=(0.01, 3),
                driver="pycurl",
            )
            ex = None
        except Exception as e:
            ex = e

        assert isinstance(ex, requests.exceptions.ConnectTimeout)
        assert "Connection timed out" in str(ex)
        assert isinstance(ex, HttpCallException)
        assert ex.error_code == "couldnt_connect_timeout"

    @mock.patch("requests.request")
    def test_requests_conn_refused(self, mock_requests_request):
        mock_requests_request.side_effect = Exception(
            "Failed to establish a new connection: [Errno 111] Connection refused"
        )
        try:
            http_call(method="GET", url="https://localhost/", driver="requests")
            ex = None
        except Exception as e:
            ex = e

        assert isinstance(ex, requests.exceptions.ConnectionError)
        assert "Connection refused" in str(ex)
        assert isinstance(ex, HttpCallException)
        assert ex.error_code == "couldnt_connect_refused"

    @mock.patch.object(wrap_pycurl.CurlMulti, "perform")
    def test_pycurl_conn_refused(self, mock_pycurl_multi_perform):
        mock_pycurl_multi_perform.side_effect = pycurl.error(
            pycurl.E_COULDNT_CONNECT,
            "Failed to connect to localhost port 443: Connection refused",
        )
        try:
            http_call(method="GET", url="https://localhost/", driver="pycurl")
            ex = None
        except Exception as e:
            ex = e

        assert isinstance(ex, requests.exceptions.ConnectionError)
        assert "Connection refused" in str(ex)
        assert isinstance(ex, HttpCallException)
        assert ex.error_code == "couldnt_connect_refused"

    @mock.patch("requests.request")
    def test_requests_bad_cert_file(self, mock_requests_request):
        mock_requests_request.side_effect = Exception("SSL_CTX_use_certificate_file")
        try:
            http_call(
                method="GET",
                url="https://somesite/",
                cert=("abc", "cde"),
                driver="requests",
            )
            ex = None
        except Exception as e:
            ex = e

        assert isinstance(ex, requests.exceptions.SSLError)
        assert "SSL_CTX_use_certificate_file" in str(ex)
        assert isinstance(ex, HttpCallException)
        assert ex.error_code == "ssl_connect_error"

    @mock.patch.object(wrap_pycurl.CurlMulti, "perform")
    def test_pycurl_bad_cert_file(self, mock_pycurl_multi_perform):
        mock_pycurl_multi_perform.side_effect = pycurl.error(
            pycurl.E_SSL_CONNECT_ERROR, "error reading X.509 key or certificate file"
        )
        try:
            http_call(
                method="GET",
                url="https://somesite/",
                cert=("abc", "cde"),
                driver="pycurl",
            )
            ex = None
        except Exception as e:
            ex = e

        assert isinstance(ex, requests.exceptions.SSLError)
        assert "error reading X.509 key or certificate file" in str(ex)
        assert isinstance(ex, HttpCallException)
        assert ex.error_code == "ssl_connect_error"

    @mock.patch("requests.request")
    def test_requests_site_invalid_cert(self, mock_requests_request):
        mock_requests_request.side_effect = Exception(
            "SSLError(SSLError(1, u'[SSL: CERTIFICATE_VERIFY_FAILED] "
            "certificate verify failed (_ssl.c:726)'),)"
        )
        try:
            http_call(
                method="GET", url="https://expired.badssl.com/", driver="requests"
            )
            ex = None
        except Exception as e:
            ex = e

        assert isinstance(ex, requests.exceptions.SSLError)
        assert "certificate verify failed" in str(ex)
        assert isinstance(ex, HttpCallException)
        assert ex.error_code == "ssl_cacert"

    @mock.patch.object(wrap_pycurl.CurlMulti, "perform")
    def test_pycurl_site_invalid_cert(self, mock_pycurl_multi_perform):
        mock_pycurl_multi_perform.side_effect = pycurl.error(
            pycurl.E_SSL_CACERT,
            "server certificate verification failed. CAfile: "
            "/etc/ssl/certs/ca-certificates.crt CRLfile: none",
        )
        try:
            http_call(
                method="GET",
                url="https://somesite/",
                cert=("abc", "cde"),
                driver="pycurl",
            )
            ex = None
        except Exception as e:
            ex = e

        assert isinstance(ex, requests.exceptions.SSLError)
        assert "certificate verification failed" in str(ex)
        assert isinstance(ex, HttpCallException)
        assert ex.error_code == "ssl_cacert"


# vim:ts=4:sts=4:sw=4:tw=88:et:
