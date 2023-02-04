import {IndoorPlanContent} from '../../../../../src/vector_render_engine/content_provider/indoor/indoor_plans/indoor_plan_backend';

export function createIndoorPlan(): IndoorPlanContent {
    return {
        levels: [
            {id: '1', name: '1', isUnderground: false},
            {id: '2', name: '2', isUnderground: false},
            {id: '3', name: '3', isUnderground: false}
        ],
        defaultLevelId: '1',
        boundary: {
            lowerCorner: {
                lon: 50,
                lat: 50
            },
            upperCorner: {
                lon: 55,
                lat: 55
            }
        }
    };
}
