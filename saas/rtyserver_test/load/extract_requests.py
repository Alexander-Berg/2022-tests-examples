import sys

inp = sys.argv[1]
outp = sys.argv[2]


with open(inp, 'r') as f:
    PQ_LABEL = 'processed-query='
    with open(outp, 'w') as fo:
        for line in f.readlines():
            pq = [p for p in line.split() if p.startswith(PQ_LABEL)]
            if not pq:
                continue
            query = pq[0][len(PQ_LABEL):]
            fo.write('/?' + query + '\n')
