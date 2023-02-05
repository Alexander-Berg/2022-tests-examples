import Yandex
import time
from tests.common import TransactionalTestCase
from WikimapPy.impl import Wikimap
from WikimapPy import db
from ytools.xml import AssertXML


class ReadonlyCheckTest(TransactionalTestCase):
    def __init__(self, *args, **kwargs):
        self.servant = Wikimap()
        super(ReadonlyCheckTest, self).__init__(*args, **kwargs)

    @db.require_rw()
    def _dummy(self): pass

    def testDefaultMode(self):
        AssertXML(self.servant.IsWriteAvailable()).equal('/available', 'true')
        AssertXML(self.servant.IsReadonlyForced()).equal('/forced', 'false')
        self._dummy()

    def testRoMode(self):
        db.RoChecker.check_interval = 0.1 # seconds
        db.lock_write(0)
        time.sleep(0.2) # seconds
        AssertXML(self.servant.IsWriteAvailable()).equal('/available', 'false')
        AssertXML(self.servant.IsReadonlyForced()).equal('/forced', 'true')
        self.assertRaises(db.RoChecker.RoException, self._dummy)
        db.unlock_write()
        time.sleep(0.2) # seconds
        AssertXML(self.servant.IsWriteAvailable()).equal('/available', 'true')
        AssertXML(self.servant.IsReadonlyForced()).equal('/forced', 'false')
        self._dummy()
