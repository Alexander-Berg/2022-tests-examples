import yt.yson as yson


def create_table(table_path, table, yt_client):
    yt_client.create('table', table_path, attributes=yson.json_to_yson(table['attributes']))
    yt_client.write_table(yt_client.TablePath(table_path, append=True), table['data'], format='json')


def get_table_data():
    return {
        'data': [
            {'Version': 1, 'Vector': 'First tensor 1'},
            {'Version': 3, 'Vector': 'Third tensor 1'},
        ],
        'attributes': {
            'schema': {
                '$value':
                    [
                        {'required': False, 'type': 'int32', 'name': 'Version'},
                        {'required': False, 'type': 'string', 'name': 'Vector'},
                    ],
                '$attributes': {'strict': True, 'unique_keys': False}
            }
        }
    }
