import * as path from 'path';
import {
    openPage,
    openStudio,
    clickToSelector,
    waitForSelector,
    setValueToInput,
    getElementsText,
    getSelectorCount,
    getElementText,
    waitForRemoved,
    clickAndNavigate,
    clickAndWaitForRequest,
    hoverOverSelector,
    TEST_DATA
} from '../../utils/commands';
import {SELECTORS} from '../library/selectors';

const SEARCH_STRING = 'poi_';
const UPLOAD_FILE_NAME = 'upload-file.svg';
const ANOTHER_UPLOAD_FILE_NAME = 'another-upload-file.svg';
const DAY_UPLOAD_FILE_NAME = 'upload-file_day.svg';
const NIGHT_UPLOAD_FILE_NAME = 'upload-file_night.svg';
const WRONG_FORMAT_FILE_NAME = 'wrong-format-file.png';

const filesDir = path.join(__dirname, 'files');

describe('[A] Кейс "Библиотека. Изображения.', () => {
    beforeEach(async () => {
        await openPage('/library', {
            service: TEST_DATA.service,
            branch: TEST_DATA.branch,
            revision: TEST_DATA.revision
        });
        await clickToSelector(SELECTORS.imageLibraryButton);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-87
    test('Контекстное меню', async () => {
        await clickToSelector(SELECTORS.image.container, {button: 'right'});

        return waitForSelector(SELECTORS.contextMenu.container);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-92
    test('Удаление через контекстное меню', async () => {
        const imagesCount = parseInt(await getElementText(SELECTORS.itemsCounter), 10);
        await clickToSelector(SELECTORS.image.container, {button: 'right'});
        await waitForSelector(SELECTORS.contextMenu.container);
        await clickToSelector(SELECTORS.contextMenu.removeButton);
        await waitForRemoved(SELECTORS.contextMenu.container);
        const newImagesCount = parseInt(await getElementText(SELECTORS.itemsCounter), 10);

        expect(newImagesCount === imagesCount - 1).toBeTruthy();
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-93
    test('Удаление по кнопке', async () => {
        const imagesCount = parseInt(await getElementText(SELECTORS.itemsCounter), 10);
        await clickToSelector(SELECTORS.image.container);
        await clickAndNavigate(SELECTORS.imageEditPanel.deleteButton);
        const newImagesCount = parseInt(await getElementText(SELECTORS.itemsCounter), 10);

        expect(newImagesCount === imagesCount - 1).toBeTruthy();
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-91
    test('Поиск по названию', async () => {
        await setValueToInput(SELECTORS.searchControl.input, SEARCH_STRING);
        const names = await getElementsText(SELECTORS.image.name);

        expect(names.every((name) => name.includes(SEARCH_STRING))).toBeTruthy();
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-81
    test('Добавление изображений', async () => {
        await addNewFiles(UPLOAD_FILE_NAME);
        await clickAndNavigate(SELECTORS.imageUploadPanel.saveButton);
        await setValueToInput(SELECTORS.searchControl.input, getFileName(UPLOAD_FILE_NAME));
        const imagesCount = await getSelectorCount(SELECTORS.image.container);

        expect(imagesCount >= 1).toBeTruthy();
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-83
    test('Добавление. Отмена', async () => {
        await addNewFiles(UPLOAD_FILE_NAME);
        await clickAndNavigate(SELECTORS.imageUploadPanel.backButton);
        await setValueToInput(SELECTORS.searchControl.input, getFileName(UPLOAD_FILE_NAME));
        const imagesCount = await getSelectorCount(SELECTORS.image.container);

        expect(imagesCount === 0).toBeTruthy();
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-85
    test('Добавление изображения с названием-дублем', async () => {
        await addNewFiles(UPLOAD_FILE_NAME);
        await clickAndNavigate(SELECTORS.imageUploadPanel.saveButton);
        await addNewFiles(UPLOAD_FILE_NAME);
        await clickToSelector(SELECTORS.imageUploadPanel.saveButton);

        await waitForSelector(SELECTORS.ranameDialog.container);
        await clickToSelector(SELECTORS.ranameDialog.cancelButton);
        await waitForRemoved(SELECTORS.ranameDialog.container);

        await clickToSelector(SELECTORS.imageUploadPanel.saveButton);
        await waitForSelector(SELECTORS.ranameDialog.container);
        await clickAndNavigate(SELECTORS.ranameDialog.replaceButton);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-84
    test('Удаление из загрузки', async () => {
        const imagesCount = parseInt(await getElementText(SELECTORS.itemsCounter), 10);
        await addNewFiles(UPLOAD_FILE_NAME, ANOTHER_UPLOAD_FILE_NAME);
        await clickToSelector(SELECTORS.imageUploadPanel.deleteUploadableButton);
        await clickAndNavigate(SELECTORS.imageUploadPanel.saveButton);
        const newImagesCount = parseInt(await getElementText(SELECTORS.itemsCounter), 10);

        expect(newImagesCount - 1 === imagesCount).toBeTruthy();
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-99
    test('Добавление дневной и ночной иконки', async () => {
        await addNewFiles(DAY_UPLOAD_FILE_NAME);

        await waitForSelector(SELECTORS.imageUploadPanel.warning);
        await clickToSelector(SELECTORS.imageUploadPanel.saveButton);
        await waitForSelector(SELECTORS.errorNotification);

        await pickFiles(NIGHT_UPLOAD_FILE_NAME);
        await clickAndNavigate(SELECTORS.imageUploadPanel.saveButton);
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-95
    test('Добавление. Недопустимый формат', async () => {
        await clickAndNavigate(SELECTORS.addNewButton);
        await pickFiles(WRONG_FORMAT_FILE_NAME);
        await waitForSelector(SELECTORS.errorNotification);
    });

    test('Редактирование имени', async () => {
        const uniqueName = Date.now();
        await clickToSelector(SELECTORS.image.container);
        await waitForSelector(SELECTORS.imageEditPanel.container);
        await setValueToInput(SELECTORS.imageEditPanel.saveInput, String(uniqueName));
        await clickAndNavigate(SELECTORS.imageEditPanel.saveButton);
        await waitForSelector(SELECTORS.imageLibraryButton);
        await setValueToInput(SELECTORS.searchControl.input, String(uniqueName));
        const imagesCount = await getSelectorCount(SELECTORS.image.container);

        expect(imagesCount >= 1).toBeTruthy();
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-90
    // https://testpalm.yandex-team.ru/testcase/gryadka-89
    describe.each<string[]>([
        ['Day', SELECTORS.imageEditPanel.dayUpdateInput],
        ['Night', SELECTORS.imageEditPanel.nightUpdateInput]
    ])('Редактирование параметра Day | Night', (mode: string, input: string) => {
        test(mode, async () => {
            await clickToSelector(SELECTORS.image.container);
            await waitForSelector(SELECTORS.imageEditPanel.container);
            const fileInput = await waitForSelector(input);
            await fileInput.uploadFile(path.join(filesDir, UPLOAD_FILE_NAME));
            await clickAndNavigate(SELECTORS.imageEditPanel.saveButton);
        });
    });

    describe('Загрузка из Фигмы', () => {
        beforeEach(async () => {
            await clickAndNavigate(SELECTORS.figmaButton);
            await waitForSelector(SELECTORS.indexPage.container);
            await setValueToInput(SELECTORS.indexPage.searchInput, TEST_DATA.branchName);
            await hoverOverSelector(SELECTORS.indexPage.userBranch);
            await waitForSelector(SELECTORS.indexPage.branchSpinner);
            await waitForSelector(SELECTORS.indexPage.branchStopFigmaButton);
        });

        // https://testpalm.yandex-team.ru/testcase/gryadka-270
        test('Успешная загрузка', async () => {
            await waitForRemoved(SELECTORS.indexPage.branchStopFigmaButton);
            await waitForRemoved(SELECTORS.indexPage.branchSpinner);
            await waitForSelector(SELECTORS.infoNotification);
        });

        // https://testpalm.yandex-team.ru/testcase/gryadka-271
        test('Остановка загрузки из Фигмы', async () => {
            await clickAndWaitForRequest(SELECTORS.indexPage.branchStopFigmaButton, (req) =>
                /\/api\/stopFigma/.test(req.url())
            );
            await waitForRemoved(SELECTORS.indexPage.branchStopFigmaButton);
            await waitForRemoved(SELECTORS.indexPage.branchSpinner);
            await waitForSelector(SELECTORS.infoNotification);
        });
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-98
    test('Использование нового изображения', async () => {
        const newImageName = getFileName(UPLOAD_FILE_NAME);
        await openStudio();
        await clickAndNavigate(SELECTORS.studioPage.libraryLink);
        await clickToSelector(SELECTORS.imageLibraryButton);

        await addNewFiles(UPLOAD_FILE_NAME);
        await clickAndNavigate(SELECTORS.imageUploadPanel.saveButton);
        await clickAndNavigate(SELECTORS.backButton);

        await waitForSelector(SELECTORS.studioPage.sidebarLeft);
        await clickToSelector(SELECTORS.studioPage.layerWithImage);
        await clickToSelector(SELECTORS.studioPage.tabWithImage);
        await clickToSelector(SELECTORS.studioPage.imageProperty.container);
        await waitForSelector(SELECTORS.studioPage.imageInputPopup.container);
        await setValueToInput(SELECTORS.studioPage.imageInputPopup.searchInput, newImageName);
        await clickToSelector(SELECTORS.studioPage.imageInputPopup.suggestItem);
        const imagePropertyText = await getElementText(SELECTORS.studioPage.imageProperty.text);

        expect(imagePropertyText === newImageName).toBeTruthy();
    });
});

async function pickFiles(...filePaths: string[]): Promise<void> {
    await waitForSelector(SELECTORS.fileUploader.container);
    const fileInput = await waitForSelector(SELECTORS.fileUploader.input);
    await fileInput.uploadFile(...filePaths.map((filePath) => path.join(filesDir, filePath)));
}

async function addNewFiles(...filePaths: string[]): Promise<void> {
    await clickAndNavigate(SELECTORS.addNewButton);
    await pickFiles(...filePaths);
    await waitForSelector(SELECTORS.imageUploadPanel.container);
}

function getFileName(filePath: string): string {
    return path.parse(filePath).name;
}
