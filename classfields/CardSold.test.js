'use strict';

const React = require('react');
const renderer = require('react-test-renderer');

const CardSold = require('./CardSold');

const HOUR = 60 * 60 * 1000;
const DAY = 24 * HOUR;

let offer;
beforeEach(() => {
    offer = {
        additional_info: {
            creation_date: Date.now() - 20 * DAY,
            is_owner: false,
        },
        recall_info: {
            recall_timestamp: String(Date.now() - 20 * DAY + 3 * HOUR),
        },
        section: 'used',
        status: 'INACTIVE',
    };
});

it('должен отрендерится для проданного объявления и показать время, если объявление продавалось меньше 10 дней', () => {
    const tree = renderer.create(
        <CardSold offer={ offer }/>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('должен отрендерится для проданного объявления и не показать время, если объявление продавалось больше 10 дней', () => {
    offer.recall_info.recall_timestamp += String(Number(offer.recall_info.recall_timestamp) + 11 * DAY);

    const tree = renderer.create(
        <CardSold offer={ offer }/>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('должен отрендерится для проданного объявления и не показать время, если объявление о продаже нового авто', () => {
    offer.section = 'new';

    const tree = renderer.create(
        <CardSold offer={ offer }/>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('не должен рендерится для владельца', () => {
    offer.additional_info.is_owner = true;
    const tree = renderer.create(
        <CardSold offer={ offer }/>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('не должен рендерится для активного объявления', () => {
    offer.status = 'ACTIVE';
    const tree = renderer.create(
        <CardSold offer={ offer }/>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('должен учитывать род продаваемого транспорта', () => {
    offer.vehicle_info = { truck_category: 'AGRICULTURAL' };
    const tree = renderer.create(
        <CardSold offer={ offer }/>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});
