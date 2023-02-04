import logging

logging.basicConfig(level=logging.INFO)


def get_logger(name):
    return logging.getLogger(name)


def get_env():
    with open('/etc/yandex/environment.type', 'r') as env_file:
        return env_file.read().strip()
