
#import <YandexMobileAds/YandexMobileNativeAds+Extended.h>

NS_ASSUME_NONNULL_BEGIN

@interface YMANativeAdLoader (MASTestEnvironmentInternal)

- (void)mas_loadTestEnvironmentPromoAdWithRequestConfiguration:(YMANativeAdRequestConfiguration *)requestConfiguration;

- (void)mas_loadTestEnvironmentSliderAdWithRequestConfiguration:(YMANativeAdRequestConfiguration *)requestConfiguration;

@end

NS_ASSUME_NONNULL_END
