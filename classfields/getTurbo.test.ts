import MockDate from 'mockdate';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import AppError from 'auto-core/lib/app_error';

import articleMock from 'auto-core/react/dataDomain/mag/articleMock';

import journalApi from 'auto-core/server/resources/journal-api/getResource.nock.fixtures';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import mockFeedItem from '../mocks/feedItem.mock';

import getTurbo from './getTurbo';

jest.mock('../../../../build/server.feed', () => {
    return () => mockFeedItem;
}, { virtual: true });

let req: THttpRequest;
let res: THttpResponse;

beforeEach(() => {
    MockDate.set('2020-05-20');

    req = createHttpReq() as unknown as THttpRequest;
    res = createHttpRes() as unknown as THttpResponse;
});

it('вернет ошибку, если отсутствуют посты', async() => {
    journalApi
        .get('/posts-for-feed/?pageSize=80&status=publish&orderBySort=lastEditedAt&pageNumber=0&rssOff=false&indexOff=false')
        .reply(200, {
            data: [],
        });

    let error;

    try {
        await getTurbo(req, res);
    } catch (e) {
        error = e;
    }

    expect(error).toEqual(AppError.createError(AppError.CODES.PAGE_NOT_FOUND));
});

const post = articleMock.withBlocks({ withDefaultBlocks: true }).value();

it('вернет содержимое фида', async() => {
    journalApi
        .get('/posts-for-feed/?pageSize=80&status=publish&orderBySort=lastEditedAt&pageNumber=0&rssOff=false&indexOff=false')
        .reply(200, {
            data: [ post, post, post ],
        });

    const result = await getTurbo(req, res);

    expect(result).toMatchSnapshot();
});
