import { LOCATION } from '../../__tests__/mocks';

import { getMetroFromLocation } from '../getMetroFromLocation';

describe('getMetroFromLocation', () => {
    it('Возвращает ничего', () => {
        expect(getMetroFromLocation({ location: {} })).toMatchSnapshot();
    });

    it.each([['ON_CAR'], ['ON_FOOT'], ['ON_TRANSPORT'], ['___UNKNOWN_TYPE___']])(
        'Возвращает объект для типа %s',
        (metroTransport) => {
            const location = { ...LOCATION, metro: { ...LOCATION.metro, metroTransport } };

            expect(getMetroFromLocation({ location })).toMatchSnapshot();
        }
    );
});
