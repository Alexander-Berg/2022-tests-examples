import { StoryPageModel, StoryPageRatio, StoryPageTheme } from '../../story-page/story-page.model';
import { StoryModel, StoryVersion } from '../../story/story.model';

const DEFAULT_STORY: Partial<StoryModel> = {
    urlPart: 'story-url-part',
    uuid: '59991017-3390-4065-ba4a-cb225645dc5b',
    title: 'Заголовок истории',
    textColor: '#fefefe',
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

const DEFAULT_STORY_PAGE: Partial<StoryPageModel> = {
    title: 'Заголовок слайда',
    body: 'Текст слайда',
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

const createStoriesAttributesArray = (count, additionalAttributes = {}) =>
    Array(count)
        .fill(0)
        .map((_, index) => ({
            ...DEFAULT_STORY,
            title: `${DEFAULT_STORY.title} ${index}`,
            urlPart: `${DEFAULT_STORY.urlPart}-${index}`,
            ...additionalAttributes,
        }));

const createPageAttributesArray = count =>
    Array(count)
        .fill(0)
        .map((_, index) => ({
            ...DEFAULT_STORY_PAGE,
            title: `${DEFAULT_STORY_PAGE.title} ${index + 1}`,
            body: `${DEFAULT_STORY_PAGE.body} ${index + 1}`,
            slide: index + 1,
        }));

export const fixtures = {
    'Export stories service findByLayerAndType Возвращает список экспортов, если они есть для переданных layer и enum':
        {
            STORIES_ATTRIBUTES: createStoriesAttributesArray(10),
            STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
        },
    'Export stories service findByLayerAndType Возвращает пустой список, если их нет для переданных layer и enum': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(10),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service update Обновляет список историй, если передано 30 историй': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service update Обновляет список историй, обрезая до 30, если передано больше 30 историй': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service update Обновляет список историй, дополняя до 30 из последних опубликованных': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service update Обновляет список историй только для переданных layer и type': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service addStoryToPosition Добавляет историю на указанную позицию в списке и сдвигает остальные': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service addStoryToPosition Возвращает ошибку, если не найдена история': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service addStoryToPosition Возвращает ошибку, если история не опубликована': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service addStoryToPosition Возвращает ошибку, если передана некорректная позиция': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service remove Удаляет экпорт': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service remove Возвращает ошибку, если не найдено экспорта с переданным id': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
    'Export stories service moveToPosition Перемещает экспорт на новую позицию': {
        STORIES_ATTRIBUTES: createStoriesAttributesArray(50),
        STORY_PAGES_ATTRIBUTES: createPageAttributesArray(1),
    },
};
