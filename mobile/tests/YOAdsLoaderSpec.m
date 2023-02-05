//
// YOAdsLoaderSpec.m
// Authors:  karama, Timur Turaev
// Copyright © 2013-2017 Yandex. All rights reserved.
//

@import Kiwi;
@import AdLib;
@import Utils;
@import YandexMobileAds;

@interface YONativeAdsLoader (YOTest)
- (YMANativeAdLoader *)loader;
@end

@interface YOTestDataSourceInLoaderSpec : NSObject<YOAdsLoaderDataSource>
@end

SPEC_BEGIN(YOAdsLoaderSpec)

describe(@"YOAdsLoaderSpec", ^{
    __block YONativeAdsLoader *adsLoader = nil;
    __block YMANativeAdLoader *internalLoader = nil;
    __block id<YMANativeAdLoaderDelegate> delegate = nil;
    __block id<YOAdsLoaderDataSource> dataSource = nil;
    __block NSString *blockID = nil;

    void (^validateAdsStartLoading)(BOOL, void (^)(void)) = ^(BOOL expectAdsStartLoading, void (^action)(void)) {
        __block BOOL adsStartLoading = NO;
        [internalLoader stub:@selector(loadAdWithRequestConfiguration:) withBlock:^id(NSArray *params) {
            adsStartLoading = YES;
            return nil;
        }];
        action();
        [[theValue(adsStartLoading) should] equal:theValue(expectAdsStartLoading)];
    };

    beforeEach(^{
        blockID = @"blockID";

        dataSource = [[YOTestDataSourceInLoaderSpec alloc] init];

        adsLoader = [[YONativeAdsLoader alloc] init];
        adsLoader.dataSource = dataSource;

        internalLoader = [YMANativeAdLoader mock];
        [internalLoader stub:@selector(blockID) andReturn:blockID];
        [internalLoader stub:@selector(delegate) andReturn:adsLoader];
        delegate = internalLoader.delegate;

        [adsLoader stub:@selector(loader) andReturn:internalLoader];
    });

    it(@"should start load ads if it has valid block id",^{
        validateAdsStartLoading(YES, ^{
            [adsLoader loadAdsWithBlockID:blockID];
        });
        id <YMANativeAd> nativeAd = (id <YMANativeAd>) [KWMock nullMockForProtocol:@protocol(YMANativeAd)];
        [delegate nativeAdLoader:internalLoader didLoadAd:nativeAd];
    });

    it(@"should not start load ads if it is already in progress",^{
        void (^testWithAction)(void (^)(void)) = ^(void (^contentLoadedBlock)(void)) {
            validateAdsStartLoading(YES, ^{
                [adsLoader loadAdsWithBlockID:blockID];
            });
            validateAdsStartLoading(NO, ^{
                [adsLoader loadAdsWithBlockID:blockID];
            });
            contentLoadedBlock();
            validateAdsStartLoading(YES, ^{
                [adsLoader loadAdsWithBlockID:blockID];
            });
            contentLoadedBlock();
        };

        testWithAction(^{
            id <YMANativeAd> nativeAd = (id <YMANativeAd>) [KWMock nullMockForProtocol:@protocol(YMANativeAd)];
            [delegate nativeAdLoader:internalLoader didLoadAd:nativeAd];
        });

        testWithAction(^{
            [delegate nativeAdLoader:internalLoader didFailLoadingWithError:[YOError errorWithCode:YOErrorCodeTemporaryError message:@"test"]];
        });

    });

});

SPEC_END

@implementation YOTestDataSourceInLoaderSpec

- (NSNumber *)currentUID {
    return @123123;
}

- (void)requestATTStatusWithCompletionBlock:(void (^)(BOOL))completionBlock {
    completionBlock(YES);
}

@end
