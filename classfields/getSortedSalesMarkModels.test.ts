import markModelsMock from '../mocks/markModels.mock';

import getSortedSalesMarkModels from './getSortedSalesMarkModels';

it(`должен правильно сортировать марки-модели по human_name марки`, () => {
    const state = {
        salesMarkModels: markModelsMock,
    };

    const sortedMarkModels = getSortedSalesMarkModels(state);

    expect(sortedMarkModels).toMatchSnapshot();
});
