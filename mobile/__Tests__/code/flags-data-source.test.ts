import { createMockInstance } from '../../../common/__tests__/__helpers__/utils'
import { FlagsConfiguration, FlagsConfigurationSource } from '../../code/api/flags-configuration'
import { FlagsDataSource, FlagsDataSourceConfiguration } from '../../code/flags-data-source'
import { FlagsDeveloperSettings } from '../../code/flags-developer-settings'
import { JSONItemFromJSON } from '../../../common/__tests__/__helpers__/json-helpers'

describe(FlagsDataSource, () => {
  describe('flagItemByFlagName', () => {
    it('should return flag item by its name', () => {
      const flagItem = new FlagsDataSourceConfiguration('condition', new Map(), JSONItemFromJSON('string value'))
      const map = new Map().set('flag', flagItem)
      const dataSource = new FlagsDataSource(map)

      expect(dataSource.flagConfigurationByFlagName('flag')).toBe(flagItem)
      expect(dataSource.flagConfigurationByFlagName('unknown')).toBeNull()
    })
  })
  describe('buildFromResponseItems', () => {
    it('should build data source (no identical flag names)', () => {
      const responseItems: FlagsConfiguration[] = [
        new FlagsConfiguration(
          FlagsConfigurationSource.experiment,
          'condition 1',
          new Map().set('exp 1: key 1', 'val 1').set('exp 1: key 2', 'val 2'),
          new Map()
            .set('exp 1, flag 1', JSONItemFromJSON('exp 1, flag 1 value'))
            .set('exp 1, flag 2', JSONItemFromJSON(['exp 1, flag 2 value'])),
        ),
        new FlagsConfiguration(
          FlagsConfigurationSource.experiment,
          'condition 2',
          new Map().set('exp 2: key 1', 'val 1').set('exp 2: key 2', 'val 2'),
          new Map()
            .set('exp 2, flag 1', JSONItemFromJSON('exp 2, flag 1 value'))
            .set('exp 2, flag 2', JSONItemFromJSON(['exp 2, flag 2 value'])),
        ),
      ]

      const dataSource = FlagsDataSource.buildFromConfigurations(responseItems)

      expect(dataSource.flagConfigurationByFlagName('exp 1, flag 1')).toStrictEqual(
        new FlagsDataSourceConfiguration(
          'condition 1',
          new Map().set('exp 1: key 1', 'val 1').set('exp 1: key 2', 'val 2'),
          JSONItemFromJSON('exp 1, flag 1 value'),
        ),
      )
      expect(dataSource.flagConfigurationByFlagName('exp 1, flag 2')).toStrictEqual(
        new FlagsDataSourceConfiguration(
          'condition 1',
          new Map().set('exp 1: key 1', 'val 1').set('exp 1: key 2', 'val 2'),
          JSONItemFromJSON(['exp 1, flag 2 value']),
        ),
      )
      expect(dataSource.flagConfigurationByFlagName('exp 2, flag 1')).toStrictEqual(
        new FlagsDataSourceConfiguration(
          'condition 2',
          new Map().set('exp 2: key 1', 'val 1').set('exp 2: key 2', 'val 2'),
          JSONItemFromJSON('exp 2, flag 1 value'),
        ),
      )
      expect(dataSource.flagConfigurationByFlagName('exp 2, flag 2')).toStrictEqual(
        new FlagsDataSourceConfiguration(
          'condition 2',
          new Map().set('exp 2: key 1', 'val 1').set('exp 2: key 2', 'val 2'),
          JSONItemFromJSON(['exp 2, flag 2 value']),
        ),
      )
      expect(dataSource.flagConfigurationByFlagName('unknown')).toBeNull()
    })
  })
  it('should build data source (one shared flag name)', () => {
    const responseItems: FlagsConfiguration[] = [
      new FlagsConfiguration(
        FlagsConfigurationSource.experiment,
        'condition 1',
        new Map().set('exp 1: key 1', 'val 1').set('exp 1: key 2', 'val 2'),
        new Map()
          .set('exp 1, flag 1', JSONItemFromJSON('exp 1, flag 1 value'))
          .set('shared flag name', JSONItemFromJSON(['exp 1, flag 2 value'])),
      ),
      new FlagsConfiguration(
        FlagsConfigurationSource.experiment,
        'condition 2',
        new Map().set('exp 2: key 1', 'val 1').set('exp 2: key 2', 'val 2'),
        new Map()
          .set('exp 2, flag 1', JSONItemFromJSON('exp 2, flag 1 value'))
          .set('shared flag name', JSONItemFromJSON(['exp 2, flag 2 value'])),
      ),
      new FlagsConfiguration(
        FlagsConfigurationSource.experiment,
        'condition 3',
        new Map().set('exp 3: key 1', 'val 1').set('exp 3: key 2', 'val 2'),
        new Map()
          .set('exp 3, flag 1', JSONItemFromJSON('exp 3, flag 1 value'))
          .set('exp 3, flag 2', JSONItemFromJSON(['exp 3, flag 2 value'])),
      ),
    ]

    const dataSource = FlagsDataSource.buildFromConfigurations(responseItems)

    expect(dataSource.flagConfigurationByFlagName('exp 1, flag 1')).toBeNull()
    expect(dataSource.flagConfigurationByFlagName('exp 2, flag 1')).toBeNull()
    expect(dataSource.flagConfigurationByFlagName('shared flag name')).toBeNull()
    expect(dataSource.flagConfigurationByFlagName('exp 3, flag 1')).toStrictEqual(
      new FlagsDataSourceConfiguration(
        'condition 3',
        new Map().set('exp 3: key 1', 'val 1').set('exp 3: key 2', 'val 2'),
        JSONItemFromJSON('exp 3, flag 1 value'),
      ),
    )
    expect(dataSource.flagConfigurationByFlagName('exp 3, flag 2')).toStrictEqual(
      new FlagsDataSourceConfiguration(
        'condition 3',
        new Map().set('exp 3: key 1', 'val 1').set('exp 3: key 2', 'val 2'),
        JSONItemFromJSON(['exp 3, flag 2 value']),
      ),
    )
    expect(dataSource.flagConfigurationByFlagName('unknown')).toBeNull()
  })
  it('should build data source (multiple shared flag names)', () => {
    const responseItems: FlagsConfiguration[] = [
      new FlagsConfiguration(
        FlagsConfigurationSource.experiment,
        'condition 1',
        new Map().set('exp 1: key 1', 'val 1').set('exp 1: key 2', 'val 2'),
        new Map()
          .set('shared flag name 1', JSONItemFromJSON('exp 1, flag 1 value'))
          .set('shared flag name 2', JSONItemFromJSON(['exp 1, flag 2 value'])),
      ),
      new FlagsConfiguration(
        FlagsConfigurationSource.experiment,
        'condition 2',
        new Map().set('exp 2: key 1', 'val 1').set('exp 2: key 2', 'val 2'),
        new Map()
          .set('shared flag name 1', JSONItemFromJSON('exp 2, flag 1 value'))
          .set('shared flag name 2', JSONItemFromJSON(['exp 2, flag 2 value'])),
      ),
      new FlagsConfiguration(
        FlagsConfigurationSource.experiment,
        'condition 3',
        new Map().set('exp 3: key 1', 'val 1').set('exp 3: key 2', 'val 2'),
        new Map()
          .set('exp 3, flag 1', JSONItemFromJSON('exp 3, flag 1 value'))
          .set('exp 3, flag 2', JSONItemFromJSON(['exp 3, flag 2 value'])),
      ),
    ]

    const dataSource = FlagsDataSource.buildFromConfigurations(responseItems)

    expect(dataSource.flagConfigurationByFlagName('shared flag name 1')).toBeNull()
    expect(dataSource.flagConfigurationByFlagName('shared flag name 2')).toBeNull()
    expect(dataSource.flagConfigurationByFlagName('exp 3, flag 1')).toStrictEqual(
      new FlagsDataSourceConfiguration(
        'condition 3',
        new Map().set('exp 3: key 1', 'val 1').set('exp 3: key 2', 'val 2'),
        JSONItemFromJSON('exp 3, flag 1 value'),
      ),
    )
    expect(dataSource.flagConfigurationByFlagName('exp 3, flag 2')).toStrictEqual(
      new FlagsDataSourceConfiguration(
        'condition 3',
        new Map().set('exp 3: key 1', 'val 1').set('exp 3: key 2', 'val 2'),
        JSONItemFromJSON(['exp 3, flag 2 value']),
      ),
    )
    expect(dataSource.flagConfigurationByFlagName('unknown')).toBeNull()
  })
  describe('buildFromDeveloperSettings', () => {
    it('should build data source from Developer Settings', () => {
      const devSettings = createMockInstance(FlagsDeveloperSettings, {
        getAllValues: () =>
          new Map([
            ['int flag', JSONItemFromJSON(1000)],
            ['double flag', JSONItemFromJSON(99.9)],
          ]),
      })

      const dataSource = FlagsDataSource.buildFromDeveloperSettings(devSettings)

      expect(dataSource.flagConfigurationByFlagName('int flag')).toStrictEqual(
        new FlagsDataSourceConfiguration(null, new Map(), JSONItemFromJSON(1000)),
      )
      expect(dataSource.flagConfigurationByFlagName('double flag')).toStrictEqual(
        new FlagsDataSourceConfiguration(null, new Map(), JSONItemFromJSON(99.9)),
      )
    })
  })
})
