const _ = require('lodash');
const MockDate = require('mockdate');

const { nbsp } = require('auto-core/react/lib/html-entities');
const getTransportTax = require('auto-core/react/lib/offer/getTransportTax');

const transportTax = require('./transportTax');

const card = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const expectedTooltipBase = `Налог рассчитан для${ nbsp }двигателя мощностью 122${ nbsp }л.с. в Москве по${ nbsp }тарифу 2019${ nbsp }года`;
const expectedTooltipBaseForElectro = `Налог рассчитан для${ nbsp }двигателя мощностью 122${ nbsp }л.с. / 90${ nbsp }кВт` +
    ` в Москве по${ nbsp }тарифу 2019${ nbsp }года с${ nbsp }учетом льгот для${ nbsp }электромобилей`;

afterEach(() => {
    MockDate.reset();
});

it('должен вернуть инфу о транспортном налоге', () => {
    const cardCopy = _.cloneDeep(card);
    const mockYear = getTransportTax(cardCopy).year;
    MockDate.set(mockYear.toString());

    expect(transportTax(cardCopy)).toEqual({
        label: 'Налог',
        fullName: 'Транспортный налог',
        tooltip: expectedTooltipBase,
        value: `17${ nbsp }325${ nbsp }₽ / год`,
    });
});

it('должен вернуть правильный тултип при годе расчета налога меньше текущего', () => {
    const cardCopy = _.cloneDeep(card);
    const mockYear = getTransportTax(cardCopy).year + 1;
    MockDate.set(mockYear.toString());

    expect(transportTax(cardCopy).tooltip).toEqual(
        expectedTooltipBase +
        `. Правительство${ nbsp }РФ пока не${ nbsp }утвердило налоговую базу ${ mockYear }${ nbsp }года`,
    );
});

it('не должен вернуть инфу о транспортном налоге, если такого нет', () => {
    const card = {
        owner_expenses: {
            transport_tax: {},
        },
    };
    expect(transportTax(card)).toBeNull();
});

it('должен вернуть инфу о транспортном налоге, если он есть, но равен 0', () => {
    const cardCopy = _.cloneDeep(card);
    const mockYear = getTransportTax(cardCopy).year;
    MockDate.set(mockYear.toString());
    cardCopy.owner_expenses.transport_tax.tax_by_year = 0;

    expect(transportTax(cardCopy)).toEqual({
        label: 'Налог',
        fullName: 'Транспортный налог',
        tooltip: expectedTooltipBase,
        value: `0${ nbsp }₽ / год`,
    });
});

it('должен вернуть правильный тултип если это нулевой налог на электромобили', () => {
    const cardCopy = _.cloneDeep(card);
    cardCopy.vehicle_info.tech_param.engine_type = 'ELECTRO';
    cardCopy.owner_expenses.transport_tax.tax_by_year = 0;

    const mockYear = getTransportTax(cardCopy).year;
    MockDate.set(mockYear.toString());

    expect(transportTax(cardCopy).tooltip).toEqual(expectedTooltipBaseForElectro);
});
