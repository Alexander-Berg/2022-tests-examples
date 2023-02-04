# coding: utf-8
import random


def random_banners(banners, num):
    banners_len = len(banners)
    selected_banners = []

    for i in range(num):
        index_candidate = random.randint(0, banners_len - 1)
        interval = sum([1 if index_candidate >= limit else 0 for limit in selected_banners])
        selected_banners.append(index_candidate + interval)

    return selected_banners


tst = random_banners(banners=list(xrange(100, 110)), num=3)
