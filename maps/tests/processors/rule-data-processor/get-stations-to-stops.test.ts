import * as assert from 'assert';
import {getStationsToStops} from '../../../server/processors/rule-data-processor';

const stop1 = {
    id: 'sp96585445',
    nodeId: 'nd96585445',
    stationId: 'st43942813',
    attributes: {}
};

describe('get-stations-to-stops', () => {
    it('should not fail on an empty stops array', () => {
        assert.deepEqual(getStationsToStops([], {}, {}, {}, {}), {});
    });

    it('should not fail on an empty nodesToLinks hashMap', () => {
        assert.deepEqual(
            getStationsToStops([stop1], {nd96585445: ['lk29579766']}, {}, {}, {}),
            {
                st43942813: [
                    {
                        id: 'sp96585445',
                        nodeId: 'nd96585445',
                        lineId: undefined,
                        graphicsIds: undefined,
                        stationId: 'st43942813'
                    }
                ]
            }
        );
    });

    it('should not fail on an empty linksToTracks hashMap', () => {
        assert.deepEqual(
            getStationsToStops([stop1], {nd96585445: ['lk29579766']}, {}, {}, {}),
            {
                st43942813: [
                    {
                        id: 'sp96585445',
                        nodeId: 'nd96585445',
                        stationId: 'st43942813',
                        lineId: undefined,
                        graphicsIds: undefined
                    }
                ]
            }
        );
    });

    it('should not fail on an empty tracksToLines hashMap', () => {
        assert.deepEqual(
            getStationsToStops([stop1], {nd96585445: ['lk29579766']}, {lk29579766: ['lk29579766']}, {}, {}),
            {
                st43942813: [
                    {
                        id: 'sp96585445',
                        nodeId: 'nd96585445',
                        stationId: 'st43942813',
                        lineId: undefined,
                        graphicsIds: undefined
                    }
                ]
            }
        );
    });

    it('should not fail on an empty rulesHashMap', () => {
        assert.deepEqual(
            getStationsToStops(
                [stop1],
                {nd96585445: ['lk29579766']},
                {lk29579766: ['lk29579766']},
                {lk29579766: ['sr70375344']},
                {}
            ),
            {
                st43942813: [
                    {
                        id: 'sp96585445',
                        nodeId: 'nd96585445',
                        stationId: 'st43942813',
                        lineId: 'sr70375344',
                        graphicsIds: undefined
                    }
                ]
            }
        );
    });

    it('should work for a simple case', () => {
        assert.deepEqual(
            getStationsToStops(
                [stop1],
                {nd96585445: ['lk29579766']},
                {lk29579766: ['lk29579766']},
                {lk29579766: ['sr70375344']},
                {st78367646: ['0d011251']}
            ),
            {
                st43942813: [
                    {
                        id: 'sp96585445',
                        nodeId: 'nd96585445',
                        stationId: 'st43942813',
                        lineId: 'sr70375344',
                        graphicsIds: undefined
                    }
                ]
            }
        );
    });

    it('should work on a multiple subobjects case', () => {
        assert.deepEqual(
            getStationsToStops(
                [stop1],
                {nd96585445: ['lk29579766', 'lk29320540']},
                {lk29579766: ['lk29579766'], lk29320540: ['lk29320540', 'lk48490846']},
                {lk25748500: ['sr70375344'], lk29320540: ['sr70375344'], lk48490846: ['sr70375344']},
                {sp96585445: ['0d011251', '820f3357']}
            ),
            {
                st43942813: [
                    {
                        id: 'sp96585445',
                        nodeId: 'nd96585445',
                        stationId: 'st43942813',
                        lineId: 'sr70375344',
                        graphicsIds: ['0d011251', '820f3357']
                    }
                ]
            }
        );
    });
});
