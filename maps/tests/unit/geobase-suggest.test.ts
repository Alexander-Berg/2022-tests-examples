import {getSuggest} from 'app/lib/geobase-suggest';

describe('getSuggest():', () => {
    it('return rich suggest', async () => {
        const result = getSuggest('моск');
        expect(result).toEqual([
            {
                country: 'Россия',
                id: 213,
                name: 'Москва'
            },
            {
                country: 'Россия',
                id: 1,
                name: 'Москва и Московская область'
            },
            {
                country: 'Россия',
                id: 103817,
                name: 'Московский'
            },
            {
                country: 'США',
                id: 103325,
                name: 'Москоу'
            }
        ]);
    });

    it('return empty suggest', async () => {
        const result = getSuggest('unknown');
        expect(result).toEqual([]);
    });
});
