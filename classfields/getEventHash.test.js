const getEventHash = require('./getEventHash');
const mock = require('auto-core/react/dataDomain/walkIn/mocks/withData.mock');

const event = mock.walkIn.eventsList.events[0];

it('должен правильно собирать хэш', () => {
    expect(getEventHash(event)).toEqual(
        '2012-12-14-Apple-Новый Audi TTС пробегом Nissan Teana-Audi TTNissan Teana',
    );
});
