/* globals window */

const FakeTimers = require('@sinonjs/fake-timers');

if (typeof window !== 'undefined') {
    window.FakeTimers = FakeTimers;
}
