import PlayerPanoramaStore, {IAddItemParams} from './player-panorama-store';
import {panoramaJsonResult} from 'mocks/panorama-json-mock';

describe('PlayerPanoramaStore', () => {
    let store: PlayerPanoramaStore;

    beforeEach(() => {
        global.fetch.resetMocks();
        store = new PlayerPanoramaStore();
    });

    it.only('should addItem', async () => {
        global.fetch.mockResponseOnce(JSON.stringify({data: panoramaJsonResult}));

        const params: IAddItemParams = {
            oid: 'some-id',
            type: 'session',
            token: 'token124'
        };
        await store.addItem(params);

        const requestBody = JSON.parse(global.fetch.mock.calls[0][1].body);

        expect(requestBody).toEqual(params);
        expect(store.activePanorama).toEqual({id: 'some-id', data: panoramaJsonResult});
    });
});
