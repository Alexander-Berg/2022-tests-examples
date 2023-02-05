# -*- coding: utf-8 -*-

from yt_geocode import ParsedToponym, geocode, reliably_geocode

import yandex.maps.geocoder as geocoder_lib

import sys
import getopt

def _geocode(geocoder, locale, address, stuructured):
    simple_toponym = geocode({}, geocoder, locale, address)
    simple_toponym_address = simple_toponym.address if simple_toponym else ""
    simple_toponym_precision = simple_toponym.precision if simple_toponym else ""
    if structured:
        toponym = reliably_geocode({}, geocoder, locale, address)
    else:
        toponym = simple_toponym

    if toponym:
        return {"address": address, "response": toponym.address, "a_geoid": toponym.geoid, "precision": toponym.precision, "simply_geocoded": simple_toponym_address, "simple_precision": simple_toponym_precision }
    else:
        return {"address": address, "response": "NOT_FOUND", "a_geoid": "10000", "precision": "other", "simply_geocoded": simple_toponym_address, "simple_precision": simple_toponym_precision }


def format_line(raw):
    return u"\t".join([raw["address"], raw["response"], raw["precision"], raw["simply_geocoded"], raw["simple_precision"]])

def lines_in_file(file_name):
    with open(file_name) as f:
        counter = 0
        for l in f:
            counter += 1

if __name__ == "__main__":
    args,_ = getopt.getopt(sys.argv[1:], 'g:a:l:s')
    args = dict(args)

    if "-g" not in args:
        raise AttributeError("Usage: ./test_geocode.py -g geocoder_address -a address [-l locale] [-s]\n" +
                             "       ./test_geocode.py -l ru_RU -s -g addrs-batch.search.yandex.net/yandsearch -a ''")

    geocoder_config = {
        "host": args.get("-g"),
        "service-type": "search",
        "collection" : ""
    }
    g = geocoder_lib.Geocoder(geocoder_config)

    locale = args.get("-l", "ru_RU")
    structured = "-s" in args
    address = args.get("-a", "").decode("utf-8")

    print(format_line(_geocode(g, locale, address, structured)))
