/* eslint-disable no-undef */
import UrlUtils from './UrlUtils';
import LinkedHashMap from '../LinkedHashMap';

describe('UrlUtils', () => {
  it('decodeArg', () => {
    expect(UrlUtils.decodeArg(''))
      .toEqual('');
    expect(UrlUtils.decodeArg('a%2Bb'))
      .toEqual('a+b');
    expect(UrlUtils.decodeArg('a+b'))
      .toEqual('a b');
    expect(UrlUtils.decodeArg('a%20b'))
      .toEqual('a b');
    expect(UrlUtils.decodeArg('a+b+c'))
      .toEqual('a b c');
    expect(UrlUtils.decodeArg('a%20b%20c'))
      .toEqual('a b c');
  });

  it('enodeArg', () => {
    expect(UrlUtils.encodeArg(''))
      .toEqual('');
    expect(UrlUtils.encodeArg('a+b'))
      .toEqual('a%2Bb');
    expect(UrlUtils.encodeArg('a b'))
      .toEqual('a%20b');
    expect(UrlUtils.encodeArg('a b c'))
      .toEqual('a%20b%20c');
  });

  it('addParameter', () => {
    expect(UrlUtils.addParameter('/', 'foo', 'bar'))
      .toEqual('/?foo=bar');
    expect(UrlUtils.addParameter('/?a=b#fragment', 'foo', 'bar'))
      .toEqual('/?a=b&foo=bar#fragment');
    expect(UrlUtils.addParameter('http://yandex.ru/#result', 'q', 'solomon'))
      .toEqual('http://yandex.ru/?q=solomon#result');
    expect(UrlUtils.addParameter('/', 'a', '&'))
      .toEqual('/?a=%26');
  });

  function testRemoveQueryArgs(expected, url, ...args) {
    expect(UrlUtils.removeQueryArgs(url, ...args))
      .toEqual(expected);
  }

  it('removeQueryArgs', () => {
    testRemoveQueryArgs('/', '/?b=c', 'b');

    testRemoveQueryArgs('/', '/', 'a');
    testRemoveQueryArgs('/foo', '/foo', 'aaa');
    testRemoveQueryArgs('/foo', '/foo?', 'aaa');
    testRemoveQueryArgs('/?b=c', '/?b=c', 'x');
    testRemoveQueryArgs('/?b=c', '/?b=c', 'x', 'y');
    // testRemoveQueryArgs('/', '/?b=c', 'b');
    testRemoveQueryArgs('/', '/?b=c&b=e', 'b');
    testRemoveQueryArgs('/?x=y', '/?b=c&b=e&x=y', 'b');
    testRemoveQueryArgs('/?x=y&x=y', '/?x=y&b=e&x=y', 'b');
    testRemoveQueryArgs('/?b=e', '/?x=y&b=e&x=y', 'x');
    testRemoveQueryArgs('/?x=%20', '/?b=c&x=%20', 'b');

    // parameter without value
    testRemoveQueryArgs('/?x=11', '/?b&x=11', 'b');

    // extra amps
    testRemoveQueryArgs('/', '/?&&xx=yy&&', 'xx');
  });

  function addSep(url) {
    if (url.indexOf('?') >= 0) {
      return `${url}&`;
    }
    return `${url}?`;
  }

  function randomNextInt(num) {
    return Math.trunc(Math.random() * num);
  }

  function randomNextBoolean() {
    return Math.random() <= 0.5;
  }

  function removeQueryArgsRandomIteration() {
    const length = randomNextInt(4);

    let expected = randomNextBoolean() ? '/foo' : '/';
    let input = expected;

    const remove = 'bar';

    const parameterNames = [remove, 'baz', 'qux', 'quux'];

    for (let i = 0; i < length; ++i) {
      input = addSep(input);

      if (randomNextInt(3) === 0) {
        input += '&';
      }

      const param = parameterNames[randomNextInt(parameterNames.length)];
      const value = randomNextInt(1000);

      if (param === remove) {
        input = addSep(input);
        if (value < 100) {
          input += `${param}=`;
        } else if (value < 200) {
          input += param;
        } else {
          input += `${param}=${value}`;
        }
      } else {
        expected = addSep(expected);
        expected += `${param}=${value}`;

        input = addSep(input);
        input += `${param}=${value}`;
      }
    }

    if (randomNextBoolean()) {
      input = addSep(input);
    }

    testRemoveQueryArgs(expected, input, remove);
  }

  it('removeQueryArgsRandom', () => {
    for (let i = 0; i < 10000; ++i) {
      removeQueryArgsRandomIteration();
    }
  });

  function map(...args) {
    if (args.length % 2 !== 0) {
      throw new Error('not odd args');
    }

    const r = new LinkedHashMap();

    for (let i = 0; i < args.length; i += 2) {
      const name = args[i];
      const value = args[i + 1];
      r.put(name, value);
    }

    return r;
  }

  it('fillDefaults', () => {
    expect(UrlUtils.fillDefaultsMap('/', map('a', 'b')))
      .toEqual('/?a=b');
    expect(UrlUtils.fillDefaultsMap('/', map('a', 'b', 'c', 'd')))
      .toEqual('/?a=b&c=d');
    expect(UrlUtils.fillDefaultsMap('/?a=b', map('a', 'x')))
      .toEqual('/?a=b');
    expect(UrlUtils.fillDefaultsMap('/?a=b&a=c', map('a', 'x')))
      .toEqual('/?a=b&a=c');
    expect(UrlUtils.fillDefaultsMap('/?a=b&c=d', map('a', 'x')))
      .toEqual('/?a=b&c=d');
  });

  it('parseQueryArgs', () => {
    expect(UrlUtils.parseQueryArgs('a=b&c=d=e'))
      .toEqual(map('a', 'b', 'c', 'd=e'));
    expect(UrlUtils.parseQueryArgs('x=%2B')
      .get('x'))
      .toEqual('+');
    expect(UrlUtils.parseQueryArgs('x=a+b')
      .get('x'))
      .toEqual('a b');
  });

  function updateParameterTest(expected, url, name, value) {
    expect(expected)
      .toEqual(UrlUtils.updateParameter(url, name, value));
  }

  it('updateParameter', () => {
    // add parameter
    updateParameterTest('/?x=y&z=w', '/?x=y', 'z', 'w');
    // replace preserving order
    updateParameterTest('/?a=b&x=2&z=w', '/?a=b&x=y&z=w', 'x', '2');
    // removes multiple entries
    updateParameterTest('/?a=b&x=2&z=w', '/?a=b&x=y1&x=y2&z=w', 'x', '2');

    updateParameterTest('/?aa=cc&cc=dd', '/?aa=bb&cc=dd', 'aa', 'cc');
    updateParameterTest('/?aa=cc', '/?aa=bb', 'aa', 'cc');
    updateParameterTest('/?cc=dd&aa=cc', '/?cc=dd&aa=bb', 'aa', 'cc');

    updateParameterTest('/?aa=bb', '/?aa=bb', 'aa', 'bb');

    // some partial strings
    updateParameterTest('/?ab=cd', '/?', 'ab', 'cd');
    updateParameterTest('/?ab=cd', '/?ab=cc&', 'ab', 'cd');
    updateParameterTest('/?ab=cd', '/?ab', 'ab', 'cd');
    updateParameterTest('/?ab=cd', '/?ab&', 'ab', 'cd');
    updateParameterTest('/?ab=cd&xx=yy', '/?ab&xx=yy', 'ab', 'cd');
  });

  it('updateOrRemoveParameter', () => {
    // removes parameter if value == null
    expect(UrlUtils.updateOrRemoveParameter('/?a=b&x=y1&z=w', 'x', null))
      .toEqual('/?a=b&z=w');
  });
});
