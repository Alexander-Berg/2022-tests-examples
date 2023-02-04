hermione.enable.in(['chrome_50']);
describe('Webgl', () => {
    it('webglreport', async ({browser}) => {
        await browser.url('http://webglreport.com/?v=1');
        await browser.pause(2000);
        await browser.saveScreenshot('./screenshots/webglreport-1.png');
        await browser.url('http://webglreport.com/?v=2');
        await browser.pause(2000);
        await browser.saveScreenshot('./screenshots/webglreport-2.png');
    });

    it('vmit.github.io/attrib-0-use', async ({browser}) => {
        await browser.url('https://vmit.github.io/attrib-0-use/');
        await browser.pause(3000);
        await browser.saveScreenshot('./screenshots/attrib-0-use.png');
        await browser.verifyNoErrors();
    });

    it('vmit.github.io/attribs-ordered', async ({browser}) => {
        await browser.url('https://vmit.github.io/attribs-ordered/index.html');
        await browser.pause(3000);
        await browser.saveScreenshot('./screenshots/attribs-ordered.png');
        await browser.verifyNoErrors();
    });

    it('vmit.github.io/no-collision-detection', async ({browser}) => {
        await browser.url('https://vmit.github.io/no-collision-detection/index.html');
        await browser.pause(3000);
        await browser.saveScreenshot('./screenshots/no-collision-detection.png');
        await browser.verifyNoErrors();
    });

    it('about://gpu', async ({browser}) => {
        await browser.url('about://gpu');
        await browser.pause(2000);
        await browser.saveScreenshot('./screenshots/about-gpu.png');
    });
});
