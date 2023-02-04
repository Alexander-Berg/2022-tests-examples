import { Test, TestingModule } from '@nestjs/testing';
import { describe } from '@jest/globals';

import { getFixtures } from '../../../tests/get-fixtures';
import { Post as PostModel } from '../../../modules/post/post.model';
import { PostSchemaMarkup as PostSchemaMarkupModel } from '../../../modules/post-schema-markup/post-schema-markup.model';
import { BatchModule } from '../batch.module';
import { BatchPostRepository } from '../batch.post.repository';

import { fixtures } from './batch.post.repository.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Autoru post schema markup generator batch post repository', () => {
    let testingModule: TestingModule;
    let batchRepository: BatchPostRepository;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [BatchModule],
        }).compile();

        batchRepository = await testingModule.resolve(BatchPostRepository);

        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });

    describe('getForNewFaqPageMarkup', () => {
        it('Возвращает посты без микроразметки и с блоками, для которых нужно создать микроразметку', async () => {
            const {
                POST_ATTRIBUTES_1, // с флагом, с блоками, без разметки -> берем!
                POST_ATTRIBUTES_2, // с флагом, с блоками, с разметкой -> скипаем
                POST_ATTRIBUTES_3, // с флагом, без блоков, без разметки -> скипаем
                POST_ATTRIBUTES_4, // с флагом, без блоков, с разметкой -> скипаем
                POST_ATTRIBUTES_5, // без флага, с блоками, без разметки -> скипаем
                POST_ATTRIBUTES_6, // без флага, с блоками, с разметкой -> скипаем
                POST_ATTRIBUTES_7, // без флага, без блоков, с разметкой -> скипаем
                POST_ATTRIBUTES_8, // без флага, без блоков, без разметки -> скипаем
                POST_SCHEMA_MARKUP_ATTRIBUTES,
            } = getFixtures(fixtures);

            // eslint-disable-next-line @typescript-eslint/no-unused-vars,no-unused-vars
            const [POST_1, POST_2, POST_3, POST_4, POST_5, POST_6, POST_7, POST_8] = await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
                POST_ATTRIBUTES_8,
            ]).then(models => Promise.all(models.map(post => post?.reload())));

            await PostSchemaMarkupModel.bulkCreate([
                { postId: POST_2?.id, ...POST_SCHEMA_MARKUP_ATTRIBUTES },
                { postId: POST_4?.id, ...POST_SCHEMA_MARKUP_ATTRIBUTES },
                { postId: POST_6?.id, ...POST_SCHEMA_MARKUP_ATTRIBUTES },
                { postId: POST_7?.id, ...POST_SCHEMA_MARKUP_ATTRIBUTES },
            ]);

            const POSTS_FOR_NEW_MARKUPS = await batchRepository.getForNewFaqPageMarkup();

            expect(POSTS_FOR_NEW_MARKUPS).toHaveLength(1);
            expect(POSTS_FOR_NEW_MARKUPS[0]).toBeInstanceOf(PostModel);
            expect(POSTS_FOR_NEW_MARKUPS[0]?.id).toBeNumber();
            expect(POSTS_FOR_NEW_MARKUPS[0]?.id).toBe(POST_1?.id);
        });
    });

    describe('getForUpdatableFaqPageMarkup', () => {
        it('Возвращает посты c микроразметкой с датой редактирования < даты редактирования поста', async () => {
            const {
                POST_SHOULD_HAVE_MARKUP_ATTRIBUTES_1,
                POST_SHOULD_HAVE_MARKUP_ATTRIBUTES_2,
                POST_SHOULD_HAVE_MARKUP_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_SCHEMA_MARKUP_ATTRIBUTES_1,
                POST_SCHEMA_MARKUP_ATTRIBUTES_2,
                POST_SCHEMA_MARKUP_ATTRIBUTES_3,
            } = getFixtures(fixtures);

            const [POST_SHOULD_HAVE_MARKUP_1, POST_SHOULD_HAVE_MARKUP_2, POST_4] = await PostModel.bulkCreate([
                POST_SHOULD_HAVE_MARKUP_ATTRIBUTES_1,
                POST_SHOULD_HAVE_MARKUP_ATTRIBUTES_2,
                POST_ATTRIBUTES_4,
                POST_SHOULD_HAVE_MARKUP_ATTRIBUTES_3,
                POST_ATTRIBUTES_5,
            ]).then(models => Promise.all(models.map(post => post?.reload())));

            await PostSchemaMarkupModel.bulkCreate([
                {
                    postId: POST_SHOULD_HAVE_MARKUP_1?.id,
                    ...POST_SCHEMA_MARKUP_ATTRIBUTES_1,
                },
                {
                    postId: POST_4?.id,
                    ...POST_SCHEMA_MARKUP_ATTRIBUTES_2,
                },
            ]);

            const UPDATE_DATE = '2021-09-08T13:30:35.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            await PostSchemaMarkupModel.create({
                postId: POST_SHOULD_HAVE_MARKUP_2?.id,
                ...POST_SCHEMA_MARKUP_ATTRIBUTES_3,
            });

            const POSTS_FOR_UPDATABLE_MARKUPS = await batchRepository.getForUpdatableFaqPageMarkup();

            expect(POSTS_FOR_UPDATABLE_MARKUPS).toHaveLength(1);
            expect(POSTS_FOR_UPDATABLE_MARKUPS[0]).toBeInstanceOf(PostModel);
            expect(POSTS_FOR_UPDATABLE_MARKUPS[0]?.id).toBeNumber();
            expect(POSTS_FOR_UPDATABLE_MARKUPS[0]?.id).toBe(POST_SHOULD_HAVE_MARKUP_2?.id);
        });
    });
});
