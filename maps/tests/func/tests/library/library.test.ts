import {openStudio, clickToSelector, clickAndNavigate, waitForSelector} from '../../utils/commands';
import {SELECTORS} from './selectors';

describe('Библиотека', () => {
    const commonSelectors = [
        SELECTORS.backButton,
        SELECTORS.librarySelector,
        SELECTORS.searchControl.input,
        SELECTORS.itemsCounter,
        SELECTORS.sortControl,
        SELECTORS.tagsPanel
    ];
    beforeEach(async () => {
        await openStudio();
        await clickAndNavigate(SELECTORS.studioPage.libraryLink);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-59
    test('Библиотека. Цвета', () => {
        return Promise.all([...commonSelectors, SELECTORS.color.container].map(waitForSelector));
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-78
    test('Библиотека. Изображения', async () => {
        await clickToSelector(SELECTORS.imageLibraryButton);

        return Promise.all([...commonSelectors, SELECTORS.figmaButton, SELECTORS.image.container].map(waitForSelector));
    });
});
