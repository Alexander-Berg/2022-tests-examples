# -*- coding: utf-8 -*-
import os


class DirHolder(object):
    def __init__(self, root_dir):
        super(DirHolder, self).__init__()
        self.__root_dir = root_dir
        if not os.path.exists(self.__root_dir):
            os.makedirs(self.__root_dir)

    @property
    def root_dir(self):
        return self.__root_dir

    def _path(self, *args):
        return os.path.abspath(os.path.join(self.root_dir, *args))
