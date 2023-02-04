import argparse
import logging
import os

from app import Application


def parse_args():
    parser = argparse.ArgumentParser(description='Service for yasm unstable contour testing')
    parser.add_argument('--port', type=int, required=True)
    parser.add_argument('--push-port', type=int, required=True)
    parser.add_argument('--second-unistat-port', type=int, required=True)
    parser.add_argument('--third-unistat-port', type=int, required=True)
    parser.add_argument('--emulate-iss', action='store_true')
    parser.add_argument('--log-dir', type=str, required=True)
    parser.add_argument('--additional-tags', type=str, required=False)

    return parser.parse_args()


def main():
    args = parse_args()

    log = logging.getLogger()
    log.setLevel(logging.DEBUG)
    handler = logging.handlers.RotatingFileHandler(os.path.join(args.log_dir, 'testit.log'), maxBytes=1024 * 1024, backupCount=5)
    handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s -- %(message)s'))
    log.addHandler(handler)

    app = Application(args.port, args.second_unistat_port, args.third_unistat_port, args.push_port, args.emulate_iss, args.additional_tags)
    app.start()


if __name__ == '__main__':
    main()
