# -*- coding: utf-8 -*-
from balancer.test.util import fs


class Certs(fs.FileSystemManager):
    def __init__(self, certs_dir):
        super(Certs, self).__init__(certs_dir)
        self.root_ca = self.abs_path('root_ca.crt')
