import { offerDataToPreview } from '../transformer';

describe('enrichBuildingInfo', () => {
    it('enrich correct searcher-models fields', () => {
        const vosCard = {
            specific: {},
            offer: {},
            user: {}
        };

        const heatmapsInfo = {
            profitability: {
                title: 'Прогноз окупаемости',
                valueDescription: 'высокая',
                valueFrom: 16,
                valueTo: 17,
                level: 7,
                maxLevel: 9,
                realValue: 17
            },
            ecology: {
                title: 'Качество воздуха',
                valueDescription: 'среднее',
                valueFrom: 4,
                valueTo: 5,
                level: 4,
                maxLevel: 9
            }
        };

        const searcherModel = offerDataToPreview({ vosCard, heatmapsInfo });

        expect(searcherModel.location.allHeatmaps).toEqual([ {
            title: 'Прогноз окупаемости',
            description: 'высокая',
            level: 7,
            maxLevel: 9,
            name: 'profitability'
        }, {
            title: 'Качество воздуха',
            description: 'среднее',
            level: 4,
            maxLevel: 9,
            name: 'ecology'
        } ]);
    });

    it('no enrich empty heatmaps if heatmapsInfo isEmpty', () => {
        const vosCard = {
            specific: {},
            offer: {},
            user: {}
        };

        expect(offerDataToPreview({ vosCard, heatmapsInfo: {} }).allHeatmaps).toBeUndefined();
        expect(offerDataToPreview({ vosCard, heatmapsInfo: null }).allHeatmaps).toBeUndefined();
    });
});
