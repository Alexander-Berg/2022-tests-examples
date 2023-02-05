
#import "YMANativeAdLoader+MASTestEnvironmentInternal.h"
#import "MASTestEnvironmentAdRequestConfigurator.h"

@implementation YMANativeAdLoader (MASTestEnvironmentInternal)

- (void)mas_loadTestEnvironmentPromoAdWithRequestConfiguration:(YMANativeAdRequestConfiguration *)requestConfiguration
{
    MASTestEnvironmentAdRequestConfigurator *configurator = [MASTestEnvironmentAdRequestConfigurator sharedInstance];
    YMANativeAdRequestConfiguration *modifiedRequestConfiguration =
        [configurator configuredNativeAdRequestConfiguration:requestConfiguration];
    return [self loadPromoAdWithRequestConfiguration:modifiedRequestConfiguration];
}

- (void)mas_loadTestEnvironmentSliderAdWithRequestConfiguration:(YMANativeAdRequestConfiguration *)requestConfiguration
{
    MASTestEnvironmentAdRequestConfigurator *configurator = [MASTestEnvironmentAdRequestConfigurator sharedInstance];
    YMANativeAdRequestConfiguration *modifiedRequestConfiguration =
        [configurator configuredNativeAdRequestConfiguration:requestConfiguration];
    return [self loadSliderAdWithRequestConfiguration:modifiedRequestConfiguration];
}

@end
