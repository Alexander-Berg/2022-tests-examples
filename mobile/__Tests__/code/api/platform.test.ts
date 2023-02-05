import { platformToClient, PlatformType } from '../../../../common/code/network/platform'
import { MockPlatform } from '../../__helpers__/mock-patches'

test('platformTypeToClient', () => {
  expect(platformToClient(MockPlatform())).toBe('aphone')
  expect(platformToClient(MockPlatform({ type: PlatformType.android, isTablet: false }))).toBe('aphone')
  expect(platformToClient(MockPlatform({ type: PlatformType.android, isTablet: true }))).toBe('apad')
  expect(platformToClient(MockPlatform({ type: PlatformType.ios, isTablet: false }))).toBe('iphone')
  expect(platformToClient(MockPlatform({ type: PlatformType.ios, isTablet: true }))).toBe('ipad')
  expect(platformToClient(MockPlatform({ type: PlatformType.electron, isTablet: false }))).toBe('unknown')
  expect(platformToClient(MockPlatform({ type: PlatformType.electron, isTablet: true }))).toBe('unknown')
  expect(platformToClient(MockPlatform({ type: PlatformType.touch, isTablet: false }))).toBe('unknown')
  expect(platformToClient(MockPlatform({ type: PlatformType.touch, isTablet: true }))).toBe('unknown')
})
