const snippetState = require('./index');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const carsOfferMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const lcvOfferMock = require('auto-core/react/dataDomain/card/mocks/card.lcv.mock');
const motorcycleOfferMock = require('auto-core/react/dataDomain/card/mocks/card.motorcycle.mock');
const availabilityDict = require('auto-core/data/offer/availability.json');

it('возвращает полный набор параметров для бу легковых', () => {
    const offer = cloneOfferWithHelpers(carsOfferMock)
        .withAvailability(availabilityDict.ON_ORDER)
        .withIsBeaten(true)
        .withIsCustomCleared(false)
        .withIsWheelLeft(false)
        .value();

    expect(snippetState(offer)).toMatchSnapshot();
});

it('возвращает полный набор параметров для новых легковых', () => {
    const offer = cloneOfferWithHelpers(carsOfferMock)
        .withSection('new')
        .withAvailability(availabilityDict.ON_ORDER)
        .withIsBeaten(true)
        .withIsCustomCleared(false)
        .withIsWheelLeft(false)
        .value();

    expect(snippetState(offer)).toMatchSnapshot();
});

it('параметры для легкого ком тс будут содержать поле "Руль"', () => {
    const offer = cloneOfferWithHelpers(lcvOfferMock)
        .withAvailability(availabilityDict.ON_ORDER)
        .withIsBeaten(true)
        .withIsCustomCleared(false)
        .withIsWheelLeft(false)
        .withSubCategory('lcv')
        .value();

    expect(snippetState(offer).steeringWheel).toBe('Правый');
});

it('параметры для грузовиков будут содержать поле "Руль"', () => {
    const offer = cloneOfferWithHelpers(lcvOfferMock)
        .withAvailability(availabilityDict.ON_ORDER)
        .withIsBeaten(true)
        .withIsCustomCleared(false)
        .withIsWheelLeft(false)
        .withSubCategory('truck')
        .value();

    expect(snippetState(offer).steeringWheel).toBe('Правый');
});

it('параметры для сх техники не будут содержать поле "Руль"', () => {
    const offer = cloneOfferWithHelpers(lcvOfferMock)
        .withAvailability(availabilityDict.ON_ORDER)
        .withIsBeaten(true)
        .withIsCustomCleared(false)
        .withIsWheelLeft(false)
        .withSubCategory('agricultural')
        .value();

    expect(snippetState(offer).steeringWheel).toBe(false);
});

it('параметры для мото не будут содержать поле "Руль"', () => {
    const offer = cloneOfferWithHelpers(motorcycleOfferMock)
        .withAvailability(availabilityDict.ON_ORDER)
        .withIsBeaten(true)
        .withIsCustomCleared(false)
        .withIsWheelLeft(false)
        .value();

    expect(snippetState(offer).steeringWheel).toBe(false);
});
