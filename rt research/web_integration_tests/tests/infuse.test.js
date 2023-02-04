const config = require('../config.js');
const urljoin = require('url-join');

const {authBrowser, catchErrors} = require('../index.js');

jest.setTimeout(config.jestTimeout);

const ADD_LABELS = [
    {
        name: 'болезни картофеля',
        id: 200006624,
    },
];
const ALLBANNER_BID = '100';
const BANNERHASH = '00003d86442db96bce6a89a5832a5a88';
const CATEGORY_PATH_TO_DELETED_CATEGORY: [200004200, 200003006];
const DELETED_CATEGORY_ID = 200003199;


describe('Infuse', () => {
    authBrowser();
    catchErrors();

    it('Infuse banner from index', async () => {
        let response = await page.goto(urljoin(config.multikHost, '/'), {
            waitUntil: ['load', 'networkidle0'],
            timeout: config.navigationTimeout,
        });
        expect(response.status()).toBe(200);
        await page.focus('.multik-filter-input[data-filter-name="bannerhash"] .Textinput input');
        await page.keyboard.type(BANNERHASH);
        await page.keyboard.type(String.fromCharCode(13)); // hit Enter

        // It will cause a get request, increase wait time
        const row = await page.waitForSelector(`.multik-banners-table tr[data-bannerhash="${BANNERHASH}"]`, {
            timeout: config.navigationTimeout,
        });
        const checkbox = await row.$('.Checkbox-Box');
        expect(checkbox).not.toBeNull();
        await checkbox.click();

        // Check that edit button is enabled
        let button = await page.$('.IndexComponent-buttons-container #edit-banner-button');
        expect(await button.evaluate((button) => button.disabled)).toBe(false);
        await button.click();

        await fillEditLabelsModal();

        await waitForLabelsVisible(getLabels(), BANNERHASH);
        await gotoChanges();
        await waitForLabelsVisible(getLabels(), BANNERHASH);
        const changes = await page.$$('.multik-banners-table tr.multik-banner-main-row');
        expect(changes.length).toBe(1);
        await runInfuse();
    });

    it('Infuse banner from allbanners', async () => {
        let response = await page.goto(urljoin(config.multikHost, '/allbanners'), {
            waitUntil: ['load', 'networkidle0'],
            timeout: config.navigationTimeout,
        });
        expect(response.status()).toBe(200);
        await page.focus('.multik-filter-input[data-filter-name="bid"] .Textinput input');
        await page.keyboard.type(ALLBANNER_BID);
        await page.keyboard.type(String.fromCharCode(13)); // hit Enter

        const row = await page.waitForSelector(`.multik-banners-table tr[data-bid="${ALLBANNER_BID}"]`, {
            timeout: config.navigationTimeout,
        });
        const checkbox = await row.$('.Checkbox-Box');
        expect(checkbox).not.toBeNull();
        await checkbox.click();

        // Check that edit button is enabled
        let button = await page.$('.AllBannersComponent-buttons-container #edit-banner-button');
        expect(await button.evaluate((button) => button.disabled)).toBe(false);
        await button.click();

        await fillEditLabelsModal();
        await gotoChanges();

        const changes = await page.$$('.multik-banners-table tr.multik-banner-main-row');
        expect(changes.length).toBe(1);
        await runInfuse();
    });

    const NEW_CATEGORY_NAME = '_ Новая кленовая категория';
    it('Infuse labels', async () => {
        let response = await page.goto(urljoin(config.multikHost, '/'), {
            waitUntil: ['load', 'networkidle0'],
            timeout: config.navigationTimeout,
        });
        expect(response.status()).toBe(200);

        // At start we wait only one (root) category node. And after it's disclosure - more category nodes
        await page.waitFor(() => {
            return document.querySelectorAll(`.LabelsTreeNode-label-container`).length === 1;
        });
        const rootLabelsTreeNode = await page.waitForSelector(`.LabelsTreeNode-label-container[data-label-id="0"]`);
        await rootLabelsTreeNode.click();
        await page.waitFor(() => {
            return document.querySelectorAll(`.LabelsTreeNode-label-container`).length > 1;
        });

        const addIcon = await page.waitForSelector('.multik-add-label-icon');
        await addIcon.click();
        await page.waitForSelector('.AddLabelModal');
        await page.focus('.AddLabelModal #AddLabelModal-name');
        await page.keyboard.type(NEW_CATEGORY_NAME);

        const button = await page.waitForSelector('.AddLabelModal button#add-label-modal-apply');
        expect(await button.evaluate((button) => button.disabled)).toBe(false);
        await button.click();

        await page.waitForSelector('.AddLabelModal', {hidden: true, timeout: config.navigationTimeout});
        await page.waitFor(
            (name) => {
                const label = document.querySelector('.multik-label-ADD .LabelsTreeNode-label-name');
                return label && label.innerText === name;
            },
            {timeout: config.navigationTimeout},
            NEW_CATEGORY_NAME,
        );

        for (const categoryId of CATEGORY_PATH_TO_DELETED_CATEGORY) {
            console.log('Trying to find and disclose category node with id=%d.', categoryId);
            const selectedLabel = await page.waitForSelector(`.LabelsTreeNode-label-container[data-label-id="${categoryId}"]`);
            await selectedLabel.click();
            console.log('Category node with id=%d has been successfully found and disclosed.', categoryId);
        }

        const labelToDelete = await page.waitForSelector(
            `.LabelsTreeNode-label-container[data-label-id="${DELETED_CATEGORY_ID}"]`,
        );

        const trashIcon = await labelToDelete.$('.LabelsTreeNode-trash-icon');
        await labelToDelete.hover();
        await trashIcon.click();
        await page.waitForSelector(
            `.LabelsTreeNode-label-container[data-label-id="${DELETED_CATEGORY_ID}"] .multik-label-DELETE`,
            {timeout: config.navigationTimeout},
        );

        await gotoChanges({checkBanners: false, checkLabels: true});
        const changes = await page.$$('.multik-labels-table tbody tr');
        expect(changes.length).toBe(2);
        await runInfuse();
    });
});

