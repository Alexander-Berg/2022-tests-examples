import { nbsp } from 'auto-core/react/lib/html-entities';

import convertTermToYears from './convertTermToYears';

it('верно конвертирует срок в целое количество лет', () => {
    expect(convertTermToYears(24)).toEqual(`2${ nbsp }года`);
});

it('верно конвертирует срок в год + месяц', () => {
    expect(convertTermToYears(32)).toEqual(`2${ nbsp }года 8${ nbsp }месяцев`);
});

it('верно конвертирует срок в месяцы, если меньше одного года', () => {
    expect(convertTermToYears(4)).toEqual(`4${ nbsp }месяца`);
});

it('верно конвертирует срок в год + месяц с сокращением количества месяцев', () => {
    expect(convertTermToYears(32, true)).toEqual(`2${ nbsp }года 8${ nbsp }мес.`);
});
