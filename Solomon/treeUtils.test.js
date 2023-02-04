/* eslint-disable no-undef */

import {
  changeNodeByPath,
  comparePathes,
  computeCommonParentPathLen,
  computeNewPathAfterMoving,
  deleteNodeByPath,
  getNodeByPath,
  insertNodeByPath,
  moveNodeAfterPath,
  moveNodeBeforePath, appendNodeToPath,
  replaceNodeByPath, computeAvailablePath,
} from './treeUtils';

describe('getNodeByPath', () => {
  const treeData = [
    { title: 'node 0', children: [{ title: 'node 0,1' }] },
  ];

  it('depth 0', () => {
    const expectedNode = treeData[0];
    const actualNode = getNodeByPath(treeData, [0]);
    expect(actualNode).toEqual(expectedNode);
  });

  it('depth 1', () => {
    const expectedNode = treeData[0].children[0];
    const actualNode = getNodeByPath(treeData, [0, 0]);
    expect(actualNode).toEqual(expectedNode);
  });
});

describe('insertNodeByPath', () => {
  const treeData = [
    { title: 'node 0', children: [{ title: 'node 0,1' }] },
  ];

  const node = { title: 'inserted node' };

  it('insert to 0', () => {
    const expectedTreeData = [node, ...treeData];
    const actualTreeData = insertNodeByPath(treeData, [0], node);
    expect(actualTreeData).toEqual(expectedTreeData);
  });

  it('insert to 1', () => {
    const expectedTreeData = [...treeData, node];
    const actualTreeData = insertNodeByPath(treeData, [1], node);
    expect(actualTreeData).toEqual(expectedTreeData);
  });

  it('insert to 0,0', () => {
    const expectedTreeData = [
      { ...treeData[0], children: [node, ...treeData[0].children] },
    ];
    const actualTreeData = insertNodeByPath(treeData, [0, 0], node);
    expect(actualTreeData).toEqual(expectedTreeData);
  });

  it('insert to 0,1', () => {
    const expectedTreeData = [
      { ...treeData[0], children: [...treeData[0].children, node] },
    ];
    const actualTreeData = insertNodeByPath(treeData, [0, 1], node);
    expect(actualTreeData).toEqual(expectedTreeData);
  });
});

describe('replaceNodeByPath', () => {
  it('replace node', () => {
    const treeData = [{ title: 'node 1', children: [{ title: 'node 2' }] }];

    const expectedTreeData = [{ title: 'node 1', children: [{ title: 'changed node 2' }] }];

    const actualTreeData = replaceNodeByPath(treeData, [0, 0], { title: 'changed node 2' });

    expect(actualTreeData).toEqual(expectedTreeData);
  });
});

describe('changeNodeByPath', () => {
  it('change node', () => {
    const treeData = [{ title: 'node 1', children: [{ title: 'node 2' }] }];

    const expectedNewTreeData = [{ title: 'node 1', children: [{ title: 'changed node 2' }] }];

    const actualNewTreeData = changeNodeByPath(
      treeData,
      [0, 0],
      (node) => ({ ...node, title: `changed ${node.title}` }),
    );

    expect(actualNewTreeData).toEqual(expectedNewTreeData);
  });
});

describe('deleteNodeByPath', () => {
  it('delete node', () => {
    const oldTreeData = [{ title: 'node 1', children: [{ title: 'node 2' }] }];

    const expectedNewTreeData = [{ title: 'node 1', children: [] }];

    const actualNewTreeData = deleteNodeByPath(oldTreeData, [0, 0]);

    expect(actualNewTreeData).toEqual(expectedNewTreeData);
  });
});

