#!/usr/bin/env python3

import argparse
import logging
import coloredlogs

from infra.cores import ui_tests


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-f', '--folder', default="")
    parser.add_argument('-c', '--cluster-type', default="testing")
    args = parser.parse_args()

    coloredlogs.install(fmt="%(asctime)s %(name)s %(levelname)s %(message)s")
    coloredlogs.set_level(logging.INFO)

    ui_tests.test_all(args.folder, cluster_type=args.cluster_type)


if __name__ == "__main__":
    main()
