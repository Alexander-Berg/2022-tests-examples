"""
Script to create ammos for lunapark load testing
"""

import argparse
import random
import json


ALPHABET = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.'


def make_tag_name(prefix='nonexistent', chars_min=1, chars_max=10):
    chars = random.randint(chars_min, chars_max)
    return '%s:%s' % (prefix, ''.join(random.choices(ALPHABET, k=chars)))


def main():
    parser = argparse.ArgumentParser(description="Create ammos for load testing")
    parser.add_argument('--host', help='Hostname to set', default='qnotifier-test.yandex-team.ru')
    parser.add_argument('--count', type=int, help='Amount of ammos to generate', default=10000)
    parser.add_argument('--gzip', action='store_true', help='Output should be gzipped', default=False)

    args = parser.parse_args()

    print("[Host: %s]" % (args.host,))
    print("[Connection: close]")
    print("[Accept: application/json]")
    print("[Content-Type: application/json]")

    for ammo in range(args.count):
        tags = random.randint(1, 5)
        data = {
            "tags": [make_tag_name() for _ in range(tags)],
            "extra": {},
            "message": make_tag_name(prefix='message ', chars_max=500),
        }
        data = json.dumps(data)
        print("%d /events/" % (len(data),))
        print(data)


if __name__ == '__main__':
    main()
