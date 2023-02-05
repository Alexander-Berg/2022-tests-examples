from maps.poi.personalized_poi.builder.lib.recommendations_utils import (
    calculate_recommendations_baseline
)
from maps.poi.personalized_poi.builder.lib.ut.recommendations_rows import (
    baseline_recommendations_rows
)
from yql.api.v1.client import YqlClient

import sys
import json


def test_recommendations_baseline(yql_api):
    yt = yql_api.yt
    yql_client = YqlClient(db='plato', server='localhost', port=yql_api.port)
    yt_client = yt.yt_wrapper.YtClient(config=yt.yt_wrapper.config.config)

    for test_case in baseline_recommendations_rows:
        yt_client.write_table('//features_schematized', test_case['features'])
        yt_client.write_table('//regulargeo', test_case['regulargeo'])

        recommendations_table = calculate_recommendations_baseline(
            yt_client, yql_client, '//features_schematized', '//regulargeo')

        recommendations = [row for row in yt_client.read_table(recommendations_table)]
        assert recommendations == test_case['recommendations']
