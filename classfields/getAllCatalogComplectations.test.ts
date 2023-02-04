import no from 'nommon';

import cardGroupComplectationsMock from 'auto-core/react/dataDomain/cardGroupComplectations/mocks/complectations';

import catalogSubtreeMock from '../mocks/subtree';

import selector from './getAllCatalogComplectations';

const result = selector({
    catalogConfigurationsSubtree: catalogSubtreeMock,
    cardGroupComplectations: cardGroupComplectationsMock,
});

it('группирует комплектации по имени', () => {
    const complectations = result.map(({ name, tech_params: techParams }) => ({
        name: name,
        tech_params: techParams.map((param) => param.human_name),
    }));
    expect(complectations).toMatchSnapshot();
});

it('добавляет к модификациям список опций', () => {
    const optionsToCheck = no.jpath('.{ .name === "Active" }[0].tech_params{ .human_name === "1.6 AT (110 л.с.)" }[0].options', result);

    expect(optionsToCheck).toMatchSnapshot();
});

it('добавляет к модификациям кол-во опций', () => {
    const optionsCountToCheck = no.jpath('.{ .name === "Active" }[0].tech_params{ .human_name === "1.6 AT (110 л.с.)" }[0].option_count', result);

    expect(optionsCountToCheck).toBe(8);
});

it('добавляет к модификациям цену если есть', () => {
    const priceToCheck = no.jpath('.{ .name === "Active" }[0].tech_params{ .human_name === "1.6 AT (110 л.с.)" }[0].price_from', result);

    expect(priceToCheck).toBe(1031505);

    const noPriceToCheck = no.jpath('.{ .name === "Laurin & Klement" }[0].tech_params{ .human_name === "1.8 MT (180 л.с.)" }[0].price_from', result);

    expect(noPriceToCheck).toBeUndefined();
});
