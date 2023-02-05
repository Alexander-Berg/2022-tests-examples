# encoding: utf-8
from copy import deepcopy
from shapely import wkb

class ObjectsDbMock(object):
    def __init__(self):
        self.objects = {}
        self.objects_backup = {}

    def add_object(self, oid, **info):
        self.objects[oid] = info
        self.objects[oid]['id'] = oid

    def set_revision(self, oid, revision):
        self.objects[oid]['revision_id'] = revision

    def is_buildings_territory(self, oid, geometry_wkb, category_id):
        geometry = wkb.loads(str(geometry_wkb))
        for sub_oid, sub_info in self.objects.iteritems():
            if sub_oid == oid:
                continue

            if sub_info.get('is_building', False) and\
                sub_info['category_id'] == category_id and\
                geometry.contains(wkb.loads(sub_info['geometry'])):
                    return True
        return False

    def set_icon_zmin(self, oid, zmin, rev_id):
        old_zmin = self.objects[oid]['icon_zmin']
        if self.objects[oid]['revision_id'] == rev_id:
            self.objects[oid]['icon_zmin'] = zmin
        return old_zmin != zmin

    def object_info(self, oid):
        return self.objects[oid]

    def near_objects_info(self, oid, zmax, center_wkb, dist):
        center = wkb.loads(str(center_wkb))
        return [sub_info for sub_oid, sub_info in self.objects.iteritems()\
                    if sub_oid != oid and\
                       sub_info['state'] < 2 and\
                       sub_info['icon_zmin'] <= zmax and\
                       wkb.loads(sub_info['center']).distance(center) <= dist]

    def commit(self):
        self.objects_backup = deepcopy(self.objects)

    def rollback(self):
        self.objects = deepcopy(self.objects_backup)
