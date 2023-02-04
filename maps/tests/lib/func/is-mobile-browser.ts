function isMobileBrowser(browser: WebdriverIO.Browser): boolean {
    return ['iphone', 'iphone-dark'].includes(browser.executionContext.browserId);
}

export default isMobileBrowser;
