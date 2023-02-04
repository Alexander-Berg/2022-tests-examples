import {expect} from 'chai';
import {intToCollisionZIndex} from '../../../src/vector_render_engine/util/z_index';
import {CollisionPriorityRank} from 'vector_render_engine/content_provider/base_vector_content_provider/content_manager/util/collision_priority';

const MIN_INT_22 = -(2 ** 21);
const MAX_INT_22 = 2 ** 21 - 1;

const MIN_INT_32 = -(2 ** 31);

describe('zIndex', () => {
    it('should convert int22 to z-index taking into account the collision rank', () => {
        const MEDIUM_MIN = intToCollisionZIndex(MIN_INT_22, CollisionPriorityRank.MEDIUM);
        const HIGH_MIN = intToCollisionZIndex(MIN_INT_22, CollisionPriorityRank.HIGH);
        const ULTRA_MIN = intToCollisionZIndex(MIN_INT_22, CollisionPriorityRank.ULTRA);

        expect(intToCollisionZIndex(MIN_INT_22, CollisionPriorityRank.LOW)).to.be.lessThan(MEDIUM_MIN);
        expect(intToCollisionZIndex(MAX_INT_22, CollisionPriorityRank.LOW)).to.be.lessThan(MEDIUM_MIN);
        expect(intToCollisionZIndex(0, CollisionPriorityRank.LOW)).to.be.lessThan(MEDIUM_MIN);

        expect(intToCollisionZIndex(MIN_INT_22, CollisionPriorityRank.MEDIUM)).to.be.lessThan(HIGH_MIN);
        expect(intToCollisionZIndex(MAX_INT_22, CollisionPriorityRank.MEDIUM)).to.be.lessThan(HIGH_MIN);
        expect(intToCollisionZIndex(0, CollisionPriorityRank.MEDIUM)).to.be.lessThan(HIGH_MIN);

        expect(intToCollisionZIndex(MIN_INT_22, CollisionPriorityRank.HIGH)).to.be.lessThan(ULTRA_MIN);
        expect(intToCollisionZIndex(MAX_INT_22, CollisionPriorityRank.HIGH)).to.be.lessThan(ULTRA_MIN);
        expect(intToCollisionZIndex(0, CollisionPriorityRank.HIGH)).to.be.lessThan(ULTRA_MIN);
    });

    it('should clamp values out of int22 range', () => {
        const LOW_MAX = intToCollisionZIndex(MAX_INT_22, CollisionPriorityRank.LOW);
        const MEDIUM_MAX = intToCollisionZIndex(MAX_INT_22, CollisionPriorityRank.MEDIUM);
        const HIGH_MAX = intToCollisionZIndex(MAX_INT_22, CollisionPriorityRank.HIGH);

        expect(intToCollisionZIndex(MIN_INT_32, CollisionPriorityRank.MEDIUM)).to.be.greaterThan(LOW_MAX);
        expect(intToCollisionZIndex(MIN_INT_32, CollisionPriorityRank.HIGH)).to.be.greaterThan(MEDIUM_MAX);
        expect(intToCollisionZIndex(MIN_INT_32, CollisionPriorityRank.ULTRA)).to.be.greaterThan(HIGH_MAX);
    });
});
