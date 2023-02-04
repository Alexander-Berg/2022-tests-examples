""" Set of unittest for staff.py module """
import pickle

import requests

from staff import Staff


def _prepare_session():
    session = requests.Session(serialize=True)
    session.headers = {'header1': 'value1'}
    f = open('/tmp/session.pdump', 'wb')
    pickle.dump(session, f)
    f.close()


def _prepare_response():
    response = requests.Response(serialize=True)
    response._response_str = '{ "body": "<html></html>"}'
    f = open('/tmp/session_response.pdump', 'wb')
    pickle.dump(response, f)
    f.close()


def test_get_user_info():
    _prepare_session()
    _prepare_response()
    staffobj = Staff(oauth_token='blablala-token')
    result = staffobj.get_user_info(login='barmaley')
    assert(result == '{ "body": "<html></html>"}')
