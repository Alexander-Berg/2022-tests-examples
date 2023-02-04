# -*- coding: utf-8 -*-

from __future__ import print_function

import copy
import time

from dns import RR
from server import BaseResolver


class FixedResolver(BaseResolver):
    """
        Respond with fixed response to all requests
    """
    def __init__(self, zone, delay_sec):
        # Parse RRs
        self.rrs = RR.fromZone(zone)
        self.delay_sec = delay_sec

    def resolve(self, request, handler):
        time.sleep(self.delay_sec)  # delay for tests
        reply = request.reply()
        qname = request.q.qname
        # Replace labels with request label
        for rr in self.rrs:
            a = copy.copy(rr)
            a.rname = qname
            reply.add_answer(a)
        return reply
