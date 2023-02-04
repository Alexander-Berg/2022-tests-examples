
import urllib2
import time, datetime
import sys
import os
import json
from multiprocessing import Process, Queue

address = sys.argv[1]
reqfile = sys.argv[2]
corut = int(sys.argv[3])
maxdoc = int(sys.argv[4])
resfilename = sys.argv[5]
const_dps = int(sys.argv[6]) if len(sys.argv) > 6 else 0

class Requests:
    def __init__(self, fname):
        with open(fname, 'r') as tfile:
            line = tfile.readline()
            if len(line.strip()) == 0:
                line = tfile.readline()
            try:
                json.loads(line)
                self.sep = 'one_rpl'
            except:
                self.sep = 'double_n'
                pass
        self.file = open(fname, 'r')

    def nextreq(self):
        rtext = []
        wasent = False
        while True:
            line = self.file.readline()
            if self.sep == 'one_rpl':
                return line
            if line == '':
                #print 'newreq'
                #print ''.join(rtext)
                return rtext
            elif line == '\n':
                #print 'NL FOUND'
                if wasent:
                    #print 'newreq_ent'
                    #print ''.join(rtext)
                    return rtext
                else:
                    wasent = 1
            else:
                rtext.append(line.strip('\n'))

    def close(self):
        self.file.close()

def doload_single(reqfile, address, num=0, doc_mcs=0):

    if not os.path.isfile(reqfile):
        print('requests file %s does not exist' % reqfile)
        return
    reqs = Requests(reqfile)

    def getUrl(text):
        url_field = '"url":{'
        ubeg = text.find(url_field) + len(url_field) + len('"value"')
        return text[ubeg+1 : ubeg+2+text[ubeg+2:].find('}')]

    rcnt = 0
    ecnt = 0
    rtolog = 1
    request = urllib2.Request('http://' + address)
    request.add_header('Content-Type', 'text/json')
    startt = time.time()
    print('start load...')
    while True:
        request = urllib2.Request('http://' + address)
        request.add_header('Content-Type', 'text/json')
        req_fields = ''.join(reqs.nextreq()).strip('\n')
        #print req_fields
        if len(req_fields) == 0:
            break
        code=0
        rstart = datetime.datetime.now()
        try:
            print(rstart.strftime("%H:%M:%S.%f proc") + str(num) + ' req' + str(rcnt+ecnt) + ' url ' + getUrl(req_fields) + ' start')
            resp = urllib2.urlopen(request, req_fields)
            code = resp.getcode()
        except urllib2.HTTPError as e:
            code = e.code
        except Exception as e:
            print(e)

        rend = datetime.datetime.now()
        print(rend.strftime("%H:%M:%S.%f proc") + str(num) + ' req' + str(rcnt+ecnt) + ' end, code=' + str(code))

        if code:
            rcnt += 1
            if rcnt == rtolog:
                print('request %s' % rtolog)
                sys.stdout.flush()
                rtolog *= 2
            if rcnt == maxdoc:
                break
            if code not in (200, 202):
                ecnt += 1
        else:
            ecnt += 1
            if ecnt == maxdoc:
                break
        if doc_mcs and rend.microsecond - rstart.microsecond < doc_mcs:
            time.sleep(0.000001 * (doc_mcs - (rend.microsecond - rstart.microsecond)))
            pass

    stopt = time.time()
    ftime = stopt - startt

    result = {}
    result['full_time'] = ftime
    result['reqs_ok'] = rcnt
    result['reqs_er'] = ecnt
    rps = rcnt/ftime
    result['rps'] = rps
    print('process %s, result %s' % (num, result))
    sys.stdout.flush()
    return result

class LoadProcess(Process):
    def __init__(self, reqfile, address, que, pnum=0, doc_mcs=0):
        self.addr = address
        self.rfile = reqfile
        self.que = que
        self.pnum = pnum
        self.doc_mcs = doc_mcs
        Process.__init__(self)
    def run(self):
        try:
            self.que.put(doload_single(self.rfile, self.addr, self.pnum, self.doc_mcs))
        except:
            self.que.put('error')

def doload(reqfile, address, corut):
    coruts = []
    try:
        resfile = open(resfilename, 'w')
    except:
        print("can't write file %s" % resfilename)
        sys.exit(1)

    if const_dps:
        doc_mcs = corut * 1000000 / const_dps
    else:
        doc_mcs = 0

    results = []
    queues = []
    startt = time.time()
    for i in range(corut):
        queues.append(Queue())
        lt = LoadProcess(reqfile, address, queues[-1], i, doc_mcs)
        print('process %s starting' % i)
        lt.start()
        print('process %s started' % i)
        coruts.append(lt)

    for que in queues:
        results.append(que.get())

    print(results)
    sys.stdout.flush()
    stopt = time.time()
    ftime = stopt - startt

    reqs = 0
    erqs = 0
    for res in results:
        if res == 'error':
            print('error in process')
            continue
        reqs += res['reqs_ok']
        erqs += res['reqs_er']

    resfile.write('full time \t %s\n' % ftime)
    resfile.write('requests \t %s\n' % str(reqs + erqs))
    resfile.write('error requests \t %s\n' % erqs)
    rps = reqs/ftime
    resfile.write('requests/sec \t %s' % rps)
    resfile.close()

if  __name__ == '__main__':
    doload(reqfile, address, corut)
