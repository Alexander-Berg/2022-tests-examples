/* eslint-disable no-undef */
import { TableWithSpansBuilder } from './TableWithSpansBuilder';

describe('TableWithSpansBuilderTest', () => {
  it('test1', () => {
    const table = new TableWithSpansBuilder();
    table.setSpannedCell(0, 0, 1, 1, '1,1');
    table.setSpannedCell(0, 1, 2, 1, '1,2 |');
    table.setSpannedCell(1, 0, 1, 2, '2,1 --');
    table.setSpannedCell(1, 1, 1, 1, '2,2');

    expect(table.getColumnCount()).toEqual(5);
    expect(table.getRowCount()).toEqual(2);
    expect(table.getCellOrEmpty(0, 0).getContentOrNull()).toEqual('1,1');
    expect(table.getCellOrEmpty(0, 1).getContentOrNull()).toEqual('1,2 |');
    expect(table.getCellOrEmpty(1, 2).getContentOrNull()).toEqual('2,1 --');
    expect(table.getCellOrEmpty(1, 4).getContentOrNull()).toEqual('2,2');
  });

  it('test2', () => {
    const table = new TableWithSpansBuilder();
    table.setSpannedCell(0, 0, 1, 1, '1,1');
    table.setSpannedCell(0, 1, 2, 1, '1,2 |');
    expect(table.getColumnCount()).toEqual(2);
    expect(table.getRowCount()).toEqual(2);
  });

  it('test3', () => {
    const table = new TableWithSpansBuilder();
    table.setSpannedCell(0, 0, 1, 1, '1,1');
    table.setSpannedCell(1, 0, 1, 2, '2,1 --');
    expect(table.getColumnCount()).toEqual(2);
    expect(table.getRowCount()).toEqual(2);
  });

  it('test4', () => {
    const table = new TableWithSpansBuilder();
    table.setSpannedCell(0, 0, 2, 1, '1,1 |');
    table.setSpannedCell(0, 1, 2, 1, '1,2 |');
    table.setSpannedCell(1, 0, 1, 2, '2,1 --');
    expect(table.getCellOrEmpty(0, 0).getContentOrNull()).toEqual('1,1 |');
    expect(table.getCellOrEmpty(0, 1).getContentOrNull()).toEqual('1,2 |');
    expect(table.getCellOrEmpty(1, 2).getContentOrNull()).toEqual('2,1 --');
  });
});
