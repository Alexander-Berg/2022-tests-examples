/* eslint-disable */
import addWildcardLinks from './Wildcards';

describe('Wildcards', () => {
  it('basicTest', () => {
    const input = [
      '/a/b/c',
      '/a/b/d',
      '/a/e',
      '/f/1',
      '/f/2',
      '/g',
    ];

    const expected = [
      '/a/*',
      '/a/b/*',
      '/a/b/c',
      '/a/b/d',
      '/a/e',
      '/f/*',
      '/f/1',
      '/f/2',
      '/g',
    ];

    const actual = addWildcardLinks(input);

    expect(actual).toEqual(expected);
  });

  it('shardsTest', () => {
    const input = [
      'a_b_test2_a',
      'a_b_test2_b',
      'a_b_test_A1',
      'a_b_test_bb',
      'a_b_test_bb_2',
      'c',
    ];
    const expected = [
      'a_b_*',
      'a_b_test2_*',
      'a_b_test2_a',
      'a_b_test2_b',
      'a_b_test_*',
      'a_b_test_A1',
      'a_b_test_bb*',
      'a_b_test_bb',
      'a_b_test_bb_2',
      'c',
    ];

    const actual = addWildcardLinks(input);

    expect(actual).toEqual(expected);
  });

  it('wordAndNotWordPrefixesTest', () => {
    const input = [
      '$(hostname -s)',
      'cluster',
      'solomon-kfront-sas-00',
      'solomon-kfront-sas-01',
    ];
    const expected = [
      '$(hostname -s)',
      'cluster',
      'solomon-kfront-sas-*',
      'solomon-kfront-sas-00',
      'solomon-kfront-sas-01',
    ];

    const actual = addWildcardLinks(input);

    expect(actual).toEqual(expected);
  });
});
