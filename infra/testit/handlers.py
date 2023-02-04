import json
import random
import logging

import tornado.web

log = logging.getLogger(__name__)

ADDITIONAL_TAGS = (('service', 'unstable-yasm-testit'),)


class StatusHandler(tornado.web.RequestHandler):
    def get(self):
        self.write("Ok")


class UnistatHandler(tornado.web.RequestHandler):
    def get(self):
        stat = self._get_stat()
        self.write(json.dumps(stat, separators=(',', ':')))

    @classmethod
    def _get_stat(cls):
        responses_count = random.randint(0, 10)
        responses_times = [round(random.uniform(0, 1), 1) for x in xrange(responses_count)]

        responses_times_buckets = [[0, 0], [0.1, 0], [0.5, 0], [0.9, 0], [1, 0]]

        responses_times.sort()
        current_bucket_idx = 0
        current_bucket = responses_times_buckets[current_bucket_idx]

        for response_time in responses_times:
            if response_time > current_bucket[0] and current_bucket_idx < len(responses_times_buckets) - 1:
                current_bucket_idx += 1
                current_bucket = responses_times_buckets[current_bucket_idx]

            current_bucket[1] += 1

        stat = [
            ['response_time_ammm', sum(responses_times)],
            ['response_time_max', max(responses_times) if responses_times else 0],
            ['response_time_ahhh', responses_times_buckets],
            ['response_count_ammm', responses_count],
            ['response_count_max', responses_count],
        ]

        cls._add_additional_tags(stat, ADDITIONAL_TAGS)

        return stat

    @classmethod
    def _add_additional_tags(cls, stat, additional_tags=None):
        if additional_tags is None:
            return

        additional_tags_string = ';'.join(
            '{tag}={value}'.format(tag=tag, value=value) for tag, value in additional_tags
        )

        for signal in stat:
            signal[0] = additional_tags_string + ';' + signal[0]


class IssHandler(tornado.web.RequestHandler):
    def initialize(self, instance_name, instance_info):
        self.instance_name = instance_name
        self.instance_info = instance_info

    def get(self):
        self.write(json.dumps({
            self.instance_name: self.instance_info
        }))
