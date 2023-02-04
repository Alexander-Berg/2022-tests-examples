import no from 'nommon';
import _ from 'lodash';

import getAllCatalogComplectations from 'auto-core/react/dataDomain/catalogConfigurationsSubtree/selectors/getAllCatalogComplectations';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import catalogSubtreeMock from 'auto-core/react/dataDomain/catalogConfigurationsSubtree/mocks/subtree';
import cardGroupComplectationsMock from 'auto-core/react/dataDomain/cardGroupComplectations/mocks/complectations';

import { prepareEquipment } from './utils';

const complectations = getAllCatalogComplectations({
    catalogConfigurationsSubtree: catalogSubtreeMock,
    cardGroupComplectations: cardGroupComplectationsMock,
});

const result = prepareEquipment(complectations, equipmentDictionaryMock.data);

describe('правильно формирует флаги отображения', () => {
    it('если опция есть во всех модификациях', () => {
        // eslint-disable-next-line no-restricted-properties
        const optionToCheck = no.jpath('.{ .groupName === "Комфорт" }[0].options{ .code === "condition" }[0].flags{ .name === "Active" }[0]', result);

        expect(optionToCheck).toMatchSnapshot();
    });

    it('если опции нет в одной из модификаций', () => {
        // eslint-disable-next-line no-restricted-properties
        const optionToCheck = no.jpath('.{ .groupName === "Комфорт" }[0].options{ .code === "computer" }[0].flags{ .name === "Active" }[0]', result);

        expect(optionToCheck).toMatchSnapshot();
    });

    it('если опции нет в одной из модификаций, а в другой она платная', () => {
        // eslint-disable-next-line no-restricted-properties
        const optionToCheck = no.jpath('.{ .groupName === "Комфорт" }[0].options{ .code === "condition" }[0].flags{ .name === "Ambition" }[0]', result);

        expect(optionToCheck).toMatchSnapshot();
    });

    it('если опция платная во всех модификациях', () => {
        // eslint-disable-next-line no-restricted-properties
        const optionToCheck = no.jpath('.{ .groupName === "Комфорт" }[0].options{ .code === "condition" }[0].flags{ .name === "Hockey Edition" }[0]', result);

        expect(optionToCheck).toMatchSnapshot();
    });
});

it('добавляет в список только опции, которые есть в комплектациях', () => {
    const optionsList = _(result).map(({ options }) => _.map(options, 'code')).flatten().value();
    expect(optionsList).toMatchSnapshot();
});

it('группирует опции согласно словарю', () => {
    const optionsGroup = _.map(result, (group) => ({ groupName: group.groupName, options: _.map(group.options, 'code') }));
    expect(optionsGroup).toMatchSnapshot();
});
