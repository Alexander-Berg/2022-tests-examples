import desktopRouter from 'auto-core/router/auto.ru/server/router';
import mobileRouter from 'auto-core/router/m.auto.ru/server/router';

import getGroupingIdFromPageParams from './getGroupingIdFromPageParams';

// NOTE: в тестах специально испльзуется роутер, чтобы не завязываться на парсинг урла
const CARD_CARS_NEW_URL = '/cars/new/group/renault/kaptur/20792941/21078356/1101546946-2ad875e9/';
const CARD_GROUP_URL = '/cars/new/group/renault/kaptur/20780680-20780824/';

describe('desktop', () => {
    it('должен сделать groupingId для карточки нового авто', () => {
        const [ , params ] = desktopRouter({ method: 'GET', url: CARD_CARS_NEW_URL });

        expect(getGroupingIdFromPageParams(params)).toEqual('tech_param_id=20792941,complectation_id=21078356');
    });

    it('должен сделать groupingId для групповой карточки', () => {
        const [ , params ] = desktopRouter({ method: 'GET', url: CARD_GROUP_URL });

        expect(getGroupingIdFromPageParams(params)).toEqual('mark=RENAULT,model=KAPTUR,generation=20780680,configuration=20780824');
    });
});

describe('mobile', () => {
    it('должен сделать groupingId для карточки нового авто', () => {
        const [ , params ] = mobileRouter({ method: 'GET', url: CARD_CARS_NEW_URL });

        expect(getGroupingIdFromPageParams(params)).toEqual('tech_param_id=20792941,complectation_id=21078356');
    });

    it('должен сделать groupingId для групповой карточки', () => {
        const [ , params ] = mobileRouter({ method: 'GET', url: CARD_GROUP_URL });

        expect(getGroupingIdFromPageParams(params)).toEqual('mark=RENAULT,model=KAPTUR,generation=20780680,configuration=20780824');
    });
});
