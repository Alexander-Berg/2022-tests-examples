
try:
    import elementtree.ElementTree as et
except:
    import xml.etree.ElementTree as et

import ycorba
import Yandex__POA

class Test(Yandex__POA.Test):
    def __init__(self, cfg):
        pass

    @ycorba.ui_method
    def helloWorld(self):
        return 'Hello, World!'

    @ycorba.ui_method
    def raiseExc(self):
        raise Exception('exception')

    @ycorba.ui_method
    def returnElementTree(self):
        return et.ElementTree(et.Element('a'))

    @ycorba.ui_method
    def touchRequest(self, req):
        req.getRequestData()

    @ycorba.ui_method_with_out_params
    def multipleOut(self):
        return (et.ElementTree(et.Element('a')),
                'aaa')

ycorba.main(Test)
