#!/usr/bin/python3

import requests
from requests.packages.urllib3.exceptions import InsecureRequestWarning
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
import sys

domain = "auto.ru"
user_agent = ""

def check_urls(urls1, urls2):
    err = []
    i = 0
    while i < len(urls1):
        url1 = urls1[i]
        url2 = urls2[i]
        i+=1
        #print("URL: " + url1 + "\n")
        try:
            cookies = dict(autotest='1')
            r = requests.get(url1, verify=False, cookies=cookies)
        except requests.exceptions.TooManyRedirects:
            error = "URL: " + url1 + "\nERROR: Too many redirects"
            err.append(error)
            print(error + "\n")
            continue
        except requests.exceptions.ConnectionError:
            error = "URL: " + url1 + "\nERROR: Connection error"
            err.append(error)
            print(error + "\n")
            continue
        if r.status_code != 200:
            error = "URL: " + url1 + "\nERROR: " + str(r.status_code)
            err.append(error)
            print(error + "\n")
            continue
        if r.url != url2:
            error = "URL: " + url1 + "\nREDIRECT: " + r.url + "\nEXPECTED: " + url2
            err.append(error)
            print(error + "\n")
    return err

f = open(sys.argv[1], "r", encoding="utf-8")
urls_from = []
urls_to = []
i = 1
for line in f:
    line = line.strip()
    if line == "":
        continue
    #line = line.replace("http", prefix)
    line = line.replace("auto.ru", domain)
    if i % 2 == 0:
        urls_to.append(line)
    else:
        urls_from.append(line)
    i+=1
f.close()

errors = check_urls(urls_from, urls_to)
if errors:
    print("ERRORS(" + str(len(errors)) + ")=============================================================================\n")
    #for err in errors:
    #    print(err + "\n")
    exit(1)

exit(0)
