import * as qs from 'qs';
import * as Url from 'url';
import {stringToPoint, arePointsEqual, Point} from 'yandex-geo-utils';
import {
    openStudio,
    waitForSelector,
    setValueToInput,
    clickToSelector,
    getInputValue,
    getUrl,
    pressKey
} from '../../utils/commands';

const SELECTORS = {
    input: '.search-input input',
    inputClearButton: '.search-input .input__clear',
    suggestItem: '.search-suggest-view .search-suggest-view__suggest'
};

const TOPONYM_SEARCH_INPUT_EN = 'Moscow';
const TOPONYM_SEARCH_INPUT_RU = 'Екатеринбург';
const BUSINESS_SEARCH_INPUT_EN = "McDonald's Novy Arbat 5";
const BUSINESS_SEARCH_INPUT_RU = 'Макдоналдс Новый Арбат 5';
const TOLERANCE = 0.0001;

describe('[A] Поиск мест на карте', () => {
    beforeEach(() => openStudio());

    test('Строка поиска', () => {
        return waitForSelector(SELECTORS.input);
    });

    test('Удаление значения поисковой строки ко клику в крестик', async () => {
        await setValueToInput(SELECTORS.input, TOPONYM_SEARCH_INPUT_EN);
        await clickToSelector(SELECTORS.inputClearButton);
        const inputValue = await getInputValue(SELECTORS.input);

        expect(inputValue).toHaveLength(0);
    });

    describe.each<[string, Point]>([
        [TOPONYM_SEARCH_INPUT_EN, [37.385534, 55.584227]],
        [TOPONYM_SEARCH_INPUT_RU, [60.475065, 56.788751]],
        [BUSINESS_SEARCH_INPUT_EN, [37.598043, 55.752607]],
        [BUSINESS_SEARCH_INPUT_RU, [37.598043, 55.752607]]
    ])('Поиск топонима | организации', (input: string, coords: Point) => {
        test(input, async () => {
            await setValueToInput(SELECTORS.input, input);
            await pressKey('Enter');
            await clickToSelector(SELECTORS.suggestItem);

            expect(arePointsEqual(coords, getUrlCoordiates(), TOLERANCE)).toBeTruthy();
        });
    });
});

function getUrlCoordiates(): Point | null {
    const url = new Url.URL(getUrl());
    const query = qs.parse(url.search, {ignoreQueryPrefix: true});

    return stringToPoint(query.ll);
}
