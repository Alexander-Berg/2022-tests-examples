import { ArrayJSONItem } from '../../../../../../common/code/json/json-types'
import {
  ContainerStats,
  containerStatsFromJSONItem,
  ContainerStatsPayload,
} from '../../../../../code/api/entities/container/container-stats'
import { NetworkStatus, NetworkStatusCode } from '../../../../../code/api/entities/status/network-status'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import response from './sample.json'

describe(ContainerStats, () => {
  it('should parse container stats from xlist response', () => {
    const jsonItem = (JSONItemFromJSON(response)! as ArrayJSONItem).get(0)!
    const result = containerStatsFromJSONItem(jsonItem)
    expect(result).toStrictEqual(
      new ContainerStats(
        new NetworkStatus(NetworkStatusCode.ok),
        new ContainerStatsPayload(response[0].md5!, response[0].mailbox_revision!),
      ),
    )
  })
  it('should parse container stats and return null from xlist response part if malformed', () => {
    expect(containerStatsFromJSONItem(new ArrayJSONItem())).toBeNull()
  })
  it('should parse container stats and return null payload if the status is not ok', () => {
    const erroneous = { ...response, status: { status: 2 } }
    const result = containerStatsFromJSONItem(JSONItemFromJSON(erroneous))
    expect(result).toStrictEqual(new ContainerStats(new NetworkStatus(NetworkStatusCode.temporaryError), null))
  })
})
