import { describe } from '@jest/globals';
import { QueryFailedError } from 'typeorm';

import { createTestingApp, ITestingApplication } from '../../../tests/app';
import { FactoryService } from '../../../tests/factory/factory.service';
import { getFixtures } from '../../../tests/get-fixtures';
import { StoryService } from '../../story/story.service';
import { StoryPageService } from '../story-page.service';

import { fixtures } from './story-page.service.fixtures';

describe('Story page service', () => {
    let testingApp: ITestingApplication;
    let factory: FactoryService;
    let storyService: StoryService;
    let storyPageService: StoryPageService;

    beforeEach(async () => {
        testingApp = await createTestingApp();

        factory = testingApp.factory;
        storyService = await testingApp.module.resolve(StoryService);
        storyPageService = await testingApp.module.resolve(StoryPageService);
    });

    afterEach(async () => {
        await testingApp.close();
    });

    describe('createMany', () => {
        it('Возвращает пустой массив, если данные страниц не переданы', async () => {
            const result = await storyPageService.createMany(0, []);

            expect(result).toBeEmpty();
        });

        it('Кидает SQL-ошибку, если история не найдена', async () => {
            const { STORY_PAGE_ATTRIBUTES } = getFixtures(fixtures);

            const result = storyPageService.createMany(0, [STORY_PAGE_ATTRIBUTES]);

            await expect(result).rejects.toThrow(QueryFailedError);
        });

        it('Создает страницы для существующей истории и возвращает их', async () => {
            const { STORY_PAGE_ATTRIBUTES_1, STORY_PAGE_ATTRIBUTES_2 } = getFixtures(fixtures);

            const story = await factory.createStory();

            const result = await storyPageService.createMany(story.id, [
                STORY_PAGE_ATTRIBUTES_1,
                STORY_PAGE_ATTRIBUTES_2,
            ]);

            expect(result).toMatchSnapshot();
        });
    });

    describe('removeByStoryId', () => {
        it('Удаляет страницы по id истории', async () => {
            const { STORY_ATTRIBUTES } = getFixtures(fixtures);

            const story = await factory.createStory(STORY_ATTRIBUTES);

            await factory.createStoryPages(2, [{ storyId: story.id }, { storyId: story.id }]);

            const createdStory = await storyService.find(story.id);

            expect(createdStory?.pages).toBeArrayOfSize(2);

            await storyPageService.removeByStoryId(story.id);

            const updatedStory = await storyService.find(story.id);

            expect(updatedStory?.pages).toBeEmpty();
        });
    });
});
