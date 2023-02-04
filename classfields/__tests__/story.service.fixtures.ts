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

const DEFAULT_STORY_1: Partial<StoryModel> = {
    urlPart: 'story-url-part-1',
    uuid: '59991017-3390-4065-ba4a-cb225645dc5b',
    title: 'Заголовок истории 1',
    textColor: '#fefefe',
    version: StoryVersion.images,
    image: IMAGE,
    xIosAppVersion: '1.0.1',
    xIosAppVersionTo: '1.0.1',
    xAndroidAppVersion: '1.0.1',
    xAndroidAppVersionTo: '1.0.1',
    rotationProbability: 0.212,
    dirty: true,
    geo: [1],
};

const DEFAULT_STORY_2: Partial<StoryModel> = {
    urlPart: 'story-url-part-2',
    uuid: 'a10b4baa-5b25-4e67-91e0-28334d6c2b5d',
    title: 'Заголовок истории 2',
    textColor: '#fafafa',
    version: StoryVersion.images,
    image: IMAGE,
    xIosAppVersion: '1.0.2',
    xIosAppVersionTo: '1.0.2',
    xAndroidAppVersion: '1.0.2',
    xAndroidAppVersionTo: '1.0.2',
    rotationProbability: 0.213,
    dirty: true,
    geo: [2],
};

const DEFAULT_STORY_3: Partial<StoryModel> = {
    urlPart: 'story-url-part-3',
    uuid: '1d3fef3c-d751-4759-ac47-455976ad984d',
    title: 'Заголовок истории 3',
    textColor: '#bebebe',
    version: StoryVersion.images,
    image: IMAGE,
    xIosAppVersion: '1.0.3',
    xIosAppVersionTo: '1.0.3',
    xAndroidAppVersion: '1.0.3',
    xAndroidAppVersionTo: '1.0.3',
    rotationProbability: 0.213,
    dirty: true,
    geo: [3],
};

const DEFAULT_STORY_4: Partial<StoryModel> = {
    urlPart: 'story-url-part-4',
    uuid: '1d3fef3c-d751-4759-ac47-455976ad984d',
    title: 'Заголовок истории 4',
    textColor: '#bebebe',
    version: StoryVersion.images,
    image: IMAGE,
    xIosAppVersion: '1.0.3',
    xIosAppVersionTo: '1.0.3',
    xAndroidAppVersion: '1.0.3',
    xAndroidAppVersionTo: '1.0.3',
    rotationProbability: 0.213,
    dirty: true,
    geo: [4],
};

const DEFAULT_STORY_PAGE_1: Partial<StoryPageModel> = {
    title: 'Заголовок слайда 1',
    body: 'Текст слайда 1',
    theme: StoryPageTheme.white,
    ratio: StoryPageRatio.square,
    backgroundColor: '#fffaa1',
    backgroundSolid: true,
    textColor: '#fffbb1',
    buttonColor: '#fffcc1',
    buttonText: 'Текст кнопки 1',
    buttonUrl: 'https://example.com/1',
    image: IMAGE,
    duration: 10,
};

const DEFAULT_STORY_PAGE_2: Partial<StoryPageModel> = {
    title: 'Заголовок слайда 2',
    body: 'Текст слайда 2',
    theme: StoryPageTheme.dark,
    ratio: StoryPageRatio.vertical,
    backgroundColor: '#fffaa2',
    backgroundSolid: true,
    textColor: '#fffbb2',
    buttonColor: '#fffcc2',
    buttonText: 'Текст кнопки 2',
    buttonUrl: 'https://example.com/2',
    image: IMAGE,
    duration: 20,
};

