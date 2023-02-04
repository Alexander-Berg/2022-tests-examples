import { getBranchType } from './getBranchType';

it('должен распознавать релизную ветку', () => {
    expect(getBranchType('08.11.2020-desktop')).toBe('RELEASE');
    expect(getBranchType('10.12.2021-cabinet-fix-average_outcome')).toBe('RELEASE');
});

it('должен распознавать фича-бранч', () => {
    expect(getBranchType('AUTORUFRONT-1234')).toBe('FEATURE');
    expect(getBranchType('autorufront-456789')).toBe('FEATURE');
});

it('не должен распознавать что-то не из конфига', () => {
    expect(getBranchType('VERTISFRONT-123')).toBe('UNKNOWN');
    expect(getBranchType('my-branch')).toBe('UNKNOWN');

    expect(getBranchType('AUTORUFRONT-20229-demo')).toBe('UNKNOWN');
    expect(getBranchType('AUTORUFRONT-trivial')).toBe('UNKNOWN');
});
