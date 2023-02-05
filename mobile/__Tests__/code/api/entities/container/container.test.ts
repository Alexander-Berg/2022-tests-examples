import { int64, stringToInt32, undefinedToNull } from '../../../../../../../common/ys'
import { idFromString } from '../../../../../code/api/common/id'
import { Container, containerFromJSONItem } from '../../../../../code/api/entities/container/container'
import { ContainerStats, ContainerStatsPayload } from '../../../../../code/api/entities/container/container-stats'
import { Folder, int32ToFolderType } from '../../../../../code/api/entities/folder/folder'
import { int32ToLabelType, Label } from '../../../../../code/api/entities/label/label'
import { NetworkStatus, NetworkStatusCode } from '../../../../../code/api/entities/status/network-status'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import sample from './sample.json'

describe(Container, () => {
  it('should fail deserialization if JSON is malformed', () => {
    const result = containerFromJSONItem(JSONItemFromJSON({ sample }))
    expect(result).toBeNull()
  })
  it('should return empty Folders and Labels if status is failure', () => {
    const badStatusSample = Object.assign([], sample, { 0: { status: { status: 2 } } })
    const result = containerFromJSONItem(JSONItemFromJSON(badStatusSample))
    expect(result).not.toBeNull()
    expect(result!.networkStatus()).toStrictEqual(new NetworkStatus(NetworkStatusCode.temporaryError))
    expect(result!.stats).toStrictEqual(new ContainerStats(new NetworkStatus(NetworkStatusCode.temporaryError), null))
    expect(result!.folders).toHaveLength(0)
    expect(result!.labels).toHaveLength(0)
  })
  it('should fail deserialization if Container Statistics cannot be parsed', () => {
    const badStatusSample = Object.assign([], sample, { 0: ['hello'] })
    const result = containerFromJSONItem(JSONItemFromJSON(badStatusSample))
    expect(result).toBeNull()
  })
  it('should return Folders and Labels parsed from JSON', () => {
    const result = containerFromJSONItem(JSONItemFromJSON(sample))
    expect(result).not.toBeNull()
    expect(result!.networkStatus()).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
    expect(result!.stats).toStrictEqual(
      new ContainerStats(
        new NetworkStatus(NetworkStatusCode.ok),
        new ContainerStatsPayload(sample[0].md5!, sample[0].mailbox_revision!),
      ),
    )
    const folders = result!.folders
    const f = (index: number): Folder =>
      new Folder(
        idFromString(undefinedToNull(sample[index].fid))!,
        int32ToFolderType(sample[index].type!),
        sample[index].display_name!,
        Number.parseInt(sample[index].options!.position, 10),
        idFromString(sample[index].parent!),
        sample[index].count_unread!,
        sample[index].count_all!,
      )
    const labels = result!.labels
    const l = (index: number): Label =>
      new Label(
        sample[index].lid!,
        int32ToLabelType(sample[index].type!),
        sample[index].display_name!,
        sample[index].count_unread!,
        sample[index].count_all!,
        stringToInt32(sample[index].color!, 16)!,
        int64(0),
      )

    expect(folders).toHaveLength(8)
    expect(folders).toStrictEqual([f(1), f(2), f(3), f(4), f(5), f(6), f(7), f(8)])
    expect(labels).toHaveLength(3)
    expect(labels).toStrictEqual([l(9), l(10), l(11)])
  })
  it('should skip those that are not Folders and Labels', () => {
    const malformed = [...sample].concat({ tid: 'BAD ITEM', display_name: 'BAD ITEM' } as any)
    const result = containerFromJSONItem(JSONItemFromJSON(malformed))
    expect(result).not.toBeNull()
    expect(result!.stats).toStrictEqual(
      new ContainerStats(
        new NetworkStatus(NetworkStatusCode.ok),
        new ContainerStatsPayload(sample[0].md5!, sample[0].mailbox_revision!),
      ),
    )
    const folders = result!.folders
    const labels = result!.labels
    expect(folders).toHaveLength(8)
    expect(labels).toHaveLength(3)
  })
})
