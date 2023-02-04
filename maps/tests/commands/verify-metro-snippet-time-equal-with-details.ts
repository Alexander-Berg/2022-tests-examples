import {wrapAsyncCommand} from '../../tests/lib/commands-utils';

async function verifyMetroSnippetTimeEqualWithDetails(
    this: WebdriverIO.Browser,
    activeSnippetTimeSelector: string,
    intervalSelector: string,
    routeTimeSelector: string
): Promise<boolean> {
    await this.waitForExist(activeSnippetTimeSelector);
    await this.waitForExist(routeTimeSelector);
    const snippetTimeText = await this.getText(activeSnippetTimeSelector);
    const snippetTime = parseInt(Array.isArray(snippetTimeText) ? snippetTimeText[0] : snippetTimeText, 10);
    const intervalsSumm = await this.execute((intervalSelector: string) => {
        let intervalsSumm = 0;
        window.document.querySelectorAll(intervalSelector).forEach((element) => {
            intervalsSumm += ((element.textContent && parseInt(element.textContent, 10)) || 0) / 2;
        });
        return intervalsSumm;
    }, intervalSelector);

    const detailsTime = await this.execute((routeTimeSelector: string) => {
        let detailsTime = 0;
        window.document.querySelectorAll(routeTimeSelector).forEach((element) => {
            detailsTime += (element.textContent && parseInt(element.textContent, 10)) || 0;
        });

        return detailsTime;
    }, routeTimeSelector);

    console.log(intervalsSumm);

    const resultTime = Math.ceil(detailsTime + intervalsSumm);

    if (resultTime !== snippetTime) {
        const expected = `Ожидается: ${snippetTime}`;
        const actual = `На самом деле: ${resultTime}`;
        throw new Error(`Некорректное суммарное время в подробностях маршрута:\n${expected}\n${actual}`);
    }
    return true;
}

export default wrapAsyncCommand(verifyMetroSnippetTimeEqualWithDetails);
