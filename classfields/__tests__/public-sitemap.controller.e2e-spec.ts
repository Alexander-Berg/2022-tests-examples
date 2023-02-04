import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import moment from 'moment';
import 'jest-extended';

import { createTestingApp } from '../../../tests/app';
import { getFixtures } from '../../../tests/get-fixtures';
import { Post as PostModel } from '../../post/post.model';
import { Tag as TagModel } from '../../tag/tag.model';
import { Category as CategoryModel } from '../../category/category.model';

import { fixtures } from './public-sitemap.controller.fixtures';

const DATE_NOW = '2021-11-26T12:12:12.000Z';

describe('Public sitemap controller', () => {
    let app: INestApplication;
    let server;

    beforeEach(async () => {
        const testingApp = await createTestingApp();

        app = testingApp.app;

        await app.init();

        server = app.getHttpServer();

        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await app.close();
    });

    describe('GET /sitemap', function () {
        it('Возвращает массив элементов для сайтмапа', async () => {
            const { POST_ATTRIBUTES_1, TAG_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_1 } = getFixtures(fixtures);

            const SITEMAP_POST_1 = await PostModel.create(POST_ATTRIBUTES_1);
            const SITEMAP_TAG_1 = await TagModel.create(TAG_ATTRIBUTES_1);
            const SITEMAP_CATEGORY_1 = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);

            const response = await request(server).get(`/sitemap`);

            expect(response.statusCode).toBe(200);
            expect(response.body).toEqual([
                {
                    pathname: `/journal/post/${SITEMAP_POST_1.urlPart}/`,
                    changefreq: 'daily',
                    priority: 1.0,
                    lastmod: moment(SITEMAP_POST_1.publishAt).format(moment.defaultFormatUtc),
                },
                {
                    pathname: `/journal/category/${SITEMAP_CATEGORY_1.urlPart}/`,
                    changefreq: 'daily',
                    priority: 0.8,
                    lastmod: moment(DATE_NOW).format(moment.defaultFormatUtc),
                },
                {
                    pathname: `/journal/tag/${SITEMAP_TAG_1.urlPart}/`,
                    changefreq: 'daily',
                    priority: 0.8,
                    lastmod: moment(DATE_NOW).format(moment.defaultFormatUtc),
                },
            ]);
        });
    });
});
