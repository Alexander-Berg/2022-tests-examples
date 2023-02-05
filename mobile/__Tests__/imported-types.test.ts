import { TSImportedTypes } from '../src/generators-model/imported-types'

describe(TSImportedTypes, () => {
  it('can be extended with new imported types', () => {
    const types = new TSImportedTypes()
    types.extend(['A', 'B'], './file')

    expect(types.get('A')).toStrictEqual('./file')
    expect(types.get('B')).toStrictEqual('./file')
  })
  it('should throw if blank name is provided', () => {
    const types = new TSImportedTypes()
    expect(() => {
      types.extend(['   '], './file')
    }).toThrow(/All names must not be blank/)
  })
  it('should throw if blank source is provided', () => {
    const types = new TSImportedTypes()
    expect(() => {
      types.extend(['A'], '  ')
    }).toThrow(/Source must not be blank/)
  })
  it('should throw if no types are provided', () => {
    const types = new TSImportedTypes()
    expect(() => {
      types.extend([], './file')
    }).toThrow(/At least one name must be provided/)
  })
  it('should throw if there are duplicate names', () => {
    const types = new TSImportedTypes()
    expect(() => {
      types.extend(['A', 'B', 'C', 'D'], './file1')
      types.extend(['Z', 'B', 'C', 'X'], './file2')
    }).toThrow(/All names must be unique\. Non-uniques are: B, C/)
  })
  it('should return all names', () => {
    const types = new TSImportedTypes()
    types.extend(['A', 'B'], './file1')
    types.extend(['C'], './file2')

    expect(types.getAllNames()).toMatchObject(['A', 'B', 'C'])
  })
  it('should return all sources uniquefied', () => {
    const types = new TSImportedTypes()
    types.extend(['A', 'B'], './file1')
    types.extend(['C'], './file2')
    types.extend(['D'], './file2')

    expect(types.getAllSources()).toMatchObject(['./file1', './file2'])
  })
  it('should return all pairs of name-source', () => {
    const types = new TSImportedTypes()
    types.extend(['A', 'B'], './file1')
    types.extend(['C'], './file2')
    types.extend(['D'], './file2')

    expect(types.getAllPairs()).toMatchObject([
      { name: 'A', source: './file1' },
      { name: 'B', source: './file1' },
      { name: 'C', source: './file2' },
      { name: 'D', source: './file2' },
    ])
  })
  it("should return null if there's no such type registered", () => {
    const types = new TSImportedTypes()
    types.extend(['A', 'B'], './file1')

    expect(types.get('C')).toBeNull()
  })
})
