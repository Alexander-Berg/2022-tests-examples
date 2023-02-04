import checkParamsValidity from 'realty-core/app/lib/middleware/seo-redirect/helpers/checkParamsValidity';
import getParams from 'realty-core/app/lib/middleware/seo-redirect/helpers/getParamsFromUrlObject';

const ValidParams = {
    rgid: '587795',
    type: 'SELL',
    category: 'APARTMENT',
};

const getParamsFromUrl = (link: string) => {
    const parsedLink = new URL(link);

    return checkParamsValidity(getParams(parsedLink));
};

describe('ParamsValidator', () => {
    it('dont affect url with valid params only', () => {
        expect(getParamsFromUrl('https://realty.yandex.ru/?rgid=587795&type=SELL&category=APARTMENT')).toMatchObject(
            ValidParams
        );
    });

    it('return empty object for url with invalid params only', () => {
        expect(
            getParamsFromUrl('https://realty.yandex.ru/?=&amp%3Bamp%3Brid=62&amp%3Bamp%3Bcurrency=RUR')
        ).toMatchObject({});
    });

    it('filter only invalid params from url', () => {
        expect(
            getParamsFromUrl('https://realty.yandex.ru/?=&a%3Bb=123&c%2Cd=62&e%20f=RUR&g.h=some&rgid=587795')
        ).toMatchObject({ rgid: '587795' });
    });
});
