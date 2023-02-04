import {
    openPage,
    waitForSelector,
    clickToSelector,
    clickAndNavigate,
    setValueToInput,
    getElementText,
    removeInputValue,
    waitForRemoved,
    getElementsText,
    clickOutside,
    hoverOverSelector
} from '../../utils/commands';

const SELECTORS = {
    studio: '.studio',
    spinner: '[class^=spinner-view]',
    serviceSelect: '.index__service button',
    serviceSelectOpened: '.index__service .menu',
    masterBranch: '.branch._master',
    userBranches: '.branch:not(._master)',
    masterBranchName: '.branch._master .branch__name',
    userBranchName: '.branch:not(._master) .branch__name',
    commitsMasterButton: '.branch._master .branch-button-view._commits',
    duplicateMasterButton: '.branch._master .branch-button-view._duplicate',
    duplicateUserButton: '.branch:not(._master) .branch-button-view._duplicate',
    createReleaseMasterButton: '.branch._master .branch-button-view._release',
    branchActionPanel: {
        container: '.branch-panel-template-veiw',
        input: '.branch-panel-template-veiw__body .input__control',
        button: {
            active: '.branch-panel-template-veiw__footer > button:not(._disabled)',
            disabled: '.branch-panel-template-veiw__footer > button._disabled'
        },
        close: '.branch-panel-template-veiw__header-close-button'
    },
    tabsSlider: '.tabs__slider',
    logo: '.index__logo',
    theme: '.index__theme',
    currentUser: '.user-view',
    filter: '.index__search .search-input-view .input__control',
    filterEmpty: '.index__search .search-input-view .input._empty .input__control',
    branchUser: '.branch .branch__user-icon',
    branchName: '.branch .branch__name',
    branchCommitMessage: '.branch .branch__commit-message',
    branchMeta: '.branch .branch__meta',
    branchTimestamp: '.branch .branch__timestamp'
};

