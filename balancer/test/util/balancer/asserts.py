# -*- coding: utf-8 -*-

from balancer.test.util.asserts import *  # noqa

import json
import sys

from xml.etree import ElementTree


# statistics asserts

def _pretty_print_lst(stats):
    tagOpenStart, tagOpenEnd, tagClose, plain = 0, 1, 2, 3
    level = 0
    last_state = tagClose
    state = tagClose
    outLst = []

    for item in stats:
        last_state = state
        if item[0:2] == '/>':
            state = tagClose
        elif item[0:2] == '</':
            level -= 1
            state = tagClose
        elif item[0:1] == '<':
            state = tagOpenStart
        elif item == '>':
            state = tagOpenEnd
            level += 1
        else:
            state = plain
        if state == tagOpenStart and outLst or (last_state == tagClose and state == tagClose):
            outLst.append("\n" + ("  " * level))
        outLst.append(item)
        state = state

    return "".join(outLst)


def pretty_print_xml(stats):
    return _pretty_print_lst(ElementTree.tostringlist(stats))


def pretty_print_json(stats):
    return json.dumps(stats, sort_keys=True, indent=2)


def _try_get_paths(stats, paths):
    for path_ in paths:
        nodes = stats.findall(path_)
        for node in nodes:
            yield path_, node.text


def _get_paths(stats, paths):
    for path_ in paths:
        nodes = stats.findall(path_)
        try:
            assert nodes, "no paths {} from {}".format(str(paths), pretty_print_xml(stats))
        except AssertionError:
            import sys
            print >> sys.stderr, pretty_print_xml(stats)
            raise
        for node in sorted(nodes):
            yield path_, node.text


def paths(stats, paths, expected_value):
    """
    :param ElementTree stats: balancer statistics
    :type paths: list of str
    :param paths: xml paths to check
    :param str expected_value: expected value for all paths
    """
    expval = str(expected_value)
    for path_, value in _get_paths(stats, paths):
        try:
            assert value == expval, "{} from {}".format(path_, pretty_print_xml(stats))
        except AssertionError:
            print >> sys.stderr, pretty_print_xml(stats)
            raise


def paths_diff(stats, old_stats, paths, expected_value):
    expval = str(expected_value)
    for old, new in zip(_get_paths(old_stats, paths), _get_paths(stats, paths)):
        try:
            assert (int(new[1]) - int(old[1])) == int(expval), \
                "{new[1]} - {old[1]} != {expval} ({old[0]} from {xml})".format(
                    new=new, old=old, expval=expval, xml=pretty_print_xml(stats)
                )
        except AssertionError:
            print >> sys.stderr, pretty_print_xml(stats)
            raise


def __assert_no_path(stats, path):
    node = stats.find(path)
    try:
        assert node is None, "{} from {}".format(path, pretty_print_xml(stats))
    except AssertionError:
        print >> sys.stderr, pretty_print_xml(stats)
        raise


def no_paths(stats, paths):
    """
    :param ElementTree stats: balancer statistics
    :type paths: list of str
    :param paths: xml paths to check
    """
    if isinstance(paths, (str, unicode)):
        __assert_no_path(stats, paths)
    else:
        for path_ in paths:
            __assert_no_path(stats, path_)


def paths_exist(stats, paths):
    """
    :param ElementTree stats: balancer statistics
    :type paths: list of str
    :param paths: xml paths to check
    """
    for path_ in paths:
        node = stats.find(path_)
        try:
            assert node is not None, "{} from {}".format(path_, pretty_print_xml(stats))
        except AssertionError, e:
            print >> sys.stderr, pretty_print_xml(stats)
            raise e


def _summ_paths_op(stats, paths, op, reason):
    """
    :param ElementTree stats: balancer statistics
    :type paths: list of str
    :param paths: xml paths to check
    :param str expected_value: expected value for all paths
    """
    cnt = 0
    for path_, value in _try_get_paths(stats, paths):
        cnt += int(value)
    try:
        assert op(cnt), reason(cnt)
    except AssertionError, e:
        print >> sys.stderr, pretty_print_xml(stats)
        raise e


def summ_paths(stats, paths, expected_value):
    _summ_paths_op(
        stats, paths,
        lambda cnt: cnt == expected_value,
        lambda cnt: "{} != {} on paths {} from {}".format(
            cnt, expected_value, str(paths),
            pretty_print_xml(stats)
        )
    )


def summ_paths_gt(stats, paths, expected_value):
    _summ_paths_op(
        stats, paths,
        lambda cnt: cnt > expected_value,
        lambda cnt: "{} <= {} on paths {} from {}".format(
            cnt, expected_value, str(paths),
            pretty_print_xml(stats)
        )
    )
