import argparse
import base64
import cProfile
import difflib
import functools
import logging
import os
import pickle
import re
import struct
import sys
from collections import defaultdict
from itertools import chain
from subprocess import PIPE, Popen

from yabs.proto.tsar_pb2 import TTsarCompressedVector

from ads.bsyeti.libs.py_tnode import Node, NodeMoveRef
from ads.bsyeti.tests.test_lib.eagle_compare_old.compare import fast_ki_group

HIDED = "HIDED"

NON_ASCII_PATTERN = re.compile(rb"[\x80-\xFF]")

basestring = (bytes, str)


def cmp(a, b):
    if a == b:
        return 0
    return (a > b) - (a < b)


def normalize_value(val):
    if isinstance(val, dict):
        d = {k: normalize_value(v) for k, v in val.items()}
        if "category" in d:
            d["category"] = ",".join(sorted(set(d["category"].split(","))))
        if d.get("id") == "873":
            assert int(d.get("element_size")) == 4, "no support for element size != 4, fixme"
            vector = TTsarCompressedVector()
            vector.ParseFromString(base64.b64decode(d["value"]))
            elements = ["%.3g" % x for x in struct.unpack("f" * int(d["vector_size"]), vector.Factors)]
            d["value"] = ",".join(elements)

        if d.get("id", "").startswith("328"):
            values = []
            for v in d["value"].split(";"):
                key, value = v.split(":")
                value = float(value)
                values.append("%s:%.6g" % (key, value))
            d["value"] = ";".join(values)
        return d
    if isinstance(val, list):
        return smart_sorted([normalize_value(v) for v in val])
    try:
        if isinstance(val, float) or (isinstance(val, basestring) and "." in val and ":" not in val):
            val = type(val)(round(float(val), 2))
    except Exception:
        pass

    if isinstance(val, basestring):
        for sep in (",", ";"):
            if sep in val:
                val = sep.join(sorted(val.split(sep)))

    return val


CMP_KEYS_FIELDS = [
    "id",
    "time",
    "value",
    "bm_category_id",
    "impression_id",
    "counter_id",
    "offer_id_md5",
    "update_time",
    "timestamp",
    "weight",
    "key",
]


def cmp_keys(a, b):
    if isinstance(a, int) and isinstance(b, int):
        return cmp(a, b)
    for field in CMP_KEYS_FIELDS:
        ret = cmp(a.get(field), b.get(field))
        if ret != 0:
            return ret
    return 0


def smart_sorted(data):
    return sorted(data, key=functools.cmp_to_key(cmp_keys))


FAST_CMP_KEYS_FIELDS = [
    "time",
    "update_time",
    "vector_id",
    "value",
    "bm_category_id",
    "impression_id",
    "counter_id",
    "offer_id_md5",
    "timestamp",
    "weight",
    "key",
    "domain_md5",
    "query_id",
    "start_timestamp",
    "page_id",
]


def fast_cmp_keys(a, b):
    for field in FAST_CMP_KEYS_FIELDS:
        ret = cmp(a.get(field), b.get(field))
        if ret != 0:
            return ret
    return 0


def fast_smart_sorted(data):
    return sorted(data, key=functools.cmp_to_key(fast_cmp_keys))


def ki_group(full):
    json_ascii = b""
    try:
        json_ascii = full[0]
        if isinstance(json_ascii, str):
            json_ascii = json_ascii.encode()
        json_ascii = NON_ASCII_PATTERN.sub(b"", json_ascii)
        json_ascii = json_ascii.decode()
        data_storage = Node.from_json(json_ascii, check_utf_8=False)
        data = data_storage[b"data"][0][b"segment"]
        fast_ki_group(data)
        return Node(NodeMoveRef(data)), full[1]
    except Exception:
        logging.exception("error: %s", json_ascii and json_ascii[:1000])
        return {}, full[1]


def normalize_segments(segments):
    return fast_smart_sorted([normalize_value(s) for s in segments])


def load_file(name):
    with open(name, "rb") as f_p:
        data = dict(pickle.load(f_p, encoding="bytes"))
    return data


def get_uids_set(request):
    return set(part.split("=", 1)[-1] for part in request.split("?", 1)[-1].split("&"))


def compare_keywords(kid, left, right):
    if kid == "873":
        if len(left) != len(right):
            return False
        for l, r in zip(left, right):
            for key in set(l.keys() | r.keys()):
                if key == "value" and not compare_tsar(l.get(key, ""), r.get(key, "")):
                    return False
                if key != "value" and l.get(key) != r.get(key):
                    return False
        return True
    return left == right


