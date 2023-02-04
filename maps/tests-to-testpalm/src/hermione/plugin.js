module.exports = (hermione) => {
    hermione.on(hermione.events.BEFORE_FILE_READ, (runner) => {
        runner.testParser.setController('testPalm', {
            setComponent: () => {}
        });
    });

    hermione.on(hermione.events.NEW_BROWSER, (browser) => {
        browser.addCommand('perform', (fn) => fn());
    });
};
