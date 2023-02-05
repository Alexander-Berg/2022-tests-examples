# -*- coding: utf-8 -*-
import simplejson as json
from pdb import set_trace
import stubout
from tests.common import TransactionalTestCase, MockPumper, Comments
import yandex.maps.wikimap.pumper_http
from WikimapPy.impl import Wikimap
from WikimapPy import db
from WikimapPy.models import ServerTask
from ytools.xml import AssertXML, ET, EM


class ServerTaskTest(TransactionalTestCase):

    geom = """<?xml version="1.0" encoding="utf-8"?>
<gml:MultiGeometry xmlns:gml="http://www.opengis.net/gml">
  <gml:geometryMembers>
    <gml:Polygon>
      <gml:exterior>
        <gml:LinearRing>
          <gml:posList>37.8188535757 44.7310987914 37.8206560202 44.7331636816
        37.8222117014 44.7323797968 37.8204146214 44.7303110544 37.8188535757
        44.7310987914</gml:posList>
        </gml:LinearRing>
      </gml:exterior>
    </gml:Polygon>
  </gml:geometryMembers>
</gml:MultiGeometry>"""

    def __init__(self, *args, **kwargs):
        self.servant = Wikimap()
        self.stubs = stubout.StubOutForTesting()
        self.stubs.Set(yandex.maps.wikimap.pumper_http, 'EventFirer', MockPumper)
        super(ServerTaskTest, self).__init__(*args, **kwargs)

    def testCreateTask(self):
        uid, dx, dy, layers = 111, 10., 20., [1, 2, 3]
        resp = self.servant.MoveLayers(json.dumps(dict(uid=uid, dx=dx, dy=dy, layers=layers,
                                geom=self.geom)))
        AssertXML(resp).exist('/task')
        task_id = int(ET.XML(resp).text)

        status_resp = self.servant.GetTaskStatus(task_id)
        AssertXML(status_resp).exist('/task').exist('/task/desc').equal('/task/status', 'open')\
            .equal('/task/created-by', str(uid))\
            .equal('/task/data/dx', str(dx))\
            .equal('/task/data/dy', str(dy))\
            .equal('/task/data/layers', ','.join(str(l) for l in layers))

        list_resp = self.servant.GetUserTasks(uid, 'move-objects', 1, 10)
        AssertXML(list_resp).exist('/tasks')\
            .equal('/tasks/@page', '1')\
            .equal('/tasks/@per-page', '10')\
            .equal('/tasks/@total-pages', '1')\
            .equal('/tasks/@total-count', '1')\
            .exist('/tasks/task')

        AssertXML(self.servant.GetUserTasks(uid, '', 1, 10)).exist('/tasks')\
            .equal('/tasks/@page', '1')\
            .equal('/tasks/@per-page', '10')\
            .equal('/tasks/@total-pages', '1')\
            .equal('/tasks/@total-count', '1')\
            .exist('/tasks/task')\
            .equal('/tasks/task/status', ['open'])\
            .equal('/tasks/task/created-by', [str(uid)])\
            .equal('/tasks/task/data/dx', [str(dx)])\
            .equal('/tasks/task/data/dy', [str(dy)])\
            .equal('/tasks/task/data/layers', [','.join(str(l) for l in layers)])

        AssertXML(self.servant.GetUserTasks(uid, 'none', 1, 10)).exist('/tasks')\
            .equal('/tasks/@page', '1')\
            .equal('/tasks/@per-page', '10')\
            .equal('/tasks/@total-pages', '0')\
            .equal('/tasks/@total-count', '0')
