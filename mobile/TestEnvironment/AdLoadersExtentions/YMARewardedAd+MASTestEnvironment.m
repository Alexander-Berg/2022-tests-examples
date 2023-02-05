
#import "YMARewardedAd+MASTestEnvironment.h"
#import "MASTestEnvironmentAdRequestConfigurator.h"

@implementation YMARewardedAd (MASTestEnvironment)

- (void)mas_loadTestEnvironmentAd
{
    return [self mas_loadTestEnvironmentAdWithRequest:nil];
}

- (void)mas_loadTestEnvironmentAdWithRequest:(YMAAdRequest *)request
{
    YMAAdRequest *modifiedAdRequest = [[MASTestEnvironmentAdRequestConfigurator sharedInstance] configuredAdRequestWithAdRequest:request];
    return [self loadWithRequest:modifiedAdRequest];
}

@end
