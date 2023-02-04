import { PostStatus } from '../../../types/post';
import { Service } from '../../../types/common';
import { PostSchemaMarkupTypes } from '../../../types/post-schema-markup';

const HTML_FOR_MARKUP = '<h2>В чём особенности зарядки электромобиля?</h2><p>Почти никаких!</p>';

export const fixtures = {
    'Autoru post schema markup generator batch service generateFaqPageMarkups Создает микроразметку': {
        POST_ATTRIBUTES: {
            blocks: [
                {
                    type: 'text',
                    text: HTML_FOR_MARKUP,
                },
                {
                    type: 'highlightedText',
                    highlightedText: HTML_FOR_MARKUP,
                },
                {
                    type: 'code',
                    code: HTML_FOR_MARKUP,
                },
                {
                    type: 'htmlCard',
                    htmlCard: HTML_FOR_MARKUP,
                },
                {
                    type: 'cardWithImages',
                    cardWithImages: [
                        {
                            type: 'text',
                            text: HTML_FOR_MARKUP,
                        },
                        {
                            type: 'code',
                            code: HTML_FOR_MARKUP,
                        },
                        {
                            type: 'imageWithDescription',
                            imageWithDescription: {
                                description: HTML_FOR_MARKUP,
                            },
                        },
                        {
                            type: 'code',
                            code: HTML_FOR_MARKUP,
                        },
                    ],
                },
                {
                    type: 'imageWithDescription',
                    imageWithDescription: {
                        description: HTML_FOR_MARKUP,
                    },
                },
                {
                    type: 'video',
                    video: {
                        description: HTML_FOR_MARKUP,
                    },
                },
                {
                    type: 'person',
                    person: {
                        subtitle: HTML_FOR_MARKUP,
                    },
                },
            ],
            status: PostStatus.publish,
            draftBlocks: [],
            urlPart: 'seriynyy-mikrokar-microlino-v-stile-bmw-isetta-pribavil-v-moshchnosti-i-zapase-hoda',
            service: Service.autoru,
            title: 'Серийный микрокар Microlino в стиле BMW Isetta прибавил в мощности и запасе хода',
            draftTitle: 'Серийный микрокар Microlino в стиле BMW Isetta прибавил в мощности и запасе хода (черновик)',
            lead: 'На выставке IAA рассекретили серийную версию городского микроэлектромобиля Microlino 2.0, назвали его базовую цену и огласили набор модификаций',
            draftMainImage: {
                meta: {
                    md5: 'f659619a29aa2c41d80c1d0735a527b6',
                    crc64: '718A85C47CD3164F',
                    'orig-size': { x: 791, y: 593 },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 67431,
                    'orig-orientation': '0',
                    'modification-time': 1631114912,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/orig',
                        width: 791,
                        height: 593,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/200x200',
                        width: 200,
                        height: 150,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/296x296',
                        width: 296,
                        height: 222,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/320x320',
                        width: 320,
                        height: 240,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/338x338',
                        width: 338,
                        height: 253,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/385x385',
                        width: 385,
                        height: 289,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/439x439',
                        width: 439,
                        height: 329,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/460x460',
                        width: 460,
                        height: 345,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/500x500',
                        width: 500,
                        height: 375,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/571x571',
                        width: 571,
                        height: 428,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/650x650',
                        width: 650,
                        height: 487,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/741x741',
                        width: 741,
                        height: 556,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/845x845',
                        width: 791,
                        height: 593,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/964x964',
                        width: 791,
                        height: 593,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/320х320',
                        width: 320,
                        height: 240,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/460х460',
                        width: 460,
                        height: 345,
                    },
                    optimize: {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/optimize',
                        width: 791,
                        height: 593,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1098x1098',
                        width: 791,
                        height: 593,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1200x1200',
                        width: 791,
                        height: 593,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1252x1252',
                        width: 791,
                        height: 593,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1428x1428',
                        width: 791,
                        height: 593,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1600x1600',
                        width: 791,
                        height: 593,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1920x1920',
                        width: 791,
                        height: 593,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/2560x2560',
                        width: 791,
                        height: 593,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1200х1200',
                        width: 791,
                        height: 593,
                    },
                },
                'group-id': 4469561,
                imagename: 'Bezymyannyy.jpg_1631114912561',
            },
            author: 'kirillmilesh',
            publishAt: '2021-09-10T08:00:00.000Z',
            lastEditLogin: 'kirillmilesh',
        },
    },
    'Autoru post schema markup generator batch service generateFaqPageMarkups Не создает микроразметку для постов без блоков':
        {
            POST_ATTRIBUTES: {
                blocks: [],
                status: PostStatus.publish,
                draftBlocks: [],
                urlPart: 'seriynyy-mikrokar-microlino-v-stile-bmw-isetta-pribavil-v-moshchnosti-i-zapase-hoda',
                service: Service.autoru,
                title: 'Серийный микрокар Microlino в стиле BMW Isetta прибавил в мощности и запасе хода',
                draftTitle:
                    'Серийный микрокар Microlino в стиле BMW Isetta прибавил в мощности и запасе хода (черновик)',
                lead: 'На выставке IAA рассекретили серийную версию городского микроэлектромобиля Microlino 2.0, назвали его базовую цену и огласили набор модификаций',
                draftMainImage: {
                    meta: {
                        md5: 'f659619a29aa2c41d80c1d0735a527b6',
                        crc64: '718A85C47CD3164F',
                        'orig-size': { x: 791, y: 593 },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 67431,
                        'orig-orientation': '0',
                        'modification-time': 1631114912,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/orig',
                            width: 791,
                            height: 593,
                        },
                        '200x200': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/200x200',
                            width: 200,
                            height: 150,
                        },
                        '296x296': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/296x296',
                            width: 296,
                            height: 222,
                        },
                        '320x320': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/320x320',
                            width: 320,
                            height: 240,
                        },
                        '338x338': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/338x338',
                            width: 338,
                            height: 253,
                        },
                        '385x385': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/385x385',
                            width: 385,
                            height: 289,
                        },
                        '439x439': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/439x439',
                            width: 439,
                            height: 329,
                        },
                        '460x460': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/460x460',
                            width: 460,
                            height: 345,
                        },
                        '500x500': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/500x500',
                            width: 500,
                            height: 375,
                        },
                        '571x571': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/571x571',
                            width: 571,
                            height: 428,
                        },
                        '650x650': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/650x650',
                            width: 650,
                            height: 487,
                        },
                        '741x741': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/741x741',
                            width: 741,
                            height: 556,
                        },
                        '845x845': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/845x845',
                            width: 791,
                            height: 593,
                        },
                        '964x964': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/964x964',
                            width: 791,
                            height: 593,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/320х320',
                            width: 320,
                            height: 240,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/460х460',
                            width: 460,
                            height: 345,
                        },
                        optimize: {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/optimize',
                            width: 791,
                            height: 593,
                        },
                        '1098x1098': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1098x1098',
                            width: 791,
                            height: 593,
                        },
                        '1200x1200': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1200x1200',
                            width: 791,
                            height: 593,
                        },
                        '1252x1252': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1252x1252',
                            width: 791,
                            height: 593,
                        },
                        '1428x1428': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1428x1428',
                            width: 791,
                            height: 593,
                        },
                        '1600x1600': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1600x1600',
                            width: 791,
                            height: 593,
                        },
                        '1920x1920': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1920x1920',
                            width: 791,
                            height: 593,
                        },
                        '2560x2560': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/2560x2560',
                            width: 791,
                            height: 593,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1200х1200',
                            width: 791,
                            height: 593,
                        },
                    },
                    'group-id': 4469561,
                    imagename: 'Bezymyannyy.jpg_1631114912561',
                },
                author: 'kirillmilesh',
                publishAt: '2021-09-10T08:00:00.000Z',
                lastEditLogin: 'kirillmilesh',
            },
        },
    'Autoru post schema markup generator batch service generateFaqPageMarkups Обновляет микроразметку': {
        POST_ATTRIBUTES: {
            blocks: [
                {
                    type: 'text',
                    text: HTML_FOR_MARKUP,
                },
                {
                    type: 'highlightedText',
                    highlightedText: HTML_FOR_MARKUP,
                },
                {
                    type: 'code',
                    code: HTML_FOR_MARKUP,
                },
                {
                    type: 'htmlCard',
                    htmlCard: HTML_FOR_MARKUP,
                },
                {
                    type: 'cardWithImages',
                    cardWithImages: [
                        {
                            type: 'text',
                            text: HTML_FOR_MARKUP,
                        },
                        {
                            type: 'code',
                            code: HTML_FOR_MARKUP,
                        },
                        {
                            type: 'imageWithDescription',
                            imageWithDescription: {
                                description: HTML_FOR_MARKUP,
                            },
                        },
                        {
                            type: 'code',
                            code: HTML_FOR_MARKUP,
                        },
                    ],
                },
                {
                    type: 'imageWithDescription',
                    imageWithDescription: {
                        description: HTML_FOR_MARKUP,
                    },
                },
                {
                    type: 'video',
                    video: {
                        description: HTML_FOR_MARKUP,
                    },
                },
                {
                    type: 'person',
                    person: {
                        subtitle: HTML_FOR_MARKUP,
                    },
                },
            ],
            status: PostStatus.publish,
            draftBlocks: [],
            urlPart: 'seriynyy-mikrokar-microlino-v-stile-bmw-isetta-pribavil-v-moshchnosti-i-zapase-hoda',
            service: Service.autoru,
            title: 'Серийный микрокар Microlino в стиле BMW Isetta прибавил в мощности и запасе хода',
            draftTitle: 'Серийный микрокар Microlino в стиле BMW Isetta прибавил в мощности и запасе хода (черновик)',
            lead: 'На выставке IAA рассекретили серийную версию городского микроэлектромобиля Microlino 2.0, назвали его базовую цену и огласили набор модификаций',
            draftMainImage: {
                meta: {
                    md5: 'f659619a29aa2c41d80c1d0735a527b6',
                    crc64: '718A85C47CD3164F',
                    'orig-size': { x: 791, y: 593 },
                    processing: 'finished',
                    'orig-format': 'JPEG',
                    'orig-animated': false,
                    'orig-size-bytes': 67431,
                    'orig-orientation': '0',
                    'modification-time': 1631114912,
                    processed_by_computer_vision: false,
                    processed_by_computer_vision_description: 'computer vision is disabled',
                },
                sizes: {
                    orig: {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/orig',
                        width: 791,
                        height: 593,
                    },
                    '200x200': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/200x200',
                        width: 200,
                        height: 150,
                    },
                    '296x296': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/296x296',
                        width: 296,
                        height: 222,
                    },
                    '320x320': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/320x320',
                        width: 320,
                        height: 240,
                    },
                    '338x338': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/338x338',
                        width: 338,
                        height: 253,
                    },
                    '385x385': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/385x385',
                        width: 385,
                        height: 289,
                    },
                    '439x439': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/439x439',
                        width: 439,
                        height: 329,
                    },
                    '460x460': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/460x460',
                        width: 460,
                        height: 345,
                    },
                    '500x500': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/500x500',
                        width: 500,
                        height: 375,
                    },
                    '571x571': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/571x571',
                        width: 571,
                        height: 428,
                    },
                    '650x650': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/650x650',
                        width: 650,
                        height: 487,
                    },
                    '741x741': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/741x741',
                        width: 741,
                        height: 556,
                    },
                    '845x845': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/845x845',
                        width: 791,
                        height: 593,
                    },
                    '964x964': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/964x964',
                        width: 791,
                        height: 593,
                    },
                    '320х320': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/320х320',
                        width: 320,
                        height: 240,
                    },
                    '460х460': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/460х460',
                        width: 460,
                        height: 345,
                    },
                    optimize: {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/optimize',
                        width: 791,
                        height: 593,
                    },
                    '1098x1098': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1098x1098',
                        width: 791,
                        height: 593,
                    },
                    '1200x1200': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1200x1200',
                        width: 791,
                        height: 593,
                    },
                    '1252x1252': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1252x1252',
                        width: 791,
                        height: 593,
                    },
                    '1428x1428': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1428x1428',
                        width: 791,
                        height: 593,
                    },
                    '1600x1600': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1600x1600',
                        width: 791,
                        height: 593,
                    },
                    '1920x1920': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1920x1920',
                        width: 791,
                        height: 593,
                    },
                    '2560x2560': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/2560x2560',
                        width: 791,
                        height: 593,
                    },
                    '1200х1200': {
                        path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1200х1200',
                        width: 791,
                        height: 593,
                    },
                },
                'group-id': 4469561,
                imagename: 'Bezymyannyy.jpg_1631114912561',
            },
            author: 'kirillmilesh',
            publishAt: '2021-09-10T08:00:00.000Z',
            lastEditLogin: 'kirillmilesh',
        },
        POST_SCHEMA_MARKUP: {
            type: PostSchemaMarkupTypes.faqPage,
            markup: [],
        },
    },
    'Autoru post schema markup generator batch service generateFaqPageMarkups Удаляет микроразметку, если она стала пустой':
        {
            POST_ATTRIBUTES: {
                blocks: [],
                status: PostStatus.publish,
                draftBlocks: [],
                urlPart: 'seriynyy-mikrokar-microlino-v-stile-bmw-isetta-pribavil-v-moshchnosti-i-zapase-hoda',
                service: Service.autoru,
                title: 'Серийный микрокар Microlino в стиле BMW Isetta прибавил в мощности и запасе хода',
                draftTitle:
                    'Серийный микрокар Microlino в стиле BMW Isetta прибавил в мощности и запасе хода (черновик)',
                lead: 'На выставке IAA рассекретили серийную версию городского микроэлектромобиля Microlino 2.0, назвали его базовую цену и огласили набор модификаций',
                draftMainImage: {
                    meta: {
                        md5: 'f659619a29aa2c41d80c1d0735a527b6',
                        crc64: '718A85C47CD3164F',
                        'orig-size': { x: 791, y: 593 },
                        processing: 'finished',
                        'orig-format': 'JPEG',
                        'orig-animated': false,
                        'orig-size-bytes': 67431,
                        'orig-orientation': '0',
                        'modification-time': 1631114912,
                        processed_by_computer_vision: false,
                        processed_by_computer_vision_description: 'computer vision is disabled',
                    },
                    sizes: {
                        orig: {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/orig',
                            width: 791,
                            height: 593,
                        },
                        '200x200': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/200x200',
                            width: 200,
                            height: 150,
                        },
                        '296x296': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/296x296',
                            width: 296,
                            height: 222,
                        },
                        '320x320': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/320x320',
                            width: 320,
                            height: 240,
                        },
                        '338x338': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/338x338',
                            width: 338,
                            height: 253,
                        },
                        '385x385': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/385x385',
                            width: 385,
                            height: 289,
                        },
                        '439x439': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/439x439',
                            width: 439,
                            height: 329,
                        },
                        '460x460': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/460x460',
                            width: 460,
                            height: 345,
                        },
                        '500x500': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/500x500',
                            width: 500,
                            height: 375,
                        },
                        '571x571': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/571x571',
                            width: 571,
                            height: 428,
                        },
                        '650x650': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/650x650',
                            width: 650,
                            height: 487,
                        },
                        '741x741': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/741x741',
                            width: 741,
                            height: 556,
                        },
                        '845x845': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/845x845',
                            width: 791,
                            height: 593,
                        },
                        '964x964': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/964x964',
                            width: 791,
                            height: 593,
                        },
                        '320х320': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/320х320',
                            width: 320,
                            height: 240,
                        },
                        '460х460': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/460х460',
                            width: 460,
                            height: 345,
                        },
                        optimize: {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/optimize',
                            width: 791,
                            height: 593,
                        },
                        '1098x1098': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1098x1098',
                            width: 791,
                            height: 593,
                        },
                        '1200x1200': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1200x1200',
                            width: 791,
                            height: 593,
                        },
                        '1252x1252': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1252x1252',
                            width: 791,
                            height: 593,
                        },
                        '1428x1428': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1428x1428',
                            width: 791,
                            height: 593,
                        },
                        '1600x1600': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1600x1600',
                            width: 791,
                            height: 593,
                        },
                        '1920x1920': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1920x1920',
                            width: 791,
                            height: 593,
                        },
                        '2560x2560': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/2560x2560',
                            width: 791,
                            height: 593,
                        },
                        '1200х1200': {
                            path: '/get-vertis-journal/4469561/Bezymyannyy.jpg_1631114912561/1200х1200',
                            width: 791,
                            height: 593,
                        },
                    },
                    'group-id': 4469561,
                    imagename: 'Bezymyannyy.jpg_1631114912561',
                },
                author: 'kirillmilesh',
                publishAt: '2021-09-10T08:00:00.000Z',
                lastEditLogin: 'kirillmilesh',
            },
            POST_SCHEMA_MARKUP: {
                type: PostSchemaMarkupTypes.faqPage,
                markup: [{ type: 'text', text: 'Test' }],
            },
        },
};
