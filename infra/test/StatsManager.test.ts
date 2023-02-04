import {StatsManager} from '../src/StatsManager';
import {omit} from '../src/utils';

let s = new StatsManager();
beforeEach(() => {
    s = new StatsManager();
});

test('should be empty by default', () => {
    const value = s.getYasmSignals();

    expect(value).toEqual([]);
});

test('should increment counter', () => {
    s.incrementCounter('test', 'mmmm');

    expect(s.getYasmSignals()).toEqual([{name: 'test_mmmm', val: 1}]);
});

test('should set counter value', () => {
    s.setCounterValue('test', 'mmmm', 10);

    expect(s.getYasmSignals()).toEqual([{name: 'test_mmmm', val: 10}]);
});

test('should add histogram sample', () => {
    s.addSample('test', 1);
    s.addSample('test', 2);

    expect(s.getYasmSignals()).toEqual([{name: 'test_hhhh', val: [1, 2]}]);
});

test('should validate signal name', () => {
    s.incrementCounter('test-test', 'mmmm', 1);
    s.addSample('test1-test', 1);

    expect(s.getYasmSignals()).toEqual([]);
});

test('should set user', () => {
    s.setUser('user');

    expect(omit(s.getTags(), 'itype')).toEqual({user: 'user'});
});

test('should set metric key', () => {
    s.setMetricKey('test');

    expect(omit(s.getTags(), 'itype')).toEqual({metric_key: 'test'});
});

test('should set custom tag', () => {
    s.setTag('test', 'value');

    expect(omit(s.getTags(), 'itype')).toEqual({test: 'value'});
});

test('should validate tag value', () => {
    s.setUser('@user$');
    s.setMetricKey('@key$');

    expect(omit(s.getTags(), 'itype', 'geo')).toEqual({});
});
