import yt.wrapper as yt
import json
import os
import logging
import codecs
import sys
from tests import test_functions
from utils import run_test
import argparse


def main():
    parser = argparse.ArgumentParser(description='Run internal tests')
    parser.add_argument('--out-file', required=True)
    parser.add_argument('--yt-token', required=True)
    parser.add_argument('--metrics-token', required=True)
    parser.add_argument('--stat-token', required=True)
    parser.add_argument('--hitman-token', required=True)
    parser.add_argument('--yt-path', required=True)
    parser.add_argument('--local', action='store_true')
    args = parser.parse_args()

    tokens = {
        'yt': args.yt_token,
        'metrics': args.metrics_token,
        'stat': args.stat_token,
        'hitman': args.hitman_token
    }
    yt.update_config({"proxy": {"url": "hahn.yt.yandex.net"}, "token": tokens['yt']})

    test_results_dir = '//home/images/monitoring/analytics_tests/test_results'
    test_files = os.listdir('internal_test')

    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - [%(levelname)s] - %(message)s'
    )

    results = []

    for name in test_files:
        in_path = '{}/{}'.format('internal_test', name)
        logging.info("Running test {}".format(name))

        try:
            with codecs.open(in_path, 'r', 'utf8') as f:
                test = json.load(f, encoding='utf8')
            test_result = run_test(test, tokens, test_functions)
            logging.info("Got status {}".format(test_result['status']))

            out_path = '{}/{}'.format(args.yt_path, test['id'])

            if args.local:
                logging.info("Local run, no dump to YT\n")
            else:
                logging.info("Writing test result to YT\n")
                yt.write_table(out_path, [test_result])

            results.append(test_result)
        except Exception as e:
            logging.error("Test exception {}\n".format(str(e)))

    with codecs.open(args.out_file, 'w', 'utf8') as f:
        json.dump(results, f, ensure_ascii=False)


if __name__ == '__main__':
    main()
