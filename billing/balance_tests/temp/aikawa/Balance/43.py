import xmlrpclib

proxy = xmlrpclib.Server("http://localhost:8000/")
print "3 is even: %s" % str(proxy.find_client(3))

