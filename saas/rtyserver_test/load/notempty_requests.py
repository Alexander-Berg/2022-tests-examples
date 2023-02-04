import json
import sys
import urllib2

addr = sys.argv[1]
inp = sys.argv[2]
outp = sys.argv[3]
add_onreq = sys.argv[4] if len(sys.argv) > 4 else ''
add_toreq = sys.argv[5] if len(sys.argv) > 5 else ''


with open(inp, 'r') as f:
    with open(outp, 'w') as fo:
        err_count = 0
        total_count = 0
        nonzero_count = 0
        for line in f.readlines():
            query = line.strip()
            total_count += 1
            try:
                req = urllib2.urlopen(addr + query + '&' + add_onreq + '&format=json', timeout=10)
                res = json.loads(req.read())
                count = int(res['response']['found']['all'])
                if not count:
                    continue
            except Exception as e:
                err_count += 1
                print '%s' % e
                continue
            nonzero_count += 1
            fo.write(query + add_toreq + '\n')
print 'total=%s, nonzero=%s, errors=%s\n' % (total_count, nonzero_count, err_count)
