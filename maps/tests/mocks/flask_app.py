import logging
import time
from abc import ABC, abstractmethod
from multiprocessing import Process

import requests
from flask import Flask
from werkzeug.serving import WSGIRequestHandler

logger = logging.getLogger(__name__)


class FlaskApp(ABC):

    def __init__(self, listen_port: int) -> None:
        self.listen_port = listen_port
        self.process = None

    @abstractmethod
    def create_app(self) -> Flask:
        pass

    def healthcheck(self) -> bool:
        try:
            response = requests.get(
                url=f'http://localhost:{self.listen_port}/ping',
                timeout=(0.1, 1.)
            )
            if response.status_code == 200:
                return True
        except Exception as err:
            response = err
        logger.info(f'{self.__class__.__name__}-mock ping ERROR: {response}')
        return False

    def start(self, attempts: int = 5, interval: int = 1) -> None:
        app = self.create_app()
        # Force flask use HTTP/1.1 to prevent maps http-lib failure
        # see HEREBEDRAGONS-282
        WSGIRequestHandler.protocol_version = "HTTP/1.1"
        self.process = Process(
            target=app.run,
            kwargs={
                'host': '::1',
                'port': self.listen_port,
                'use_reloader': False
            }
        )
        self.process.start()

        logger.info('Waiting for abc-mock to start...')
        for _ in range(attempts):
            time.sleep(interval)
            if not self.process.is_alive():
                raise RuntimeError('abc-mock failed to start: process exited')
            if self.healthcheck():
                return

        try:
            self.stop()
        except RuntimeError:
            pass  # ignore stopping exceptions

        raise RuntimeError(f'{self.__class__.__name__}-mock failed to start: healthcheck-error')

    def stop(self, timeout: int = 5) -> None:
        if not (self.process and self.process.is_alive()):
            return
        try:
            self.process.terminate()
            # Wait timeout secs then kill the process
            self.process.join(timeout=timeout)
            if self.process.is_alive():
                self.process.kill()
        except Exception:
            raise RuntimeError(f'{self.__class__.__name__}-mock failed to stop')
