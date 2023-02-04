
import urllib2
import time
import sys
import os
import threading
from multiprocessing import Process, Queue

import string
import random
import json

address = sys.argv[1]
ndocs = int(sys.argv[2])
corut = int(sys.argv[3])
resfilename = sys.argv[4]

def GenerateText(leng, dl = 0):
    if dl:
        leng = random.randint(leng - dl, leng + dl)
    if leng <= 0:
        leng = 1
    return ''.join(random.choice(string.ascii_lowercase + '    ') for i in range(leng))

def newreq(i):
    text = GenerateText(1024, 300)
    fields = {"prefix": 10, "action": "modify",
              "docs": [{"options": {"mime_type": "text/html", "charset": "utf-8", "language": "rus"},
                        "url": "http://localhost/i%s" % str(i), "body": text}]} #% (str(i), text)
    return json.dumps(fields)

def doload_single(n, address):
    result = {}
    rcnt = 0
    rtolog = 1
    print 'start sender...'
    for i in range(n):
        request = urllib2.Request('http://' + address)
        request.add_header('Content-Type', 'text/json')
        req_fields = newreq(i)
        #print req_fields
        code=0
        try:
            resp = urllib2.urlopen(request, req_fields)
            code = resp.getcode()
        except urllib2.HTTPError as e:
            code = e.code
        except Exception as e:
            print e

        result[str(code)] = result.get(str(code), 0) + 1
        rcnt += 1
        if rcnt == rtolog:
            print 'request %s, code %s' % (rtolog, code)
            sys.stdout.flush()
            rtolog *= 2

    return result

class LoadProcess(Process):
    def __init__(self, n, address, que):
        self.addr = address
        self.n_docs = n
        self.que = que
        Process.__init__(self)
    def run(self):
        try:
            self.result = doload_single(self.n_docs, self.addr)
            self.que.put(self.result)
        except:
            self.que.put('error')

def doload(address, n, corut):
    coruts = []
    try:
        resfile = open(resfilename, 'w')
    except:
        print "can't write file %s" % resfilename
        sys.exit(1)

    queues = []
    startt = time.time()
    for i in range(corut):
        queues.append(Queue())
        lt = LoadProcess(n, address, queues[-1])
        print 'process %s starting' % i
        lt.start()
        print 'process %s started' % i
        coruts.append(lt)

    results = []
    for que in queues:
        results.append(que.get())

    stopt = time.time()
    ftime = stopt - startt

    resstat = {}

    greqs = 0
    ereqs = 0
    for res in results:
        if res == 'error':
            print 'error in process'
            continue
        greqs += res.get('200', 0)
        for code in res:
            ereqs += res[code]
            resstat[code] = resstat.get(code, 0) + res[code]
        ereqs -= res.get('200', 0)

    resfile.write('full time \t %s\n' % ftime)
    resfile.write('requests \t %s\n' % str(greqs + ereqs))
    resfile.write('error requests \t %s\n' % ereqs)
    resfile.write('code_not_200 \t %s\n' % ereqs)

    for code in resstat:
        resfile.write('code_%s %s\n' % (code, resstat[code]))
    rps = greqs/ftime
    resfile.write('requests/sec \t %s' % rps)

    resfile.close()

if  __name__ == '__main__':
    doload(address, ndocs, corut)
