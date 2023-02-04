import logging
import time
import json
from tornado import httpclient, gen

log = logging.getLogger(__name__)


class PushWorker(object):
    ITERATION_DURATION = 5
    REPEAT_AFTER = 479

    def __init__(self, ioloop, push_port, all_tags):
        self._ioloop = ioloop
        self._push_port = push_port
        self._next_call_at = None
        self._tags = {
            key: value for key, value in all_tags.iteritems() if key in {'itype', 'ctype', 'prj', 'geo'}
        }

    def schedule_first_push(self):
        self._next_call_at = time.time()
        self._ioloop.add_callback(self._callback)

    def _schedule_next_push(self):
        self._ioloop.call_at(self._next_call_at, self._callback)

    def _callback(self):
        self._do_work()
        self._next_call_at += self.ITERATION_DURATION
        cur_time = time.time()
        while cur_time > self._next_call_at:
            log.warn("Missed iteration at {}".format(self._next_call_at))
            self._next_call_at += self.ITERATION_DURATION
        self._schedule_next_push()

    @gen.coroutine
    def _do_work(self):
        log.info("Push values. Iteration at {}".format(self._next_call_at))

        try:
            values_to_push = self._make_values(int(self._next_call_at) / self.ITERATION_DURATION)
            structured_request_body = self._compose_structured_request_body(values_to_push)
            request_body = json.dumps(structured_request_body)
            yield httpclient.AsyncHTTPClient().fetch('http://localhost:{}'.format(self._push_port),
                                                     method="POST",
                                                     body=request_body,
                                                     request_timeout=4)
        except Exception as e:
            log.exception(e)

    def _compose_structured_request_body(self, values):
        response = []
        for signal, value in values.iteritems():
            response.append({
                'name': signal,
                'tags': self._tags,
                'val': value
            })
        return response

    def _make_values(self, iteration):
        ugram_value = self._make_hgram_value(iteration, ugram=True, reverse=False)
        hgram_value = self._make_hgram_value(iteration, ugram=False, reverse=False)
        ugram_reversed_value = self._make_hgram_value(iteration, ugram=True, reverse=True)
        hgram_reversed_value = self._make_hgram_value(iteration, ugram=False, reverse=True)
        values = {
            'aver_signal_avvv': iteration % self.REPEAT_AFTER,
            'ugram_signal_ahhh': ugram_value,
            'hgram_signal_ahhh': hgram_value,
            'reversed_ugram_signal_ahhh': ugram_reversed_value,
            'reversed_hgram_signal_ahhh': hgram_reversed_value
        }

        return values

    def _make_hgram_value(self, iteration, ugram, reverse):
        value_count = iteration % self.REPEAT_AFTER

        if not reverse:
            values = [i for i in xrange(0, value_count)]
        else:
            values = [i for i in xrange(value_count, self.REPEAT_AFTER)]

        if ugram:
            bucket_weights = [0] * (int(values[-1]) + 2)
            for value in values:
                bucket_weights[int(value)] += 1
            return [[i, j] for i, j in enumerate(bucket_weights)]
        else:
            return [v for v in values]
