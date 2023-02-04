import { Test, TestingModule } from '@nestjs/testing';

import {
    GetTagBlocksTransformer,
    ITransformedBlocksWithPosts,
} from '../../../transformers/get-tag/tag-blocks.transformer';
import { GetTagTransformer } from '../../../transformers/get-tag/get-tag.transformer';
import { Tag as TagModel } from '../../../../tag/tag.model';
import { TagModule } from '../../../tag.module';
import { getFixtures } from '../../../../../tests/get-fixtures';
import { PostService } from '../../../../post/post.service';
import { ImageTransformer } from '../../../../image-transformer/image.transformer';

import { fixtures } from './get-tag.transformer.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Get tag transformer', () => {
    let testingModule: TestingModule;
    let blocksTransformerSpy: jest.SpyInstance;
    let postServiceSpy: jest.SpyInstance;
    let imageTransformerSpy: jest.SpyInstance;
    let transformer: GetTagTransformer;
    const postsCount = 10;
    const transformedBlocks: ITransformedBlocksWithPosts = {
        posts: {},
        blocks: [{ type: 'text', text: 'i am transformed' }],
    };

    beforeEach(async () => {
        const imageTransformer = new ImageTransformer();

        testingModule = await Test.createTestingModule({
            imports: [TagModule],
        })
            .overrideProvider(ImageTransformer)
            .useValue(imageTransformer)
            .compile();

        imageTransformerSpy = jest.spyOn(imageTransformer, 'transform').mockImplementation(() => Promise.resolve(null));

        const blockTransformer = await testingModule.resolve(GetTagBlocksTransformer);

        blocksTransformerSpy = jest
            .spyOn(blockTransformer, 'transform')
            .mockImplementation(() => Promise.resolve(transformedBlocks));

        const postService = await testingModule.resolve(PostService);

        postServiceSpy = jest.spyOn(postService, 'getCount').mockImplementation(() => Promise.resolve(postsCount));

        transformer = await testingModule.resolve(GetTagTransformer);

        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });

    it('Возвращает трансформированный тег', async () => {
        const { TAG_ATTRIBUTES } = getFixtures(fixtures);

        const TAG = await TagModel.create(TAG_ATTRIBUTES);

        const result = await transformer.transform(TAG);

        expect(blocksTransformerSpy).toHaveBeenCalledTimes(0);
        expect(postServiceSpy).toHaveBeenCalledTimes(1);
        expect(postServiceSpy).toHaveBeenCalledWith({ onlyPublished: true, tags: [TAG.urlPart] });
        expect(imageTransformerSpy).toHaveBeenCalledTimes(1);
        expect(imageTransformerSpy).toHaveBeenCalledWith(TAG.partnershipImage);
        expect(result).toMatchSnapshot();
    });

    it('Может возвращать трансформированный черновик тега', async () => {
        const { TAG_ATTRIBUTES } = getFixtures(fixtures);

        const TAG = await TagModel.create(TAG_ATTRIBUTES);

        const result = await transformer.transform(TAG, { draft: true });

        expect(blocksTransformerSpy).toHaveBeenCalledTimes(0);
        expect(postServiceSpy).toHaveBeenCalledTimes(1);
        expect(postServiceSpy).toHaveBeenCalledWith({ onlyPublished: true, tags: [TAG.urlPart] });
        expect(imageTransformerSpy).toHaveBeenCalledTimes(1);
        expect(imageTransformerSpy).toHaveBeenCalledWith(TAG.partnershipImage);
        expect(result).toMatchSnapshot();
    });

    it('Может возвращать тег с трансформированными блоками', async () => {
        const { TAG_ATTRIBUTES } = getFixtures(fixtures);

        const TAG = await TagModel.create(TAG_ATTRIBUTES);

        const result = await transformer.transform(TAG, { withPostsModels: true });

        expect(blocksTransformerSpy).toHaveBeenCalledTimes(1);
        expect(blocksTransformerSpy).toHaveBeenCalledWith({
            tagUrlPart: TAG.urlPart,
            blocks: TAG.blocks,
        });
        expect(postServiceSpy).toHaveBeenCalledTimes(1);
        expect(postServiceSpy).toHaveBeenCalledWith({ onlyPublished: true, tags: [TAG.urlPart] });
        expect(imageTransformerSpy).toHaveBeenCalledTimes(1);
        expect(imageTransformerSpy).toHaveBeenCalledWith(TAG.partnershipImage);
        expect(result).toMatchSnapshot();
    });

    it('Может возвращает тег с трансформированными черновыми блоками', async () => {
        const { TAG_ATTRIBUTES } = getFixtures(fixtures);

        const TAG = await TagModel.create(TAG_ATTRIBUTES);

        const result = await transformer.transform(TAG, {
            draft: true,
            withPostsModels: true,
        });

        expect(blocksTransformerSpy).toHaveBeenCalledTimes(1);
        expect(blocksTransformerSpy).toHaveBeenCalledWith({
            tagUrlPart: TAG.urlPart,
            blocks: TAG.draftBlocks,
        });
        expect(postServiceSpy).toHaveBeenCalledTimes(1);
        expect(postServiceSpy).toHaveBeenCalledWith({ onlyPublished: true, tags: [TAG.urlPart] });
        expect(imageTransformerSpy).toHaveBeenCalledTimes(1);
        expect(imageTransformerSpy).toHaveBeenCalledWith(TAG.partnershipImage);
        expect(result).toMatchSnapshot();
    });
});