def compare_tsar(left, right):
    left = list(map(float, left.split(",")))
    right = list(map(float, right.split(",")))
    if len(left) != len(right):
        return False
    for a, b in zip(left, right):
        if abs(a - b) > 0.1:
            return False
    return True


def compare(first, second, uids, max_diffs=30, show_diff=False):
    good = True
    if uids is None:
        if len(first) != len(second):
            logging.error("different size")
            good = False
    else:
        left_set, rigth_set = set(), set()
        for key, _ in first.items():
            left_set |= get_uids_set(key[0]) & uids
        if len(left_set) != len(uids):
            logging.error("not all uids in first")
            good = False
        for key, _ in second.items():
            rigth_set |= get_uids_set(key[0]) & uids
        if left_set != rigth_set:
            logging.error("different size")
            good = False

    tests = defaultdict(lambda: defaultdict(lambda: defaultdict(int)))

    total_diffs = 0
    for request, test_id in set(chain(first.keys(), second.keys())):
        if uids is not None:
            if not get_uids_set(request) & uids:
                continue
        tests[test_id]["_total"]["all_full"] += 1

        first_resp = first.get((request, test_id), ("{}", {}))
        second_resp = second.get((request, test_id), ("{}", {}))

        if first_resp[0] == second_resp[0]:
            continue

        tests[test_id]["_total"]["all"] += 1

        a_node, _ = ki_group(first_resp)
        b_node, _ = ki_group(second_resp)

        current_good = True
        for kid in set(chain(a_node, b_node)):
            tests[test_id][kid]["all"] += 1
            a_resp = a_node.get(kid, [])
            b_resp = b_node.get(kid, [])
            if a_resp == b_resp:
                continue
            akid = normalize_segments(+a_resp if a_resp != [] else [])
            bkid = normalize_segments(+b_resp if b_resp != [] else [])

            if not compare_keywords(kid, akid, bkid):
                current_good = False
                tests[test_id][kid]["fail"] += 1
                logging.error("--------")
                logging.error("%s - %s", request, test_id)
                logging.error("%s - is bad", kid)
                logging.error("expected %d recs:", len(akid))
                logging.error(str(akid))
                logging.error("got %d recs:", len(bkid))
                logging.error(str(bkid))
                if show_diff:
                    if total_diffs < max_diffs and len(akid) and len(bkid):
                        logging.error("diff:\n" + "\n".join(difflib.ndiff([str(akid)], [str(bkid)])))
                        total_diffs += 1
        if not current_good:
            good = False
            tests[test_id]["_total"]["fail"] += 1

    for key, stat in sorted(tests.items(), key=lambda x: x[0]):
        logging.info("-----")
        logging.info("Request: %s", str(key))
        logging.info("we do not count profiles (and keywords in this profiles), which profiles is absolutely identical")
        not_failed = []
        for node, val in sorted(stat.items(), key=lambda x: x[0]):
            if val["fail"] == 0 and val["all"] != 0:
                not_failed.append("%s(%s)" % (node, val["all"]))
            else:
                logging.info("%s - failed: %d/%d", node, val["fail"], val["all"])
        if stat.get("_total", {}).get("fail"):
            logging.info(
                "_total - failed: %d/%d (with absolutely identical responses)",
                stat["_total"]["fail"],
                stat["_total"]["all_full"],
            )
        logging.info("Not failed: %s", ", ".join(not_failed))

    return good


def main():
    logging.basicConfig(
        format="%(filename)s[LINE:%(lineno)d]# %(levelname)-8s %(message)s",
        level=logging.INFO,
    )

    parser = argparse.ArgumentParser(description="compare two jsons with bigb answers")
    parser.add_argument("files", type=str, nargs=2)
    parser.add_argument("--uids", type=str, default=None)
    parser.add_argument("--show_diff", type=bool, default=False)
    args = parser.parse_args()
    proc1 = Popen(["md5sum", args.files[0]], stdout=PIPE)
    proc2 = Popen(["md5sum", args.files[1]], stdout=PIPE)
    proc1.wait()
    proc2.wait()
    if proc1.stdout.read().strip().split(b" ")[0] == proc2.stdout.read().strip().split(b" ")[0]:
        logging.info("Files equal, skipping difftool")
        sys.exit(0)
    first = load_file(args.files[0])
    second = load_file(args.files[1])
    retcode = None
    try:
        uids = None
        if args.uids is not None and args.uids != "":
            uids = set(args.uids.split(","))
        retcode = 0 if compare(first, second, uids, show_diff=args.show_diff) else 1
    except Exception:
        logging.error("Failed:", exc_info=True)
        retcode = 1
    sys.exit(retcode)


if __name__ == "__main__":
    if os.environ.get("PROFILE_COMPARE"):
        cProfile.run("main()", filename="./compare.pstats")
    else:
        main()
