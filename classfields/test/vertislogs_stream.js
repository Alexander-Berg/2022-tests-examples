'use strict';

const assert = require('assert');
const fs = require('fs');
const path = require('path');

const MockDate = require('mockdate');
const VertislogsStream = require('../lib/vertislogs_stream');

// eslint-disable-next-line max-lines-per-function
describe('VertislogsStream', function() {
    beforeEach(function() {
        MockDate.set('2014-10-10T00:10:53.234Z');
    });

    afterEach(function() {
        MockDate.reset();
    });

    it('should prefix each line of input with timestamp', function(done) {
        let result = '';
        const vertisStream = new VertislogsStream(null, {
            defaultLevel: 'DEBUG',
            splitChunkOnTransform: true,
        });
        const tss = fs
            .createReadStream(path.resolve(__dirname, 'data/vertislogs_stream_input.log'), { encoding: 'utf8' })
            .pipe(vertisStream);

        tss.on('data', function(chunk) {
            result += chunk;
        });

        tss.on('end', function() {
            const expected = fs.readFileSync(path.resolve(__dirname, 'data/vertislogs_stream_output.log'), { encoding: 'utf8' });

            assert.equal(expected, result);
            done();
        });
    });

    it('should prefix each line of input with timestamp and thread', function(done) {
        let result = '';
        const vertisStream = new VertislogsStream(null, {
            defaultLevel: 'DEBUG',
            splitChunkOnTransform: true,
            thread: 'master',
        });
        const tss = fs
            .createReadStream(path.resolve(__dirname, 'data/vertislogs_stream_input.log'), { encoding: 'utf8' })
            .pipe(vertisStream);

        tss.on('data', function(chunk) {
            result += chunk;
        });

        tss.on('end', function() {
            const expected = fs.readFileSync(path.resolve(__dirname, 'data/vertislogs_stream_thread_output.log'), { encoding: 'utf8' });

            assert.equal(expected, result);
            done();
        });
    });

    it('should ignore levels', function(done) {
        let result = '';

        const vertisStream = new VertislogsStream(null, {
            defaultLevel: 'DEBUG',
            splitChunkOnTransform: true,
            thread: 'master',
            ignoreLevels: [ 'DEBUG', 'INFO' ],
        });

        const tss = fs
            .createReadStream(path.resolve(__dirname, 'data/vertislogs_stream_ingore_levels_input.log'), { encoding: 'utf8' })
            .pipe(vertisStream);

        tss.on('data', function(chunk) {
            result += chunk;
        });

        tss.on('end', function() {
            const expected = fs.readFileSync(path.resolve(__dirname, 'data/vertislogs_stream_ingore_levels_output.log'), { encoding: 'utf8' });

            assert.equal(expected, result);
            done();
        });
    });
});
