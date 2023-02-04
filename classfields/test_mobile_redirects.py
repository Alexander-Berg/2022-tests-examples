import requests
from requests.packages.urllib3.exceptions import InsecureRequestWarning
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
import re
import sys

domain = "auto.ru"
desktop_url = "https://auto.ru"
mobile_url = "https://m.auto.ru"
prefix = "https"
user_agent = "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, " \
                                          "like Gecko) Version/9.0 Mobile/13B143 Safari/601.1"
ip = "2a02:6b8:0:40c:d048:4b0d:e0fc:4c22"

def check_urls(urls1, urls2, urls3):
    err = []
    i = 0
    while i < len(urls1):
        url1 = urls1[i]
        url2 = urls2[i]
        url3 = urls3[i]
        i+=1
        #print("URL: " + url1 + "\n")
        headers = {'user-agent': user_agent, 'x-real-ip': ip}
        cookies = dict(gids='213', autotest='1')

        try:
            r = requests.get(url1, headers=headers, verify=False, cookies=cookies, allow_redirects=False)
        except requests.exceptions.RequestException as e:
            error = "URL: " + url1 + "\nUnknown error"
            err.append(error)
            continue
        
        if (r.status_code == 301 or r.status_code == 302) and r.headers['Location'] != url2:
           error = "URL: " + url1 + "\nLOCATION: " + r.headers['Location']  + "\nEXP LOCATION: " + url2
           err.append(error)
        
        try:
            r = requests.get(url1, headers=headers, verify=False, cookies=cookies)
        except requests.exceptions.RequestException as e:
            error = "URL: " + url1 + "\nUnknown error"
            err.append(error) 
            continue   
        
        if r.status_code != 200:
            error = "URL: " + url1 + "\nRED: " + r.url +"\nERROR: " + str(r.status_code)
            err.append(error)
            continue
        
        if r.url != url3:
            error = "URL: " + url1 + "\nRED: " + r.url + "\nEXP: " + url3
            err.append(error)
    
    return err


f = open(sys.argv[1], "r", encoding="utf-8")
urls = f.readlines()
f.close()
urls = [url.strip() for url in urls]

urls_from = []
locations = []
urls_to = []

i = 0
while i < len(urls): 
    #urls[i] = urls[i].replace("https://auto.ru", desktop_url)
    #urls[i] = urls[i].replace("auto.ru", domain)
    urls_from.append(urls[i])
    
    #urls[i+1] = urls[i+1].replace("https://m.auto.ru", mobile_url)
    #urls[i+1] = urls[i+1].replace("auto.ru", domain)
    locations.append(urls[i+1])
        
    #urls[i+2] = urls[i+2].replace("https://m.auto.ru", mobile_url)
    #urls[i+2] = urls[i+2].replace("auto.ru", domain)
    urls_to.append(urls[i+2])
    
    i+=4

print("Domain: " + domain + "\n")

errors = check_urls(urls_from, locations, urls_to)
if errors:
    print("ERRORS(" + str(len(errors)) + ")=============================================================================\n")
    for e in errors:
        print(e)
        print("\n")
    exit(1)

exit(0)
