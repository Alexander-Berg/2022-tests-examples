import type { TDescriptContext } from 'auto-core/server/descript/createContext';

import isTrueInfiniteListing from './isTrueInfiniteListing';

it('вернет true для региона в России', () => {
    const req = {
        geoIds: [ 15 ],
        geoIdsParents: {
            '15': [ 225, 1000 ],
        },
    };
    const result = isTrueInfiniteListing({ req } as unknown as TDescriptContext);
    expect(result).toBe(true);
});

describe('вернет false', () => {
    it('для региона вне России', () => {
        const req = {
            geoIds: [ 155 ],
            geoIdsParents: {
                '155': [ 333 ],
            },
        };
        const result = isTrueInfiniteListing({ req } as unknown as TDescriptContext);
        expect(result).toBe(false);
    });

    it('для неизвестного региона', () => {
        const req = {
            geoIds: [ 111 ],
        };
        const result = isTrueInfiniteListing({ req } as unknown as TDescriptContext);
        expect(result).toBe(false);
    });

    it('если выбрано несколько регионов', () => {
        const req = {
            geoIds: [ 15, 18 ],
            geoIdsParents: {
                '15': [ 225, 1000 ],
            },
        };
        const result = isTrueInfiniteListing({ req } as unknown as TDescriptContext);
        expect(result).toBe(false);
    });
});
