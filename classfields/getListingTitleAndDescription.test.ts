import getListingTitleAndDescription from './getListingTitleAndDescription';

it('только с названием', () => {
    const result = getListingTitleAndDescription('BMW');

    expect(result).toEqual({
        description: 'В Журнале Авто.ру собраны все материалы авторынка про BMW - последние новости, обзоры экспертов, тест-драйвы и последние релизы',
        title: 'BMW - экспертные статьи и новости авторынка в Журнале Авто.ру',
    });
});

describe('с названием и подсекцией', () => {
    it('рубрика', () => {
        const result = getListingTitleAndDescription('Новости', { type: 'category', title: 'Игры', urlPart: 'games' });

        expect(result).toEqual({
            description: 'Самые свежие материалы из рубрики Новости Журнала Авто.ру по теме Игры',
            title: 'Новости по теме Игры в Журнале Авто.ру',
        });
    });

    it('тег', () => {
        const result = getListingTitleAndDescription('Будущее', { type: 'tag', title: 'BMW', urlPart: 'bmw' });

        expect(result).toEqual({
            description: 'Самые свежие материалы из тега Будущее Журнала Авто.ру по теме BMW',
            title: 'Будущее по теме BMW в Журнале Авто.ру',
        });
    });
});
