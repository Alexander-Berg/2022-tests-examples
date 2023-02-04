import {tlds, Tld} from 'types/locale';
import {Landing} from 'server/seourl/types';
import {getLandingByPath} from 'server/seourl/landing';
import {BasicRequest} from 'types/request';

function landingWithTlds(
    actualFactory: (tld: Tld) => string | string[],
    expectedFactory: (tld: Tld, index: number) => Landing | undefined
): void {
    withTlds<Landing | void>((tld) => {
        const actualResult = actualFactory(tld);
        const actualArray = Array.isArray(actualResult) ? actualResult : [actualResult];
        return actualArray.map((actual) => getLandingByPath({tld} as BasicRequest, actual));
    }, expectedFactory);
}

function withTlds<T>(actualFactory: (tld: Tld) => T[], expectedFactory: (tld: Tld, index: number) => T): void {
    tlds.forEach((tld) => {
        it(`.${tld}`, () => {
            actualFactory(tld).forEach((actualElement, index) => {
                expect(actualElement).toEqual(expectedFactory(tld, index));
            });
        });
    });
}

// eslint-disable-next-line jest/no-export
export {landingWithTlds, withTlds};