function getLabels() {
    if (config.infuseStaticCategories) {
        return ADD_LABELS;
    } else {
        throw 'Using non-static categories is not implemented yet';
    }
}

async function waitForLabelsVisible(labels, bannerhash) {
    for (const label of labels) {
        await page.waitForSelector(
            `.multik-banners-table tr[data-bannerhash="${bannerhash}"] .multik-banner-delta-added[data-label-id="${label.id}"]`,
        );
    }
}

async function fillEditLabelsModal() {
    await page.waitForSelector('.EditLabelsModal');

    const labels = getLabels();
    for (const label of labels) {
        await page.focus('.EditLabelsModal-add-labels .Textinput input');
        await page.keyboard.type(label.name);

        const suggest = await page.waitForSelector(
            `.EditLabelsModal-suggest .EditLabelsModal-suggest-item[data-label-id="${label.id}"]`,
        );
        await suggest.click();
    }

    const button = await page.waitForSelector('.EditLabelsModal button#edit-labels-modal-apply');
    expect(await button.evaluate((button) => button.disabled)).toBe(false);
    await button.click();

    await page.waitForSelector('.EditLabelsModal', {hidden: true, timeout: config.navigationTimeout});
}

async function gotoChanges({checkBanners = true, checkLabels = false} = {}) {
    const response = await page.goto(urljoin(config.multikHost, '/changes'), {
        waitUntil: ['load', 'networkidle0'],
        timeout: config.navigationTimeout,
    });
    expect(response.status()).toBe(200);
    if (checkBanners) {
        await page.waitForSelector('.Changes-layout .BannersTable-table-container', {
            timeout: config.navigationTimeout,
        });
    }
    if (checkLabels) {
        await page.waitForSelector('.Changes-layout .multik-labels-table', {
            timeout: config.navigationTimeout,
        });
    }
}

async function runInfuse() {
    let button = await page.$('.Changes-buttons-container #infuse-button');
    expect(await button.evaluate((button) => button.disabled)).toBe(false);
    await button.click();

    // Loader should appear, indicating api request, then disappear indicating successfull request
    await page.waitForSelector('.ApiStatusModal');
    await page.waitForSelector('.ApiStatusModal', {hidden: true, timeout: config.navigationTimeout});

    let start = Date.now() / 1000;
    while (true) {
        const opContainer = await page.waitForSelector('.Operations-list-container', {
            timeout: config.navigationTimeout,
        });
        const op = await opContainer.$('.Operations-list-item.Operations-list-current');
        const opData = await op.evaluate((node) => {
            return {dataset: {...node.dataset}, classList: [...node.classList]};
        });
        expect(await page.evaluate(() => location.search)).toBe(`?id=${opData.dataset.operationId}`); // Check current element is the one we came with
        expect(opData.classList).not.toContain('Operations-list-error'); // Operation should not fail
        if (opData.classList.includes('Operations-list-success')) {
            break;
        }

        let now = Date.now() / 1000;
        expect(now - start).toBeLessThan(config.maxInfuseWaitSec);
        await page.waitFor(config.infuseWait);
        await page.reload();
    }
}
