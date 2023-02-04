import Color from 'color';
import {
    openPage,
    openStudio,
    getElementText,
    removeInputValue,
    setValueToInput,
    clickAndNavigate,
    waitForSelector,
    getSelectorCount,
    clickToSelector,
    acceptOnDialog,
    getElementsText,
    TEST_DATA
} from '../../utils/commands';
import {SELECTORS} from './selectors';

describe('Библиотека. Цвета.', () => {
    beforeEach(() =>
        openPage('/library', {
            service: TEST_DATA.service,
            branch: TEST_DATA.branch,
            revision: TEST_DATA.revision
        })
    );

    // https://testpalm.yandex-team.ru/testcase/gryadka-60
    test('Добавление hex-цвета', async () => {
        const newColorName = String(Date.now());
        await clickAndNavigate(SELECTORS.addNewButton);
        await waitForSelector(SELECTORS.colorEditPanel.container);

        await Promise.all(
            [
                SELECTORS.backButton,
                SELECTORS.headerTitle,
                SELECTORS.tagButton,
                SELECTORS.colorPicker.basicHexInput,
                SELECTORS.colorPicker.nightHexInput,
                SELECTORS.colorPicker.basicDesignPreview,
                SELECTORS.colorPicker.nightDesignPreview,
                SELECTORS.colorEditPanel.saveInput,
                SELECTORS.colorEditPanel.saveButton
            ].map(waitForSelector)
        );

        const colorParts = [
            ['#3355ff', SELECTORS.colorPicker.basicHexInput, SELECTORS.colorPicker.basicDesignPreview],
            ['#ff5533', SELECTORS.colorPicker.nightHexInput, SELECTORS.colorPicker.nightDesignPreview]
        ];

        await Promise.all(colorParts.map(checkColorUpdate));

        await setValueToInput(SELECTORS.colorEditPanel.saveInput, newColorName);
        await clickAndNavigate(SELECTORS.colorEditPanel.saveButton);

        await setValueToInput(SELECTORS.searchControl.input, newColorName);

        const colorsCount = await getSelectorCount(SELECTORS.color.container);

        expect(colorsCount === 1);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-62
    test('Добавление цвета с названием-дублем', async () => {
        await waitForSelector(SELECTORS.list);
        const existingColorName = await getElementText(SELECTORS.color.title);
        if (!existingColorName) {
            throw new Error('Color name can not be empty.');
        }
        await clickAndNavigate(SELECTORS.addNewButton);
        await waitForSelector(SELECTORS.colorEditPanel.container);
        await setValueToInput(SELECTORS.colorEditPanel.saveInput, existingColorName);

        return Promise.all(
            [SELECTORS.colorEditPanel.error, SELECTORS.colorEditPanel.saveButtonDisabled].map(waitForSelector)
        );
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-71
    test('Контекстное меню', async () => {
        await clickToSelector(SELECTORS.color.container, {button: 'right'});

        return Promise.all(Object.values(SELECTORS.contextMenu).map(waitForSelector));
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-73
    test('Удаление через контекстное меню', async () => {
        const unusedColorId = 'carparks_line_100_199';
        const colorsCount = await getColorsCount();

        await removeInputValue(SELECTORS.searchControl.input);
        await setValueToInput(SELECTORS.searchControl.input, unusedColorId);

        await waitForSelector(SELECTORS.color.unused);
        await clickToSelector(SELECTORS.color.unused, {button: 'right'});
        await clickToSelector(SELECTORS.contextMenu.removeButton);

        await removeInputValue(SELECTORS.searchControl.input);
        const newColorsCount = await getColorsCount();

        expect(newColorsCount).toEqual(colorsCount - 1);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-77
    test('Удаление используемого цвета', async () => {
        acceptOnDialog();
        const colorsCount = await getColorsCount();

        await clickToSelector(SELECTORS.color.used, {button: 'right'});
        await clickToSelector(SELECTORS.contextMenu.removeButton);

        const newColorsCount = await getColorsCount();

        expect(newColorsCount).toEqual(colorsCount);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-67
    describe('Поиск по названию', () => {
        test('Поиск существующего изображения', async () => {
            const searchString = 'metro';
            await removeInputValue(SELECTORS.searchControl.input);
            await setValueToInput(SELECTORS.searchControl.input, searchString);

            const colorViews = await getElementsText(SELECTORS.color.title);
            expect(colorViews.every((colorTitle) => colorTitle?.includes(searchString))).toBeTruthy();
        });

        test('Поиск несуществующего изображения', async () => {
            const searchString = 'not-exist' + Math.random();
            await removeInputValue(SELECTORS.searchControl.input);
            await setValueToInput(SELECTORS.searchControl.input, searchString);

            const colorViews = await getElementsText(SELECTORS.color.title);
            expect(colorViews).toHaveLength(0);
        });

        test('Очистка поисковой строки', async () => {
            const searchString = 'metro';
            await removeInputValue(SELECTORS.searchControl.input);
            const fullColorListLength = await getColorsCount();
            await setValueToInput(SELECTORS.searchControl.input, searchString);
            const partialColorListLength = await getColorsCount();
            expect(partialColorListLength).toBeLessThan(fullColorListLength);

            await removeInputValue(SELECTORS.searchControl.input);

            expect(await getColorsCount()).toEqual(fullColorListLength);
        });
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-75
    test('Создание дубликата', async () => {
        const colorView = await waitForSelector(SELECTORS.color.title);
        const colorTitle = await colorView.evaluate((node) => node.textContent);
        await colorView.click({button: 'right'});
        await clickToSelector(SELECTORS.contextMenu.duplicateButton);
        const colorTitles = await getElementsText(SELECTORS.color.title);
        const duplicateTileRegex = new RegExp(`${colorTitle}_copy_\\d+`);

        expect(colorTitles.some((title) => duplicateTileRegex.test(title!))).toBeTruthy();
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-66
    describe.each<[string, string, string, string]>([
        ['Day', '#3355ff', SELECTORS.colorPicker.basicHexInput, SELECTORS.colorPicker.basicDesignPreview],
        ['Night', '#ff5533', SELECTORS.colorPicker.nightHexInput, SELECTORS.colorPicker.nightDesignPreview]
    ])('Библиотека. Цвета. Редактирование параметра Day | Night', (mode: string, ...params: string[]) => {
        test(mode, async () => {
            await clickAndNavigate(SELECTORS.color.container);
            await waitForSelector(SELECTORS.colorEditPanel.container);
            await checkColorUpdate(params);
        });
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-97
    test('Использование нового цвета', async () => {
        const newColorName = String(Date.now());
        await openStudio();
        await clickAndNavigate(SELECTORS.studioPage.libraryLink);

        await clickAndNavigate(SELECTORS.addNewButton);
        await waitForSelector(SELECTORS.colorEditPanel.container);
        await setValueToInput(SELECTORS.colorEditPanel.saveInput, newColorName);
        await clickAndNavigate(SELECTORS.colorEditPanel.saveButton);
        await clickAndNavigate(SELECTORS.backButton);

        await waitForSelector(SELECTORS.studioPage.sidebarLeft);
        await clickToSelector(SELECTORS.studioPage.layerWithColor);
        await clickToSelector(SELECTORS.studioPage.tabWithColor);
        await clickToSelector(SELECTORS.studioPage.colorProperty.container);
        await waitForSelector(SELECTORS.studioPage.colorInputPopup.container);
        await clickToSelector(SELECTORS.studioPage.colorInputPopup.librarySelector);
        await setValueToInput(SELECTORS.studioPage.colorInputPopup.searchInput, newColorName);
        await clickToSelector(SELECTORS.studioPage.colorInputPopup.suggestItem);
        const colorPropertyText = await getElementText(SELECTORS.studioPage.colorProperty.text);

        expect(colorPropertyText === newColorName).toBeTruthy();
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-61
    test('CARTOGRAPH-1213: [A] Кейс: "Библиотека. Цвета. Добавление rgba-цвета"', async () => {
        const newColorName = 'unique-rgb-color' + Date.now();
        await clickAndNavigate(SELECTORS.addNewButton);
        await waitForSelector(SELECTORS.colorEditPanel.container);

        const colorParts: [number[], string[], string][] = [
            [
                [253, 230, 5, 50],
                [
                    SELECTORS.colorPicker.basicRGBARedInput,
                    SELECTORS.colorPicker.basicRGBAGreenInput,
                    SELECTORS.colorPicker.basicRGBABlueInput,
                    SELECTORS.colorPicker.basicRGBAAlphaInput
                ],
                SELECTORS.colorPicker.basicDesignPreview
            ],
            [
                [5, 255, 100, 30],
                [
                    SELECTORS.colorPicker.nightRGBARedInput,
                    SELECTORS.colorPicker.nightRGBAGreenInput,
                    SELECTORS.colorPicker.nightRGBABlueInput,
                    SELECTORS.colorPicker.nightRGBAAlphaInput
                ],
                SELECTORS.colorPicker.nightDesignPreview
            ]
        ];
        for (let i = 0; i < colorParts.length; i++) {
            await checkRGBColorUpdate(colorParts[i]);
        }

        await setValueToInput(SELECTORS.colorEditPanel.saveInput, newColorName);
        await clickAndNavigate(SELECTORS.colorEditPanel.saveButton);

        await setValueToInput(SELECTORS.searchControl.input, newColorName);

        const colorsCount = await getSelectorCount(SELECTORS.color.container);

        expect(colorsCount === 1);
    });
});

async function checkColorUpdate([newValue, input, preview]: string[]): Promise<void> {
    await setValueToInput(input, newValue);
    await page.keyboard.press('Enter');

    const colorPreview = await waitForSelector(preview);
    const colorPreviewValue = await colorPreview.evaluate((node) => (node as HTMLElement).style.backgroundColor);

    expect(new Color(newValue).hex() === new Color(colorPreviewValue).hex());
}

async function getColorsCount(): Promise<number> {
    const counterSelectorText = await getElementText(SELECTORS.itemsCounter);
    if (!counterSelectorText) {
        throw new Error('Counter can not be empty.');
    }

    return parseInt(counterSelectorText, 10);
}

async function checkRGBColorUpdate([color, inputs, preview]: [number[], string[], string]): Promise<void> {
    for (let i = 0; i < inputs.length; i++) {
        await setValueToInput(inputs[i], String(color[i]));
    }

    const colorPreview = await waitForSelector(preview);
    const colorPreviewValue = await colorPreview.evaluate((node) => (node as HTMLElement).style.backgroundColor);

    expect(Color.rgb(color).hex() === new Color(colorPreviewValue).hex());
}
