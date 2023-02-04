import { SocialNetworkType } from '../../../types/author-social-network';
import { Position } from '../../../types/author';

export const fixtures = {
    'Public authors controller GET /authors/:urlPart Возвращает автора по его urlPart': {
        AUTHOR_DATA: {
            urlPart: 'redakcyia-avtoru',
            name: 'Редакция Авто.ру',
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
};
