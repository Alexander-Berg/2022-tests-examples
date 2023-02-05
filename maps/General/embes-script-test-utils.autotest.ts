interface OpenEmbedPageParams {
    bodyInnerHTML?: string;
    headInnerHTML?: string;
}

async function openEmbedPage(
    browser: WebdriverIO.Browser,
    url: string,
    params: OpenEmbedPageParams = {bodyInnerHTML: '', headInnerHTML: ''}
): Promise<void> {
    const mock = await browser.mock(url);
    mock.respond(
        `
        <!DOCTYPE html>
        <html>
            <head>
                ${params.headInnerHTML}
            </head>
            <body>
                ${params.bodyInnerHTML}
            </body>
        </html>
        `,
        {
            headers: () => ({'content-type': 'text/html; charset=utf-8'})
        }
    );
    await browser.url(url);
}

export {openEmbedPage, OpenEmbedPageParams};
