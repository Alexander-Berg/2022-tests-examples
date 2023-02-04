import mongoengine as me
from bson import json_util
from pymongo.collection import Collection
from pymongo.read_preferences import ReadPreference

from billing.apikeys.apikeys import rpcutil
from billing.apikeys.apikeys.http_core import HttpError
from billing.apikeys.apikeys.mapper import context as ctx
from billing.apikeys.apikeys.rpcutil import XMLRPCInvalidParam


class arg_bson(rpcutil.arg_str):
    @staticmethod
    def type_instance(v, attribs={}, ti=None):
        string = rpcutil.arg_str.type_instance(v, attribs, ti)
        if string:
            try:
                return json_util.loads(string)
            except ValueError:
                raise XMLRPCInvalidParam(attribs['context'], "invalid json string")


class Logic:

    def __init__(self):
        super().__init__()
        self.db = me.connection.get_db('apikeys-cloud')

    def _get_collection(self, collection_name):
        collection = getattr(self.db, collection_name)
        if not isinstance(collection, Collection):
            raise HttpError(400, 'Wrong collection name')
        return collection

    @rpcutil.call_description({
        'collection': rpcutil.arg_str(mandatory=True),
        'document': arg_bson(mandatory=True),
    })
    def insert(self, params):
        collection = self._get_collection(params['collection'])
        return {
            'result': collection.insert(params['document'])
        }

    @ctx.context_deco(ctx.ReadPreferenceSettings(ReadPreference.PRIMARY_PREFERRED))
    @ctx.context_deco(ctx.NoCacheSettings())
    @rpcutil.call_description({
        'collection': rpcutil.arg_str(mandatory=True),
        'query': arg_bson(),
    })
    def find(self, params):
        collection = self._get_collection(params['collection'])
        return list(collection.find(params.get('query', {})))

    @rpcutil.call_description({
        'collection': rpcutil.arg_str(mandatory=True),
        'query': arg_bson(),
        'update': arg_bson(mandatory=True),
    })
    def update(self, params):
        collection = self._get_collection(params['collection'])
        return {
            'result': collection.update(params.get('query', {}), params['update']),
        }

    @rpcutil.call_description({
        'collection': rpcutil.arg_str(mandatory=True),
        'query': arg_bson(mandatory=True),
    })
    def remove(self, params):
        collection = self._get_collection(params['collection'])
        return {
            'result': collection.remove(params['query'])
        }
