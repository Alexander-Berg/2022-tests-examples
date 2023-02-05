
#import "YMANativeAdLoader+MASTestEnvironment.h"
#import "MASTestEnvironmentAdRequestConfigurator.h"

@implementation YMANativeAdLoader (MASTestEnvironment)

- (void)mas_loadTestEnvironmentAdWithRequestConfiguration:(YMANativeAdRequestConfiguration *)requestConfiguration
{
    MASTestEnvironmentAdRequestConfigurator *configurator = [MASTestEnvironmentAdRequestConfigurator sharedInstance];
    YMANativeAdRequestConfiguration *modifiedRequestConfiguration =
        [configurator configuredNativeAdRequestConfiguration:requestConfiguration];
    [self loadAdWithRequestConfiguration:modifiedRequestConfiguration];
}

@end
