import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import articleMock from 'auto-core/react/dataDomain/mag/articleMock';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';

import preparePostBlocks from './preparePostBlocks';

const contextMock = { req: createHttpReq(), res: createHttpRes() } as unknown as TDescriptContext;

const BLOCKS = articleMock.withBlocks({ withDefaultBlocks: true }).value().blocks;

it('возвращает подготовленные блоки поста', () => {
    return de.run(preparePostBlocks, { params: BLOCKS, context: contextMock })
        .then((result) => {
            expect(Array.isArray(result)).toBe(true);
        });
});
