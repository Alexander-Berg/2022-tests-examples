import {openPage, clickToSelector, waitForSelector, setValueToInput, TEST_DATA} from '../../utils/commands';

const SELECTORS = {
    backButton: '.header__back',
    title: '.header__title',
    commitMessageInput: '.save__field .save__input .input__control',
    commitButton: {
        active: '.save__field .button:not(._disabled)',
        disabled: '.save__field .button._disabled'
    },
    reportButton: '.report-button-view',
    errorReportButton: {
        active: '.save__error-report .button:not(._disabled)',
        disabled: '.save__error-report .button._disabled'
    },
    errorNotification: '.notification-item-view._type_error'
};

describe('Save page', () => {
    beforeEach(() =>
        openPage('/save', {
            service: TEST_DATA.service,
            branch: TEST_DATA.branch,
            revision: TEST_DATA.revision
        })
    );

    // https://testpalm.yandex-team.ru/testcase/gryadka-25
    test('should show save page', () => {
        return Promise.all(
            [
                SELECTORS.backButton,
                SELECTORS.title,
                SELECTORS.commitMessageInput,
                SELECTORS.commitButton.disabled,
                SELECTORS.reportButton
            ].map(waitForSelector)
        );
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-243
    test.skip('should send bugreport', async () => {
        await waitForSelector(SELECTORS.commitButton.disabled);

        await page.setOfflineMode(true);
        await setValueToInput(SELECTORS.commitMessageInput, 'Test commit');
        await clickToSelector(SELECTORS.commitButton.active);
        await page.setOfflineMode(false);

        await waitForSelector(SELECTORS.errorNotification);
        await clickToSelector(SELECTORS.errorReportButton.active);
        await waitForSelector(SELECTORS.errorReportButton.disabled);
    });
});
