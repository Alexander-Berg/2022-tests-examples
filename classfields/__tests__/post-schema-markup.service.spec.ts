import { Test, TestingModule } from '@nestjs/testing';
import { describe } from '@jest/globals';

import { getFixtures } from '../../../tests/get-fixtures';
import { ActionsLogService } from '../../actions-log/actions-log.service';
import { ActionLogAction, ActionLogEntity } from '../../../types/actions-log';
import { PostSchemaMarkupModule } from '../post-schema-markup.module';
import { PostSchemaMarkupService } from '../post-schema-markup.service';
import { Post as PostModel } from '../../post/post.model';
import { PostSchemaMarkup as PostSchemaMarkupModel } from '../post-schema-markup.model';

import { fixtures } from './post-schema-markup.service.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Post schema markup service', () => {
    let testingModule: TestingModule;
    let postSchemaMarkupService: PostSchemaMarkupService;
    let actionsLogService: ActionsLogService;
    let spyCreateLog: jest.SpyInstance;
    let spyCreateManyLogs: jest.SpyInstance;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [PostSchemaMarkupModule],
        }).compile();

        postSchemaMarkupService = await testingModule.resolve(PostSchemaMarkupService);
        actionsLogService = await testingModule.resolve(ActionsLogService);

        spyCreateLog = jest.spyOn(actionsLogService, 'create');
        spyCreateManyLogs = jest.spyOn(actionsLogService, 'createMany');

        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });

    describe('create', () => {
        it('Создает новую микроразметку', async () => {
            const { POST_ATTRIBUTES, POST_SCHEMA_MARKUP_ATTRIBUTES, USER_LOGIN } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const NEW_MARKUP = await postSchemaMarkupService.create(
                {
                    postId: POST.id,
                    ...POST_SCHEMA_MARKUP_ATTRIBUTES,
                },
                { userLogin: USER_LOGIN }
            );

            expect(NEW_MARKUP).toBeInstanceOf(PostSchemaMarkupModel);
            expect(NEW_MARKUP.toJSON()).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.postSchemaMarkup,
                action: ActionLogAction.create,
                urlPart: NEW_MARKUP.id,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
        });
    });

    describe('update', () => {
        it('Возвращает null, если микроразметка не найдена', async () => {
            const ID = 10;
            const USER_LOGIN = 'editor-1';

            const UPDATED_MARKUP = await postSchemaMarkupService.update(ID, {}, { userLogin: USER_LOGIN });

            expect(UPDATED_MARKUP).toBe(null);
            expect(spyCreateLog).not.toHaveBeenCalled();
        });

        it('Обновляет микроразметку и возвращает обновленную модель', async () => {
            const { POST_ATTRIBUTES, POST_SCHEMA_MARKUP_ATTRIBUTES, POST_SCHEMA_MARKUP_UPDATE_ATTRIBUTES, USER_LOGIN } =
                getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);
            const MARKUP = await PostSchemaMarkupModel.create({ postId: POST.id, ...POST_SCHEMA_MARKUP_ATTRIBUTES });

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_MARKUP = await postSchemaMarkupService.update(
                MARKUP.id,
                POST_SCHEMA_MARKUP_UPDATE_ATTRIBUTES,
                {
                    userLogin: USER_LOGIN,
                }
            );

            expect(UPDATED_MARKUP).toBeInstanceOf(PostSchemaMarkupModel);
            expect(UPDATED_MARKUP?.toJSON()).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.postSchemaMarkup,
                action: ActionLogAction.update,
                urlPart: MARKUP.id,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
        });
    });

    describe('deleteNonActualFaqPageMarkups', () => {
        it('Удаляет ненужную микроразметку постов и возвращает кол-во удаленных моделей', async () => {
            const {
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_SCHEMA_MARKUP_ATTRIBUTES,
                USER_LOGIN,
            } = getFixtures(fixtures);

            const [POST_1, POST_2, POST_WITHOUT_MARKUP_1, POST_WITHOUT_MARKUP_2] = await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
            ]).then(models => Promise.all(models.map(post => post?.reload())));

            const [MARKUP_1, MARKUP_2, DELETABLE_MARKUP_1, DELETABLE_MARKUP_2] = await PostSchemaMarkupModel.bulkCreate(
                [
                    { postId: POST_1?.id as number, ...POST_SCHEMA_MARKUP_ATTRIBUTES },
                    { postId: POST_2?.id as number, ...POST_SCHEMA_MARKUP_ATTRIBUTES },
                    { postId: POST_WITHOUT_MARKUP_1?.id as number, ...POST_SCHEMA_MARKUP_ATTRIBUTES },
                    { postId: POST_WITHOUT_MARKUP_2?.id as number, ...POST_SCHEMA_MARKUP_ATTRIBUTES },
                ]
            );

            const DELETED_COUNT = await postSchemaMarkupService.deleteNonActualFaqPageMarkups({
                userLogin: USER_LOGIN,
            });

            expect(DELETED_COUNT).toBe(2);

            const ACTUAL_MARKUP_IDS = await PostSchemaMarkupModel.findAll({
                attributes: ['id'],
                raw: true,
            }).then(models => models.map(({ id }) => id));

            expect(ACTUAL_MARKUP_IDS).toEqual([MARKUP_1?.id, MARKUP_2?.id]);

            expect(spyCreateManyLogs).toHaveBeenCalledTimes(1);
            expect(spyCreateManyLogs).toHaveBeenCalledWith([
                {
                    author: USER_LOGIN,
                    entity: ActionLogEntity.postSchemaMarkup,
                    action: ActionLogAction.delete,
                    urlPart: DELETABLE_MARKUP_1?.id,
                },
                {
                    author: USER_LOGIN,
                    entity: ActionLogEntity.postSchemaMarkup,
                    action: ActionLogAction.delete,
                    urlPart: DELETABLE_MARKUP_2?.id,
                },
            ]);
        });
    });

    describe('delete', () => {
        it('Возвращает false, если микроразметка не найдена', async () => {
            const USER_LOGIN = 'editor-1';
            const result = await postSchemaMarkupService.delete(1, { userLogin: USER_LOGIN });

            expect(result).toEqual(false);
            expect(spyCreateLog).not.toHaveBeenCalled();
        });

        it('Удаляет микроразметку и возвращает true', async () => {
            const { POST_ATTRIBUTES, POST_SCHEMA_MARKUP_ATTRIBUTES, USER_LOGIN } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES).then(model => model.reload());
            const MARKUP = await PostSchemaMarkupModel.create({
                postId: POST.id,
                ...POST_SCHEMA_MARKUP_ATTRIBUTES,
            }).then(model => model.reload());

            const IS_DELETED = await postSchemaMarkupService.delete(MARKUP.id, { userLogin: USER_LOGIN });

            await expect(PostSchemaMarkupModel.findByPk(MARKUP.id)).resolves.toBe(null);
            expect(IS_DELETED).toBe(true);
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.postSchemaMarkup,
                action: ActionLogAction.delete,
                urlPart: MARKUP.id,
            });
        });
    });
});
