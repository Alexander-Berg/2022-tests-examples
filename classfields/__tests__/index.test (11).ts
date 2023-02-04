/* eslint-disable max-len */
import { indexSeoTextsSelector } from '../seoTexts';

import { mockState } from './mock';

test('сео тексты для главной на таче', () => {
    const seoTexts = indexSeoTextsSelector(mockState);

    expect(seoTexts.title).toBe(
        'Недвижимость в Москве и МО: продажа и аренда квартир и коммерческой недвижимости – Яндекс.Недвижимость'
    );
    expect(seoTexts.h1).toBe('Недвижимость в Москве и МО');
    expect(seoTexts.description).toBe(
        'Яндекс.Недвижимость: объявления о покупке, продаже, аренде квартир, домов, и коммерческой недвижимости в Москве и МО. Вторичное жилье и новостройки. Офисы и торговые помещения. Цены на недвижимость в Москве и МО.'
    );
    expect(seoTexts.footerText).toBe(
        'Яндекс.Недвижимость — аренда и продажа недвижимости в Москве и МО: поможем сдать, снять, продать или купить квартиру или другое жилье.'
    );
});
