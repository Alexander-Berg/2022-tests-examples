import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import 'jest-extended';
import { omit } from 'lodash';

import { FactoryService } from '../../../services/factory.service';
import { Tag } from '../tag.model';
import { createTestingApp } from '../../../tests/app';

import { TAG_DATA_1, CREATE_TAG_DATA_1, UPDATE_TAG_DATA_1 } from './fixtures';

describe('Internal tags controller', () => {
    let app: INestApplication;
    let factory: FactoryService;
    let server;

    beforeEach(async () => {
        const testingApp = await createTestingApp();

        app = testingApp.app;
        factory = testingApp.factory;
        await app.init();

        server = app.getHttpServer();
    });

    afterEach(async () => {
        await app.close();
    });

    describe('GET /internal/tags', function () {
        const DEFAULT_LIMIT = 10;

        it('Возвращает 10 неархивных тегов, отсортированных по убыванию даты создания', async () => {
            await factory.createManyTags('Tag', 3, { isArchived: true });
            const tags = await factory
                .createManyTags('Tag', 12, [
                    { isArchived: false, createdAt: new Date('2021-01-20T20:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T19:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T18:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T17:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T16:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T15:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T14:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T13:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T12:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T11:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T10:10:30.000Z') },
                    { isArchived: false, createdAt: new Date('2021-01-20T09:10:30.000Z') },
                ])
                .then(models =>
                    models.map(model => ({
                        urlPart: model.urlPart,
                        service: model.service,
                        title: model.title,
                        shortTitle: model.shortTitle,
                        blocks: model.blocks,
                        draftTitle: model.draftTitle,
                        draftShortTitle: model.draftShortTitle,
                        draftBlocks: model.draftBlocks,
                        isArchived: model.isArchived,
                        isHot: model.isHot,
                        isPartnership: model.isPartnership,
                        partnershipLink: model.partnershipLink,
                        partnershipName: model.partnershipName,
                        partnershipBadgeName: model.partnershipBadgeName,
                        metaTitle: model.metaTitle,
                        metaDescription: model.metaDescription,
                        mmm: model.mmm,
                        createdAt: new Date(model.createdAt).toISOString(),
                        lastEditedAt: new Date(model.lastEditedAt).toISOString(),
                    }))
                );

            const response = await request(server).get('/internal/tags');

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: tags.slice(0, DEFAULT_LIMIT),
                total: 12,
            });
        });

        it('Может фильтровать теги по заголовку и подзаголовку', async () => {
            const tag1 = await factory.createTag('Tag', {
                isArchived: false,
                title: '',
                shortTitle: '',
                draftTitle: 'Элитная недвижимость',
                draftShortTitle: '',
                createdAt: new Date('2021-01-20T20:10:30.000Z'),
            });
            const tag2 = await factory.createTag('Tag', {
                isArchived: false,
                title: '',
                shortTitle: '',
                draftTitle: '',
                draftShortTitle: 'Коммерческая недвижимость',
                createdAt: new Date('2021-01-20T19:10:30.000Z'),
            });
            const tag3 = await factory.createTag('Tag', {
                isArchived: false,
                title: '',
                shortTitle: '',
                draftTitle: 'Загородная недвижимость',
                draftShortTitle: 'Загородная недвижимость',
                createdAt: new Date('2021-01-20T18:10:30.000Z'),
            });
            const tag4 = await factory.createTag('Tag', {
                isArchived: false,
                title: 'Недвижимость',
                shortTitle: '',
                draftTitle: '',
                draftShortTitle: '',
                createdAt: new Date('2021-01-20T17:10:30.000Z'),
            });
            const tag5 = await factory.createTag('Tag', {
                isArchived: false,
                title: 'Зарубежная недвижимость',
                shortTitle: '',
                draftTitle: '',
                draftShortTitle: '',
                createdAt: new Date('2021-01-20T16:10:30.000Z'),
            });

            await factory.createTag('Tag', {
                isArchived: false,
                draftTitle: 'Аренда',
                draftShortTitle: 'Аренда',
            });

            const response = await request(server).get('/internal/tags').query({ name: 'недвижимость', pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        urlPart: tag1.urlPart,
                        service: tag1.service,
                        title: tag1.title,
                        shortTitle: tag1.shortTitle,
                        blocks: tag1.blocks,
                        draftTitle: tag1.draftTitle,
                        draftShortTitle: tag1.draftShortTitle,
                        draftBlocks: tag1.draftBlocks,
                        isArchived: tag1.isArchived,
                        isHot: tag1.isHot,
                        isPartnership: tag1.isPartnership,
                        partnershipLink: tag1.partnershipLink,
                        partnershipName: tag1.partnershipName,
                        partnershipBadgeName: tag1.partnershipBadgeName,
                        metaTitle: tag1.metaTitle,
                        metaDescription: tag1.metaDescription,
                        mmm: tag1.mmm,
                        createdAt: new Date(tag1.createdAt).toISOString(),
                        lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag2.urlPart,
                        service: tag2.service,
                        title: tag2.title,
                        shortTitle: tag2.shortTitle,
                        blocks: tag2.blocks,
                        draftTitle: tag2.draftTitle,
                        draftShortTitle: tag2.draftShortTitle,
                        draftBlocks: tag2.draftBlocks,
                        isArchived: tag2.isArchived,
                        isHot: tag2.isHot,
                        isPartnership: tag2.isPartnership,
                        partnershipLink: tag2.partnershipLink,
                        partnershipName: tag2.partnershipName,
                        partnershipBadgeName: tag2.partnershipBadgeName,
                        metaTitle: tag2.metaTitle,
                        metaDescription: tag2.metaDescription,
                        mmm: tag2.mmm,
                        createdAt: new Date(tag2.createdAt).toISOString(),
                        lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag3.urlPart,
                        service: tag3.service,
                        title: tag3.title,
                        shortTitle: tag3.shortTitle,
                        blocks: tag3.blocks,
                        draftTitle: tag3.draftTitle,
                        draftShortTitle: tag3.draftShortTitle,
                        draftBlocks: tag3.draftBlocks,
                        isArchived: tag3.isArchived,
                        isHot: tag3.isHot,
                        isPartnership: tag3.isPartnership,
                        partnershipLink: tag3.partnershipLink,
                        partnershipName: tag3.partnershipName,
                        partnershipBadgeName: tag3.partnershipBadgeName,
                        metaTitle: tag3.metaTitle,
                        metaDescription: tag3.metaDescription,
                        mmm: tag3.mmm,
                        createdAt: new Date(tag3.createdAt).toISOString(),
                        lastEditedAt: new Date(tag3.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag4.urlPart,
                        service: tag4.service,
                        title: tag4.title,
                        shortTitle: tag4.shortTitle,
                        blocks: tag4.blocks,
                        draftTitle: tag4.draftTitle,
                        draftShortTitle: tag4.draftShortTitle,
                        draftBlocks: tag4.draftBlocks,
                        isArchived: tag4.isArchived,
                        isHot: tag4.isHot,
                        isPartnership: tag4.isPartnership,
                        partnershipLink: tag4.partnershipLink,
                        partnershipName: tag4.partnershipName,
                        partnershipBadgeName: tag4.partnershipBadgeName,
                        metaTitle: tag4.metaTitle,
                        metaDescription: tag4.metaDescription,
                        mmm: tag4.mmm,
                        createdAt: new Date(tag4.createdAt).toISOString(),
                        lastEditedAt: new Date(tag4.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag5.urlPart,
                        service: tag5.service,
                        title: tag5.title,
                        shortTitle: tag5.shortTitle,
                        blocks: tag5.blocks,
                        draftTitle: tag5.draftTitle,
                        draftShortTitle: tag5.draftShortTitle,
                        draftBlocks: tag5.draftBlocks,
                        isArchived: tag5.isArchived,
                        isHot: tag5.isHot,
                        isPartnership: tag5.isPartnership,
                        partnershipLink: tag5.partnershipLink,
                        partnershipName: tag5.partnershipName,
                        partnershipBadgeName: tag5.partnershipBadgeName,
                        metaTitle: tag5.metaTitle,
                        metaDescription: tag5.metaDescription,
                        mmm: tag5.mmm,
                        createdAt: new Date(tag5.createdAt).toISOString(),
                        lastEditedAt: new Date(tag5.lastEditedAt).toISOString(),
                    },
                ],
                total: 5,
            });
        });

        it('Может фильтровать теги точно по заголовку (title)', async () => {
            await factory.createTag('Tag', {
                isArchived: false,
                title: '',
                shortTitle: '',
                draftTitle: 'Элитная недвижимость',
                draftShortTitle: '',
                createdAt: '2021-01-20T20:10:30.000Z',
            });
            await factory.createTag('Tag', {
                isArchived: false,
                title: '',
                shortTitle: '',
                draftTitle: '',
                draftShortTitle: 'Коммерческая Недвижимость',
                createdAt: '2021-01-20T19:10:30.000Z',
            });

            const tag = await factory.createTag('Tag', {
                isArchived: false,
                title: 'Недвижимость',
                shortTitle: '',
                draftTitle: '',
                draftShortTitle: '',
                createdAt: '2021-01-20T17:10:30.000Z',
            });

            await factory.createTag('Tag', {
                isArchived: false,
                title: '',
                shortTitle: '',
                draftTitle: 'Загородная недвижимость',
                draftShortTitle: 'Загородная Недвижимость',
                createdAt: '2021-01-20T18:10:30.000Z',
            });
            await factory.createTag('Tag', {
                isArchived: false,
                title: 'Зарубежная Недвижимость',
                shortTitle: '',
                draftTitle: '',
                draftShortTitle: '',
                createdAt: '2021-01-20T16:10:30.000Z',
            });
            await factory.createTag('Tag', {
                isArchived: false,
                draftTitle: 'Аренда',
                draftShortTitle: 'Аренда',
            });

            const response = await request(server).get('/internal/tags').query({ title: 'Недвижимость', pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        urlPart: tag.urlPart,
                        service: tag.service,
                        title: tag.title,
                        shortTitle: tag.shortTitle,
                        blocks: tag.blocks,
                        draftTitle: tag.draftTitle,
                        draftShortTitle: tag.draftShortTitle,
                        draftBlocks: tag.draftBlocks,
                        isArchived: tag.isArchived,
                        isHot: tag.isHot,
                        isPartnership: tag.isPartnership,
                        partnershipLink: tag.partnershipLink,
                        partnershipName: tag.partnershipName,
                        partnershipBadgeName: tag.partnershipBadgeName,
                        metaTitle: tag.metaTitle,
                        metaDescription: tag.metaDescription,
                        mmm: tag.mmm,
                        createdAt: new Date(tag.createdAt).toISOString(),
                        lastEditedAt: new Date(tag.lastEditedAt).toISOString(),
                    },
                ],
                total: 1,
            });
        });

        it('Может возвращать архивные и неархивные теги', async () => {
            const tag1 = await factory.createTag('Tag', {
                isArchived: true,
                createdAt: new Date('2021-01-20T20:10:30.000Z'),
            });
            const tag2 = await factory.createTag('Tag', {
                isArchived: false,
                createdAt: new Date('2021-01-20T19:10:30.000Z'),
            });
            const tag3 = await factory.createTag('Tag', {
                isArchived: false,
                createdAt: new Date('2021-01-20T18:10:30.000Z'),
            });

            const response = await request(server).get('/internal/tags').query({ withArchived: true });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        urlPart: tag1.urlPart,
                        service: tag1.service,
                        title: tag1.title,
                        shortTitle: tag1.shortTitle,
                        blocks: tag1.blocks,
                        draftTitle: tag1.draftTitle,
                        draftShortTitle: tag1.draftShortTitle,
                        draftBlocks: tag1.draftBlocks,
                        isArchived: tag1.isArchived,
                        isHot: tag1.isHot,
                        isPartnership: tag1.isPartnership,
                        partnershipLink: tag1.partnershipLink,
                        partnershipName: tag1.partnershipName,
                        partnershipBadgeName: tag1.partnershipBadgeName,
                        metaTitle: tag1.metaTitle,
                        metaDescription: tag1.metaDescription,
                        mmm: tag1.mmm,
                        createdAt: new Date(tag1.createdAt).toISOString(),
                        lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag2.urlPart,
                        service: tag2.service,
                        title: tag2.title,
                        shortTitle: tag2.shortTitle,
                        blocks: tag2.blocks,
                        draftTitle: tag2.draftTitle,
                        draftShortTitle: tag2.draftShortTitle,
                        draftBlocks: tag2.draftBlocks,
                        isArchived: tag2.isArchived,
                        isHot: tag2.isHot,
                        isPartnership: tag2.isPartnership,
                        partnershipLink: tag2.partnershipLink,
                        partnershipName: tag2.partnershipName,
                        partnershipBadgeName: tag2.partnershipBadgeName,
                        metaTitle: tag2.metaTitle,
                        metaDescription: tag2.metaDescription,
                        mmm: tag2.mmm,
                        createdAt: new Date(tag2.createdAt).toISOString(),
                        lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag3.urlPart,
                        service: tag3.service,
                        title: tag3.title,
                        shortTitle: tag3.shortTitle,
                        blocks: tag3.blocks,
                        draftTitle: tag3.draftTitle,
                        draftShortTitle: tag3.draftShortTitle,
                        draftBlocks: tag3.draftBlocks,
                        isArchived: tag3.isArchived,
                        isHot: tag3.isHot,
                        isPartnership: tag3.isPartnership,
                        partnershipLink: tag3.partnershipLink,
                        partnershipName: tag3.partnershipName,
                        partnershipBadgeName: tag3.partnershipBadgeName,
                        metaTitle: tag3.metaTitle,
                        metaDescription: tag3.metaDescription,
                        mmm: tag3.mmm,
                        createdAt: new Date(tag3.createdAt).toISOString(),
                        lastEditedAt: new Date(tag3.lastEditedAt).toISOString(),
                    },
                ],
                total: 3,
            });
        });

        it('Может фильтровать по МММ (марка, модель, модификация)', async () => {
            const tag1 = await factory.createTag('Tag', {
                mmm: 'TOYOTA:COROLLA:2020-ABC',
                createdAt: new Date('2021-01-20T20:10:30.000Z'),
            });
            const tag2 = await factory.createTag('Tag', {
                mmm: 'TOYOTA:COROLLA',
                createdAt: new Date('2021-01-20T19:10:30.000Z'),
            });

            await factory.createTag('Tag', { mmm: 'BMW' });
            await factory.createTag('Tag', { mmm: 'TOYOTA' });

            const response = await request(server).get('/internal/tags').query({ mmm: 'TOYOTA:COROLLA', pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        urlPart: tag1.urlPart,
                        service: tag1.service,
                        title: tag1.title,
                        shortTitle: tag1.shortTitle,
                        blocks: tag1.blocks,
                        draftTitle: tag1.draftTitle,
                        draftShortTitle: tag1.draftShortTitle,
                        draftBlocks: tag1.draftBlocks,
                        isArchived: tag1.isArchived,
                        isHot: tag1.isHot,
                        isPartnership: tag1.isPartnership,
                        partnershipLink: tag1.partnershipLink,
                        partnershipName: tag1.partnershipName,
                        partnershipBadgeName: tag1.partnershipBadgeName,
                        metaTitle: tag1.metaTitle,
                        metaDescription: tag1.metaDescription,
                        mmm: tag1.mmm,
                        createdAt: new Date(tag1.createdAt).toISOString(),
                        lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag2.urlPart,
                        service: tag2.service,
                        title: tag2.title,
                        shortTitle: tag2.shortTitle,
                        blocks: tag2.blocks,
                        draftTitle: tag2.draftTitle,
                        draftShortTitle: tag2.draftShortTitle,
                        draftBlocks: tag2.draftBlocks,
                        isArchived: tag2.isArchived,
                        isHot: tag2.isHot,
                        isPartnership: tag2.isPartnership,
                        partnershipLink: tag2.partnershipLink,
                        partnershipName: tag2.partnershipName,
                        partnershipBadgeName: tag2.partnershipBadgeName,
                        metaTitle: tag2.metaTitle,
                        metaDescription: tag2.metaDescription,
                        mmm: tag2.mmm,
                        createdAt: new Date(tag2.createdAt).toISOString(),
                        lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                    },
                ],
                total: 2,
            });
        });

        it('Может фильтровать по МММ с полным соответствием (марка, модель, модификация)', async () => {
            const tag1 = await factory.createTag('Tag', {
                mmm: 'TOYOTA:COROLLA',
                createdAt: '2021-01-20T19:10:30.000Z',
            });

            await factory.createTag('Tag', { mmm: 'TOYOTA' });
            await factory.createTag('Tag', { mmm: 'TOYOTA:COROLLA:2020-ABC' });

            const response = await request(server).get('/internal/tags').query({
                mmm: 'TOYOTA:COROLLA',
                mmmExact: true,
                pageSize: 10,
            });

            expect(response.body).toEqual({
                data: [
                    {
                        urlPart: tag1.urlPart,
                        service: tag1.service,
                        title: tag1.title,
                        shortTitle: tag1.shortTitle,
                        blocks: tag1.blocks,
                        draftTitle: tag1.draftTitle,
                        draftShortTitle: tag1.draftShortTitle,
                        draftBlocks: tag1.draftBlocks,
                        isArchived: tag1.isArchived,
                        isHot: tag1.isHot,
                        isPartnership: tag1.isPartnership,
                        partnershipLink: tag1.partnershipLink,
                        partnershipName: tag1.partnershipName,
                        partnershipBadgeName: tag1.partnershipBadgeName,
                        metaTitle: tag1.metaTitle,
                        metaDescription: tag1.metaDescription,
                        mmm: tag1.mmm,
                        createdAt: new Date(tag1.createdAt).toISOString(),
                        lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                    },
                ],
                total: 1,
            });
            expect(response.statusCode).toBe(200);
        });

        it('Может отдавать теги с указанным лимитом', async () => {
            const tag1 = await factory.createTag('Tag', {
                isArchived: false,
                createdAt: new Date('2021-01-20T20:10:30.000Z'),
            });
            const tag2 = await factory.createTag('Tag', {
                isArchived: false,
                createdAt: new Date('2021-01-20T19:10:30.000Z'),
            });
            const tag3 = await factory.createTag('Tag', {
                isArchived: false,
                createdAt: new Date('2021-01-20T18:10:30.000Z'),
            });

            await factory.createTag('Tag', { isArchived: false, createdAt: new Date('2021-01-20T17:10:30.000Z') });

            const response = await request(server).get('/internal/tags').query({ pageSize: 3 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        urlPart: tag1.urlPart,
                        service: tag1.service,
                        title: tag1.title,
                        shortTitle: tag1.shortTitle,
                        blocks: tag1.blocks,
                        draftTitle: tag1.draftTitle,
                        draftShortTitle: tag1.draftShortTitle,
                        draftBlocks: tag1.draftBlocks,
                        isArchived: tag1.isArchived,
                        isHot: tag1.isHot,
                        isPartnership: tag1.isPartnership,
                        partnershipLink: tag1.partnershipLink,
                        partnershipName: tag1.partnershipName,
                        partnershipBadgeName: tag1.partnershipBadgeName,
                        metaTitle: tag1.metaTitle,
                        metaDescription: tag1.metaDescription,
                        mmm: tag1.mmm,
                        createdAt: new Date(tag1.createdAt).toISOString(),
                        lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag2.urlPart,
                        service: tag2.service,
                        title: tag2.title,
                        shortTitle: tag2.shortTitle,
                        blocks: tag2.blocks,
                        draftTitle: tag2.draftTitle,
                        draftShortTitle: tag2.draftShortTitle,
                        draftBlocks: tag2.draftBlocks,
                        isArchived: tag2.isArchived,
                        isHot: tag2.isHot,
                        isPartnership: tag2.isPartnership,
                        partnershipLink: tag2.partnershipLink,
                        partnershipName: tag2.partnershipName,
                        partnershipBadgeName: tag2.partnershipBadgeName,
                        metaTitle: tag2.metaTitle,
                        metaDescription: tag2.metaDescription,
                        mmm: tag2.mmm,
                        createdAt: new Date(tag2.createdAt).toISOString(),
                        lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag3.urlPart,
                        service: tag3.service,
                        title: tag3.title,
                        shortTitle: tag3.shortTitle,
                        blocks: tag3.blocks,
                        draftTitle: tag3.draftTitle,
                        draftShortTitle: tag3.draftShortTitle,
                        draftBlocks: tag3.draftBlocks,
                        isArchived: tag3.isArchived,
                        isHot: tag3.isHot,
                        isPartnership: tag3.isPartnership,
                        partnershipLink: tag3.partnershipLink,
                        partnershipName: tag3.partnershipName,
                        partnershipBadgeName: tag3.partnershipBadgeName,
                        metaTitle: tag3.metaTitle,
                        metaDescription: tag3.metaDescription,
                        mmm: tag3.mmm,
                        createdAt: new Date(tag3.createdAt).toISOString(),
                        lastEditedAt: new Date(tag3.lastEditedAt).toISOString(),
                    },
                ],
                total: 4,
            });
        });

        it('Может отдавать теги с указанными: лимитом и страницей', async () => {
            await factory.createTag('Tag', { isArchived: false, createdAt: new Date('2021-01-20T20:10:30.000Z') });
            await factory.createTag('Tag', { isArchived: false, createdAt: new Date('2021-01-20T19:10:30.000Z') });
            await factory.createTag('Tag', { isArchived: false, createdAt: new Date('2021-01-20T18:10:30.000Z') });
            const tag4 = await factory.createTag('Tag', {
                isArchived: false,
                createdAt: new Date('2021-01-20T17:10:30.000Z'),
            });

            const response = await request(server).get('/internal/tags').query({ pageSize: 3, pageNumber: 1 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        urlPart: tag4.urlPart,
                        service: tag4.service,
                        title: tag4.title,
                        shortTitle: tag4.shortTitle,
                        blocks: tag4.blocks,
                        draftTitle: tag4.draftTitle,
                        draftShortTitle: tag4.draftShortTitle,
                        draftBlocks: tag4.draftBlocks,
                        isArchived: tag4.isArchived,
                        isHot: tag4.isHot,
                        isPartnership: tag4.isPartnership,
                        partnershipLink: tag4.partnershipLink,
                        partnershipName: tag4.partnershipName,
                        partnershipBadgeName: tag4.partnershipBadgeName,
                        metaTitle: tag4.metaTitle,
                        metaDescription: tag4.metaDescription,
                        mmm: tag4.mmm,
                        createdAt: new Date(tag4.createdAt).toISOString(),
                        lastEditedAt: new Date(tag4.lastEditedAt).toISOString(),
                    },
                ],
                total: 4,
            });
        });

        it('Может сортировать теги по указанному полю', async () => {
            const tag1 = await factory.createTag('Tag', { isArchived: false, title: 'Вторичное жилье' });
            const tag2 = await factory.createTag('Tag', { isArchived: false, title: 'Новостройки' });
            const tag3 = await factory.createTag('Tag', { isArchived: false, title: 'Аренда' });
            const tag4 = await factory.createTag('Tag', { isArchived: false, title: 'Офисы' });
            const tag5 = await factory.createTag('Tag', { isArchived: false, title: 'Застройщики' });
            const tag6 = await factory.createTag('Tag', { isArchived: false, title: 'Ипотека' });

            const response = await request(server).get('/internal/tags').query({ orderBySort: 'title' });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        urlPart: tag4.urlPart,
                        service: tag4.service,
                        title: tag4.title,
                        shortTitle: tag4.shortTitle,
                        blocks: tag4.blocks,
                        draftTitle: tag4.draftTitle,
                        draftShortTitle: tag4.draftShortTitle,
                        draftBlocks: tag4.draftBlocks,
                        isArchived: tag4.isArchived,
                        isHot: tag4.isHot,
                        isPartnership: tag4.isPartnership,
                        partnershipLink: tag4.partnershipLink,
                        partnershipName: tag4.partnershipName,
                        partnershipBadgeName: tag4.partnershipBadgeName,
                        metaTitle: tag4.metaTitle,
                        metaDescription: tag4.metaDescription,
                        mmm: tag4.mmm,
                        createdAt: new Date(tag4.createdAt).toISOString(),
                        lastEditedAt: new Date(tag4.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag2.urlPart,
                        service: tag2.service,
                        title: tag2.title,
                        shortTitle: tag2.shortTitle,
                        blocks: tag2.blocks,
                        draftTitle: tag2.draftTitle,
                        draftShortTitle: tag2.draftShortTitle,
                        draftBlocks: tag2.draftBlocks,
                        isArchived: tag2.isArchived,
                        isHot: tag2.isHot,
                        isPartnership: tag2.isPartnership,
                        partnershipLink: tag2.partnershipLink,
                        partnershipName: tag2.partnershipName,
                        partnershipBadgeName: tag2.partnershipBadgeName,
                        metaTitle: tag2.metaTitle,
                        metaDescription: tag2.metaDescription,
                        mmm: tag2.mmm,
                        createdAt: new Date(tag2.createdAt).toISOString(),
                        lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag6.urlPart,
                        service: tag6.service,
                        title: tag6.title,
                        shortTitle: tag6.shortTitle,
                        blocks: tag6.blocks,
                        draftTitle: tag6.draftTitle,
                        draftShortTitle: tag6.draftShortTitle,
                        draftBlocks: tag6.draftBlocks,
                        isArchived: tag6.isArchived,
                        isHot: tag6.isHot,
                        isPartnership: tag6.isPartnership,
                        partnershipLink: tag6.partnershipLink,
                        partnershipName: tag6.partnershipName,
                        partnershipBadgeName: tag6.partnershipBadgeName,
                        metaTitle: tag6.metaTitle,
                        metaDescription: tag6.metaDescription,
                        mmm: tag6.mmm,
                        createdAt: new Date(tag6.createdAt).toISOString(),
                        lastEditedAt: new Date(tag6.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag5.urlPart,
                        service: tag5.service,
                        title: tag5.title,
                        shortTitle: tag5.shortTitle,
                        blocks: tag5.blocks,
                        draftTitle: tag5.draftTitle,
                        draftShortTitle: tag5.draftShortTitle,
                        draftBlocks: tag5.draftBlocks,
                        isArchived: tag5.isArchived,
                        isHot: tag5.isHot,
                        isPartnership: tag5.isPartnership,
                        partnershipLink: tag5.partnershipLink,
                        partnershipName: tag5.partnershipName,
                        partnershipBadgeName: tag5.partnershipBadgeName,
                        metaTitle: tag5.metaTitle,
                        metaDescription: tag5.metaDescription,
                        mmm: tag5.mmm,
                        createdAt: new Date(tag5.createdAt).toISOString(),
                        lastEditedAt: new Date(tag5.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag1.urlPart,
                        service: tag1.service,
                        title: tag1.title,
                        shortTitle: tag1.shortTitle,
                        blocks: tag1.blocks,
                        draftTitle: tag1.draftTitle,
                        draftShortTitle: tag1.draftShortTitle,
                        draftBlocks: tag1.draftBlocks,
                        isArchived: tag1.isArchived,
                        isHot: tag1.isHot,
                        isPartnership: tag1.isPartnership,
                        partnershipLink: tag1.partnershipLink,
                        partnershipName: tag1.partnershipName,
                        partnershipBadgeName: tag1.partnershipBadgeName,
                        metaTitle: tag1.metaTitle,
                        metaDescription: tag1.metaDescription,
                        mmm: tag1.mmm,
                        createdAt: new Date(tag1.createdAt).toISOString(),
                        lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag3.urlPart,
                        service: tag3.service,
                        title: tag3.title,
                        shortTitle: tag3.shortTitle,
                        blocks: tag3.blocks,
                        draftTitle: tag3.draftTitle,
                        draftShortTitle: tag3.draftShortTitle,
                        draftBlocks: tag3.draftBlocks,
                        isArchived: tag3.isArchived,
                        isHot: tag3.isHot,
                        isPartnership: tag3.isPartnership,
                        partnershipLink: tag3.partnershipLink,
                        partnershipName: tag3.partnershipName,
                        partnershipBadgeName: tag3.partnershipBadgeName,
                        metaTitle: tag3.metaTitle,
                        metaDescription: tag3.metaDescription,
                        mmm: tag3.mmm,
                        createdAt: new Date(tag3.createdAt).toISOString(),
                        lastEditedAt: new Date(tag3.lastEditedAt).toISOString(),
                    },
                ],
                total: 6,
            });
        });

        it('Может сортировать теги в указанном направлении', async () => {
            const tag1 = await factory.createTag('Tag', { isArchived: false, title: 'Вторичное жилье' });
            const tag2 = await factory.createTag('Tag', { isArchived: false, title: 'Новостройки' });
            const tag3 = await factory.createTag('Tag', { isArchived: false, title: 'Аренда' });
            const tag4 = await factory.createTag('Tag', { isArchived: false, title: 'Офисы' });
            const tag5 = await factory.createTag('Tag', { isArchived: false, title: 'Застройщики' });
            const tag6 = await factory.createTag('Tag', { isArchived: false, title: 'Ипотека' });

            const response = await request(server).get('/internal/tags').query({
                orderBySort: 'title',
                orderByAsc: true,
            });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [
                    {
                        urlPart: tag3.urlPart,
                        service: tag3.service,
                        title: tag3.title,
                        shortTitle: tag3.shortTitle,
                        blocks: tag3.blocks,
                        draftTitle: tag3.draftTitle,
                        draftShortTitle: tag3.draftShortTitle,
                        draftBlocks: tag3.draftBlocks,
                        isArchived: tag3.isArchived,
                        isHot: tag3.isHot,
                        isPartnership: tag3.isPartnership,
                        partnershipLink: tag3.partnershipLink,
                        partnershipName: tag3.partnershipName,
                        partnershipBadgeName: tag3.partnershipBadgeName,
                        metaTitle: tag3.metaTitle,
                        metaDescription: tag3.metaDescription,
                        mmm: tag3.mmm,
                        createdAt: new Date(tag3.createdAt).toISOString(),
                        lastEditedAt: new Date(tag3.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag1.urlPart,
                        service: tag1.service,
                        title: tag1.title,
                        shortTitle: tag1.shortTitle,
                        blocks: tag1.blocks,
                        draftTitle: tag1.draftTitle,
                        draftShortTitle: tag1.draftShortTitle,
                        draftBlocks: tag1.draftBlocks,
                        isArchived: tag1.isArchived,
                        isHot: tag1.isHot,
                        isPartnership: tag1.isPartnership,
                        partnershipLink: tag1.partnershipLink,
                        partnershipName: tag1.partnershipName,
                        partnershipBadgeName: tag1.partnershipBadgeName,
                        metaTitle: tag1.metaTitle,
                        metaDescription: tag1.metaDescription,
                        mmm: tag1.mmm,
                        createdAt: new Date(tag1.createdAt).toISOString(),
                        lastEditedAt: new Date(tag1.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag5.urlPart,
                        service: tag5.service,
                        title: tag5.title,
                        shortTitle: tag5.shortTitle,
                        blocks: tag5.blocks,
                        draftTitle: tag5.draftTitle,
                        draftShortTitle: tag5.draftShortTitle,
                        draftBlocks: tag5.draftBlocks,
                        isArchived: tag5.isArchived,
                        isHot: tag5.isHot,
                        isPartnership: tag5.isPartnership,
                        partnershipLink: tag5.partnershipLink,
                        partnershipName: tag5.partnershipName,
                        partnershipBadgeName: tag5.partnershipBadgeName,
                        metaTitle: tag5.metaTitle,
                        metaDescription: tag5.metaDescription,
                        mmm: tag5.mmm,
                        createdAt: new Date(tag5.createdAt).toISOString(),
                        lastEditedAt: new Date(tag5.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag6.urlPart,
                        service: tag6.service,
                        title: tag6.title,
                        shortTitle: tag6.shortTitle,
                        blocks: tag6.blocks,
                        draftTitle: tag6.draftTitle,
                        draftShortTitle: tag6.draftShortTitle,
                        draftBlocks: tag6.draftBlocks,
                        isArchived: tag6.isArchived,
                        isHot: tag6.isHot,
                        isPartnership: tag6.isPartnership,
                        partnershipLink: tag6.partnershipLink,
                        partnershipName: tag6.partnershipName,
                        partnershipBadgeName: tag6.partnershipBadgeName,
                        metaTitle: tag6.metaTitle,
                        metaDescription: tag6.metaDescription,
                        mmm: tag6.mmm,
                        createdAt: new Date(tag6.createdAt).toISOString(),
                        lastEditedAt: new Date(tag6.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag2.urlPart,
                        service: tag2.service,
                        title: tag2.title,
                        shortTitle: tag2.shortTitle,
                        blocks: tag2.blocks,
                        draftTitle: tag2.draftTitle,
                        draftShortTitle: tag2.draftShortTitle,
                        draftBlocks: tag2.draftBlocks,
                        isArchived: tag2.isArchived,
                        isHot: tag2.isHot,
                        isPartnership: tag2.isPartnership,
                        partnershipLink: tag2.partnershipLink,
                        partnershipName: tag2.partnershipName,
                        partnershipBadgeName: tag2.partnershipBadgeName,
                        metaTitle: tag2.metaTitle,
                        metaDescription: tag2.metaDescription,
                        mmm: tag2.mmm,
                        createdAt: new Date(tag2.createdAt).toISOString(),
                        lastEditedAt: new Date(tag2.lastEditedAt).toISOString(),
                    },
                    {
                        urlPart: tag4.urlPart,
                        service: tag4.service,
                        title: tag4.title,
                        shortTitle: tag4.shortTitle,
                        blocks: tag4.blocks,
                        draftTitle: tag4.draftTitle,
                        draftShortTitle: tag4.draftShortTitle,
                        draftBlocks: tag4.draftBlocks,
                        isArchived: tag4.isArchived,
                        isHot: tag4.isHot,
                        isPartnership: tag4.isPartnership,
                        partnershipLink: tag4.partnershipLink,
                        partnershipName: tag4.partnershipName,
                        partnershipBadgeName: tag4.partnershipBadgeName,
                        metaTitle: tag4.metaTitle,
                        metaDescription: tag4.metaDescription,
                        mmm: tag4.mmm,
                        createdAt: new Date(tag4.createdAt).toISOString(),
                        lastEditedAt: new Date(tag4.lastEditedAt).toISOString(),
                    },
                ],
                total: 6,
            });
        });

        it('Возвращает data = [] и total = 0, если теги не найдены', async () => {
            const response = await request(server).get('/internal/tags').query({ pageSize: 10 });

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                data: [],
                total: 0,
            });
        });
    });

    describe('GET /internal/tags/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если тег не найден', async () => {
            const NON_EXISTING_TAG_ID = 'non-existing-tag-id';

            const response = await request(server).get(`/internal/tags/${NON_EXISTING_TAG_ID}`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                status: 404,
                error: 'Тег не найден',
            });
        });

        it('Возвращает тег', async () => {
            const TAG_DATA = { ...TAG_DATA_1 };
            const tag = await factory.createTag('Tag', TAG_DATA);

            const response = await request(server).get(`/internal/tags/${TAG_DATA.urlPart}`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                ...omit(TAG_DATA, ['partnershipImage', 'partnershipLink']),
                createdAt: new Date(tag.createdAt).toISOString(),
                lastEditedAt: new Date(tag.lastEditedAt).toISOString(),
            });
        });
    });

    describe('POST /internal/tags', function () {
        it('Возвращает ошибку и статус 400, если не задан "urlPart"', async () => {
            const TAG_DATA = { ...CREATE_TAG_DATA_1, urlPart: undefined };

            const response = await request(server).post('/internal/tags').send(TAG_DATA);

            expect(response.statusCode).toBe(400);
            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать URL',
            });
        });

        it('Возвращает ошибку и статус 400, если не задан черновик заголовка', async () => {
            const TAG_DATA = { ...CREATE_TAG_DATA_1, draftTitle: undefined };

            const response = await request(server).post('/internal/tags').send(TAG_DATA);

            expect(response.statusCode).toBe(400);
            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать заголовок',
            });
        });

        it('Возвращает ошибку и статус 400, если тег с таким же "urlPart" уже существует', async () => {
            const TAG_DATA = { ...CREATE_TAG_DATA_1 };

            await factory.createTag('Tag', TAG_DATA);

            const response = await request(server).post('/internal/tags').send(TAG_DATA);

            expect(response.statusCode).toBe(400);
            expect(response.body).toEqual({
                status: 400,
                error: `Тег с URL "${TAG_DATA.urlPart}" уже существует`,
            });
        });

        it('Создает и возвращает новый тег', async () => {
            const TAG_DATA = { ...CREATE_TAG_DATA_1 };

            const response = await request(server)
                .post('/internal/tags')
                .send({
                    ...TAG_DATA,
                    userLogin: 'user123',
                });

            const { createdAt, lastEditedAt, ...tagFields } = response.body;

            expect(tagFields).toMatchSnapshot(tagFields);
            expect(new Date(createdAt)).toBeValidDate();
            expect(new Date(lastEditedAt)).toBeValidDate();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('PUT /internal/tags/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если тег не найден', async () => {
            const NON_EXISTING_TAG_ID = 'non-existing-tag';
            const UPDATE_TAG_DATA = { ...UPDATE_TAG_DATA_1 };

            const response = await request(server)
                .put(`/internal/tags/${NON_EXISTING_TAG_ID}`)
                .send({
                    ...UPDATE_TAG_DATA,
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                status: 404,
                error: 'Тег не найден',
            });
        });

        it('Возвращает ошибку и статус 400, если не задан черновик заголовка', async () => {
            const UPDATE_TAG_DATA = { ...UPDATE_TAG_DATA_1, draftTitle: undefined };
            const tag = await factory.createTag('Tag');

            const response = await request(server)
                .put(`/internal/tags/${tag.urlPart}`)
                .send({
                    ...UPDATE_TAG_DATA,
                    userLogin: 'user123',
                });

            expect(response.statusCode).toBe(400);
            expect(response.body).toEqual({
                status: 400,
                error: 'Необходимо указать заголовок',
            });
        });

        it('Обновляет и возвращает тег', async () => {
            const TAG_DATA = { ...TAG_DATA_1 };
            const UPDATE_TAG_DATA = { ...UPDATE_TAG_DATA_1 };

            await factory.createTag('Tag', TAG_DATA);

            const response = await request(server)
                .put(`/internal/tags/${TAG_DATA.urlPart}`)
                .send({
                    ...UPDATE_TAG_DATA,
                    userLogin: 'user123',
                });

            const { createdAt, lastEditedAt, ...tagFields } = response.body;

            expect(tagFields).toMatchSnapshot(tagFields);
            expect(new Date(createdAt)).toBeValidDate();
            expect(new Date(lastEditedAt)).toBeValidDate();
            expect(response.statusCode).toBe(200);
        });
    });

    describe('DELETE /internal/tags/:urlPart', function () {
        it('Возвращает ошибку и статус 404, если тег не найден', async () => {
            const NON_EXISTING_TAG_ID = 'non-existing-tag-id';

            const response = await request(server).delete(`/internal/tags/${NON_EXISTING_TAG_ID}`);

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                status: 404,
                error: 'Тег не найден',
            });
        });

        it('Удаляет тег и его связи и возвращает его модель', async () => {
            const TAG_DATA = { ...TAG_DATA_1 };
            const tag = await factory.createTag('Tag', TAG_DATA);
            const category = await factory.createCategory('Category').then(async model => {
                await model.$add('tag', tag);
                return model;
            });
            const post = await factory.createPost('Post').then(async model => {
                await model.$add('tag', tag);
                return model;
            });
            const exportModel = await factory.createExport('Export').then(async model => {
                await model.$add('tag', tag);
                return model;
            });

            let categoryTags = await category.$get('tags');
            let postTags = await post.$get('tags');
            let exportTags = await exportModel.$get('tags');

            expect(categoryTags).toHaveLength(1);
            expect(postTags).toHaveLength(1);
            expect(exportTags).toHaveLength(1);

            const response = await request(server)
                .delete(`/internal/tags/${TAG_DATA.urlPart}`)
                .send({ userLogin: 'user123' });

            const dbTag = await Tag.findByPk(TAG_DATA.urlPart);

            categoryTags = await category.$get('tags');
            postTags = await post.$get('tags');
            exportTags = await exportModel.$get('tags');

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                ...omit(TAG_DATA, ['partnershipImage', 'partnershipLink']),
                createdAt: new Date(tag.createdAt).toISOString(),
                lastEditedAt: new Date(tag.lastEditedAt).toISOString(),
            });
            expect(dbTag).toBeFalsy();
            expect(categoryTags).toHaveLength(0);
            expect(postTags).toHaveLength(0);
            expect(exportTags).toHaveLength(0);
        });
    });

    describe('POST /internal/tags/:urlPart/commitDraft', function () {
        it('Возвращает ошибку и статус 404, если тег не найден', async () => {
            const NON_EXISTING_TAG_ID = 'non-existing-tag';

            const response = await request(server).post(`/internal/tags/${NON_EXISTING_TAG_ID}/commitDraft`).send();

            expect(response.statusCode).toBe(404);
            expect(response.body).toEqual({
                status: 404,
                error: 'Тег не найден',
            });
        });

        it('Возвращает ошибку и статус 400, если существует тег с таким же заголовком', async () => {
            const TITLE = 'Аренда';

            await factory.createTag('Tag', { title: TITLE });
            const tag = await factory.createTag('Tag', { draftTitle: TITLE });

            const response = await request(server).post(`/internal/tags/${tag.urlPart}/commitDraft`);

            expect(response.statusCode).toBe(400);
            expect(response.body).toEqual({
                status: 400,
                error: 'Тег с таким заголовком уже существует',
            });
        });

        it('Копирует черновые поля в основные и возвращает тег', async () => {
            const TAG_DATA = { ...TAG_DATA_1 };
            const tag = await factory.createTag('Tag', TAG_DATA);

            const response = await request(server).post(`/internal/tags/${TAG_DATA.urlPart}/commitDraft`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual({
                ...omit(TAG_DATA, ['partnershipImage', 'partnershipLink']),
                title: TAG_DATA.draftTitle,
                shortTitle: TAG_DATA.draftShortTitle,
                blocks: TAG_DATA.draftBlocks,
                createdAt: new Date(tag.createdAt).toISOString(),
                lastEditedAt: expect.stringMatching(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d+Z/),
            });
        });
    });
});
