# coding: utf-8
import Cookie
from nose.tools import ok_, eq_
from urllib import urlencode
from ytools.xml import AssertXML, ET

#local imports
import tools
from tasks import app


class MoveTaskTestCase(tools.AuthorizationTestCase, tools.LogErrorsTestCase):
    app = app

    def test_move(self):
        data = """<?xml version="1.0" encoding="utf-8"?>
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
        dx, dy = 10., 20.
        layers = '1,2,3'
        cookie = Cookie.SimpleCookie()
        cookie['Session_id'] = tools.AuthorizationTestCase.MODERATOR_SESSION_ID
        create_task = tools.urlopen(app, '/objects/move',
          query=urlencode(dict(dx=dx, dy=dy, layers=layers)), post=data,
          env={'HTTP_COOKIE': str(cookie)})
        eq_(create_task['code'], 200)
        AssertXML(create_task['body']).exist('/task')
        task_id = int(ET.XML(create_task['body']).text)

        #check task status
        task_status = tools.urlopen(app, '/move-objects/%s' % task_id)
        eq_(task_status['code'], 200)
        AssertXML(task_status['body'])\
          .exist('/task')\
          .exist('/task/desc')\
          .equal('/task/status', 'open')\
          .equal('/task/created-by', str(tools.AuthorizationTestCase.MODERATOR_UID))\
          .equal('/task/data/dx', str(dx))\
          .equal('/task/data/dy', str(dy))\
          .equal('/task/data/layers', layers)

        #list tasks
        page = 1
        per_page = 10
        list_tasks = tools.urlopen(app, '/', query=urlencode({'page': page, 'per-page': per_page}))
        eq_(list_tasks['code'], 200)
        AssertXML(list_tasks['body'])\
          .exist('/tasks')\
          .equal('/tasks/@page', str(page))\
          .equal('/tasks/@per-page', str(per_page))\
          .equal('/tasks/@total-pages', '1')\
          .equal('/tasks/@total-count', '1')\
          .exist('/tasks/task')

        #list only move-objects tasks
        list_tasks = tools.urlopen(app, '/move-objects',
            query=urlencode({'page': page, 'per-page': per_page}))
        eq_(list_tasks['code'], 200)
        AssertXML(list_tasks['body'])\
          .exist('/tasks')\
          .equal('/tasks/@page', str(page))\
          .equal('/tasks/@per-page', str(per_page))\
          .equal('/tasks/@total-pages', '1')\
          .equal('/tasks/@total-count', '1')\
          .count_equal('/tasks/task', 1)

        list_tasks = tools.urlopen(app, '/objects/move',
            query=urlencode({'page': page, 'per-page': per_page}))
        eq_(list_tasks['code'], 200)
        AssertXML(list_tasks['body'])\
          .exist('/tasks')\
          .equal('/tasks/@page', str(page))\
          .equal('/tasks/@per-page', str(per_page))\
          .equal('/tasks/@total-pages', '1')\
          .equal('/tasks/@total-count', '1')\
          .count_equal('/tasks/task', 1)

