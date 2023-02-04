import { StoryModel, StoryVersion } from '../story.model';
import { StoryPageModel, StoryPageRatio, StoryPageTheme } from '../../story-page/story-page.model';

const IMAGE = {
    'group-id': 1398410,
    imagename: '4747.jpg_1654518939714',
    meta: {
        'orig-animated': false,
        'orig-format': 'JPEG',
        'orig-size': {
            x: 960,
            y: 1280,
        },
        'orig-size-bytes': 168385,
    },
    sizes: {
        '1098x1098': {
            height: 1098,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/1098x1098',
            width: 824,
        },
        '1200х1200': {
            height: 1200,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/1200х1200',
            width: 900,
        },
        '1252x1252': {
            height: 1252,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/1252x1252',
            width: 939,
        },
        '1428x1428': {
            height: 1280,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/1428x1428',
            width: 960,
        },
        '1600x1600': {
            height: 1280,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/1600x1600',
            width: 960,
        },
        '1920x1920': {
            height: 1280,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/1920x1920',
            width: 960,
        },
        '320x320': {
            height: 320,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/320x320',
            width: 240,
        },
        '338x338': {
            height: 338,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/338x338',
            width: 253,
        },
        '439x439': {
            height: 439,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/439x439',
            width: 329,
        },
        '460x460': {
            height: 460,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/460x460',
            width: 345,
        },
        '571x571': {
            height: 571,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/571x571',
            width: 428,
        },
        '650x650': {
            height: 650,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/650x650',
            width: 488,
        },
        '845x845': {
            height: 845,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/845x845',
            width: 634,
        },
        optimize: {
            height: 1280,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/optimize',
            width: 960,
        },
        orig: {
            height: 1280,
            path: '/get-vertis-journal/1398410/4747.jpg_1654518939714/orig',
            width: 960,
        },
    },
};

const DEFAULT_STORY: Partial<StoryModel> = {
    urlPart: 'story-url-part',
    uuid: '59991017-3390-4065-ba4a-cb225645dc5b',
    title: 'Заголовок истории',
    textColor: '#fefefe',
    version: StoryVersion.images,
    image: IMAGE,
    xIosAppVersion: '1.0.42',
    xIosAppVersionTo: '1.0.212',
    xAndroidAppVersion: '1.0.42',
    xAndroidAppVersionTo: '1.0.212',
    rotationProbability: 0.212,
    dirty: true,
    geo: [1, 2],
};

const DEFAULT_STORY_PAGE: Partial<StoryPageModel> = {
    title: 'Заголовок слайда',
    body: 'Текст слайда',
    theme: StoryPageTheme.white,
    ratio: StoryPageRatio.square,
    backgroundColor: '#fffaaa',
    backgroundSolid: true,
    textColor: '#fffbbb',
    buttonColor: '#fffccc',
    buttonText: 'Текст кнопки',
    buttonUrl: 'https://example.com/',
    image: IMAGE,
    duration: 10,
};

export const fixtures = {
    'Public story controller GET /stories Возвращает опубликованные трансформированные сторис': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY,
            urlPart: 'story-url-part1',
            uuid: '59991017-3390-4065-ba4a-cb225645dc5b',
            published: true,
            publishedAt: '2022-04-07T10:21:30Z',
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY,
            urlPart: 'story-url-part2',
            uuid: '59991017-3461-4166-ba4a-ba335745db5a',
            published: true,
            publishedAt: '2022-04-07T10:23:30Z',
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY,
            urlPart: 'story-url-part3',
            uuid: '59991017-3279-4065-ba4a-ac324645cc5d',
            published: true,
            publishedAt: '2022-04-07T10:25:30Z',
        },
    },

    'Public story controller GET /stories Возвращает опубликованные сторис, отсортированные по убыванию даты публикации':
        {
            STORY_ATTRIBUTES_1: {
                ...DEFAULT_STORY,
                urlPart: 'story-url-part1',
                uuid: '59991017-3390-4065-ba4a-cb225645dc5b',
                published: true,
                publishedAt: '2022-04-07T10:21:30Z',
            },
            STORY_ATTRIBUTES_2: {
                ...DEFAULT_STORY,
                urlPart: 'story-url-part2',
                uuid: '59991017-3461-4166-ba4a-ba335745db5a',
                published: true,
                publishedAt: '2022-04-09T10:23:30Z',
            },
            STORY_ATTRIBUTES_3: {
                ...DEFAULT_STORY,
                urlPart: 'story-url-part3',
                uuid: '59991017-3279-4065-ba4a-ac324645cc5d',
                published: true,
                publishedAt: '2022-04-08T10:25:30Z',
            },
        },

    'Public story controller GET /stories Может отдавать сторис с указанным лимитом и страницей': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY,
            urlPart: 'story-url-part1',
            uuid: '59991017-3390-4065-ba4a-cb225645dc5b',
            published: true,
            publishedAt: '2022-04-07T10:21:30Z',
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY,
            urlPart: 'story-url-part2',
            uuid: '59991017-3461-4166-ba4a-ba335745db5a',
            published: true,
            publishedAt: '2022-04-09T10:23:30Z',
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY,
            urlPart: 'story-url-part3',
            uuid: '59991017-3279-4065-ba4a-ac324645cc5d',
            published: true,
            publishedAt: '2022-04-08T10:25:30Z',
        },
    },

    'Public story controller GET /stories Возвращает ошибку и статус 400, если порядок сортировки некорректен': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY,
            urlPart: 'story-url-part1',
            uuid: '59991017-3390-4065-ba4a-cb225645dc5b',
            published: true,
            publishedAt: '2022-04-07T10:21:30Z',
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY,
            urlPart: 'story-url-part2',
            uuid: '59991017-3461-4166-ba4a-ba335745db5a',
            published: true,
            publishedAt: '2022-04-09T10:23:30Z',
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY,
            urlPart: 'story-url-part3',
            uuid: '59991017-3279-4065-ba4a-ac324645cc5d',
            published: true,
            publishedAt: '2022-04-08T10:25:30Z',
        },
    },

    'Public story controller GET /stories/:urlPart Возвращает ошибку и статус 404, если сторис не опубликован': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY,
            publishedAt: null,
            published: false,
        },
        STORY_PAGE_ATTRIBUTES: {
            ...DEFAULT_STORY_PAGE,
        },
    },

    'Public story controller GET /stories/:urlPart Возвращает опубликованный трансформированный сторис и его страницы по urlPart':
        {
            STORY_ATTRIBUTES: {
                ...DEFAULT_STORY,
                publishedAt: '2022-04-07T10:20:30Z',
                published: true,
            },
            STORY_PAGE_ATTRIBUTES: {
                ...DEFAULT_STORY_PAGE,
            },
        },
};
