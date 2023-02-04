import {ObjectMetadata} from '../../../../src/vector_render_engine/util/metadata';

export function createIndoorMetadata(planId: string, levelId: string): ObjectMetadata {
    return new Map([['indoor_plan_id', planId], ['indoor_level_id', levelId]]);
}
