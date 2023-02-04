import { cloneDeep } from 'lodash';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import warranty from './warranty';

it('должен вернуть пустую строку, если нет гарантии', () => {
    const offer = cloneDeep(offerMock);
    offer.documents.warranty = false;
    delete offer.documents.warranty_expire;

    expect(warranty(offer)).toEqual('');
});

it('должен вернуть число, месяц и год, если объявление создано не в этом году', () => {
    const offer = cloneDeep(offerMock);
    offer.documents.warranty = true;
    offer.documents.warranty_expire = {
        year: 2020,
        month: 10,
        day: 5,
    };

    expect(warranty(offer)).toEqual('До октября 2020');
});
