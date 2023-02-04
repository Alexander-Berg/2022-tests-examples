#!/usr/bin/env python

import os

from yp import client
from yp_proto.yp.client.api.proto import object_service_pb2
import yp.data_model as data_model
import yt_yson_bindings

if __name__ == '__main__':
    token = os.getenv('TOKEN')
    c = client.YpClient(address='m01.sas-test.yp.yandex.net:8090', config={'token': token})
    s = c.create_grpc_object_stub()
    req = object_service_pb2.TReqCreateObject()
    req.object_type = data_model.OT_SCHEMA
    req.attributes = yt_yson_bindings.dumps({
        'meta': {
            'id': 'replica_set',
            'acl': [{
                'action': 'allow',
                'subjects': ['everyone'],
                'permissions': ['read', 'create']
            }]
        }
    })
    print s.CreateObject(req)