describe('comparePathes', () => {
  it('empty pathes', () => {
    expect(comparePathes([], [])).toEqual(0);
  });
  it('empty and non-empty', () => {
    expect(comparePathes([], [0])).toEqual(-1);
  });
  it('non-empty and empty', () => {
    expect(comparePathes([0], [])).toEqual(1);
  });
  it('flatten nodes (<)', () => {
    expect(comparePathes([0], [1])).toEqual(-1);
  });
  it('flatten nodes (>)', () => {
    expect(comparePathes([1], [0])).toEqual(1);
  });
  it('deep flatten nodes (<)', () => {
    expect(comparePathes([0, 0, 0], [0, 0, 1])).toEqual(-1);
  });
  it('deep flatten nodes (>)', () => {
    expect(comparePathes([0, 0, 1], [0, 0, 0])).toEqual(1);
  });
  it('different nodes (<)', () => {
    expect(comparePathes([0, 1], [1, 0])).toEqual(-1);
  });
  it('different nodes (>)', () => {
    expect(comparePathes([1, 0], [0, 1])).toEqual(1);
  });
  it('equal nodes (depth 0)', () => {
    expect(comparePathes([0, 0], [0, 0])).toEqual(0);
  });
  it('equal nodes (depth 1)', () => {
    expect(comparePathes([0], [0])).toEqual(0);
  });
});

describe('computeCommonParentPathLen', () => {
  it('empty paths', () => {
    expect(computeCommonParentPathLen([], [])).toEqual(0);
  });
  it('empty and not empty paths', () => {
    expect(computeCommonParentPathLen([], [0])).toEqual(0);
  });
  it('flatten rows', () => {
    expect(computeCommonParentPathLen([0], [1])).toEqual(0);
  });
  it('first path is parent', () => {
    expect(computeCommonParentPathLen([0], [0, 1])).toEqual(1);
  });
  it('deep parentness', () => {
    expect(computeCommonParentPathLen([0, 1, 0], [0, 1, 1, 2])).toEqual(2);
  });
  it('no common parents', () => {
    expect(computeCommonParentPathLen([0, 1, 2], [1, 0, 0])).toEqual(0);
  });
});

describe('computeNewPathAfterMoving', () => {
  it('empty paths', () => {
    expect(computeNewPathAfterMoving([], [])).toEqual([]);
  });
  it('empty and not empty paths', () => {
    expect(computeNewPathAfterMoving([], [0])).toEqual([]);
  });
  it('not empty and empty paths', () => {
    expect(computeNewPathAfterMoving([0], [])).toEqual([]);
  });
  it('0 to 1', () => {
    expect(computeNewPathAfterMoving([0], [1])).toEqual([0]);
  });
  it('0 to 2', () => {
    expect(computeNewPathAfterMoving([0], [2])).toEqual([1]);
  });
  it('1 to 0', () => {
    expect(computeNewPathAfterMoving([1], [0])).toEqual([0]);
  });
  it('1 to 2', () => {
    expect(computeNewPathAfterMoving([1], [2])).toEqual([1]);
  });
  it('2 to 0', () => {
    expect(computeNewPathAfterMoving([2], [0])).toEqual([0]);
  });
  it('2 to 1', () => {
    expect(computeNewPathAfterMoving([2], [1])).toEqual([1]);
  });
  it('0 to 1,0', () => {
    expect(computeNewPathAfterMoving([0], [1, 0])).toEqual([0, 0]);
  });
  it('1 to 0,0', () => {
    expect(computeNewPathAfterMoving([1], [0, 0])).toEqual([0, 0]);
  });
  it('0,0 to 1,0', () => {
    expect(computeNewPathAfterMoving([0, 0], [1, 0])).toEqual([1, 0]);
  });
  it('1,0 to 0,0', () => {
    expect(computeNewPathAfterMoving([1, 0], [0, 0])).toEqual([0, 0]);
  });
});

