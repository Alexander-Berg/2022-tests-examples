import os
import unittest

import html5lib
from bs4 import BeautifulSoup

import genisys.web.app


class GenisysWebTestCase(unittest.TestCase):
    CONFIG = {}

    def setUp(self):
        os.environ['GENISYS_WEB_CONFIG'] = '../../test/web/config.py'
        self.app = genisys.web.app.make_app()
        self.config = self.app.config
        self.config.update(self.CONFIG)
        self.client = self.app.test_client()
        self.storage = self.app.storage
        self.database = self.storage.db
        self.clear_db()
        self.storage.init_db(self.config['ROOT_OWNERS'])

    def tearDown(self):
        super(GenisysWebTestCase, self).tearDown()
        self.clear_db()

    def clear_db(self):
        for collection in self.database.collection_names(False):
            self.database[collection].drop()

    def _request_html(self, method, url, **kwargs):
        response = getattr(self.client, method)(url, **kwargs)
        self.assertEquals(response.status_code, 200)
        self.assertEquals(response.content_type, 'text/html; charset=utf-8')

        parser = html5lib.HTMLParser()
        parser.parse(response.data)
        if parser.errors:
            (line, col), error, args = parser.errors[0]
            lines = response.data.decode('utf8').splitlines()
            firstline = max(0, line - 15)
            seen_first_nonempty = False
            for i, line in enumerate(lines[firstline:line]):
                if not line and not seen_first_nonempty:
                    continue
                seen_first_nonempty = True
                print("%3d %s" % (i + firstline + 1, line))
            print("   %s^ %s %s" % (" " * col, error, args or ""))
            self.fail("found %d html parsing errors, first one is shown below"
                      % len(parser.errors))

        return BeautifulSoup(response.data, 'html5lib')

    def get_html(self, url, **kwargs):
        return self._request_html('get', url, **kwargs)

    def get_and_redirect(self, url, expected_redirect, **kwargs):
        res = self.client.get(url, **kwargs)
        self.assertEquals(res.status_code, 302)
        self.assertEquals(res.headers['Location'], expected_redirect)

    def _post(self, data, url):
        res = self.client.post(url, data=data)
        if res.status_code // 100 in (4, 5):
            self.fail(BeautifulSoup(res.data, 'html5lib').text)

        errors = None
        if res.status_code == 200:
            html = BeautifulSoup(res.data, 'html5lib')
            errors = {}
            for formgroup in html.select('.form-group.has-error'):
                field = formgroup.find('label').text
                errorlist = [e.text for e in formgroup.select('.error-text')]
                errors[field] = errorlist

        return res, errors

    def post_and_form_errors(self, data, url, expected_errors):
        res, errors = self._post(data, url)
        self.assertEquals(res.status_code, 200)
        self.assertEquals(errors, expected_errors)
        return BeautifulSoup(res.data, 'html5lib')

    def post_and_redirect(self, data, url, expected_redirect):
        res, errors = self._post(data, url)
        if errors:
            print("Found form errors:")
            for field in sorted(errors):
                print("  {}".format(field))
                for error in errors[field]:
                    print("    {}".format(error))
        self.assertEquals(res.status_code, 302)
        self.assertEquals(res.location,
                          'http://test.serv:500%s' % expected_redirect)
        return self.get_html(res.location)

    def post_and_csrf_error(self, url):
        res = self.client.post(url)
        self.assertEquals(res.status_code, 400)
        html = BeautifulSoup(res.data, 'html5lib')
        self.assertEquals(html.find('p').text,
                          'CSRF token missing or incorrect.')

    def post_and_unauthorized(self, data, url):
        res = self.client.post(url, data=data)
        self.assertEquals(res.status_code, 401)

    def post_and_conflict(self, data, url):
        res = self.client.post(url, data=data)
        self.assertEquals(res.status_code, 409)

    def assert_nav(self, html, expected_nav):
        actual_nav = [(a.text, a.attrs['href'])
                      for a in html.select('.nav-breadcrumbs li a')]
        self.assertEquals(expected_nav, actual_nav)
