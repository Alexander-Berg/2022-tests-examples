#! /usr/bin/env python
# -*- coding: utf-8 -*-

import json
import requests
import ConfigParser
import sys
# suppress warnings in Urllib3 1.9+
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from requests.packages.urllib3.exceptions import SNIMissingWarning
from requests.packages.urllib3.exceptions import InsecurePlatformWarning

requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
requests.packages.urllib3.disable_warnings(SNIMissingWarning)
requests.packages.urllib3.disable_warnings(InsecurePlatformWarning)

USERNAME = 'monhue'
PASSWORD = 'monhue'

host = "https://127.0.0.1"
def hue_login(user, password, retry=0):
    url = host+"/accounts/login/?next=/"
    session = requests.session()
    r = session.get (url, allow_redirects=False, verify=False)
    csrf = r.cookies['csrftoken']

    payload = {'username': user, 'password': password, 'server':'LDAP', 'csrfmiddlewaretoken': csrf}
    r = session.post(url, data=payload, verify=False, headers={'Referer': url})
    if r.status_code != 200 and len(r.history) != 1 and r.history[0].status_code != 302:
        print "2;ERROR: Can not login into hue"
        sys.exit()
    if r.text.find("var LOGGED_USERNAME = '';") != -1:
        if retry <3 :
            return hue_login(user,password, retry+1)
        else:
            print "2;ERROR: LDAP ERROR"
            sys.exit()

    return session

def main():
    try:
        session = hue_login(user=USERNAME, password=PASSWORD)
        r = session.get(host+"/beeswax/api/autocomplete/", verify=False, allow_redirects=True)
        if r.status_code == 200:
            data = json.loads(r.text)
            if 'code' in data.keys():
                print "2;HTTP Code: {0}, error: {1}".format(data['code'], data['error'])
            elif 'databases' in data.keys() and len(data['databases']) >= 0:
                print "0;OK"
            else:
                print '2; Can not parse JSON'
        else:
            print '2;fetching databases failed'
    except Exception as e:
        print "2;ERROR: {0}".format(e.message)

if __name__ == '__main__':
    main()
