import { createRouter, createRouteLocator } from '..';
import { createMemoryHistory } from 'history';

/* eslint-disable new-cap */

describe('router', () => {
    it('push should work', async() => {
        const routeLocator = createRouteLocator();

        const history = createMemoryHistory({ initialEntries: [ '/clients' ] });
        const router = createRouter(history, routeLocator);

        const locationsP = router.location$.take(3)
            .reduce((acc, x) => [ ...acc, x ], []);

        await Promise.resolve(); // because initial values are pushed asynchronous

        router.push({ page: 'clients', params: { segment: 'rent' } });
        router.push({ page: 'clients', params: { regions: '1' } });

        return expect(locationsP).resolves.toEqual([
            { page: 'clients', params: {} },
            { page: 'clients', params: { segment: 'rent' } },
            { page: 'clients', params: { regions: '1' } }
        ]);
    });

    it('replace should work', async() => {
        const routeLocator = createRouteLocator();

        const history = createMemoryHistory({ initialEntries: [ '/clients' ] });
        const router = createRouter(history, routeLocator);

        const locationsP = router.location$.take(3)
            .reduce((acc, x) => [ ...acc, x ], []);

        await Promise.resolve(); // because initial values are pushed asynchronous

        router.push({ page: 'clients', params: { segment: 'rent' } });
        router.replace({ page: 'clients', params: { regions: '1' } });

        return expect(locationsP).resolves.toEqual([
            { page: 'clients', params: {} },
            { page: 'clients', params: { segment: 'rent' } },
            { page: 'clients', params: { regions: '1' } }
        ]);
    });

    it('should skip the same location', async() => {
        const routeLocator = createRouteLocator();

        const history = createMemoryHistory({ initialEntries: [ '/clients' ] });
        const router = createRouter(history, routeLocator);

        const locationsP = router.location$.take(3)
            .reduce((acc, x) => [ ...acc, x ], []);

        await Promise.resolve(); // because initial values are pushed asynchronous

        router.push({ page: 'clients', params: { segment: 'rent' } });
        router.push({ page: 'clients', params: {} });

        return expect(locationsP).resolves.toEqual([
            { page: 'clients', params: {} },
            { page: 'clients', params: { segment: 'rent' } },
            { page: 'clients', params: {} }
        ]);
    });
});
