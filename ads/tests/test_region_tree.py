#!/usr/bin/env python
#coding: utf-8

import pytest
from tempfile import NamedTemporaryFile
import importlib

from yabs.tabutils import read_ts_table, downloadTSFile
from calc_tools import region_tree as rt


def print_region_name(regions, region_name):
    for r in regions:
        print region_name.get(r, r)


@pytest.yield_fixture(scope='module')
def region_tree():
    with NamedTemporaryFile() as tmp:
        downloadTSFile(tmp.file, '//home/yabs/dict/RegionTree')
        tmp.seek(0)
        yield rt.get_region_tree(tmp.file)


@pytest.yield_fixture(scope='module')
def region_name():
    with NamedTemporaryFile() as tmp:
        downloadTSFile(tmp.file, '//home/yabs/dict/RegionName')
        tmp.seek(0)
        yield rt.get_region_name(tmp.file)


def test_region_tree_read(region_tree):
    assert 213 in region_tree


def test_region_name_read(region_name):
    assert 213 in region_name


def test_region_tree_213(region_name, region_tree):
    for r_id in (213, 117, 179, 20572):
        ## Москва Литва Эстония Ирак
        ladder = rt.get_region_ladder(r_id, region_tree)
        print region_name[r_id], ladder
        print_region_name(ladder, region_name)


# def test_region_tree(region_name, region_tree):
    # p_1 = set()
    # p_2 = set()
    # p_3 = set()
    # for region in region_tree:
        # r_ladder = rt.get_region_ladder(region, region_tree)
        # try:
            # p_1.add(r_ladder[1])
            # p_2.add(r_ladder[2])
            # p_3.add(r_ladder[3])
        # except IndexError:
            # pass
    # print p_1
    # print_region_name(p_1, region_name)
    # print p_2
    # print_region_name(p_2, region_name)
    # print p_3
    # print_region_name(p_3, region_name)

Litva_Est_Iraq = set((117, 179, 20572))
## Литва Эстония Ирак
def test_country(region_name, region_tree):
    countries = set()
    regions = region_tree.keys()
    assert Litva_Est_Iraq.issubset(regions)
    for region in regions:
        country = rt.get_country(region, region_tree)
        countries.add(country)
    assert Litva_Est_Iraq.issubset(countries)
    print_region_name(countries, region_name)


def test_get_rtree(region_tree):
    my_rtree = rt.get_region_tree()
    assert my_rtree == region_tree


def test_get_rname(region_name):
    my_rname = rt.get_region_name()
    assert my_rname == region_name
