# -*- coding: utf-8 -*-

from temp.MTestlib import MTestlib as mtl

data = open('acts_list.txt', 'r')
for line in data:
    mtl.unhide_act(int(line[:-1]))
