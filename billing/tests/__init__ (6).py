# coding: utf-8

import os
import logging

logging.basicConfig(
    level=logging.CRITICAL,
    stream=open(os.devnull, 'a')
)

# Можно раскомментировать (и закомментировать блок выше)
# для просмотра логов во время работы тестов
# import sys
# logging.basicConfig(
#     level=logging.DEBUG,
#     stream=sys.stdout
# )
# logging.getLogger('sqlalchemy.engine').setLevel(logging.INFO)
