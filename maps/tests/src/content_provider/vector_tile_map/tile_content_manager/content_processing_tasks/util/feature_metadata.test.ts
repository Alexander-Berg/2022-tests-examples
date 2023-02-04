import {expect} from 'chai';
import * as sinon from 'sinon';
import {
    packPrimitiveTags,
    extractPrimitiveTags,
    makeTagsMatcher,
    TagsMatcher
} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/util/feature_metadata';
import {PrimitiveTag} from '../../../../../../../src/vector_render_engine/content_provider/vector_tile_map/tile_content_manager/content_processing_tasks/proto_aliases';

describe('FeatureMetadata', () => {
    describe('packPrimitiveTags', () => {
        it('should make 64bit flags from list of numbers', () => {
            expect(packPrimitiveTags([])).to.deep.equal([0, 0]);
            expect(packPrimitiveTags([
                PrimitiveTag.BUILDING, PrimitiveTag.COUNTRY, PrimitiveTag.LANDCOVER
            ])).to.deep.equal([
                (1 << PrimitiveTag.BUILDING) | (1 << PrimitiveTag.COUNTRY) | (1 << PrimitiveTag.LANDCOVER),
                0
            ]);
            expect(packPrimitiveTags([
                PrimitiveTag.FENCE, PrimitiveTag.IS_TUNNEL
            ])).to.deep.equal([
                0,
                (1 << (PrimitiveTag.FENCE - 32)) | (1 << (PrimitiveTag.IS_TUNNEL - 32))
            ]);
            expect(packPrimitiveTags([
                PrimitiveTag.URBAN_AREA, PrimitiveTag.VEGETATION, PrimitiveTag.TRANSIT_ENTRANCE
            ])).to.deep.equal([
                (1 << PrimitiveTag.URBAN_AREA) | (1 << PrimitiveTag.VEGETATION),
                (1 << (PrimitiveTag.TRANSIT_ENTRANCE - 32))
            ]);
        });
    });

    describe('extractPrimitiveTags', () => {
        it('should return empty list for unknown index', () => {
            expect(extractPrimitiveTags({metadata: []} as any, 0)).to.deep.equal([]);
            expect(extractPrimitiveTags({metadata: [0, 1]} as any, 2)).to.deep.equal([]);
        });

        it('should return empty list if there is no tag field in metadata', () => {
            expect(extractPrimitiveTags({
                metadata: [{}]
            } as any, 0)).to.deep.equal([]);
        });

        it('should return tag field from metadata', () => {
            expect(extractPrimitiveTags({
                metadata: [
                    {['.yandex.maps.proto.renderer.common.TAG_METADATA']: {tags: [1, 2]}},
                    {['.yandex.maps.proto.renderer.common.TAG_METADATA']: {tags: [3, 4]}}
                ]
            } as any, 1)).to.deep.equal([3, 4]);
        });
    });

    describe('makeTagsMatcher', () => {
        let warnStub: sinon.SinonStub;
        beforeEach(() => {
            warnStub = sinon.stub(console, 'warn');
        });
        afterEach(() => {
            warnStub.restore();
        });

        function check(match: TagsMatcher, tags: number[], expected: boolean): void {
            expect(match(tags)).to.equal(expected);
        }

        it('should return "true" matcher if no tags are provided', () => {
            const match = makeTagsMatcher(undefined, undefined, undefined);
            check(match, [], true);
            check(match, [1, 12, 15], true);
            expect(warnStub.callCount).to.equal(0);
        });

        it('should return "false" matcher if there are unknown tags', () => {
            const match = makeTagsMatcher(['building', 'test_unknown'], undefined, undefined);
            check(match, [], false);
            check(match, [1, 12, 15], false);
            expect(warnStub.callCount).to.equal(2);
            expect(warnStub.getCall(0).args[0]).to.equal('unknown "tag": "test_unknown"');
            expect(warnStub.getCall(1).args[0])
                .to.equal('not valid "tags": all=[building,test_unknown] any=[] none=[]');
        });

        it('should apply "all" condition', () => {
            const match = makeTagsMatcher(['land', 'country', 'fence'], undefined, undefined);
            check(match, [PrimitiveTag.PARK], false);
            check(match, [PrimitiveTag.COUNTRY, PrimitiveTag.PARK], false);
            check(match, [PrimitiveTag.LAND, PrimitiveTag.COUNTRY, PrimitiveTag.FENCE], true);
            check(match, [PrimitiveTag.IS_TUNNEL, PrimitiveTag.LAND, PrimitiveTag.COUNTRY, PrimitiveTag.FENCE], true);
        });

        it('should apply "any" condition', () => {
            const match = makeTagsMatcher(undefined, ['land', 'country', 'fence'], undefined);
            check(match, [PrimitiveTag.PARK], false);
            check(match, [PrimitiveTag.LAND], true);
            check(match, [PrimitiveTag.FENCE, PrimitiveTag.COUNTRY], true);
        });

        it('should apply "none" condition', () => {
            const match = makeTagsMatcher(undefined, undefined, ['land', 'country', 'fence']);
            check(match, [PrimitiveTag.PARK], true);
            check(match, [PrimitiveTag.LAND], false);
            check(match, [PrimitiveTag.FENCE, PrimitiveTag.COUNTRY], false);
        });

        it('should support numbers', () => {
            const match = makeTagsMatcher([PrimitiveTag.BUILDING, PrimitiveTag.LAND], undefined, undefined);
            check(match, [PrimitiveTag.BUILDING, PrimitiveTag.LAND, PrimitiveTag.ADDRESS], true);
            check(match, [PrimitiveTag.BUILDING, PrimitiveTag.COUNTRY], false);
            check(match, [PrimitiveTag.LAND], false);
        });
    });
});
