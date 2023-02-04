'use strict';

const {run, runQtools, copyTarget, removeTarget} = require('./utils');

jest.setTimeout(60000);

describe('qtools build', () => {
    const imageName = 'registry.yandex.net/junk/qtools-test-project:0.0.1';
    const localImageName = 'my-local-image-tag';

    const allImageNames = [imageName, localImageName];

    const removeImages = () => {
        try {
            run(`docker rmi -f ${allImageNames.join(' ')}`);
        } catch (e) {}
    };

    let initialDir;

    beforeAll(() => {
        removeImages();
    });

    beforeEach(() => {
        initialDir = copyTarget();
    });

    afterEach(() => {
        removeImages();
        removeTarget(initialDir);
    });

    describe('via .qtools.json', () => {
        it('should create image', () => {
            expect(() => runQtools('build')).not.toThrowError();

            const stdout = run(`docker images -q ${imageName}`);
            expect(stdout).not.toBe('');
        });

        it('should create image with docker options', () => {
            expect(() => runQtools(`build -- -t ${localImageName}`)).not.toThrowError();

            allImageNames.forEach((name) => {
                const stdout = run(`docker images -q ${name}`);
                expect(stdout).not.toBe('');
            });
        });
    });
});
