import sys
import urllib2

addr = sys.argv[1]
inpf = sys.argv[2]
outpf = sys.argv[3]
line_append = sys.argv[4] if len(sys.argv) > 4 else '&ms=proto&hr=da&service=tests'

if not addr.startswith('http:'):
    addr = 'http://' + addr
if  addr[-1] != '/':
    addr = addr + '/'

cnt = 1
delim = '-'*100

with open(outpf, 'w') as outp:
    for line in open(inpf, 'r').readlines():
        try:
            resp = urllib2.urlopen(addr + line.strip() + line_append).read()
        except Exception as e:
            print 'exception %s, url: %s' % (e, addr + line.strip() + line_append)
            try:
                resp = e.read()
            except:
                resp = ''
        outp.write(resp)
        outp.write('\n' + delim + str(cnt) + '\n')
        cnt += 1

