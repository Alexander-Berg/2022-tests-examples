import { Int32 } from '../../../../common/ys'

export interface ReportIntegration {
  addTestpalmId(id: Int32): void

  addFeatureName(feature: string): void
}
