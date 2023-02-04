const getSideImages = require('./getSideImages');
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

it('Должен отдать боковые картинки, если они есть', () => {
    const offerMockWithSideImage = {
        ...offerMock,
        state: {
            ...offerMock.state,
            image_urls: [
                {
                    ...offerMock.state.image_urls[0],
                    photo_class: 'AUTO_VIEW_3_4_BACK_LEFT',
                },
            ],
        },
    };
    expect(getSideImages(offerMockWithSideImage)).toHaveLength(1);
});

it('Не должен отдать боковые картинки, если их нет', () => {
    expect(getSideImages(offerMock)).toStrictEqual([]);
});
