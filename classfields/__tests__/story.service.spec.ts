import { describe } from '@jest/globals';

import { createTestingApp, ITestingApplication } from '../../../tests/app';
import { FactoryService } from '../../../tests/factory/factory.service';
import { getFixtures } from '../../../tests/get-fixtures';
import { StoryService } from '../story.service';
import { NotFoundException } from '../../../exceptions';

import { fixtures } from './story.service.fixtures';

describe('Story service', () => {
    let testingApp: ITestingApplication;
    let factory: FactoryService;
    let storyService: StoryService;

    beforeEach(async () => {
        Date.now = jest.fn(() => new Date('2022-04-07T10:20:30Z').getTime());

        testingApp = await createTestingApp();

        factory = testingApp.factory;
        storyService = await testingApp.module.resolve(StoryService);
    });

    afterEach(async () => {
        await testingApp.close();
    });

    describe('find', () => {
        it('Возвращает undefined, если сторис не существует', async () => {
            const NON_EXISTING_ID = 212;

            const result = await storyService.find(NON_EXISTING_ID);

            expect(result).toBeUndefined();
        });

        it('Находит сторис по id и возвращает его со страницами и тегами', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            await factory.createStoryPages(2, [
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_1 },
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_2 },
            ]);

            const result = await storyService.find(story.id);

            expect(result).toMatchSnapshot();
        });

        it('Находит сторис по uuid и возвращает его со страницами и тегами', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            await factory.createStoryPages(2, [
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_1 },
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_2 },
            ]);

            const result = await storyService.find(story.uuid as string);

            expect(result).toMatchSnapshot();
        });
    });

    describe('findByUrlPart', () => {
        it('Возвращает undefined, если сторис не существует', async () => {
            const NON_EXISTING_URLPART = 'non-existing';

            const result = await storyService.findByUrlPart(NON_EXISTING_URLPART);

            expect(result).toBeUndefined();
        });

        it('Возвращает undefined, если сторис не опубликован', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            await factory.createStoryPages(2, [
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_1 },
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_2 },
            ]);

            const result = await storyService.findByUrlPart(story.urlPart, { published: true });

            expect(result).toBeUndefined();
        });

        it('Находит сторис и возвращает его со страницами и тегами', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            await factory.createStoryPages(2, [
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_1 },
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_2 },
            ]);

            const result = await storyService.findByUrlPart(story.urlPart);

            expect(result).toMatchSnapshot();
        });

        it('Находит опубликованный сторис со страницами и тегами', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            await factory.createStoryPages(2, [
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_1 },
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_2 },
            ]);

            const result = await storyService.findByUrlPart(story.urlPart);

            expect(result).toMatchSnapshot();
        });

        it('Возвращает неопубликованный сторис со страницами и тегами при published = false', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            await factory.createStoryPages(2, [
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_1 },
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_2 },
            ]);

            const result = await storyService.findByUrlPart(story.urlPart, { published: false });

            expect(result).toMatchSnapshot();
        });
    });

    describe('findAndCountAll', () => {
        it('Возвращает пустой объект, если сторис нет', async () => {
            const result = await storyService.findAndCountAll();

            expect(result).toMatchSnapshot();
        });

        it('Возвращает сторис с указанным лимитом, отсортированные по дате создания', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createStories(5, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

            const result = await storyService.findAndCountAll({
                pageNumber: 0,
                pageSize: 3,
                orderBy: 'createdAt',
                orderByAsc: false,
            });

            expect(result).toMatchSnapshot();
        });

        it('Возвращает сторис со связанными страницами и тегами', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            await factory.createStoryPages(2, [
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_1 },
                { storyId: story.id, ...STORY_PAGE_ATTRIBUTES_2 },
            ]);

            const result = await storyService.findAndCountAll();

            expect(result).toMatchSnapshot();
        });

        it('Может фильтровать по urlPart', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createStories(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

            const result = await storyService.findAndCountAll({ urlPart: 'story-url-part' });

            expect(result).toMatchSnapshot();
        });

        it('Может фильтровать по title', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

            await factory.createStories(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

            const result = await storyService.findAndCountAll({ title: 'заголовок' });

            expect(result).toMatchSnapshot();
        });

        describe('Может фильтровать по tags', () => {
            it('По одному тегу', async () => {
                const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

                await factory.createStoriesWithTags(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

                const result = await storyService.findAndCountAll({ tags: ['тег 1'] });

                expect(result).toMatchSnapshot();
            });

            it('По нескольким тегам', async () => {
                const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

                await factory.createStoriesWithTags(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

                const result = await storyService.findAndCountAll({ tags: ['тег 1', 'тег 2'] });

                expect(result).toMatchSnapshot();
            });
        });

        describe('Может фильтровать по geo', () => {
            it('По одному geo', async () => {
                const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

                await factory.createStories(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

                const result = await storyService.findAndCountAll({ geo: [1] });

                expect(result).toMatchSnapshot();
            });

            it('По нескольким geo', async () => {
                const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

                await factory.createStories(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

                const result = await storyService.findAndCountAll({ geo: [1, 2] });

                expect(result).toMatchSnapshot();
            });
        });

        describe('Может фильтровать по published', () => {
            it('Возвращает только опубликованные сторис', async () => {
                const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

                await factory.createStories(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

                const result = await storyService.findAndCountAll({ published: true });

                expect(result).toMatchSnapshot();
            });

            it('Возвращает только неопубликованные сторис', async () => {
                const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3 } = getFixtures(fixtures);

                await factory.createStories(3, [STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_ATTRIBUTES_3]);

                const result = await storyService.findAndCountAll({ published: false });

                expect(result).toMatchSnapshot();
            });
        });
    });

    describe('create', () => {
        it('Создаёт и возвращает сторис', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);

            const result = await storyService.create(STORY_ATTRIBUTES);

            expect(result).toMatchSnapshot();
        });

        it('Создаёт и возвращает сторис со связанными страницами', async () => {
            const { STORY_ATTRIBUTES, STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2 } = getFixtures(fixtures);

            const result = await storyService.create({
                ...STORY_ATTRIBUTES,
                pages: [STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2],
            });

            expect(result).toMatchSnapshot();
        });

        it('Создаёт и возвращает сторис со связанными тегами', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);

            const result = await storyService.create(STORY_ATTRIBUTES);

            expect(result).toMatchSnapshot();
        });
    });

    describe('update', () => {
        it('Кидает ошибку, если сторис не найден по id', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);
            const NON_EXISTING_ID = 212;

            const result = storyService.update(NON_EXISTING_ID, STORY_ATTRIBUTES);

            await expect(result).rejects.toEqual(new NotFoundException('История не найдена'));
        });

        it('Кидает ошибку, если сторис не найден по uuid', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);
            const NOT_EXISTING_UUID = 'c0efa04e-2072-4aeb-bd90-064aaa9d5aa7';

            const result = storyService.update(NOT_EXISTING_UUID, STORY_ATTRIBUTES);

            await expect(result).rejects.toEqual(new NotFoundException('История не найдена'));
        });

        it('Обновляет и возвращает сторис по id', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES_1);

            const result = await storyService.update(story.id, STORY_ATTRIBUTES_2);

            expect(result).toMatchSnapshot();
        });

        it('Обновляет и возвращает сторис по uuid', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES_1);

            const result = await storyService.update(story.uuid as string, STORY_ATTRIBUTES_2);

            expect(result).toMatchSnapshot();
        });

        it('Обновляет и возвращает сторис со связанными страницами, перезаписывая страницы', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2, STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2 } =
                getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES_1);

            await factory.createStoryPages(3, [{ storyId: story.id }, { storyId: story.id }, { storyId: story.id }]);

            const result = await storyService.update(story.id, {
                ...STORY_ATTRIBUTES_2,
                pages: [STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2],
            });

            expect(result).toMatchSnapshot();
        });

        it('Обновляет и возвращает сторис со связанными тегами, сохраняя существующие теги', async () => {
            const { STORY_ATTRIBUTES_1, STORY_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES_1);

            const result = await storyService.update(story.id, STORY_ATTRIBUTES_2);

            expect(result).toMatchSnapshot();
        });
    });

    describe('delete', () => {
        it('Кидает ошибку, если сторис не найден по id', async () => {
            const NON_EXISTING_ID = 212;

            const result = storyService.delete(NON_EXISTING_ID);

            await expect(result).rejects.toEqual(new NotFoundException('История не найдена'));
        });

        it('Кидает ошибку, если сторис не найден по uuid', async () => {
            const NOT_EXISTING_UUID = 'c0efa04e-2072-4aeb-bd90-064aaa9d5aa7';

            const result = storyService.delete(NOT_EXISTING_UUID);

            await expect(result).rejects.toEqual(new NotFoundException('История не найдена'));
        });

        it('Удаляет сторис по id', async () => {
            const story = await factory.createStory();

            const deleteResult = storyService.delete(story.id);

            await expect(deleteResult).resolves.toBeUndefined();

            const deletedStory = await storyService.find(story.id);

            await expect(deletedStory).toBeUndefined();
        });

        it('Удаляет сторис по uuid', async () => {
            const story = await factory.createStory();

            const deleteResult = storyService.delete(story.uuid as string);

            await expect(deleteResult).resolves.toBeUndefined();

            const deletedStory = await storyService.find(story.id);

            await expect(deletedStory).toBeUndefined();
        });
    });

    describe('publish', () => {
        it('Кидает ошибку, если сторис не найден по id', async () => {
            const NON_EXISTING_ID = 212;

            const result = storyService.publish(NON_EXISTING_ID);

            await expect(result).rejects.toEqual(new NotFoundException('История не найдена'));
        });

        it('Кидает ошибку, если сторис не найден по uuid', async () => {
            const NOT_EXISTING_UUID = 'c0efa04e-2072-4aeb-bd90-064aaa9d5aa7';

            const result = storyService.publish(NOT_EXISTING_UUID);

            await expect(result).rejects.toEqual(new NotFoundException('История не найдена'));
        });

        it('Публикует сторис по id и возвращает его', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            const result = await storyService.publish(story.id);

            expect(result).toMatchSnapshot();
        });

        it('Публикует сторис по uuid и возвращает его', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            const result = await storyService.publish(story.uuid as string);

            expect(result).toMatchSnapshot();
        });
    });

    describe('unpublish', () => {
        it('Кидает ошибку, если сторис не найден по id', async () => {
            const NON_EXISTING_ID = 212;

            const result = storyService.unpublish(NON_EXISTING_ID);

            await expect(result).rejects.toEqual(new NotFoundException('История не найдена'));
        });

        it('Кидает ошибку, если сторис не найден по uuid', async () => {
            const NOT_EXISTING_UUID = 'c0efa04e-2072-4aeb-bd90-064aaa9d5aa7';

            const result = storyService.unpublish(NOT_EXISTING_UUID);

            await expect(result).rejects.toEqual(new NotFoundException('История не найдена'));
        });

        it('Снимает сторис с публикации по id и возвращает его', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            const result = await storyService.unpublish(story.id);

            expect(result.published).toBeFalse();
            expect(result.publishedAt).toBeNull();
            expect(result).toMatchSnapshot();
        });

        it('Снимает сторис с публикации по uuid и возвращает его', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            const result = await storyService.unpublish(story.uuid as string);

            expect(result.published).toBeFalse();
            expect(result.publishedAt).toBeNull();
            expect(result).toMatchSnapshot();
        });
    });
});
