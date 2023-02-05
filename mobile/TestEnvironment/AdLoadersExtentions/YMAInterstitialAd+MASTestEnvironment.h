
#import <YandexMobileAds/YandexMobileAds.h>

NS_ASSUME_NONNULL_BEGIN

@interface YMAInterstitialAd (MASTestEnvironment)

- (void)mas_loadTestEnvironmentAd;

- (void)mas_loadTestEnvironmentAdWithRequest:(nullable YMAAdRequest *)request;

@end

NS_ASSUME_NONNULL_END
