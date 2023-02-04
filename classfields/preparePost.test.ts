import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import articleMock from 'auto-core/react/dataDomain/mag/articleMock';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';

import preparePost from './preparePost';

const contextMock = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

it('возвращает пост в котором есть подготовленные: блоки, уникальные типы блоков и баннеры', () => {
    return de.run(preparePost, { params: articleMock.withBlocks({ withDefaultBlocks: true }).value(), context: contextMock })
        .then((result) => {
            expect(Array.isArray(result.blocks)).toBe(true);
            expect(Array.isArray(result.uniqBlockTypes)).toBe(true);
            expect(typeof result.banners).toBe('object');
        });
});