export const fixtures = {
    'Story service find Находит сторис по id и возвращает его со страницами и тегами': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
            tags: ['тег 1', 'тег 2'],
        },
        STORY_PAGE_ATTRIBUTES_1: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
        STORY_PAGE_ATTRIBUTES_2: {
            ...DEFAULT_STORY_PAGE_2,
            slide: 2,
        },
    },
    'Story service find Находит сторис по uuid и возвращает его со страницами и тегами': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
            tags: ['тег 1', 'тег 2'],
        },
        STORY_PAGE_ATTRIBUTES_1: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
        STORY_PAGE_ATTRIBUTES_2: {
            ...DEFAULT_STORY_PAGE_2,
            slide: 2,
        },
    },

    'Story service findByUrlPart Находит сторис и возвращает его со страницами и тегами': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_4,
            tags: ['тег 1', 'тег 2'],
        },
        STORY_PAGE_ATTRIBUTES_1: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
        STORY_PAGE_ATTRIBUTES_2: {
            ...DEFAULT_STORY_PAGE_2,
            slide: 2,
        },
    },

    'Story service findByUrlPart Находит опубликованный сторис со страницами и тегами': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_4,
            published: true,
            publishedAt: '2022-03-22T00:00:00.000Z',
            tags: ['тег 1', 'тег 2'],
        },
        STORY_PAGE_ATTRIBUTES_1: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
        STORY_PAGE_ATTRIBUTES_2: {
            ...DEFAULT_STORY_PAGE_2,
            slide: 2,
        },
    },

    'Story service findByUrlPart Возвращает undefined, если сторис не опубликован': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_4,
            published: false,
            tags: ['тег 1', 'тег 2'],
        },
        STORY_PAGE_ATTRIBUTES_1: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
        STORY_PAGE_ATTRIBUTES_2: {
            ...DEFAULT_STORY_PAGE_2,
            slide: 2,
        },
    },

    'Story service findByUrlPart Возвращает неопубликованный сторис со страницами и тегами при published = false': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_4,
            published: false,
            tags: ['тег 1', 'тег 2'],
        },
        STORY_PAGE_ATTRIBUTES_1: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
        STORY_PAGE_ATTRIBUTES_2: {
            ...DEFAULT_STORY_PAGE_2,
            slide: 2,
        },
    },

    'Story service findAndCountAll Возвращает сторис с указанным лимитом, отсортированные по дате создания': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
            createdAt: '2022-03-21T00:00:00.000Z',
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
            createdAt: '2022-03-20T00:00:00.000Z',
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY_3,
            createdAt: '2022-03-22T00:00:00.000Z',
        },
    },

    'Story service findAndCountAll Может фильтровать по published Возвращает только опубликованные сторис': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
            createdAt: '2022-03-20T00:00:00.000Z',
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
            createdAt: '2022-03-21T00:00:00.000Z',
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY_3,
            createdAt: '2022-03-22T00:00:00.000Z',
            published: true,
            publishedAt: '2022-04-07T10:20:30Z',
        },
    },

    'Story service findAndCountAll Может фильтровать по published Возвращает только неопубликованные сторис': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
            published: true,
            publishedAt: '2022-04-07T10:20:30Z',
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
            createdAt: '2022-03-21T00:00:00.000Z',
            published: false,
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY_3,
            createdAt: '2022-03-22T00:00:00.000Z',
            published: false,
        },
    },

    'Story service findAndCountAll Может фильтровать по urlPart': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
            createdAt: '2022-03-20T00:00:00.000Z',
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
            createdAt: '2022-03-21T00:00:00.000Z',
            urlPart: 'story-url-part',
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY_3,
            createdAt: '2022-03-22T00:00:00.000Z',
        },
    },

    'Story service findAndCountAll Может фильтровать по title': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
            createdAt: '2022-03-20T00:00:00.000Z',
            title: 'заголовок',
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
            createdAt: '2022-03-21T00:00:00.000Z',
            title: 'я не придумал как назвать',
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY_3,
            createdAt: '2022-03-22T00:00:00.000Z',
            title: 'другой заголовок абсолютно',
        },
    },

    'Story service findAndCountAll Может фильтровать по tags По одному тегу': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
            createdAt: '2022-03-20T00:00:00.000Z',
            tags: ['тег 1', 'тег 2'],
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
            createdAt: '2022-03-21T00:00:00.000Z',
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY_3,
            createdAt: '2022-03-22T00:00:00.000Z',
            tags: ['тег 1', 'тег 3'],
        },
    },

    'Story service findAndCountAll Может фильтровать по tags По нескольким тегам': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
            createdAt: '2022-03-20T00:00:00.000Z',
            tags: ['тег 1', 'тег 2'],
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
            createdAt: '2022-03-21T00:00:00.000Z',
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY_3,
            createdAt: '2022-03-22T00:00:00.000Z',
            tags: ['тег 2', 'тег 3'],
        },
    },

    'Story service findAndCountAll Может фильтровать по geo По одному geo': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
            createdAt: '2022-03-20T00:00:00.000Z',
            geo: [1, 2],
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
            createdAt: '2022-03-21T00:00:00.000Z',
            geo: undefined,
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY_3,
            createdAt: '2022-03-22T00:00:00.000Z',
            geo: [1],
        },
    },

    'Story service findAndCountAll Может фильтровать по geo По нескольким geo': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
            createdAt: '2022-03-20T00:00:00.000Z',
            geo: [1, 2],
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
            createdAt: '2022-03-21T00:00:00.000Z',
            geo: undefined,
        },
        STORY_ATTRIBUTES_3: {
            ...DEFAULT_STORY_3,
            createdAt: '2022-03-22T00:00:00.000Z',
            geo: [2, 3],
        },
    },

    'Story service findAndCountAll Возвращает сторис со связанными страницами и тегами': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
            tags: ['тег 1', 'тег 2'],
        },
        STORY_PAGE_ATTRIBUTES_1: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
        STORY_PAGE_ATTRIBUTES_2: {
            ...DEFAULT_STORY_PAGE_2,
            slide: 2,
        },
    },

    'Story service create Создаёт и возвращает сторис': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
        },
    },
    'Story service create Создаёт и возвращает сторис со связанными страницами': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
        },
        STORY_PAGE_ATTRIBUTES_1: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
        STORY_PAGE_ATTRIBUTES_2: {
            ...DEFAULT_STORY_PAGE_2,
            slide: 2,
        },
    },
    'Story service create Создаёт и возвращает сторис со связанными тегами': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
            tags: ['тег 1', 'тег 2'],
        },
    },

    'Story service update Кидает ошибку, если сторис не найден по id': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
        },
    },
    'Story service update Кидает ошибку, если сторис не найден по uuid': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
        },
    },
    'Story service update Обновляет и возвращает сторис по id': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
        },
    },
    'Story service update Обновляет и возвращает сторис по uuid': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
        },
    },
    'Story service update Обновляет и возвращает сторис со связанными страницами, перезаписывая страницы': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
        },
        STORY_PAGE_ATTRIBUTES_1: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
        STORY_PAGE_ATTRIBUTES_2: {
            ...DEFAULT_STORY_PAGE_2,
            slide: 2,
        },
    },
    'Story service update Обновляет и возвращает сторис со связанными тегами, сохраняя существующие теги': {
        STORY_ATTRIBUTES_1: {
            ...DEFAULT_STORY_1,
            tags: ['тег 1', 'тег 2'],
        },
        STORY_ATTRIBUTES_2: {
            ...DEFAULT_STORY_2,
            tags: ['тег 1', 'тег 2', 'тег 3'],
        },
    },

    'Story service publish Публикует сторис по id и возвращает его': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
            published: false,
        },
    },
    'Story service publish Публикует сторис по uuid и возвращает его': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
            published: false,
        },
    },

    'Story service unpublish Снимает сторис с публикации по id и возвращает его': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
            published: true,
            publishedAt: '2022-04-18T15:57:14.000Z',
        },
    },
    'Story service unpublish Снимает сторис с публикации по uuid и возвращает его': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY_1,
            published: true,
            publishedAt: '2022-04-18T15:57:14.000Z',
        },
    },
};
