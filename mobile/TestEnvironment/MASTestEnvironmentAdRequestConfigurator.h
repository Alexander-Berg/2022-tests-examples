
#import <YandexMobileAds/YandexMobileAds.h>
#import <YandexMobileAds/YandexMobileNativeAds.h>

NS_ASSUME_NONNULL_BEGIN

@interface MASTestEnvironmentAdRequestConfigurator : NSObject

+ (instancetype)sharedInstance;

- (YMAAdRequest *)configuredAdRequestWithAdRequest:(nullable YMAAdRequest *)request;

- (YMANativeAdRequestConfiguration *)configuredNativeAdRequestConfiguration:(YMANativeAdRequestConfiguration *)configuration;

@end

NS_ASSUME_NONNULL_END
