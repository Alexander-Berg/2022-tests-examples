import logging
from simple_brother.senders.base import SenderBase


class DockerSender(SenderBase):
    is_for_meta = False

    def __init__(self, *args, **kwargs):
        super(DockerSender, self).__init__(*args, **kwargs)

    def send_batch(self, results):
        logging.info("DockerSender batch %s", results)

    def send(self, results):
        logging.info("DockerSender %s", results)


class DockerMetaSender(SenderBase):
    is_for_meta = True

    def __init__(self, *args, **kwargs):
        super(DockerMetaSender, self).__init__(*args, **kwargs)

    def send_batch(self, results):
        logging.info("DockerMetaSender batch %s", results)

    def send(self, results):
        logging.info("DockerMetaSender %s", results)
