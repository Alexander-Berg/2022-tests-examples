#!/usr/bin/env python

import unittest
from rb import ReviewBoard, ReviewBoardConfig

class TestSvnTnx:
    def __init__(self):
        self.author = 'strashnov'
        
    
class TestReviewBoard(unittest.TestCase):
    
    def setUp(self):
        self.rb = ReviewBoard(ReviewBoardConfig("config.xml"))
        
    #def testCreateReview(self):
    #    tnx = TestSvnTnx()
    #    print self.rb.createReview(tnx)
    #def testUpdateReview(self):
    #    tnx = TestSvnTnx()
    #    print self.rb.updateReview('20', tnx)
    
if __name__ == '__main__':
    unittest.main()
