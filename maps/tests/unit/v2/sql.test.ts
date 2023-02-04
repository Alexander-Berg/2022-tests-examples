import {expect} from 'chai';
import {generatePlaceholders, MultirowValues} from 'src/v2/sql';

describe('sql helpers', () => {
    describe('generatePlaceholders()', () => {
        it('should generate given number of placeholders', () => {
            expect(generatePlaceholders(4)).to.equal('$1,$2,$3,$4');
        });

        it('should generate placeholders from the specified start number', () => {
            expect(generatePlaceholders(5, 3)).to.equal('$5,$6,$7');
        });
    });

    describe('MultirowValues', () => {
        let values: MultirowValues<string, any>;

        beforeEach(() => {
            values = new MultirowValues(['columnA', 'columnB']);
        });

        describe('columnNames property', () => {
            it('should return column names in the same order', () => {
                expect(values.columnNames).to.equal('columnA,columnB');
            });
        });

        describe('getting placeholder and values', () => {
            beforeEach(() => {
                values.addRow({columnA: 1, columnB: 2});
                values.addRow({columnA: 3, columnB: 4});
            });

            describe('placeholders property', () => {
                it('should return parameter placeholders for each row', () => {
                    expect(values.placeholders).to.equal('($1,$2),($3,$4)');
                });
            });

            describe('values property', () => {
                it('should return values for each placeholder as plain array', () => {
                    expect(values.values).to.deep.equal([1, 2, 3, 4]);
                });
            });
        });
    });
});
