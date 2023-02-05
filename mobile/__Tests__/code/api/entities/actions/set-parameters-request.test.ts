/* eslint-disable @typescript-eslint/unbound-method */
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import {
  SetParametersItem,
  SetParametersItems,
  SetParametersRequest,
} from '../../../../../code/api/entities/actions/set-parameters-request'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../common/code/network/network-request'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(SetParametersItem, () => {
  describe(SetParametersItem.createOpenFromWeb, () => {
    it('should create a parameter item with key and value', () => {
      let res = SetParametersItem.createOpenFromWeb(true)
      expect(res.key).toBe('mobile_open_from_web')
      expect(res.value).toBe('1')

      res = SetParametersItem.createOpenFromWeb(false)
      expect(res.key).toBe('mobile_open_from_web')
      expect(res.value).toBe('0')
    })
  })
  describe(SetParametersItem.createShowFolderTabs, () => {
    it('should create a parameter item with key and value', () => {
      let res = SetParametersItem.createShowFolderTabs(true)
      expect(res.key).toBe('show_folders_tabs')
      expect(res.value).toBe('on')

      res = SetParametersItem.createShowFolderTabs(false)
      expect(res.key).toBe('show_folders_tabs')
      expect(res.value).toBe('')
    })
  })
})

describe(SetParametersItems, () => {
  describe(SetParametersItems.constructor, () => {
    it('only takes each parameter once', () => {
      const items = new SetParametersItems([
        new SetParametersItem('key1', 'value1'),
        new SetParametersItem('key2', 'value2'),
        new SetParametersItem('key1', 'value3'),
      ])
      expect(items.items).toStrictEqual([
        new SetParametersItem('key1', 'value1'),
        new SetParametersItem('key2', 'value2'),
      ])
    })
  })
  describe(SetParametersItems.fromJSONItem, () => {
    it('should be deserializable', () => {
      expect(
        SetParametersItems.fromJSONItem(
          JSONItemFromJSON([
            {
              hello: 'world',
            },
          ]),
        ),
      ).toBeNull()

      expect(
        SetParametersItems.fromJSONItem(
          JSONItemFromJSON({
            key1: 'value1',
            key2: 'value2',
          }),
        ),
      ).toStrictEqual(
        new SetParametersItems([new SetParametersItem('key1', 'value1'), new SetParametersItem('key2', 'value2')]),
      )

      expect(
        SetParametersItems.fromJSONItem(
          JSONItemFromJSON({
            key1: 'value1',
            key2: 100,
          }),
        ),
      ).toStrictEqual(new SetParametersItems([new SetParametersItem('key1', 'value1')]))
    })
  })
  describe(SetParametersItems.prototype.toJSONItem, () => {
    it('should be serializable', () => {
      expect(
        new SetParametersItems([
          new SetParametersItem('key1', 'value1'),
          new SetParametersItem('key2', 'value2'),
        ]).toJSONItem(),
      ).toStrictEqual(
        JSONItemFromJSON({
          key1: 'value1',
          key2: 'value2',
        }),
      )

      expect(new SetParametersItems([]).toJSONItem()).toStrictEqual(JSONItemFromJSON({}))
    })
  })
})

describe(SetParametersRequest, () => {
  it('should build proper request with SetParametersItems', () => {
    let request = new SetParametersRequest(
      new SetParametersItems([new SetParametersItem('key1', 'value1'), new SetParametersItem('key2', 'value2')]),
    )
    expect(request.path()).toBe('set_parameters')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.version()).toBe(NetworkAPIVersions.v1)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: 'key1=value1:key2=value2',
      }),
    )

    request = new SetParametersRequest(new SetParametersItems([]))
    expect(request.path()).toBe('set_parameters')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.version()).toBe(NetworkAPIVersions.v1)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: '',
      }),
    )
  })
})