describe('moveNodeBeforePath', () => {
  const original = [
    { title: 'node 0' },
    { title: 'node 1' },
    { title: 'node 2' },
  ];

  const complexOriginal = [
    { title: 'node 0', children: [{ title: 'node 0.0' }, { title: 'node 0.1' }] },
    { title: 'node 1', children: [{ title: 'node 1.0' }, { title: 'node 1.1' }] },
  ];

  it('0 to 1', () => {
    const expectedTreeData = [original[0], original[1], original[2]];
    const result = moveNodeBeforePath(original, [0], [1]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('0 to 2', () => {
    const expectedTreeData = [original[1], original[0], original[2]];
    const result = moveNodeBeforePath(original, [0], [2]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('0 to 3', () => {
    const expectedTreeData = [original[1], original[2], original[0]];
    const result = moveNodeBeforePath(original, [0], [3]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('1 to 2', () => {
    const expectedTreeData = [original[0], original[1], original[2]];
    const result = moveNodeBeforePath(original, [1], [2]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('1 to 3', () => {
    const expectedTreeData = [original[0], original[2], original[1]];
    const result = moveNodeBeforePath(original, [1], [3]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('1 to 0', () => {
    // target: [0] (as is)
    const expectedTreeData = [original[1], original[0], original[2]];
    const result = moveNodeBeforePath(original, [1], [0]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('2 to 0', () => {
    const expectedTreeData = [original[2], original[0], original[1]];
    const result = moveNodeBeforePath(original, [2], [0]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('2 to 1', () => {
    const expectedTreeData = [original[0], original[2], original[1]];
    const result = moveNodeBeforePath(original, [2], [1]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('0,0 to 0', () => {
    const expectedTreeData = [
      complexOriginal[0].children[0],
      { ...complexOriginal[0], children: [complexOriginal[0].children[1]] },
      complexOriginal[1],
    ];
    const result = moveNodeBeforePath(complexOriginal, [0, 0], [0]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('0,0 to 1', () => {
    const expectedTreeData = [
      { ...complexOriginal[0], children: [complexOriginal[0].children[1]] },
      complexOriginal[0].children[0],
      complexOriginal[1],
    ];
    const result = moveNodeBeforePath(complexOriginal, [0, 0], [1]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('0 to 1,0', () => {
    const expectedTreeData = [
      {
        ...complexOriginal[1],
        children: [complexOriginal[0], ...complexOriginal[1].children],
      },
    ];
    const result = moveNodeBeforePath(complexOriginal, [0], [1, 0]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('0,0 to 1,0', () => {
    const expectedTreeData = [
      { ...complexOriginal[0], children: [complexOriginal[0].children[1]] },
      {
        ...complexOriginal[1],
        children: [complexOriginal[0].children[0], ...complexOriginal[1].children],
      },
    ];
    const result = moveNodeBeforePath(complexOriginal, [0, 0], [1, 0]);
    expect(result.treeData).toEqual(expectedTreeData);
  });
});

describe('moveNodeAfterPath', () => {
  const original = [
    { title: 'node 0' },
    { title: 'node 1' },
    { title: 'node 2' },
  ];

  const complexOriginal = [
    { title: 'node 0', children: [{ title: 'node 0.0' }] },
  ];

  it('0 to 2', () => {
    const expectedTreeData = [original[1], original[2], original[0]];
    const result = moveNodeAfterPath(original, [0], [2]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('2 to 0', () => {
    const expectedTreeData = [original[0], original[2], original[1]];
    const result = moveNodeAfterPath(original, [2], [0]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('0 to 1', () => {
    const expectedTreeData = [original[1], original[0], original[2]];
    const result = moveNodeAfterPath(original, [0], [1]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('1 to 0', () => {
    const expectedTreeData = [original[0], original[1], original[2]];
    const result = moveNodeAfterPath(original, [1], [0]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('1 to 2', () => {
    const expectedTreeData = [original[0], original[2], original[1]];
    const result = moveNodeAfterPath(original, [1], [2]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('2 to 1', () => {
    const expectedTreeData = [original[0], original[1], original[2]];
    const result = moveNodeAfterPath(original, [2], [1]);
    expect(result.treeData).toEqual(expectedTreeData);
  });

  it('0,0 to 0', () => {
    const expectedTreeData = [
      { ...complexOriginal[0], children: [] },
      complexOriginal[0].children[0],
    ];
    const result = moveNodeAfterPath(complexOriginal, [0, 0], [0]);
    expect(result.treeData).toEqual(expectedTreeData);
  });
});

describe('appendNodeToPath', () => {
  const original = [
    { title: 'node 0' },
    { title: 'node 1' },
    { title: 'node 2' },
  ];

  const complexOriginal = [
    { title: 'node 0', children: [{ title: 'node 0.0' }] },
    { title: 'node 1', children: [{ title: 'node 1.0' }] },
  ];

  it('0 to 1', () => {
    const expectedTreeData = [
      { ...original[1], expanded: true, children: [original[0]] },
      original[2],
    ];
    const expectedNewPath = [0, 0];
    const result = appendNodeToPath(original, [0], [1]);
    expect(result.treeData).toEqual(expectedTreeData);
    expect(result.newPath).toEqual(expectedNewPath);
  });

  it('1 to 0', () => {
    const expectedTreeData = [
      { ...original[0], expanded: true, children: [original[1]] },
      original[2],
    ];
    const expectedNewPath = [0, 0];
    const result = appendNodeToPath(original, [1], [0]);
    expect(result.treeData).toEqual(expectedTreeData);
    expect(result.newPath).toEqual(expectedNewPath);
  });

  it('0,0 to 0', () => {
    const expectedTreeData = [
      { ...complexOriginal[0], expanded: true },
      complexOriginal[1],
    ];
    const expectedNewPath = [0, 0];
    const result = appendNodeToPath(complexOriginal, [0, 0], [0]);
    expect(result.treeData).toEqual(expectedTreeData);
    expect(result.newPath).toEqual(expectedNewPath);
  });

  it('0,0 to 1', () => {
    const expectedTreeData = [
      { ...complexOriginal[0], children: [] },
      {
        ...complexOriginal[1],
        expanded: true,
        children: [...complexOriginal[1].children, complexOriginal[0].children[0]],
      },
    ];
    const expectedNewPath = [1, 1];
    const result = appendNodeToPath(complexOriginal, [0, 0], [1]);
    expect(result.treeData).toEqual(expectedTreeData);
    expect(result.newPath).toEqual(expectedNewPath);
  });

  it('1,0 to 0', () => {
    const expectedTreeData = [
      {
        ...complexOriginal[0],
        expanded: true,
        children: [...complexOriginal[0].children, complexOriginal[1].children[0]],
      },
      { ...complexOriginal[1], children: [] },
    ];
    const expectedNewPath = [0, 1];
    const result = appendNodeToPath(complexOriginal, [1, 0], [0]);
    expect(result.treeData).toEqual(expectedTreeData);
    expect(result.newPath).toEqual(expectedNewPath);
  });
});

describe('computeAvailablePath', () => {
  it('empty tree data', () => {
    expect(computeAvailablePath([], [0])).toEqual([]);
  });
  it('empty path', () => {
    expect(computeAvailablePath(['node 1', 'node 2'], [])).toEqual([]);
  });
  it('available path', () => {
    const treeData = ['node 1', 'node 2'];
    expect(computeAvailablePath(treeData, [0])).toEqual([0]);
  });
  it('return last child path', () => {
    const treeData = ['node 1', 'node 2'];
    expect(computeAvailablePath(treeData, [2])).toEqual([1]);
  });
  it('return parent', () => {
    const treeData = [{ title: 'node 1', children: [] }];
    expect(computeAvailablePath(treeData, [0, 0])).toEqual([0]);
  });
  it('deep path', () => {
    const treeData = [{ title: 'node 1', children: [{ title: 'node 1,1', children: [] }] }];
    expect(computeAvailablePath(treeData, [0, 1, 2, 3])).toEqual([0, 0]);
  });
});
