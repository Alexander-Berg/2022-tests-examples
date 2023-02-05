import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { LabelType } from '../../../../../mapi/../mapi/code/api/entities/label/label'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Labels } from '../../../../code/busilogics/labels/labels'
import { SearchModel } from '../../../../code/busilogics/search/search-model'
import { TaskType } from '../../../../code/service/task'
import { MultiMarkWithLabelTaskOffline } from '../../../../code/service/tasks/multi-mark-with-label-task-offline'
import { MockModels } from '../../../__helpers__/models'

describe(MultiMarkWithLabelTaskOffline, () => {
  it('should throw on deserialize', () => {
    const models = MockModels()
    expect(() => MultiMarkWithLabelTaskOffline.fromJSONItem(JSONItemFromJSON({}), models)).toThrowError(
      'that command is only for offline, so no serialization',
    )
  })

  it('should throw on serialize', () => {
    const models = MockModels()
    const task = new MultiMarkWithLabelTaskOffline(
      1,
      int64(2),
      [int64(3), int64(4)],
      ['lbl_1', 'lbl_2'],
      new Map(),
      models,
    )
    expect(() => task.serialize()).toThrowError('that command is only for offline, so no serialization')
  })

  it('should throw on send request to server', () => {
    const models = MockModels()
    const task = new MultiMarkWithLabelTaskOffline(
      1,
      int64(2),
      [int64(3), int64(4)],
      ['lbl_1', 'lbl_2'],
      new Map(),
      models,
    )
    expect(() => task.sendDataToServer()).toThrowError('that command is only for offline')
  })

  it('should return type', () => {
    const models = MockModels()
    const task = new MultiMarkWithLabelTaskOffline(
      1,
      int64(2),
      [int64(3), int64(4)],
      ['lbl_1', 'lbl_2'],
      new Map(),
      models,
    )
    expect(task.getType()).toBe(TaskType.multiMarkWithLabelOffline)
  })

  it('should update database if mark with label', (done) => {
    const search = createMockInstance(SearchModel, {
      dropLabelsShowFor: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const labels = createMockInstance(Labels, {
      markWithLabel: jest.fn().mockReturnValue(resolve(getVoid())),
      unmarkWithLabels: jest.fn().mockReturnValue(resolve(getVoid())),
      getLabelsIDsByLidsAndTypes: jest.fn().mockReturnValue(resolve(['lbl_1', 'lbl_2', 'lbl_3'])),
    })
    const models = MockModels(undefined, { labels, search })

    const task = new MultiMarkWithLabelTaskOffline(
      1,
      int64(2),
      [int64(3), int64(4)],
      ['lbl_1', 'lbl_2', 'lbl_3'],
      new Map([
        ['lbl_1', true],
        ['lbl_2', false],
      ]),
      models,
    )
    expect.assertions(4)
    task.updateDatabase().then((res) => {
      expect(labels.getLabelsIDsByLidsAndTypes).toBeCalledWith(
        ['lbl_1', 'lbl_2', 'lbl_3'],
        [LabelType.important, LabelType.user],
      )
      expect(labels.markWithLabel).toBeCalledWith([int64(3), int64(4)], ['lbl_1'])
      expect(labels.unmarkWithLabels).toBeCalledWith([int64(3), int64(4)], ['lbl_2'])
      expect(search.dropLabelsShowFor).toBeCalledWith([int64(3), int64(4)], ['lbl_2'])
      done()
    })
  })
})