describe('Index page', () => {
    beforeEach(() => openPage());

    // https://testpalm.yandex-team.ru/testcase/gryadka-1
    test('should show index page with branches', () => {
        const expectedIndexSelectors = [
            SELECTORS.logo,
            SELECTORS.theme,
            SELECTORS.currentUser,
            SELECTORS.filter,
            SELECTORS.masterBranch,
            SELECTORS.userBranches,
            SELECTORS.branchUser,
            SELECTORS.branchName,
            SELECTORS.branchCommitMessage,
            SELECTORS.branchMeta,
            SELECTORS.branchTimestamp
        ];
        return Promise.all(expectedIndexSelectors.map(waitForSelector));
    });

    test('should show service selector', async () => {
        await clickToSelector(SELECTORS.serviceSelect);

        return waitForSelector(SELECTORS.serviceSelectOpened);
    });

    test('should render master branch', () => {
        return waitForSelector(SELECTORS.masterBranch);
    });

    test('should render branches', () => {
        return waitForSelector(SELECTORS.userBranches);
    });

    describe.each<[string]>([[SELECTORS.duplicateMasterButton], [SELECTORS.createReleaseMasterButton]])(
        'master branch actions',
        (selector: string) => {
            test('should render action panel', async () => {
                await clickToSelector(selector);

                return waitForSelector(SELECTORS.branchActionPanel.container);
            });
        }
    );

    describe.each<[string, string]>([
        [SELECTORS.masterBranch, '/studio'],
        [SELECTORS.commitsMasterButton, '/branch']
    ])('master branch actions', (selector: string, expected: string) => {
        test(`should open ${expected} page`, async () => {
            return clickAndNavigate(selector);
        });
    });

    describe('Duplicate master branch.', () => {
        beforeEach(async () => {
            await clickToSelector(SELECTORS.duplicateMasterButton);
            await waitForSelector(SELECTORS.branchActionPanel.container);
            await removeInputValue(SELECTORS.branchActionPanel.input);
        });

        // https://testpalm.yandex-team.ru/testcase/gryadka-2
        test('button should be disabled', async () => {
            const masterBranchName = await getElementText(SELECTORS.masterBranchName);

            await setValueToInput(SELECTORS.branchActionPanel.input, masterBranchName!);
            await waitForSelector(SELECTORS.branchActionPanel.button.disabled);
        });

        test.skip('should duplicate master', async () => {
            const masterBranchName = await getElementText(SELECTORS.masterBranchName);
            // Ветка с таким именем не скопируется при повторной записи моков.
            const newBranchName = `${masterBranchName}-copy-1635429509638`;

            await setValueToInput(SELECTORS.branchActionPanel.input, newBranchName);
            await clickToSelector(SELECTORS.branchActionPanel.button.active);
            await waitForRemoved(SELECTORS.branchActionPanel.container);
            await setValueToInput(SELECTORS.filter, newBranchName);

            const branchesNames = await getElementsText(SELECTORS.branchName);
            expect(branchesNames).toContain(newBranchName);
        });
    });

    describe('Duplicate user branch.', () => {
        beforeEach(async () => {
            await hoverOverSelector(SELECTORS.userBranches);
            await clickToSelector(SELECTORS.duplicateUserButton);
            await waitForSelector(SELECTORS.branchActionPanel.container);
        });

        // https://testpalm.yandex-team.ru/testcase/gryadka-21
        test('button should be disabled', async () => {
            const userBranchName = await getElementText(SELECTORS.userBranchName);

            await removeInputValue(SELECTORS.branchActionPanel.input);
            await setValueToInput(SELECTORS.branchActionPanel.input, userBranchName!);
            await waitForSelector(SELECTORS.branchActionPanel.button.disabled);
        });

        // https://testpalm.yandex-team.ru/testcase/gryadka-3
        test.skip('should duplicate user branch', async () => {
            const userBranchName = await getElementText(SELECTORS.userBranchName);
            // Ветка с таким именем не скопируется при повторной записи моков.
            const newBranchName = `${userBranchName}-copy-1635429509639`;

            await removeInputValue(SELECTORS.branchActionPanel.input);
            await setValueToInput(SELECTORS.branchActionPanel.input, newBranchName);
            await clickToSelector(SELECTORS.branchActionPanel.button.active);
            await waitForRemoved(SELECTORS.branchActionPanel.container);
            await setValueToInput(SELECTORS.filter, newBranchName);

            const branchesNames = await getElementsText(SELECTORS.branchName);
            expect(branchesNames).toContain(newBranchName);
        });

        // https://testpalm.yandex-team.ru/testcase/gryadka-5
        test('should close popup on outside click', async () => {
            await clickOutside();
            await waitForRemoved(SELECTORS.branchActionPanel.container);
        });

        test('should close popup on x click', async () => {
            await clickToSelector(SELECTORS.branchActionPanel.close);
            await waitForRemoved(SELECTORS.branchActionPanel.container);
        });
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-6
    describe('Branch search.', () => {
        test('should search existing branch', async () => {
            await waitForSelector(SELECTORS.userBranches);
            const userBranchesNames = await getElementsText(SELECTORS.userBranchName);
            const existingBranchName = userBranchesNames[Math.floor(Math.random() * userBranchesNames.length)]!;
            await setValueToInput(SELECTORS.filter, existingBranchName);
            const filtredBranchNames = await getElementsText(SELECTORS.userBranchName);
            expect(filtredBranchNames).toContain(existingBranchName);
        });

        test('should show all branches', async () => {
            await waitForSelector(SELECTORS.userBranches);
            const userBranchesNames = await getElementsText(SELECTORS.userBranchName);
            await setValueToInput(SELECTORS.filter, String(Date.now()));
            await removeInputValue(SELECTORS.filter);
            await waitForSelector(SELECTORS.filterEmpty);
            expect(await getElementsText(SELECTORS.userBranchName)).toEqual(userBranchesNames);
        });
    });
});
