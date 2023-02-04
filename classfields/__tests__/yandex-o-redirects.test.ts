import notFound from 'realty-core/app/lib/middleware/not-found';

import RedirectError from 'realty-core/app/lib/redirect-error';

import { Request, Response, nextFunction } from './mocks/404';

describe('Редирект с урлов объявлений', () => {
    beforeEach(() => {
        nextFunction.mockClear();
    });

    const testCases = [
        '/tag/odezhda-iz-yaponii/',
        '/abakan/tag/odezhda-iz-yaponii/',
        '/abakan/offer/125436123/',
        '/abakan/bitovaya-tehnika/',
        '/abakan/odezhda-obuv-i-aksessuari/',
        '/abakan/transport-i-zapchasti/',
        '/abakan/elektronika/',
        '/abakan/tovari-dlya-zhivotnih/',
        '/abakan/dacha-sad-i-ogorod/',
        '/abakan/biznes-i-oborudovanie-dlya-biznesa/',
        '/abakan/rabota/',
        '/abakan/tovari-dlya-zdorovya/',
        '/abakan/tovary-dlya-dosuga-i-razvlecheniya/',
        '/abakan/stroitelstvo-i-remont/',
        '/abakan/detskie-tovari/',
        '/abakan/tovari-dlya-krasoti/',
        '/abakan/byuro-nahodok/',
        '/abakan/komputernaya-tehnika/',
        '/abakan/tovari-dlya-doma/',
        '/abakan/sport-i-otdih/',
        '/abakan/uslugi/kek',
    ];

    testCases.forEach((url: string) => {
        it(`Редирект с ${url} на главную`, () => {
            const request = new Request(url);
            const response = new Response();

            notFound(null)(request, response, nextFunction);

            expect(nextFunction).toHaveBeenCalled();

            const error = nextFunction.mock.calls[0][0];

            expect(RedirectError.isRedirectError(error)).toBeTruthy();
            expect(error.data.location).toBe('/');
            expect(error.data.status).toBe(301);
        });
    });
});
