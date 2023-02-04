# coding: utf-8
"""
Пример вики-страниц.
Подобные тесты могут быть сложны в поддержке, если структура
данных меняется, но как протестировать иначе разбор bemjson
я пока не придумал.
"""

from __future__ import unicode_literals


FIRST_NOTE = """Договорились сделать бла-бла в Q5.

Уходит в отпуск в сентябре."""

SECOND_NOTE = """В Q5 бла-бла не сделали
"""


# https://wiki-api.test.yandex-team.ru/_api/frontend/<tag>/.raw
ONE_HEADER_MARKUP = """==2016-08-02
Договорились сделать бла-бла в Q5.

Уходит в отпуск в сентябре."""

TWO_HEADER_MARKUP = """==2016-09-02
В Q5 бла-бла не сделали
==2016-08-02
Договорились сделать бла-бла в Q5.

Уходит в отпуск в сентябре."""


# https://wiki-api.test.yandex-team.ru/_api/frontend/<tag>
ONE_HEADER_BEMJSON = {
    "block": "wiki-doc",
    "wiki-attrs": {
        "pos_start": 0,
        "pos_end": 76,
    },
    "content": [
        {
            "block": "wiki-head",
            "wiki-attrs": {
                "pos_start": 0,
                "pos_end": 12,
                "level": 1,
                "section_global": 1,
                "section_local": 1,
                "anchor": "2016-08-02",
            },
            "content": [
                {
                    "block": "wiki-txt",
                    "wiki-attrs": {
                        "txt": "2016-08-02",
                        "pos_start": 2,
                        "pos_end": 12,
                    },
                },
            ],
        },
        {
            "block": "wiki-p",
            "wiki-attrs": {
                "pos_start": 13,
                "pos_end": 47,
            },
            "content": [
                {
                    "wiki-attrs": {
                        "txt": "Договорились сделать бла-бла в Q5.",
                        "pos_start": 13,
                        "pos_end": 47
                    },
                    "block": "wiki-txt"
                }
            ],
        },
        {
            "wiki-attrs": {
                "pos_end": 49,
                "pos_start": 47
            },
            "block": "wiki-psep"
        },
        {
            "block": "wiki-p",
            "content": [
                {
                    "wiki-attrs": {
                        "txt": "Уходит в отпуск в сентябре.",
                        "pos_start": 49,
                        "pos_end": 76,
                    },
                    "block": "wiki-txt"
                }
            ],
            "wiki-attrs": {
                "pos_start": 49,
                "pos_end": 76,
            },
        }
    ],
}


TWO_HEADER_BEMJSON = {
    "block": "wiki-doc",
    "wiki-attrs": {
        "pos_start": 0,
        "pos_end": 113,
    },
    "content": [
        {
            "block": "wiki-head",
            "wiki-attrs": {
                "pos_start": 0,
                "pos_end": 12,
                "level": 1,
                "section_global": 1,
                "section_local": 1,
                "anchor": "2016-09-02",
            },
            "content": [
                {
                    "block": "wiki-txt",
                    "wiki-attrs": {
                        "txt": "2016-09-02",
                        "pos_start": 2,
                        "pos_end": 12
                    },
                }
            ],
        },
        {
            "block": "wiki-head",
            "wiki-attrs": {
                "pos_start": 37,
                "pos_end": 49,
                "level": 1,
                "section_global": 2,
                "section_local": 2,
                "anchor": "2016-08-02",
            },
            "content": [
                {
                    "wiki-attrs": {
                        "txt": "2016-08-02",
                        "pos_start": 39,
                        "pos_end": 49
                    },
                    "block": "wiki-txt"
                }
            ],
        },
        {
            "block": "wiki-p",
            "wiki-attrs": {
                "pos_end": 84,
                "pos_start": 50,
            },
            "content": [
                {
                    "block": "wiki-txt",
                    "wiki-attrs": {
                        "txt": "Договорились сделать бла-бла в Q5.",
                        "pos_start": 50,
                        "pos_end": 84
                    },
                }
            ],
        },
        {
            "block": "wiki-psep",
            "wiki-attrs": {
                "pos_start": 84,
                "pos_end": 86,
            },
        },
        {
            "block": "wiki-txt",
            "wiki-attrs": {
                "txt": "Уходит в отпуск в сентябре.",
                "pos_start": 86,
                "pos_end": 113
            },
            "content": [
                {
                    "block": "wiki-p",
                    "wiki-attrs": {
                        "pos_start": 86,
                        "pos_end": 113,
                    },
                },
            ],
        }
    ]
}

