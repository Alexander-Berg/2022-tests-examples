import { newbuildingMortgageSeoTextsSelector } from '../seoTexts.ts';

import mocks from './mocks/mocks.index';

test('get mortgage title', () => {
    const title = newbuildingMortgageSeoTextsSelector(mocks).title;

    // eslint-disable-next-line max-len
    expect(title).toBe('Ипотека от 13% в жилом комплексе «Люблинский парк» - список банков и ипотечных программ для покупки квартиры в ипотеку в жилом комплексе «Люблинский парк»');
});

test('get mortgage description', () => {
    const description = newbuildingMortgageSeoTextsSelector(mocks).description;

    // eslint-disable-next-line max-len
    expect(description).toBe('Купить квартиру в ипотеку в жилом комплексе «Люблинский парк» - полный список программ от 100 банков, аккредитовавших ЖК. Ставки от 13% и удобная заявка на одобрение ипотеки в жилом комплексе «Люблинский парк».');
});

test('get mortgage footer', () => {
    const footer = newbuildingMortgageSeoTextsSelector(mocks).footerText;

    // eslint-disable-next-line max-len
    expect(footer).toBe('Купить квартиру в\u00A0ипотеку в жилом комплексе «Люблинский парк» на\u00A0Яндекс.Недвижимости и\u00A0сразу оформить заявку в\u00A0100 банках очень удобно. Минимальная ставка по\u00A0ипотеке\u00A0- от\u00A013%, доступно 123 программы на\u00A0срок до\u00A030 лет. Удобная заявка на\u00A0одобрение ипотеки в жилом комплексе «Люблинский парк» сразу в\u00A0нескольких банках');
});
