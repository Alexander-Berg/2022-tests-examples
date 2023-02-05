//
// YOAdsProviderSpec.m
// Authors:  karama, Timur Turaev
// Copyright © 2013-2017 Yandex. All rights reserved.
//

@import Kiwi;
@import AdLib;
@import Utils;
@import YandexMobileAds;

@interface YOTestDataSourceInProviderSpec: NSObject<YOAdsProviderDataSource, YOAdsLoaderDataSource>
@end

@interface YOTestDelegateInProviderSpec: NSObject<YOAdsProviderDelegate>
@property(nonatomic) NSUInteger delegateCallsCounter;
@end

SPEC_BEGIN(YOAdsProviderSpec)

describe(@"YOAdsProviderSpec", ^{
    __block YOAdsProvider *adsProvider = nil;
    __block YONativeAdsLoader *adsLoader = nil;
    __block id<YOAdsProviderDataSource, YOAdsLoaderDataSource> dataSource = nil;
    __block YOTestDelegateInProviderSpec *delegate = nil;

    void (^validateAdsReloaded)(BOOL, void (^)(void)) = ^(BOOL expectAdsReloaded, void (^action)(void)) {
        __block BOOL adsReloaded = NO;
        [adsLoader stub:@selector(loadAdsWithBlockID:) withBlock:^id(NSArray *params) {
            adsReloaded = YES;
            id <YMANativeAd> nativeAd = (id <YMANativeAd>) [KWMock mockForProtocol:@protocol(YMANativeAd)];
            [adsLoader.delegate baseAdsLoader:adsLoader didFinishLoadingAdsContent:[[YOAdsContent alloc] initWithNativeAd:nativeAd]];
            return nil;
        }];
        action();
        [[theValue(adsReloaded) should] equal:theValue(expectAdsReloaded)];
    };

    beforeEach(^{
        dataSource = [[YOTestDataSourceInProviderSpec alloc] init];
        delegate = [[YOTestDelegateInProviderSpec alloc] init];

        adsLoader = [[YONativeAdsLoader alloc] init];
        adsLoader.dataSource = dataSource;

        adsProvider = [[YOAdsProvider alloc] initWithAdsLoadersChain:[YOAdsLoadersChain adsLoadersChainWithLoaders:@[adsLoader]]];
        adsProvider.delegate = delegate;
        adsProvider.dataSource = dataSource;
    });

    afterEach(^{
        adsProvider = nil;
    });

    it(@"should load ads and call delegate callback on main thread",^{
        validateAdsReloaded(YES, ^{
             [adsProvider reloadAds];
        });

        [[theValue(delegate.delegateCallsCounter) should] equal:theValue(1)];
    });

    it(@"should load ads only if cached content is invalid",^{
        validateAdsReloaded(YES, ^{
            [adsProvider reloadAds];
        });
        validateAdsReloaded(NO, ^{
            [adsProvider reloadAds];
        });

        [adsProvider markAdContentAs:YOAdContentStatusInvalid];

        validateAdsReloaded(YES, ^{
            [adsProvider reloadAds];
        });
    });

    it(@"should not reload if dataSource is nil",^{
        adsProvider.dataSource = nil;
        
        validateAdsReloaded(NO, ^{
            [adsProvider reloadAds];
        });
    });
});

SPEC_END

@implementation YOTestDataSourceInProviderSpec

- (NSNumber *)currentUID {
    return @123123;
}

- (void)requestATTStatusWithCompletionBlock:(void (^)(BOOL))completionBlock {
    completionBlock(YES);
}

- (NSString *)blockID {
    return @"123";
}

@end

@implementation YOTestDelegateInProviderSpec

- (instancetype)init {
    self = [super init];
    if (self) {
        _delegateCallsCounter = 0;
    }
    return self;
}

- (void)adsProviderDidReloadAdContent:(nonnull YOAdsProvider *)adsProvider {
    self.delegateCallsCounter += 1;
}

@end
