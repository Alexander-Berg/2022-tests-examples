#!/usr/bin/python
# -*- coding: utf-8 -*-

from commit import CommitData
from crucible import Crucible, CrucibleConfig
from valuemap import ValueMap
from crucibleserver import HttpServerError

import unittest

class Review:
    # create review
    # @param rs: review system
    # @param data: commit data
    # @param key: review id (move to data)
    #   if key is empty, creates new review, 
    #   otherwise trying to find existing one
    def __init__(self, rs, data, key = None):
        self.rs = rs
        self.data = data
        if (key == None or key == ""):
            self.key, self.closed = self.rs.createReview(data)
        else:
            self.key, self.closed = self.rs.getReview(key)

    def update(self, diff):
        self.rs.updateReview(self.key, diff)


############### usage example
def review(cr, key, data):
    r = Review(cr, data, key)
    if r.closed:
        print "review",r.key,"has been closed, commiting revision $id"
    else:
        r.update(data)

class TestCrucible(unittest.TestCase):

    def setUp(self):
        self.cr = Crucible(CrucibleConfig("config.xml"))
	self.closedReview = "MAPS-15"

    def failUnlessRaisesEx(self, excClass, callableObj, *args, **kwargs):
	try:
	    callableObj(*args, **kwargs)
	except excClass, ex:
	    print str(ex)
	    return
	else:
	    if hasattr(excClass,'__name__'): excName = excClass.__name__
	    else: excName = str(excClass)
	    raise self.failureException, "%s not raised" % excName
											       
    def testBadServer(self):
	crconfig = CrucibleConfig("config.xml")
	crconfig.host = "http://blabla.ff/"
	cr = Crucible(crconfig)
	self.failUnlessRaisesEx(HttpServerError, cr.getReview, self.closedReview)
	
    def testBadUrl(self):
	crconfig = CrucibleConfig("config.xml")
	crconfig.host = crconfig.host[:-1]
	
	cr = Crucible(crconfig)
	self.failUnlessRaisesEx(HttpServerError, cr.getReview, self.closedReview)
    
    def testBadAuth(self):
	crconfig = CrucibleConfig("config.xml")
	crconfig.password = ''
	cr = Crucible(crconfig)
	self.failUnlessRaisesEx(HttpServerError, cr.getReview, self.closedReview)
	
    def testBadRequest(self):
	self.failUnlessRaisesEx(Exception, self.cr.getReview, "MAPS-15999999999999")

    def testGetReview(self):
        key, closed = self.cr.getReview(self.closedReview)
        print key, closed

    def testCreateReview(self):
	patch = """Index: trunk/tools/svnscripts/config.xml
===================================================================
--- trunk/tools/svnscripts/config.xml (revision 7866)
+++ trunk/tools/svnscripts/config.xml
@@ -4,5 +4,4 @@
 <project>MAPS</project>
 <login>mapssvn</login>
 <password>1234</password>
-    <moderator>4c4d</moderator>
</crucible>"""
	patch2="""Index: trunk/tools/svnscripts/crucibleserver.py
===================================================================
--- crucibleserver.py   (revision 7866)
+++ crucibleserver.py   (working copy)
@@ -20,6 +20,7 @@
	
    def postHttp(self, method, data):
	url=self.host+method
+       data = u'<?xml version="1.0" encoding="' + self.encoding + '"?>' + data
        return self.queryHttp(url, data.encode(self.encoding))
			       
    def getHttp(self, method, **args):"""	    
	    
        cdata = dict({"author": "strashnov", "moderator":"4c4d", "name": "review name", "description": "review description", "patch": ""})
        key, closed = self.cr.createReview(cdata)	
	print key, closed
	self.cr.addPatch(key, patch)
      
if __name__ == '__main__':
    unittest.main()
