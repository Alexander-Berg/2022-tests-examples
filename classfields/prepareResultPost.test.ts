/**
 * @jest-environment node
 */
import { POST_MOCK_4 } from 'core/services/post/mocks/post.mock';
import { POST_BLOCKS_MOCK_1 } from 'core/services/post/mocks/blocks.mock';

import { prepareResultPost } from './prepareResultPost';

it('возвращает подготовленный пост с переданными блоками', () => {
    const result = prepareResultPost({
        result: {
            post: POST_MOCK_4,
            preparePostBlocks: POST_BLOCKS_MOCK_1,
        },
    });

    const expectedResult = {
        ...POST_MOCK_4,
        blocks: POST_BLOCKS_MOCK_1,
    };

    expect(result).toEqual(expectedResult);
});
