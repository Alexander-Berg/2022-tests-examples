import {sessionsResult} from 'mocks/sessions-mock';
import {sessionGeoJsonResult} from 'mocks/session_geojson-mock';
import SessionItemStore from './session-item-store';
import {getPanoramaPoints, getMultiLineStringCoordinates} from 'lib/geo-data';

describe('SessionItemStore', () => {
    let store: SessionItemStore;

    beforeEach(() => {
        global.fetch.resetMocks();
        store = new SessionItemStore({data: sessionsResult.items[0], shipmentId: '1', useBbox: true});
    });

    it('should fetch geojson', async () => {
        global.fetch.mockResponseOnce(JSON.stringify({data: sessionGeoJsonResult}));

        await store.fetchGeoJSON();

        const requestBody = JSON.parse(global.fetch.mock.calls[0][1].body);

        expect(requestBody).toEqual({sessionId: '17687'});
        expect(store.points).toEqual(getPanoramaPoints(sessionGeoJsonResult));
        expect(store.multiLineStrings).toEqual(getMultiLineStringCoordinates(sessionGeoJsonResult));
    });

    it('should update session state', async () => {
        global.fetch.mockResponseOnce(JSON.stringify({data: sessionGeoJsonResult}));

        await store.updateSessionState({isAccept: true});

        const requestBody = JSON.parse(global.fetch.mock.calls[0][1].body);

        expect(requestBody).toEqual({sessionId: '17687', sessionStatus: 'APPROVED'});
        expect(store.status).toBe('APPROVED');
    });
});
