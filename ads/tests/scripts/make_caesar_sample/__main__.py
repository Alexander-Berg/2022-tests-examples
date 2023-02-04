from ads.bsyeti.big_rt.lib.events.proto.event_pb2 import TEventMessage
from ads.bsyeti.big_rt import py_lib as bigrt
from library.python.framing.unpacker import Unpacker
from yt.yson.convert import to_yson_type

import json
import os
import time
import yt.yson as yson
import yt.wrapper as yt

from grut.python.sampler.sampler import make_sample


# directory on hahn, where ypu will store grut samples
YT_WORKING_DIRECTORY = "//home/bigb/caesar/test_data/grut_sample"
ROWS_MULTIPLIER = 100
QUEUES = {
    'OrderID': '//home/bigb_resharded_queues/caesar/stable/EventsQueues/OrderID',
    'BannerID': '//home/bigb_resharded_queues/caesar/stable/EventsQueues/BannerID',
    'AdGroupID': '//home/bigb_resharded_queues/caesar/stable/EventsQueues/AdGroupID'
}

MESSAGE_TYPES_WHITELIST = {
    'OrderID': {109, 137},
    'BannerID': {102, 112},
    'AdGroupID': {135},
}

ROWS_LIMIT = {
    'OrderID': 1000,
    'BannerID': 1000,
    'AdGroupID': 1000,
}

GRUT_ROWS_LIMIT = {
    'OrderID': 1000,
    'BannerID': 0,
    'AdGroupID': 0,
}


def run_sample():
    assert os.environ['YQL_TOKEN']

    os.environ['YT_PROXY'] = 'markov'
    res = {}
    global_ids = {}
    max_timestamp = 0

    for queue in QUEUES:
        all_rows = set()
        total_size = 0
        grut_size = 0
        ids = set()
        additional_ids = set()
        while total_size < ROWS_LIMIT[queue]:
            queue_instance = bigrt.YtQueue(QUEUES[queue])
            for shard in range(0, 30):
                result = queue_instance.read(shard, 0, ROWS_MULTIPLIER * ROWS_LIMIT[queue])['rows']
                for raw_row in result:
                    unpacker = Unpacker(raw_row)
                    while True:
                        tmp_msg, skip_data = unpacker.next_frame()
                        if tmp_msg is None or total_size >= ROWS_LIMIT[queue]:
                            break
                        event = TEventMessage().FromString(tmp_msg)

                        # grut messages
                        if event.Type == 300 and grut_size < GRUT_ROWS_LIMIT[queue]:
                            all_rows.add(tmp_msg)
                            grut_size += 1
                            ids.add(event.ProfileID)
                            max_timestamp = max(max_timestamp, event.TimeStamp)

                        # profile creating messages
                        if event.Type in MESSAGE_TYPES_WHITELIST[queue] and event.ProfileID not in ids:
                            all_rows.add(tmp_msg)
                            total_size += 1
                            ids.add(event.ProfileID)
                            max_timestamp = max(max_timestamp, event.TimeStamp)

                        # additional messages
                        if event.Type not in MESSAGE_TYPES_WHITELIST[queue] and event.Type != 300 and event.ProfileID in ids and event.ProfileID not in additional_ids:
                            all_rows.add(tmp_msg)
                            total_size += 1
                            ids.add(event.ProfileID)
                            additional_ids.add(event.ProfileID)
                            max_timestamp = max(max_timestamp, event.TimeStamp)
                    if total_size >= ROWS_LIMIT[queue]:
                        break
                if total_size >= ROWS_LIMIT[queue]:
                    break
            print(f'{queue}: current ids:' + str(len(ids)))
            time.sleep(10)
        print(f'{queue}: finished')

        l_ids = list(ids)
        for i in range(len(l_ids)):
            l_ids[i] = int(l_ids[i])
        global_ids[queue] = l_ids
        res[queue] = list(all_rows)

    os.system("mkdir worker_input_sample")
    os.system("mkdir worker_input_sample/grut_data")
    os.system("touch worker_input_sample/metadata.json")
    os.system("touch worker_input_sample/worker_input_sample.yson")

    print('Processing grut data...')

    grut_tables_list = make_sample(global_ids['OrderID'], YT_WORKING_DIRECTORY, os.environ['YQL_TOKEN'])

    # post process grut data
    yt_client = yt.YtClient(proxy='hahn')
    for table in grut_tables_list:
        data = []
        schema = yt_client.get_attribute(f'{YT_WORKING_DIRECTORY}/{table}', "schema")
        rows = yt_client.read_table(f'{YT_WORKING_DIRECTORY}/{table}', format=yt.YsonFormat(encoding=None))
        for row in rows:
            row_dict = {}
            for key in row:
                if key != 'hash':
                    if row[key] is not None:
                        row_dict[key] = row[key]
                    else:
                        # make default value from null
                        for item in schema:
                            if item['name'] == key:
                                if item['type'] == 'string':
                                    row_dict[key] = ''
                                else:
                                    row_dict[key] = 0
            data.append(yson.to_yson_type(row_dict))

        ysn = to_yson_type(data)
        with open(f'worker_input_sample/grut_data/{table}.yson', 'wb') as f:
            f.write(yson.dumps(ysn))

    with open('worker_input_sample/worker_input_sample.yson', 'wb') as f:
        ysn = to_yson_type(res)
        f.write(yson.dumps(ysn))

    metadata_dict = {
        'now': max_timestamp + 1,
        'grut_tables_list': grut_tables_list,
    }

    print(f'Adding timestamp :{max_timestamp + 1}')

    with open('worker_input_sample/metadata.json', 'w') as f:
        json.dump(metadata_dict, f)

    os.system(f"ya upload worker_input_sample -T TASK_LOGS --owner {os.environ['USER']} --ttl 30 -d \'simple caesar worker data sample\'")
    os.system("rm -r worker_input_sample")


if __name__ == '__main__':
    run_sample()
