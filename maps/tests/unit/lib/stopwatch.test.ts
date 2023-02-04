import {expect} from 'chai';
import {delay} from 'prex';
import {Stopwatch} from 'src/lib/stopwatch';

describe('Stopwatch', () => {
    describe('elapsedMs property', () => {
        let stopwatch: Stopwatch;

        beforeEach(() => {
            stopwatch = new Stopwatch();
        });

        it('should return 0 if not started', async () => {
            expect(stopwatch.elapsedMs).to.equal(0);
            await delay(10);
            expect(stopwatch.elapsedMs).to.equal(0);
        });

        it('should return elapsed time from start to current time', async () => {
            stopwatch.start();
            await delay(100);
            const time1 = stopwatch.elapsedMs;
            await delay(100);
            const time2 = stopwatch.elapsedMs;
            // See https://github.com/nodejs/node/issues/10154#issuecomment-321894774
            expect(time1).to.be.above(99);
            expect(time2).to.be.above(199);
            expect(time1).to.be.below(time2);
        });

        it('should return elapsed time from start to stop', async () => {
            stopwatch.start();
            await delay(100);
            stopwatch.stop();
            const time = stopwatch.elapsedMs;
            await delay(100);
            expect(time).to.be.above(99);
            expect(time).to.be.below(200);
        });

        it('should return elapsed time of two periods', async () => {
            stopwatch.start();
            await delay(100);
            stopwatch.stop();
            await delay(100);
            stopwatch.start();
            await delay(100);

            const time = stopwatch.elapsedMs;
            expect(time).to.be.above(199);
            expect(time).to.be.below(300);
        });
    });

    describe('start()', () => {
        it('should not restart started stopwatch', async () => {
            const stopwatch = Stopwatch.start();
            await delay(100);
            const time1 = stopwatch.elapsedMs;
            stopwatch.start();
            expect(stopwatch.elapsedMs).to.be.at.least(time1);
        });
    });

    describe('reset()', () => {
        it('should should reset start time', async () => {
            const stopwatch = Stopwatch.start();
            await delay(10);
            stopwatch.reset();
            expect(stopwatch.elapsedMs).to.be.below(1);
        });

        it('should reset time after stop', async () => {
            const stopwatch = Stopwatch.start();
            await delay(10);
            stopwatch.stop().reset();
            await delay(10);
            expect(stopwatch.elapsedMs).to.equal(0);
        });
    });

    describe('isRunning property', () => {
        it('should return `true` if running', () => {
            const stopwatch = new Stopwatch();
            expect(stopwatch.isRunning).to.be.false;
            stopwatch.start();
            expect(stopwatch.isRunning).to.be.true;
            stopwatch.reset();
            expect(stopwatch.isRunning).to.be.true;
            stopwatch.stop();
            expect(stopwatch.isRunning).to.be.false;
        });
    });
});
