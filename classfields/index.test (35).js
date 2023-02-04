jest.mock('lodash', () => ({ shuffle: arr => arr }));

const getMagArticlesWithTabs = require('../getMagArticlesWithTabs');
const magMock = require('../../../mocks/magMordaS3MockWithTabs');

const params = {
    theme: 'dark',
    platform: 'desktop',
};

it('Должен вернуть div-json верстку журнала для дэсктопа с табами', () => {
    expect(getMagArticlesWithTabs(magMock, params)).toMatchSnapshot();
});

it('Должен вернуть div-json верстку журнала для дэсктопа с табами в экспе zen_new_desktop', () => {
    expect(getMagArticlesWithTabs(magMock, { ...params, zen_new_desktop: true })).toMatchSnapshot();
});

it('Должен вернуть div-json верстку журнала для дэсктопа с табами в экспе zen_new_touch', () => {
    expect(getMagArticlesWithTabs(magMock, { ...params, zen_new_touch: true })).toMatchSnapshot();
});
