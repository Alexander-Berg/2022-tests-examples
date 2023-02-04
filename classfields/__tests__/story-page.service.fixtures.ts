import { StoryModel, StoryVersion } from '../../story/story.model';
import { StoryPageModel, StoryPageRatio, StoryPageTheme } from '../story-page.model';

const DEFAULT_STORY: Partial<StoryModel> = {
    urlPart: 'story-url-part',
    uuid: '59991017-3390-4065-ba4a-cb225645dc5b',
    title: 'Заголовок истории',
    textColor: '#fffbb1',
    version: StoryVersion.images,
    imageUrl: '/test/materials/images/59991017-3390-4065-ba4a-cb225645dc5b/image/default_1.jpg',
    xIosAppVersion: '1.0.1',
    xIosAppVersionTo: '1.0.1',
    xAndroidAppVersion: '1.0.1',
    xAndroidAppVersionTo: '1.0.1',
    rotationProbability: 0.212,
    dirty: true,
    geo: [1],
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
    imageUrl: '/59991017-3390-4065-ba4a-cb225645dc5b/1/default_1',
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
    imageUrl: '/59991017-3390-4065-ba4a-cb225645dc5b/2/default_1',
    duration: 20,
};

export const fixtures = {
    'Story page service createMany Кидает SQL-ошибку, если история не найдена': {
        STORY_PAGE_ATTRIBUTES: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
    },
    'Story page service createMany Создает страницы для существующей истории и возвращает их': {
        STORY_PAGE_ATTRIBUTES_1: {
            ...DEFAULT_STORY_PAGE_1,
            slide: 1,
        },
        STORY_PAGE_ATTRIBUTES_2: {
            ...DEFAULT_STORY_PAGE_2,
            slide: 2,
        },
    },

    'Story page service removeByStoryId Удаляет страницы по id истории': {
        STORY_ATTRIBUTES: {
            ...DEFAULT_STORY,
        },
    },
};
