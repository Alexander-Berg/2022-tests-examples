import sys

with open(sys.argv[1]) as random_queries:
    for line in random_queries:
        line = line.strip()
        print line
        print "{} HTTP/1.0\\nAccept: application/x-protobuf\\n\\n".format(line)

for filename in sys.argv[2:]:
    with open(filename) as special_queries:
        for line in special_queries:
            print line.strip()
