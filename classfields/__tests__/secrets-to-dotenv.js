const fs = require('fs');
const path = require('path');

const secretsToDotenv = require('../');

describe('default options', () => {
    let envPath;
    let configPath;
    beforeEach(() => {
        envPath = path.resolve(__dirname, '.env');
        configPath = path.resolve(__dirname, 'secrets.config.json');
    });

    afterEach(() => {
        if (envPath) {
            fs.rmSync(envPath, { force: true });
        }
    });

    it('should fetch secrets from yav and load it to ENV', async() => {
        await secretsToDotenv({ configPath, envPath });

        // check if .env file exists
        expect(fs.existsSync(envPath)).toBe(true);

        // check for clear environment
        expect(process.env.TEST_MULTILINE).toBeUndefined();
        expect(process.env.TEST_STRING).toBeUndefined();
        expect(process.env.TEST_NUMBER).toBeUndefined();

        // load .env file
        require('dotenv').config({ path: envPath });

        expect(process.env.TEST_MULTILINE).toBe('hello\nworld');
        expect(process.env.TEST_STRING).toBe('hello world');
        expect(process.env.TEST_NUMBER).toBe('12345');
    });
});
