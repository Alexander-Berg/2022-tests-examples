
#import <YandexMobileAds/YandexMobileAds.h>

NS_ASSUME_NONNULL_BEGIN

@interface YMAAdView (MASTestEnvironment)

- (void)mas_loadTestEnvironmentAd;

- (void)mas_loadTestEnvironmentAdWithRequest:(nullable YMAAdRequest *)request;

@end

NS_ASSUME_NONNULL_END
