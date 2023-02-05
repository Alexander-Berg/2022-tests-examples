import { MapJSONItem } from '../../xpackages/common/code/json/json-types'
import { MapJsonByPath } from '../../xpackages/common/__tests__/__helpers__/json-helpers'
import { FlagsConfigurationSource } from '../../xpackages/xflags/code/api/flags-configuration'
import { MailConditionParameterNames } from '../../xpackages/xmail/code/busilogics/flags/mail-condition-parameter'
import { MAIL_ANDROID_GLOBAL_CONFIG } from '../code/mail/android/android'
import { clientMail, mailFlags } from '../code/common'
import { FlagsConfigBuilder } from '../code/flags-config-builder'
import { MAIL_IOS_GLOBAL_CONFIG } from '../code/mail/ios/ios'
import { prepareStoriesByData } from '../code/stories-builder-flags'
import { stories_iteration } from '../code/stories-data'
import { testStories } from './__helpers__/files/stories-data-test'

describe('global flags config', () => {
  it('android: should contain at least one item and at least one flag', () => {
    const config = MAIL_ANDROID_GLOBAL_CONFIG.build().toJson()
    const configurations = config.getArray('configurations')
    const firstItem = configurations![0] as MapJSONItem
    expect(MapJsonByPath(firstItem, ['CONTEXT', 'MOBMAIL', 'flags']).asMap().size).toBeGreaterThan(0)
  })

  it('ios: should contain at least one item and at least one flag', () => {
    const config = MAIL_IOS_GLOBAL_CONFIG.build().toJson()
    const configurations = config.getArray('configurations')
    const firstItem = configurations![0] as MapJSONItem
    expect(MapJsonByPath(firstItem, ['CONTEXT', 'MOBMAIL', 'flags']).asMap().size).toBeGreaterThan(0)
  })

  it('stories config add', () => {
    const builder = new FlagsConfigBuilder(
      `${mailFlags}/ios`,
      clientMail,
      FlagsConfigurationSource.global,
      MailConditionParameterNames.mailParams,
    )
    prepareStoriesByData('ios', builder, testStories, stories_iteration)
    const config = builder.build().toJson()
    const configurations = config.getArray('configurations')
    expect(configurations!.length).toBe(4)
    const firstItem = configurations![0] as MapJSONItem
    const firstMap = MapJsonByPath(firstItem, ['CONTEXT', 'MOBMAIL', 'flags']).asMap()
    expect(firstMap.has('story.mailish.ru')).toBeTruthy()
    const listItem = configurations![2] as MapJSONItem
    const listMap = MapJsonByPath(listItem, ['CONTEXT', 'MOBMAIL', 'flags']).asMap()
    expect(listMap.has('stories_list')).toBeTruthy()
    const iterationItem = configurations![3] as MapJSONItem
    const iterationMap = MapJsonByPath(iterationItem, ['CONTEXT', 'MOBMAIL', 'flags']).asMap()
    expect(iterationMap.has('stories_iteration')).toBeTruthy()
  })
})
