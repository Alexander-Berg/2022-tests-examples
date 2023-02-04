const React = require('react');
const renderer = require('react-test-renderer');

const TESTS = [
    // eslint-disable-next-line max-len
    'Еще много мелочей о которых писать не буду, все расскажу по телефону или при осмотре. Бортжурнал на Drive2.ru —https://www.drive2.ru/r/mitsubishi/1627761/. Причина продажи — покупка автомобиля поменьше. Для постановки на учет отдам оригинальные фары, без дефектов.',
    'shdfakj hadjkshfshaf h Бортжурнал на Drive2.ru — https://www.drive2.ru/r/selfmade/4035225266124009669/',
    'привет https://www.drive2.ru/r/selfmade/4035225266124009669/,\nтексты',
    'www.drive2.ru/r/selfmade/4035225266124009669/ привет',
    'тест drive2.ru/r/selfmade/4035225266124009669/ привет',
    'тест (https://drive2.ru/r/selfmade/4035225266124009669/) привет',
];

const CardDescriptionHTML = require('./CardDescriptionHTML');

describe('desktop (наше решение)', () => {
    it('правильно переносит строки', () => {
        const tree = renderer.create(
            <CardDescriptionHTML description={ 'test\ntest\n' }/>,
        ).toJSON();
        expect(tree).toMatchSnapshot();
    });

    describe('выделяет ссылки на drive2', () => {
        TESTS.forEach((testCase) => {
            it(`должен распарсить "${ testCase }"`, () => {
                const tree = renderer.create(
                    <CardDescriptionHTML description={ testCase }/>,
                ).toJSON();
                expect(tree).toMatchSnapshot();
            });
        });
    });

    describe('выделяет ссылки на mirrent.ru', () => {
        const TESTS = [
            'руководитель компании по аренде спортивных и эксклюзивных автомобилей mirrent.ru, выкупил болид',
        ];

        TESTS.forEach((testCase) => {
            it(`должен распарсить "${ testCase }"`, () => {
                const tree = renderer.create(
                    <CardDescriptionHTML description={ testCase }/>,
                ).toJSON();
                expect(tree).toMatchSnapshot();
            });
        });
    });

    describe('не выделяет ссылки на somelink.com', () => {
        const TESTS = [
            'ссылку somelink.com не надо делать ссылкой в описании',
        ];

        TESTS.forEach((testCase) => {
            it(`должен распарсить "${ testCase }"`, () => {
                const tree = renderer.create(
                    <CardDescriptionHTML description={ testCase }/>,
                ).toJSON();
                expect(tree).toMatchSnapshot();
            });
        });
    });
});

describe('mobile (linkifyjs)', () => {
    it('правильно переносит строки', () => {
        const tree = renderer.create(
            <CardDescriptionHTML isMobile description={ 'test\ntest\n' }/>,
        ).toJSON();
        expect(tree).toMatchSnapshot();
    });

    describe('выделяет ссылки на drive2', () => {
        TESTS.forEach((testCase) => {
            it(`должен распарсить "${ testCase }"`, () => {
                const tree = renderer.create(
                    <CardDescriptionHTML isMobile description={ testCase }/>,
                ).toJSON();
                expect(tree).toMatchSnapshot();
            });
        });
    });

    describe('выделяет ссылки на mirrent.ru', () => {
        const TESTS = [
            'руководитель компании по аренде спортивных и эксклюзивных автомобилей mirrent.ru, выкупил болид',
        ];

        TESTS.forEach((testCase) => {
            it(`должен распарсить "${ testCase }"`, () => {
                const tree = renderer.create(
                    <CardDescriptionHTML isMobile description={ testCase }/>,
                ).toJSON();
                expect(tree).toMatchSnapshot();
            });
        });
    });

    describe('не выделяет ссылки на somelink.com', () => {
        const TESTS = [
            'ссылку somelink.com не надо делать ссылкой в описании',
        ];

        TESTS.forEach((testCase) => {
            it(`должен распарсить "${ testCase }"`, () => {
                const tree = renderer.create(
                    <CardDescriptionHTML isMobile description={ testCase }/>,
                ).toJSON();
                expect(tree).toMatchSnapshot();
            });
        });
    });
});
