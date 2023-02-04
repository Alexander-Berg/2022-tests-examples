import { SocialNetworkType } from '../../../types/author-social-network';
import { Position } from '../../../types/author';

export const fixtures = {
    'Internal authors controller GET /internal/authors Возвращает всех авторов': {
        AUTHOR_DATA_1: {
            urlPart: 'test',
            name: 'test',
            position: Position.author,
            photo: {
                meta: {
                    md5: 'c4782f6be1710b107031cee0a8b42da3',
                    crc64: '24D5842733EDC82E',
                    'orig-size': {
                        x: 598,
                        y: 336,
                    },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 19697,
                    'orig-orientation': '0',
                    'modification-time': 1648103509,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                        width: 598,
                        height: 336,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                        width: 200,
                        height: 112,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                        width: 296,
                        height: 166,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                        width: 320,
                        height: 180,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                        width: 338,
                        height: 190,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                        width: 385,
                        height: 216,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                        width: 439,
                        height: 247,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                        width: 460,
                        height: 258,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                        width: 500,
                        height: 281,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                        width: 571,
                        height: 321,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                        width: 598,
                        height: 336,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                        width: 598,
                        height: 336,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                        width: 598,
                        height: 336,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                        width: 598,
                        height: 336,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                        width: 320,
                        height: 180,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                        width: 460,
                        height: 258,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                        width: 598,
                        height: 336,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                        width: 598,
                        height: 336,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                        width: 598,
                        height: 336,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                        width: 598,
                        height: 336,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                        width: 598,
                        height: 336,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                        width: 598,
                        height: 336,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                        width: 598,
                        height: 336,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                        width: 598,
                        height: 336,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                        width: 598,
                        height: 336,
                    },
                },
                'group-id': 1399832,
                imagename: 'car.jpeg_1648103509611',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id111',
                },
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id222',
                },
            ],
        },
        AUTHOR_DATA_2: {
            urlPart: 'aleksandr-gruzdev',
            name: 'Александр Груздев',
            position: Position.editor,
            socialNetworks: [],
        },
        AUTHOR_DATA_3: {
            urlPart: 'aleksandr-evdokimov',
            name: 'Александр Евдокимов',
            position: Position.editor,
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/aleksand-evdokimov',
                },
            ],
        },
    },
    'Internal authors controller GET /internal/authors/:id Возвращает автора': {
        AUTHOR_DATA: {
            urlPart: 'test',
            name: 'test',
            position: Position.author,
            photo: {
                meta: {
                    md5: 'c4782f6be1710b107031cee0a8b42da3',
                    crc64: '24D5842733EDC82E',
                    'orig-size': {
                        x: 598,
                        y: 336,
                    },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 19697,
                    'orig-orientation': '0',
                    'modification-time': 1648103509,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                        width: 598,
                        height: 336,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                        width: 200,
                        height: 112,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                        width: 296,
                        height: 166,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                        width: 320,
                        height: 180,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                        width: 338,
                        height: 190,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                        width: 385,
                        height: 216,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                        width: 439,
                        height: 247,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                        width: 460,
                        height: 258,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                        width: 500,
                        height: 281,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                        width: 571,
                        height: 321,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                        width: 598,
                        height: 336,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                        width: 598,
                        height: 336,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                        width: 598,
                        height: 336,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                        width: 598,
                        height: 336,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                        width: 320,
                        height: 180,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                        width: 460,
                        height: 258,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                        width: 598,
                        height: 336,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                        width: 598,
                        height: 336,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                        width: 598,
                        height: 336,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                        width: 598,
                        height: 336,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                        width: 598,
                        height: 336,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                        width: 598,
                        height: 336,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                        width: 598,
                        height: 336,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                        width: 598,
                        height: 336,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                        width: 598,
                        height: 336,
                    },
                },
                'group-id': 1399832,
                imagename: 'car.jpeg_1648103509611',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id111',
                },
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id222',
                },
            ],
        },
    },
    'Internal authors controller POST /internal/authors Возвращает ошибку и статус 400, если не указано имя': {
        AUTHOR_DATA: {
            urlPart: 'test',
            position: Position.author,
            photo: {
                meta: {
                    md5: 'c4782f6be1710b107031cee0a8b42da3',
                    crc64: '24D5842733EDC82E',
                    'orig-size': {
                        x: 598,
                        y: 336,
                    },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 19697,
                    'orig-orientation': '0',
                    'modification-time': 1648103509,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                        width: 598,
                        height: 336,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                        width: 200,
                        height: 112,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                        width: 296,
                        height: 166,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                        width: 320,
                        height: 180,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                        width: 338,
                        height: 190,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                        width: 385,
                        height: 216,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                        width: 439,
                        height: 247,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                        width: 460,
                        height: 258,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                        width: 500,
                        height: 281,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                        width: 571,
                        height: 321,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                        width: 598,
                        height: 336,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                        width: 598,
                        height: 336,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                        width: 598,
                        height: 336,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                        width: 598,
                        height: 336,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                        width: 320,
                        height: 180,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                        width: 460,
                        height: 258,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                        width: 598,
                        height: 336,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                        width: 598,
                        height: 336,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                        width: 598,
                        height: 336,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                        width: 598,
                        height: 336,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                        width: 598,
                        height: 336,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                        width: 598,
                        height: 336,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                        width: 598,
                        height: 336,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                        width: 598,
                        height: 336,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                        width: 598,
                        height: 336,
                    },
                },
                'group-id': 1399832,
                imagename: 'car.jpeg_1648103509611',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id111',
                },
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id222',
                },
            ],
        },
        USER_LOGIN: 'editor-1',
    },
    'Internal authors controller POST /internal/authors Возвращает ошибку и статус 400, если не указана должность': {
        AUTHOR_DATA: {
            urlPart: 'test',
            name: 'Тест',
            photo: {
                meta: {
                    md5: 'c4782f6be1710b107031cee0a8b42da3',
                    crc64: '24D5842733EDC82E',
                    'orig-size': {
                        x: 598,
                        y: 336,
                    },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 19697,
                    'orig-orientation': '0',
                    'modification-time': 1648103509,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                        width: 598,
                        height: 336,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                        width: 200,
                        height: 112,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                        width: 296,
                        height: 166,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                        width: 320,
                        height: 180,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                        width: 338,
                        height: 190,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                        width: 385,
                        height: 216,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                        width: 439,
                        height: 247,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                        width: 460,
                        height: 258,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                        width: 500,
                        height: 281,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                        width: 571,
                        height: 321,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                        width: 598,
                        height: 336,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                        width: 598,
                        height: 336,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                        width: 598,
                        height: 336,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                        width: 598,
                        height: 336,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                        width: 320,
                        height: 180,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                        width: 460,
                        height: 258,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                        width: 598,
                        height: 336,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                        width: 598,
                        height: 336,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                        width: 598,
                        height: 336,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                        width: 598,
                        height: 336,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                        width: 598,
                        height: 336,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                        width: 598,
                        height: 336,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                        width: 598,
                        height: 336,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                        width: 598,
                        height: 336,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                        width: 598,
                        height: 336,
                    },
                },
                'group-id': 1399832,
                imagename: 'car.jpeg_1648103509611',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id111',
                },
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id222',
                },
            ],
        },
        USER_LOGIN: 'editor-1',
    },
    'Internal authors controller POST /internal/authors Возвращает ошибку и статус 400, если не указан urlPart': {
        AUTHOR_DATA: {
            name: 'Тест',
            photo: {
                meta: {
                    md5: 'c4782f6be1710b107031cee0a8b42da3',
                    crc64: '24D5842733EDC82E',
                    'orig-size': {
                        x: 598,
                        y: 336,
                    },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 19697,
                    'orig-orientation': '0',
                    'modification-time': 1648103509,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                        width: 598,
                        height: 336,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                        width: 200,
                        height: 112,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                        width: 296,
                        height: 166,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                        width: 320,
                        height: 180,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                        width: 338,
                        height: 190,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                        width: 385,
                        height: 216,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                        width: 439,
                        height: 247,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                        width: 460,
                        height: 258,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                        width: 500,
                        height: 281,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                        width: 571,
                        height: 321,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                        width: 598,
                        height: 336,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                        width: 598,
                        height: 336,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                        width: 598,
                        height: 336,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                        width: 598,
                        height: 336,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                        width: 320,
                        height: 180,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                        width: 460,
                        height: 258,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                        width: 598,
                        height: 336,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                        width: 598,
                        height: 336,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                        width: 598,
                        height: 336,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                        width: 598,
                        height: 336,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                        width: 598,
                        height: 336,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                        width: 598,
                        height: 336,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                        width: 598,
                        height: 336,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                        width: 598,
                        height: 336,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                        width: 598,
                        height: 336,
                    },
                },
                'group-id': 1399832,
                imagename: 'car.jpeg_1648103509611',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id111',
                },
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id222',
                },
            ],
        },
        USER_LOGIN: 'editor-1',
    },
    'Internal authors controller POST /internal/authors Возвращает ошибку и статус 400, если не указан логин пользователя':
        {
            AUTHOR_DATA: {
                urlPart: 'test',
                name: 'Тест',
                position: Position.author,
                photo: {
                    meta: {
                        md5: 'c4782f6be1710b107031cee0a8b42da3',
                        crc64: '24D5842733EDC82E',
                        'orig-size': {
                            x: 598,
                            y: 336,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 19697,
                        'orig-orientation': '0',
                        'modification-time': 1648103509,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                            width: 598,
                            height: 336,
                        },
                        '200x200': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                            width: 200,
                            height: 112,
                        },
                        '296x296': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                            width: 296,
                            height: 166,
                        },
                        '320x320': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                            width: 320,
                            height: 180,
                        },
                        '338x338': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                            width: 338,
                            height: 190,
                        },
                        '385x385': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                            width: 385,
                            height: 216,
                        },
                        '439x439': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                            width: 439,
                            height: 247,
                        },
                        '460x460': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                            width: 460,
                            height: 258,
                        },
                        '500x500': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                            width: 500,
                            height: 281,
                        },
                        '571x571': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                            width: 571,
                            height: 321,
                        },
                        '650x650': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                            width: 598,
                            height: 336,
                        },
                        '741x741': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                            width: 598,
                            height: 336,
                        },
                        '845x845': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                            width: 598,
                            height: 336,
                        },
                        '964x964': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                            width: 598,
                            height: 336,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                            width: 320,
                            height: 180,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                            width: 460,
                            height: 258,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                            width: 598,
                            height: 336,
                        },
                        '1098x1098': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                            width: 598,
                            height: 336,
                        },
                        '1200x1200': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                            width: 598,
                            height: 336,
                        },
                        '1252x1252': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                            width: 598,
                            height: 336,
                        },
                        '1428x1428': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                            width: 598,
                            height: 336,
                        },
                        '1600x1600': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                            width: 598,
                            height: 336,
                        },
                        '1920x1920': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                            width: 598,
                            height: 336,
                        },
                        '2560x2560': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                            width: 598,
                            height: 336,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                            width: 598,
                            height: 336,
                        },
                    },
                    'group-id': 1399832,
                    imagename: 'car.jpeg_1648103509611',
                },
                socialNetworks: [
                    {
                        type: SocialNetworkType.vk,
                        url: 'https://vk.com/id111',
                    },
                    {
                        type: SocialNetworkType.vk,
                        url: 'https://vk.com/id222',
                    },
                ],
            },
        },
    'Internal authors controller POST /internal/authors Возвращает ошибку и статус 400, если уже существует пользователь с таким urlPart':
        {
            AUTHOR_DATA_1: {
                urlPart: 'testovyi-avtor',
                name: 'Тестовый автор',
                position: Position.editor,
                socialNetworks: [],
            },
            AUTHOR_DATA_2: {
                urlPart: ' TESTovyi-AVTOR ',
                name: 'Тест',
                position: Position.author,
                photo: {
                    meta: {
                        md5: 'c4782f6be1710b107031cee0a8b42da3',
                        crc64: '24D5842733EDC82E',
                        'orig-size': {
                            x: 598,
                            y: 336,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 19697,
                        'orig-orientation': '0',
                        'modification-time': 1648103509,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                            width: 598,
                            height: 336,
                        },
                        '200x200': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                            width: 200,
                            height: 112,
                        },
                        '296x296': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                            width: 296,
                            height: 166,
                        },
                        '320x320': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                            width: 320,
                            height: 180,
                        },
                        '338x338': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                            width: 338,
                            height: 190,
                        },
                        '385x385': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                            width: 385,
                            height: 216,
                        },
                        '439x439': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                            width: 439,
                            height: 247,
                        },
                        '460x460': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                            width: 460,
                            height: 258,
                        },
                        '500x500': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                            width: 500,
                            height: 281,
                        },
                        '571x571': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                            width: 571,
                            height: 321,
                        },
                        '650x650': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                            width: 598,
                            height: 336,
                        },
                        '741x741': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                            width: 598,
                            height: 336,
                        },
                        '845x845': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                            width: 598,
                            height: 336,
                        },
                        '964x964': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                            width: 598,
                            height: 336,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                            width: 320,
                            height: 180,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                            width: 460,
                            height: 258,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                            width: 598,
                            height: 336,
                        },
                        '1098x1098': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                            width: 598,
                            height: 336,
                        },
                        '1200x1200': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                            width: 598,
                            height: 336,
                        },
                        '1252x1252': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                            width: 598,
                            height: 336,
                        },
                        '1428x1428': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                            width: 598,
                            height: 336,
                        },
                        '1600x1600': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                            width: 598,
                            height: 336,
                        },
                        '1920x1920': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                            width: 598,
                            height: 336,
                        },
                        '2560x2560': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                            width: 598,
                            height: 336,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                            width: 598,
                            height: 336,
                        },
                    },
                    'group-id': 1399832,
                    imagename: 'car.jpeg_1648103509611',
                },
                socialNetworks: [
                    {
                        type: SocialNetworkType.vk,
                        url: 'https://vk.com/id111',
                    },
                    {
                        type: SocialNetworkType.vk,
                        url: 'https://vk.com/id222',
                    },
                ],
            },
            USER_LOGIN: 'editor-1',
        },
    'Internal authors controller POST /internal/authors Создает и возвращает нового автора': {
        AUTHOR_DATA: {
            urlPart: ' TESTovyi-AVTOR ',
            name: 'test',
            position: Position.author,
            photo: {
                meta: {
                    md5: 'c4782f6be1710b107031cee0a8b42da3',
                    crc64: '24D5842733EDC82E',
                    'orig-size': {
                        x: 598,
                        y: 336,
                    },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 19697,
                    'orig-orientation': '0',
                    'modification-time': 1648103509,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                        width: 598,
                        height: 336,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                        width: 200,
                        height: 112,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                        width: 296,
                        height: 166,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                        width: 320,
                        height: 180,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                        width: 338,
                        height: 190,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                        width: 385,
                        height: 216,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                        width: 439,
                        height: 247,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                        width: 460,
                        height: 258,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                        width: 500,
                        height: 281,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                        width: 571,
                        height: 321,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                        width: 598,
                        height: 336,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                        width: 598,
                        height: 336,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                        width: 598,
                        height: 336,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                        width: 598,
                        height: 336,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                        width: 320,
                        height: 180,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                        width: 460,
                        height: 258,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                        width: 598,
                        height: 336,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                        width: 598,
                        height: 336,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                        width: 598,
                        height: 336,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                        width: 598,
                        height: 336,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                        width: 598,
                        height: 336,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                        width: 598,
                        height: 336,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                        width: 598,
                        height: 336,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                        width: 598,
                        height: 336,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                        width: 598,
                        height: 336,
                    },
                },
                'group-id': 1399832,
                imagename: 'car.jpeg_1648103509611',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id111',
                },
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id222',
                },
            ],
        },
        USER_LOGIN: 'editor-1',
    },
    'Internal authors controller PUT /internal/authors/:id Возвращает ошибку и статус 400, если не указано имя': {
        AUTHOR_DATA: {
            urlPart: 'test',
            name: 'Тест',
            position: Position.author,
            photo: {
                meta: {
                    md5: 'c4782f6be1710b107031cee0a8b42da3',
                    crc64: '24D5842733EDC82E',
                    'orig-size': {
                        x: 598,
                        y: 336,
                    },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 19697,
                    'orig-orientation': '0',
                    'modification-time': 1648103509,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                        width: 598,
                        height: 336,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                        width: 200,
                        height: 112,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                        width: 296,
                        height: 166,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                        width: 320,
                        height: 180,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                        width: 338,
                        height: 190,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                        width: 385,
                        height: 216,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                        width: 439,
                        height: 247,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                        width: 460,
                        height: 258,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                        width: 500,
                        height: 281,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                        width: 571,
                        height: 321,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                        width: 598,
                        height: 336,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                        width: 598,
                        height: 336,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                        width: 598,
                        height: 336,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                        width: 598,
                        height: 336,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                        width: 320,
                        height: 180,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                        width: 460,
                        height: 258,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                        width: 598,
                        height: 336,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                        width: 598,
                        height: 336,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                        width: 598,
                        height: 336,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                        width: 598,
                        height: 336,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                        width: 598,
                        height: 336,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                        width: 598,
                        height: 336,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                        width: 598,
                        height: 336,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                        width: 598,
                        height: 336,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                        width: 598,
                        height: 336,
                    },
                },
                'group-id': 1399832,
                imagename: 'car.jpeg_1648103509611',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id111',
                },
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id222',
                },
            ],
        },
        AUTHOR_UPDATE_DATA: {
            urlPart: 'redakciya-avtoru',
            position: Position.editor,
            photo: {
                meta: {
                    md5: 'b6db85a4300596c93045d977081768d1',
                    crc64: '4004BBE4DFEBAD37',
                    'orig-size': {
                        x: 1280,
                        y: 1280,
                    },
                    processing: 'finished',
                    'orig-format': 'PNG',
                    'orig-animated': false,
                    'orig-size-bytes': 1480201,
                    'orig-orientation': '0',
                    'modification-time': 1648055259,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/orig',
                        width: 1280,
                        height: 1280,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/200x200',
                        width: 200,
                        height: 200,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/296x296',
                        width: 296,
                        height: 296,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320x320',
                        width: 320,
                        height: 320,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/338x338',
                        width: 338,
                        height: 338,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/385x385',
                        width: 385,
                        height: 385,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/439x439',
                        width: 439,
                        height: 439,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460x460',
                        width: 460,
                        height: 460,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/500x500',
                        width: 500,
                        height: 500,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/571x571',
                        width: 571,
                        height: 571,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/650x650',
                        width: 650,
                        height: 650,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/741x741',
                        width: 741,
                        height: 741,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/845x845',
                        width: 845,
                        height: 845,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/964x964',
                        width: 964,
                        height: 964,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320х320',
                        width: 320,
                        height: 320,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460х460',
                        width: 460,
                        height: 460,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/optimize',
                        width: 1280,
                        height: 1280,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1098x1098',
                        width: 1098,
                        height: 1098,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200x1200',
                        width: 1200,
                        height: 1200,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1252x1252',
                        width: 1252,
                        height: 1252,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1428x1428',
                        width: 1280,
                        height: 1280,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1600x1600',
                        width: 1280,
                        height: 1280,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1920x1920',
                        width: 1280,
                        height: 1280,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/2560x2560',
                        width: 1280,
                        height: 1280,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200х1200',
                        width: 1200,
                        height: 1200,
                    },
                },
                'group-id': 1399832,
                imagename: 'kitten.png_1648055258930',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id456',
                },
            ],
        },
        USER_LOGIN: 'editor-1',
    },
    'Internal authors controller PUT /internal/authors/:id Возвращает ошибку и статус 400, если не указана должность': {
        AUTHOR_DATA: {
            urlPart: 'test',
            name: 'Тест',
            position: Position.editor,
            photo: {
                meta: {
                    md5: 'c4782f6be1710b107031cee0a8b42da3',
                    crc64: '24D5842733EDC82E',
                    'orig-size': {
                        x: 598,
                        y: 336,
                    },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 19697,
                    'orig-orientation': '0',
                    'modification-time': 1648103509,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                        width: 598,
                        height: 336,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                        width: 200,
                        height: 112,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                        width: 296,
                        height: 166,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                        width: 320,
                        height: 180,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                        width: 338,
                        height: 190,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                        width: 385,
                        height: 216,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                        width: 439,
                        height: 247,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                        width: 460,
                        height: 258,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                        width: 500,
                        height: 281,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                        width: 571,
                        height: 321,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                        width: 598,
                        height: 336,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                        width: 598,
                        height: 336,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                        width: 598,
                        height: 336,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                        width: 598,
                        height: 336,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                        width: 320,
                        height: 180,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                        width: 460,
                        height: 258,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                        width: 598,
                        height: 336,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                        width: 598,
                        height: 336,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                        width: 598,
                        height: 336,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                        width: 598,
                        height: 336,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                        width: 598,
                        height: 336,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                        width: 598,
                        height: 336,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                        width: 598,
                        height: 336,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                        width: 598,
                        height: 336,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                        width: 598,
                        height: 336,
                    },
                },
                'group-id': 1399832,
                imagename: 'car.jpeg_1648103509611',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id111',
                },
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id222',
                },
            ],
        },
        AUTHOR_UPDATE_DATA: {
            urlPart: 'redakciya-avtoru',
            name: 'Редакция Авто.ру',
            photo: {
                meta: {
                    md5: 'b6db85a4300596c93045d977081768d1',
                    crc64: '4004BBE4DFEBAD37',
                    'orig-size': {
                        x: 1280,
                        y: 1280,
                    },
                    processing: 'finished',
                    'orig-format': 'PNG',
                    'orig-animated': false,
                    'orig-size-bytes': 1480201,
                    'orig-orientation': '0',
                    'modification-time': 1648055259,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/orig',
                        width: 1280,
                        height: 1280,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/200x200',
                        width: 200,
                        height: 200,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/296x296',
                        width: 296,
                        height: 296,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320x320',
                        width: 320,
                        height: 320,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/338x338',
                        width: 338,
                        height: 338,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/385x385',
                        width: 385,
                        height: 385,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/439x439',
                        width: 439,
                        height: 439,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460x460',
                        width: 460,
                        height: 460,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/500x500',
                        width: 500,
                        height: 500,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/571x571',
                        width: 571,
                        height: 571,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/650x650',
                        width: 650,
                        height: 650,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/741x741',
                        width: 741,
                        height: 741,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/845x845',
                        width: 845,
                        height: 845,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/964x964',
                        width: 964,
                        height: 964,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320х320',
                        width: 320,
                        height: 320,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460х460',
                        width: 460,
                        height: 460,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/optimize',
                        width: 1280,
                        height: 1280,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1098x1098',
                        width: 1098,
                        height: 1098,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200x1200',
                        width: 1200,
                        height: 1200,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1252x1252',
                        width: 1252,
                        height: 1252,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1428x1428',
                        width: 1280,
                        height: 1280,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1600x1600',
                        width: 1280,
                        height: 1280,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1920x1920',
                        width: 1280,
                        height: 1280,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/2560x2560',
                        width: 1280,
                        height: 1280,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200х1200',
                        width: 1200,
                        height: 1200,
                    },
                },
                'group-id': 1399832,
                imagename: 'kitten.png_1648055258930',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id456',
                },
            ],
        },
        USER_LOGIN: 'editor-1',
    },
    'Internal authors controller PUT /internal/authors/:id Возвращает ошибку и статус 400, если не указан urlPart': {
        AUTHOR_DATA: {
            urlPart: 'redakciya-avtoru',
            name: 'Тест',
            position: Position.editor,
            photo: {
                meta: {
                    md5: 'c4782f6be1710b107031cee0a8b42da3',
                    crc64: '24D5842733EDC82E',
                    'orig-size': {
                        x: 598,
                        y: 336,
                    },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 19697,
                    'orig-orientation': '0',
                    'modification-time': 1648103509,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                        width: 598,
                        height: 336,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                        width: 200,
                        height: 112,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                        width: 296,
                        height: 166,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                        width: 320,
                        height: 180,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                        width: 338,
                        height: 190,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                        width: 385,
                        height: 216,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                        width: 439,
                        height: 247,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                        width: 460,
                        height: 258,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                        width: 500,
                        height: 281,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                        width: 571,
                        height: 321,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                        width: 598,
                        height: 336,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                        width: 598,
                        height: 336,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                        width: 598,
                        height: 336,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                        width: 598,
                        height: 336,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                        width: 320,
                        height: 180,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                        width: 460,
                        height: 258,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                        width: 598,
                        height: 336,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                        width: 598,
                        height: 336,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                        width: 598,
                        height: 336,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                        width: 598,
                        height: 336,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                        width: 598,
                        height: 336,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                        width: 598,
                        height: 336,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                        width: 598,
                        height: 336,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                        width: 598,
                        height: 336,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                        width: 598,
                        height: 336,
                    },
                },
                'group-id': 1399832,
                imagename: 'car.jpeg_1648103509611',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id111',
                },
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id222',
                },
            ],
        },
        AUTHOR_UPDATE_DATA: {
            name: 'Редакция Авто.ру',
            position: Position.editor,
            photo: {
                meta: {
                    md5: 'b6db85a4300596c93045d977081768d1',
                    crc64: '4004BBE4DFEBAD37',
                    'orig-size': {
                        x: 1280,
                        y: 1280,
                    },
                    processing: 'finished',
                    'orig-format': 'PNG',
                    'orig-animated': false,
                    'orig-size-bytes': 1480201,
                    'orig-orientation': '0',
                    'modification-time': 1648055259,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/orig',
                        width: 1280,
                        height: 1280,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/200x200',
                        width: 200,
                        height: 200,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/296x296',
                        width: 296,
                        height: 296,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320x320',
                        width: 320,
                        height: 320,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/338x338',
                        width: 338,
                        height: 338,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/385x385',
                        width: 385,
                        height: 385,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/439x439',
                        width: 439,
                        height: 439,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460x460',
                        width: 460,
                        height: 460,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/500x500',
                        width: 500,
                        height: 500,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/571x571',
                        width: 571,
                        height: 571,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/650x650',
                        width: 650,
                        height: 650,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/741x741',
                        width: 741,
                        height: 741,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/845x845',
                        width: 845,
                        height: 845,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/964x964',
                        width: 964,
                        height: 964,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320х320',
                        width: 320,
                        height: 320,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460х460',
                        width: 460,
                        height: 460,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/optimize',
                        width: 1280,
                        height: 1280,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1098x1098',
                        width: 1098,
                        height: 1098,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200x1200',
                        width: 1200,
                        height: 1200,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1252x1252',
                        width: 1252,
                        height: 1252,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1428x1428',
                        width: 1280,
                        height: 1280,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1600x1600',
                        width: 1280,
                        height: 1280,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1920x1920',
                        width: 1280,
                        height: 1280,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/2560x2560',
                        width: 1280,
                        height: 1280,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200х1200',
                        width: 1200,
                        height: 1200,
                    },
                },
                'group-id': 1399832,
                imagename: 'kitten.png_1648055258930',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id456',
                },
            ],
        },
        USER_LOGIN: 'editor-1',
    },
    'Internal authors controller PUT /internal/authors/:id Возвращает ошибку и статус 400, если не указан логин пользователя':
        {
            AUTHOR_DATA: {
                urlPart: 'test',
                name: 'Тест',
                position: Position.author,
                photo: {
                    meta: {
                        md5: 'c4782f6be1710b107031cee0a8b42da3',
                        crc64: '24D5842733EDC82E',
                        'orig-size': {
                            x: 598,
                            y: 336,
                        },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 19697,
                        'orig-orientation': '0',
                        'modification-time': 1648103509,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                            width: 598,
                            height: 336,
                        },
                        '200x200': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                            width: 200,
                            height: 112,
                        },
                        '296x296': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                            width: 296,
                            height: 166,
                        },
                        '320x320': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                            width: 320,
                            height: 180,
                        },
                        '338x338': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                            width: 338,
                            height: 190,
                        },
                        '385x385': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                            width: 385,
                            height: 216,
                        },
                        '439x439': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                            width: 439,
                            height: 247,
                        },
                        '460x460': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                            width: 460,
                            height: 258,
                        },
                        '500x500': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                            width: 500,
                            height: 281,
                        },
                        '571x571': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                            width: 571,
                            height: 321,
                        },
                        '650x650': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                            width: 598,
                            height: 336,
                        },
                        '741x741': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                            width: 598,
                            height: 336,
                        },
                        '845x845': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                            width: 598,
                            height: 336,
                        },
                        '964x964': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                            width: 598,
                            height: 336,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                            width: 320,
                            height: 180,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                            width: 460,
                            height: 258,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                            width: 598,
                            height: 336,
                        },
                        '1098x1098': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                            width: 598,
                            height: 336,
                        },
                        '1200x1200': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                            width: 598,
                            height: 336,
                        },
                        '1252x1252': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                            width: 598,
                            height: 336,
                        },
                        '1428x1428': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                            width: 598,
                            height: 336,
                        },
                        '1600x1600': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                            width: 598,
                            height: 336,
                        },
                        '1920x1920': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                            width: 598,
                            height: 336,
                        },
                        '2560x2560': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                            width: 598,
                            height: 336,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                            width: 598,
                            height: 336,
                        },
                    },
                    'group-id': 1399832,
                    imagename: 'car.jpeg_1648103509611',
                },
                socialNetworks: [
                    {
                        type: SocialNetworkType.vk,
                        url: 'https://vk.com/id111',
                    },
                    {
                        type: SocialNetworkType.vk,
                        url: 'https://vk.com/id222',
                    },
                ],
            },
            AUTHOR_UPDATE_DATA: {
                urlPart: 'redakciya-avtoru',
                name: 'Редакция Авто.ру',
                position: Position.editor,
                photo: {
                    meta: {
                        md5: 'b6db85a4300596c93045d977081768d1',
                        crc64: '4004BBE4DFEBAD37',
                        'orig-size': {
                            x: 1280,
                            y: 1280,
                        },
                        processing: 'finished',
                        'orig-format': 'PNG',
                        'orig-animated': false,
                        'orig-size-bytes': 1480201,
                        'orig-orientation': '0',
                        'modification-time': 1648055259,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/orig',
                            width: 1280,
                            height: 1280,
                        },
                        '200x200': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/200x200',
                            width: 200,
                            height: 200,
                        },
                        '296x296': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/296x296',
                            width: 296,
                            height: 296,
                        },
                        '320x320': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320x320',
                            width: 320,
                            height: 320,
                        },
                        '338x338': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/338x338',
                            width: 338,
                            height: 338,
                        },
                        '385x385': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/385x385',
                            width: 385,
                            height: 385,
                        },
                        '439x439': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/439x439',
                            width: 439,
                            height: 439,
                        },
                        '460x460': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460x460',
                            width: 460,
                            height: 460,
                        },
                        '500x500': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/500x500',
                            width: 500,
                            height: 500,
                        },
                        '571x571': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/571x571',
                            width: 571,
                            height: 571,
                        },
                        '650x650': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/650x650',
                            width: 650,
                            height: 650,
                        },
                        '741x741': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/741x741',
                            width: 741,
                            height: 741,
                        },
                        '845x845': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/845x845',
                            width: 845,
                            height: 845,
                        },
                        '964x964': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/964x964',
                            width: 964,
                            height: 964,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320х320',
                            width: 320,
                            height: 320,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460х460',
                            width: 460,
                            height: 460,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/optimize',
                            width: 1280,
                            height: 1280,
                        },
                        '1098x1098': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1098x1098',
                            width: 1098,
                            height: 1098,
                        },
                        '1200x1200': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200x1200',
                            width: 1200,
                            height: 1200,
                        },
                        '1252x1252': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1252x1252',
                            width: 1252,
                            height: 1252,
                        },
                        '1428x1428': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1428x1428',
                            width: 1280,
                            height: 1280,
                        },
                        '1600x1600': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1600x1600',
                            width: 1280,
                            height: 1280,
                        },
                        '1920x1920': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1920x1920',
                            width: 1280,
                            height: 1280,
                        },
                        '2560x2560': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/2560x2560',
                            width: 1280,
                            height: 1280,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200х1200',
                            width: 1200,
                            height: 1200,
                        },
                    },
                    'group-id': 1399832,
                    imagename: 'kitten.png_1648055258930',
                },
                socialNetworks: [
                    {
                        type: SocialNetworkType.vk,
                        url: 'https://vk.com/id456',
                    },
                ],
            },
        },
    'Internal authors controller PUT /internal/authors/:id Возвращает ошибку и статус 400, если уже существует пользователь с таким urlPart':
        {
            AUTHOR_DATA_1: {
                urlPart: 'aleksandr-miroshkin',
                name: 'Александр Мирошкин',
                position: Position.editor,
                socialNetworks: [],
            },
            AUTHOR_DATA_2: {
                urlPart: 'testovyi-avtor',
                name: 'Тестовый автор',
                position: Position.editor,
                socialNetworks: [],
            },
            AUTHOR_UPDATE_DATA: {
                urlPart: ' TESTovyi-AVTOR ',
                name: 'Редакция Авто.ру',
                position: Position.editor,
                photo: {
                    meta: {
                        md5: 'b6db85a4300596c93045d977081768d1',
                        crc64: '4004BBE4DFEBAD37',
                        'orig-size': {
                            x: 1280,
                            y: 1280,
                        },
                        processing: 'finished',
                        'orig-format': 'PNG',
                        'orig-animated': false,
                        'orig-size-bytes': 1480201,
                        'orig-orientation': '0',
                        'modification-time': 1648055259,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/orig',
                            width: 1280,
                            height: 1280,
                        },
                        '200x200': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/200x200',
                            width: 200,
                            height: 200,
                        },
                        '296x296': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/296x296',
                            width: 296,
                            height: 296,
                        },
                        '320x320': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320x320',
                            width: 320,
                            height: 320,
                        },
                        '338x338': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/338x338',
                            width: 338,
                            height: 338,
                        },
                        '385x385': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/385x385',
                            width: 385,
                            height: 385,
                        },
                        '439x439': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/439x439',
                            width: 439,
                            height: 439,
                        },
                        '460x460': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460x460',
                            width: 460,
                            height: 460,
                        },
                        '500x500': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/500x500',
                            width: 500,
                            height: 500,
                        },
                        '571x571': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/571x571',
                            width: 571,
                            height: 571,
                        },
                        '650x650': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/650x650',
                            width: 650,
                            height: 650,
                        },
                        '741x741': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/741x741',
                            width: 741,
                            height: 741,
                        },
                        '845x845': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/845x845',
                            width: 845,
                            height: 845,
                        },
                        '964x964': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/964x964',
                            width: 964,
                            height: 964,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320х320',
                            width: 320,
                            height: 320,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460х460',
                            width: 460,
                            height: 460,
                        },
                        optimize: {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/optimize',
                            width: 1280,
                            height: 1280,
                        },
                        '1098x1098': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1098x1098',
                            width: 1098,
                            height: 1098,
                        },
                        '1200x1200': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200x1200',
                            width: 1200,
                            height: 1200,
                        },
                        '1252x1252': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1252x1252',
                            width: 1252,
                            height: 1252,
                        },
                        '1428x1428': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1428x1428',
                            width: 1280,
                            height: 1280,
                        },
                        '1600x1600': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1600x1600',
                            width: 1280,
                            height: 1280,
                        },
                        '1920x1920': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1920x1920',
                            width: 1280,
                            height: 1280,
                        },
                        '2560x2560': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/2560x2560',
                            width: 1280,
                            height: 1280,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200х1200',
                            width: 1200,
                            height: 1200,
                        },
                    },
                    'group-id': 1399832,
                    imagename: 'kitten.png_1648055258930',
                },
                socialNetworks: [
                    {
                        type: SocialNetworkType.vk,
                        url: 'https://vk.com/id456',
                    },
                ],
            },
            USER_LOGIN: 'editor-1',
        },
    'Internal authors controller PUT /internal/authors/:id Обновляет и возвращает автора': {
        AUTHOR_DATA: {
            urlPart: ' TESTovyi-AVTOR ',
            name: 'test',
            position: Position.author,
            photo: {
                meta: {
                    md5: 'c4782f6be1710b107031cee0a8b42da3',
                    crc64: '24D5842733EDC82E',
                    'orig-size': {
                        x: 598,
                        y: 336,
                    },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 19697,
                    'orig-orientation': '0',
                    'modification-time': 1648103509,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/orig',
                        width: 598,
                        height: 336,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/200x200',
                        width: 200,
                        height: 112,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/296x296',
                        width: 296,
                        height: 166,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320x320',
                        width: 320,
                        height: 180,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/338x338',
                        width: 338,
                        height: 190,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/385x385',
                        width: 385,
                        height: 216,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/439x439',
                        width: 439,
                        height: 247,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460x460',
                        width: 460,
                        height: 258,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/500x500',
                        width: 500,
                        height: 281,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/571x571',
                        width: 571,
                        height: 321,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/650x650',
                        width: 598,
                        height: 336,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/741x741',
                        width: 598,
                        height: 336,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/845x845',
                        width: 598,
                        height: 336,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/964x964',
                        width: 598,
                        height: 336,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/320х320',
                        width: 320,
                        height: 180,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/460х460',
                        width: 460,
                        height: 258,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/optimize',
                        width: 598,
                        height: 336,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1098x1098',
                        width: 598,
                        height: 336,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200x1200',
                        width: 598,
                        height: 336,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1252x1252',
                        width: 598,
                        height: 336,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1428x1428',
                        width: 598,
                        height: 336,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1600x1600',
                        width: 598,
                        height: 336,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1920x1920',
                        width: 598,
                        height: 336,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/2560x2560',
                        width: 598,
                        height: 336,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/car.jpeg_1648103509611/1200х1200',
                        width: 598,
                        height: 336,
                    },
                },
                'group-id': 1399832,
                imagename: 'car.jpeg_1648103509611',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id111',
                },
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id222',
                },
            ],
        },
        AUTHOR_UPDATE_DATA: {
            urlPart: 'redakciya-avtoru',
            name: 'Редакция Авто.ру',
            position: Position.editor,
            photo: {
                meta: {
                    md5: 'b6db85a4300596c93045d977081768d1',
                    crc64: '4004BBE4DFEBAD37',
                    'orig-size': {
                        x: 1280,
                        y: 1280,
                    },
                    processing: 'finished',
                    'orig-format': 'PNG',
                    'orig-animated': false,
                    'orig-size-bytes': 1480201,
                    'orig-orientation': '0',
                    'modification-time': 1648055259,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/orig',
                        width: 1280,
                        height: 1280,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/200x200',
                        width: 200,
                        height: 200,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/296x296',
                        width: 296,
                        height: 296,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320x320',
                        width: 320,
                        height: 320,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/338x338',
                        width: 338,
                        height: 338,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/385x385',
                        width: 385,
                        height: 385,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/439x439',
                        width: 439,
                        height: 439,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460x460',
                        width: 460,
                        height: 460,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/500x500',
                        width: 500,
                        height: 500,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/571x571',
                        width: 571,
                        height: 571,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/650x650',
                        width: 650,
                        height: 650,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/741x741',
                        width: 741,
                        height: 741,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/845x845',
                        width: 845,
                        height: 845,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/964x964',
                        width: 964,
                        height: 964,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/320х320',
                        width: 320,
                        height: 320,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/460х460',
                        width: 460,
                        height: 460,
                    },
                    optimize: {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/optimize',
                        width: 1280,
                        height: 1280,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1098x1098',
                        width: 1098,
                        height: 1098,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200x1200',
                        width: 1200,
                        height: 1200,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1252x1252',
                        width: 1252,
                        height: 1252,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1428x1428',
                        width: 1280,
                        height: 1280,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1600x1600',
                        width: 1280,
                        height: 1280,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1920x1920',
                        width: 1280,
                        height: 1280,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/2560x2560',
                        width: 1280,
                        height: 1280,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/1399832/kitten.png_1648055258930/1200х1200',
                        width: 1200,
                        height: 1200,
                    },
                },
                'group-id': 1399832,
                imagename: 'kitten.png_1648055258930',
            },
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id456',
                },
            ],
        },
        USER_LOGIN: 'editor-1',
    },
    'Internal authors controller DELETE /internal/authors/:id Возвращает ошибку и статус 400, если не указан логин пользователя':
        {
            AUTHOR_DATA: {
                urlPart: 'redakciya-avtoru',
                name: 'Редакция Авто.ру',
                position: Position.editor,
                socialNetworks: [
                    {
                        type: SocialNetworkType.vk,
                        url: 'https://vk.com/id456',
                    },
                ],
            },
        },
    'Internal authors controller DELETE /internal/authors/:id Удаляет и возвращает удаленного автора': {
        AUTHOR_DATA: {
            urlPart: 'redakciya-avtoru',
            name: 'Редакция Авто.ру',
            position: Position.editor,
            socialNetworks: [
                {
                    type: SocialNetworkType.vk,
                    url: 'https://vk.com/id456',
                },
            ],
        },
        USER_LOGIN: 'editor-1',
    },
};
