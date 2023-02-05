//
//  TestData.swift
//  QATools
//
//  Created by Dmitrii Abanin on 12/4/20.
//

import Foundation

enum TestData {
    
    static var dataMenu = [
        Menu(title: "Изображения",
             row: [DataSet(name: "Гифки", link: "https://yadi.sk/d/YOeRzYTNOM7prA"),
                   DataSet(name: "Разные соотношения сторон", link: "https://yadi.sk/d/qIq51vw624ZCGw"),
                   DataSet(name: "Разные форматы", link: "https://yadi.sk/d/5L9IpCXTU6PmzQ"),
                   DataSet(name: "Векторы", link: "https://yadi.sk/d/y8909FxBaQK8OA"),
                   DataSet(name: "Вертикальные панорамы", link: "https://yadi.sk/d/0tGKkg-K1XotJQ"),
                   DataSet(name: "Конвертер форматы", link: "https://yadi.sk/d/yiuvnBWArpSbhQ"),
                   DataSet(name: "Белая картинка", link: "https://yadi.sk/i/b2w1e-orhejbuA")
             ],
             isExpanded: false),
        
        Menu(title: "Видео",
             row: [DataSet(name: "Конвертер форматы", link: "https://yadi.sk/d/iBUAUA1Zl5bgog"),
                   DataSet(name: "Проблемные форматы", link: "https://yadi.sk/d/Pxm_zKIwF__P8A"),
                   DataSet(name: "Длинные видео (фильмы)", link: "https://yadi.sk/d/7EIvAVitS_7ZKA"),
                   DataSet(name: "Папка с разными форматами", link: "https://yadi.sk/d/jnbObDs93E6nkm")
             ],
             isExpanded: false),
        
        //Документы находятся на аккаунте doc.test.data - https://wiki.yandex-team.ru/MobileDisk/TestData/
        Menu(title: "Документы",
             row: [DataSet(name: "Все форматы в одной папке", link: "https://disk.yandex.ru/d/eJ-UwtPJrplKrg"),
                   DataSet(name: "Форматы по папкам", link: "https://disk.yandex.ru/d/tZ-Pf3ukU5PAww")
             ],
             isExpanded: false),
        
        Menu(title: "Документы. По форматам",
             row: [DataSet(name: "pdf", link: "https://disk.yandex.ru/d/GSqcyCVJ1Hgmug"),
                   DataSet(name: "xlsx", link: "https://disk.yandex.ru/d/Qfuwz-v2fP4ixw"),
                   DataSet(name: "docx", link: "https://disk.yandex.ru/d/2_tp9ViHcx-WUg"),
                   DataSet(name: "pptx", link: "https://disk.yandex.ru/d/slHBQmVFcW-ZmA"),
                   DataSet(name: "7z", link: "https://disk.yandex.ru/d/bS15YCpvIV3iMA"),
                   DataSet(name: "cab", link: "https://disk.yandex.ru/d/DuY7Cj9R3HD4Tw"),
                   DataSet(name: "chm", link: "https://disk.yandex.ru/d/acRXrkN7Vxf2KQ"),
                   DataSet(name: "csv", link: "https://disk.yandex.ru/d/u8U11oVtRFReCg"),
                   DataSet(name: "doc", link: "https://disk.yandex.ru/d/RfZVtVvsxhAZnw"),
                   DataSet(name: "docm", link: "https://disk.yandex.ru/d/AWPTEBYF3RrPmg"),
                   DataSet(name: "dot", link: "https://disk.yandex.ru/d/8BZUbhOpsl2gMQ"),
                   DataSet(name: "dotm", link: "https://disk.yandex.ru/d/6C34UAkCoZT9Jg"),
                   DataSet(name: "dotx", link: "https://disk.yandex.ru/d/hXeGsZqjyDpJUQ"),
                   DataSet(name: "fb2", link: "https://disk.yandex.ru/d/GVkR8oI7aVdAxA"),
                   DataSet(name: "log", link: "https://disk.yandex.ru/d/Cq_hiQJfFltb1Q"),
                   DataSet(name: "odp", link: "https://disk.yandex.ru/d/RwhEcUBIyHCYCw"),
                   DataSet(name: "ods", link: "https://disk.yandex.ru/d/qXeJrRxyp6jlqw"),
                   DataSet(name: "odt", link: "https://disk.yandex.ru/d/oO20qhPl7jG8qg"),
                   DataSet(name: "ppt", link: "https://disk.yandex.ru/d/YQm8xVFv6Q1xDQ"),
                   DataSet(name: "rar", link: "https://disk.yandex.ru/d/d3ZPqZs-0xR78A"),
                   DataSet(name: "rtf", link: "https://disk.yandex.ru/d/eiM8aeRVbdY3bg"),
                   DataSet(name: "txt", link: "https://disk.yandex.ru/d/w-tzNAdq2nbbBA"),
                   DataSet(name: "xls", link: "https://disk.yandex.ru/d/uAb6J-aBi-yU3w"),
                   DataSet(name: "xlsb", link: "https://disk.yandex.ru/d/HW0aLMbbdDHNHw"),
                   DataSet(name: "xlsm", link: "https://disk.yandex.ru/d/oPUMY-vAcELgWQ"),
                   DataSet(name: "xlt", link: "https://disk.yandex.ru/d/i36_sKvFsv-UzA"),
                   DataSet(name: "xml", link: "https://disk.yandex.ru/d/AMQW2pR0n4Wb9Q"),
                   DataSet(name: "zip", link: "https://disk.yandex.ru/d/g0lDZLSckUZAOA"),
                   DataSet(name: "nb", link: "https://disk.yandex.ru/d/rCYxx1arm_06vg"),
                   DataSet(name: "pages", link: "https://disk.yandex.ru/d/CJcJJMTZxWsg6w"),
                   DataSet(name: "tex", link: "https://disk.yandex.ru/d/mL-bnruuVtTDqg")    
             ],
             isExpanded: false),

        Menu(title: "Аудио",
             row: [DataSet(name: "5 аудиофайлов", link: "https://yadi.sk/d/gnPfSwQGv01QRg"),
                   DataSet(name: "Аудиокнига", link: "https://yadi.sk/d/qEDXthcSlTENxQ"),
                   DataSet(name: "Еще одна аудиокнига", link: "https://yadi.sk/d/BSJhJwAs1IbPmw"),
                   DataSet(name: "Длинное аудио", link: "https://yadi.sk/d/y-ztCXAqbkwSdw"),
                   DataSet(name: "Разные форматы", link: "https://yadi.sk/d/S1SW9hpiNoomhg")
             ],
             isExpanded: false),
        
        Menu(title: "Тестирование Ленты",
             row: [DataSet(name: "10 видео", link: "https://yadi.sk/d/5HatpByu3Nrf4M"),
                   DataSet(name: "10 картинок", link: "https://yadi.sk/d/Xlt4GtGu3NrfAb"),
                   DataSet(name: "10 книг", link: "https://yadi.sk/d/28DvJKVu3NrfG6"),
                   DataSet(name: "3 блока (без 'показать все')", link: "https://yadi.sk/d/qA14MiI13Nrevq"),
                   DataSet(name: "4 блока (с 'показать все')", link: "https://yadi.sk/d/MKIA-38r3Nres8"),
                   DataSet(name: "4 видео", link: "https://yadi.sk/d/VzErcuE_3Nrf6r"),
                   DataSet(name: "4 картинки", link: "https://yadi.sk/d/pVg0a1nG3NrfBb"),
                   DataSet(name: "4 книги", link: "https://yadi.sk/d/3ezmmvTf3NrfKC"),
                   DataSet(name: "Папка для фолдерблока", link: "https://yadi.sk/d/u8f0enQr3NrgKA"),
                   DataSet(name: "Фото (портрет и ландшафт)", link: "https://yadi.sk/d/z9ORKHdg3NrgRZ")
             ],
             isExpanded: false),
        
        Menu(title: "Остальное",
             row: [DataSet(name: "Кривые имена", link: "https://yadi.sk/d/LbpU_QwL3KHdZw"),
                   DataSet(name: "Карабченные файлы", link: "https://yadi.sk/d/I9ezf3Cb3Gq3D3"),
                   DataSet(name: "Сортировка", link: "https://yadi.sk/d/ofece9TuyDfB8")
             ],
             isExpanded: false)
    ]
}
