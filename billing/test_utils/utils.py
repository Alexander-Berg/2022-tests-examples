import os

from yql.api.v1.client import YqlClient


def create_yql_client():
    return YqlClient(
        server=os.environ['YQL_HOST'],
        port=int(os.environ['YQL_PORT']),
        db=os.environ['YQL_DB'],
        db_proxy=os.environ['YT_PROXY'],
    )
