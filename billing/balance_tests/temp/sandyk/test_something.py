__author__ = 'sandyk'

class Figure(object):

    def __init__(self, height, width):
        self.height = height
        self.width = width

    def get_square(self):
        print (self.height*self.width)

    def __repr__(self):
        return 'Figure with height {} and width {}'.format(self.height, self.width)

a = Figure(5,10)
b = Figure(10,20)
c = 1
pass
