// 
// YOAdsLoadersChainSpec.m 
// Authors:  Timur Turaev 
// Copyright © 2013-2017 Yandex. All rights reserved. 
// 

@import Kiwi;
@import AdLib;
@import Utils;
@import YandexMobileAds;

static NSString *const blockFailedAllLoaders = @"blockFailedAllLoaders";
static NSString *const blockFailedPrimaryLoaderPassedSecondary = @"blockFailedPrimaryLoaderPassedSecondary";
static NSString *const blockPassedPrimaryLoaders = @"blockPassedPrimaryLoaders";

@interface YOTestPrimaryAdsLoader : YOBaseAdsLoader
@property(nonatomic) BOOL isFinish;
@end

@implementation YOTestPrimaryAdsLoader

- (instancetype)init {
    self = [super init];
    if (self) {
        _isFinish = NO;
    }
    return self;
}

- (void)fetchAdsWithBlockID:(NSString *)blockID {
    NSParameterAssert(blockID);
    [super fetchAdsWithBlockID:blockID];
    
    STRONGIFY(self.delegate, delegate);
    if ([blockID isEqualToString:blockPassedPrimaryLoaders]) {
        self.isFinish = YES;
        YOAdsContent *mockContent = [[YOAdsContent alloc] initWithNativeAd:(id <YMANativeAd>) [KWMock nullMockForProtocol:@protocol(YMANativeAd)]];
        [delegate baseAdsLoader:self didFinishLoadingAdsContent:mockContent];
    } else {
        [delegate baseAdsLoader:self failedLoadingAdsContentWithError:[NSError errorWithDomain:YOErrorDomain code:1 userInfo:nil]];
    }
}

@end

@interface YOTestSecondaryAdsLoader : YOBaseAdsLoader
@end

@implementation YOTestSecondaryAdsLoader

- (void)fetchAdsWithBlockID:(NSString *)blockID {
    NSParameterAssert(blockID);
    [super fetchAdsWithBlockID:blockID];

    STRONGIFY(self.delegate, delegate);
    if ([blockID isEqualToString:blockFailedPrimaryLoaderPassedSecondary]) {
        [delegate baseAdsLoader:self
     didFinishLoadingAdsContent:[[YOAdsContent alloc] initWithNativeAd:(id <YMANativeAd>) [KWMock mockForProtocol:@protocol(YMANativeAd)]]];
    } else {
        [delegate baseAdsLoader:self failedLoadingAdsContentWithError:[NSError errorWithDomain:YOErrorDomain
                                                                                          code:2
                                                                                      userInfo:nil]];
    }
}

@end

@interface YOTestAdsLoadersChainDelegate : NSObject<YOAdsLoadersChainDelegate>
@property(nonatomic) YOAdsContent *loadedContent;
@property(nonatomic) NSError *loadingError;
@end

@implementation YOTestAdsLoadersChainDelegate

- (void)adsLoadersChainDidFinishLoadingAdsContent:(YOAdsContent *)adsContent {
    self.loadedContent = adsContent;
}

- (void)adsLoadersChainFailedLoadingAdsContentWithError:(NSError *)error {
    self.loadingError = error;
}

@end

SPEC_BEGIN(YOAdsLoadersChainSpec)
    describe(@"YOAdsLoadersChain", ^{
        __block YOBaseAdsLoader *primaryLoader;
        __block YOBaseAdsLoader *secondaryLoader;

        beforeEach(^{
            primaryLoader = [YOTestPrimaryAdsLoader new];
            secondaryLoader = [YOTestSecondaryAdsLoader new];
        });

        it(@"it should be able to create loaders chain with empty chain", ^{
            [[[YOAdsLoadersChain adsLoadersChainWithLoaders:@[]] should] beNil];
        });

        it(@"it should correctly load ad content with chain of length 1", ^{
            YOAdsLoadersChain *loadersChain = [YOAdsLoadersChain adsLoadersChainWithLoaders:@[primaryLoader]];
            YOTestAdsLoadersChainDelegate *delegate = [YOTestAdsLoadersChainDelegate new];
            loadersChain.delegate = delegate;
            [loadersChain loadAdsWithBlockID:blockPassedPrimaryLoaders];

            [[(NSObject *) delegate.loadedContent.nativeAd should] beNonNil];
            [[delegate.loadingError should] beNil];
        });

        it(@"it should correctly load ad content with chain of length 2", ^{
            YOAdsLoadersChain *loadersChain = [YOAdsLoadersChain adsLoadersChainWithLoaders:@[primaryLoader, secondaryLoader]];
            YOTestAdsLoadersChainDelegate *delegate = [YOTestAdsLoadersChainDelegate new];
            loadersChain.delegate = delegate;
            [loadersChain loadAdsWithBlockID:blockPassedPrimaryLoaders];

            [[(NSObject *) delegate.loadedContent.nativeAd should] beNonNil];
            [[delegate.loadingError should] beNil];
        });

        it(@"it should correctly load ad content with chain of length 2 (with failing only 1st loader)", ^{
            YOAdsLoadersChain *loadersChain = [YOAdsLoadersChain adsLoadersChainWithLoaders:@[primaryLoader, secondaryLoader]];
            YOTestAdsLoadersChainDelegate *delegate = [YOTestAdsLoadersChainDelegate new];
            loadersChain.delegate = delegate;
            [loadersChain loadAdsWithBlockID:blockFailedPrimaryLoaderPassedSecondary];

            [[(NSObject *) delegate.loadedContent.nativeAd should] beNonNil];

            YOTestPrimaryAdsLoader *testPrimaryLoader = (YOTestPrimaryAdsLoader *) primaryLoader;
            [[theValue(testPrimaryLoader.isFinish) should] equal:theValue(NO)];

            [[delegate.loadingError should] beNil];
        });

        it(@"it should not load ad content with chain of length 2 (with failing both 1st and 2nd loader)", ^{
            YOAdsLoadersChain *loadersChain = [YOAdsLoadersChain adsLoadersChainWithLoaders:@[primaryLoader, secondaryLoader]];
            YOTestAdsLoadersChainDelegate *delegate = [YOTestAdsLoadersChainDelegate new];
            loadersChain.delegate = delegate;
            [loadersChain loadAdsWithBlockID:blockFailedAllLoaders];

            [[delegate.loadedContent should] beNil];
            [[delegate.loadingError should] beNonNil];
            [[theValue(delegate.loadingError.code) should] equal:theValue(2)];
        });
    });
SPEC_END
