
#import <YandexMobileAds/YandexMobileNativeAds.h>
#import "MASTestEnvironmentAdRequestConfigurator.h"
#import "MASBase64Encoder.h"
#import "MASSampleAppSettings.h"
#import "MASTestEnvironmentStorage.h"

static NSString *const kMASTestEnvironmentAdRequestParametersKey = @"test_environment";

@interface MASTestEnvironmentAdRequestConfigurator ()

@property (nonatomic, strong) MASBase64Encoder *base64Encoder;
@property (nonatomic, strong) MASTestEnvironmentStorage *testEnvironmentStorage;

@end

@implementation MASTestEnvironmentAdRequestConfigurator

+ (instancetype)sharedInstance
{
    static MASTestEnvironmentAdRequestConfigurator *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[MASTestEnvironmentAdRequestConfigurator alloc] init];
    });
    return sharedInstance;
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        _base64Encoder = [[MASBase64Encoder alloc] init];
        _testEnvironmentStorage = [[MASTestEnvironmentStorage alloc] init];
    }
    return self;
}

- (YMANativeAdRequestConfiguration *)configuredNativeAdRequestConfiguration:(YMANativeAdRequestConfiguration *)configuration
{
    YMAMutableNativeAdRequestConfiguration *mutableConfiguration = [configuration mutableCopy];
    NSString *testEnvironment = [self.testEnvironmentStorage testEnvironment];

    if ([testEnvironment length] != 0) {
        mutableConfiguration.parameters = [self testEnvironmentParametersWithParameters:configuration.parameters
                                                                        testEnvironment:testEnvironment];
    }
    return mutableConfiguration;
}

- (YMAAdRequest *)configuredAdRequestWithAdRequest:(YMAAdRequest *)request
{
    YMAAdRequest *nonNullRequest = request ?: [[YMAAdRequest alloc] init];
    if ([MASSampleAppSettings isTestEnvironmentEnabled] == NO) {
        return nonNullRequest;
    }

    YMAMutableAdRequest *mutableRequest = [nonNullRequest mutableCopy];
    NSString *testEnvironment = [self.testEnvironmentStorage testEnvironment];

    if ([testEnvironment length] != 0) {
        mutableRequest.parameters = [self testEnvironmentParametersWithParameters:nonNullRequest.parameters
                                                                  testEnvironment:testEnvironment];
    }

    return [mutableRequest copy];
}

- (NSDictionary *)testEnvironmentParametersWithParameters:(NSDictionary *)parameters
                                          testEnvironment:(NSString *)testEnvironment
{
    NSString *base64EncodedTestEnvironment = [self.base64Encoder base64EncodedStringFromString:testEnvironment];
    NSMutableDictionary *mutableParameters = [parameters mutableCopy] ?: [NSMutableDictionary dictionary];
    mutableParameters[kMASTestEnvironmentAdRequestParametersKey] = base64EncodedTestEnvironment;
    return [mutableParameters copy];
}

@end
