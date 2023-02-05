// 
// YOArchiveContentMapSpec.m 
// Authors:  Timur Turaev 
// Copyright © 2013-2017 Yandex. All rights reserved. 
// 

#import <Kiwi/Kiwi.h>
#import "YOArchiveContentMap.h"
#import "YOArchiveDescriptor.h"
#import "YOArchiveContentMapNode.h"
#import "YOArchiveNode.h"
#import "YOConstants.h"

SPEC_BEGIN(YOArchiveContentMapSpec)
    describe(@"YOArchiveContentMap", ^{
        __block YOArchiveDescriptor *badArchiveDescriptor;
        __block YOArchiveDescriptor *zipArchiveDescriptor;
        __block YOArchiveContentMap *zipArchiveContentMap;
        __block YOArchiveContentMap *badArchiveContentMap;

        YOArchiveDescriptor *(^createZipArchiveDescriptorWithName)(NSString *) = ^(NSString *name) {
            NSURL *zipArchiveURL = [[NSBundle bundleForClass:[self class]] URLForResource:name withExtension:@"zip"];
            [[zipArchiveURL should] beNonNil];

            YOArchiveDescriptor *archiveDescriptor = [[YOArchiveDescriptor alloc] initWithMimeType:[[YOMimeType alloc] initWithMimeType:@"application"
                                                                                                                            mimeSubtype:@"zip"]
                                                                                           fileURL:zipArchiveURL
                                                                                    cacheFolderUrl:[NSURL URLWithString:@""]];
            [[archiveDescriptor should] beNonNil];
            return archiveDescriptor;
        };

        beforeAll(^{
            zipArchiveDescriptor = createZipArchiveDescriptorWithName(@"test zip archive");
            badArchiveDescriptor = createZipArchiveDescriptorWithName(@"not archive");
        });

        beforeEach(^{
            zipArchiveContentMap = [[YOArchiveContentMap alloc] initWithArchiveDescriptor:zipArchiveDescriptor];
            badArchiveContentMap = [[YOArchiveContentMap alloc] initWithArchiveDescriptor:badArchiveDescriptor];
        });

        NSArray *(^getEntityNamesInContent)(NSArray *) = ^(NSArray *content) {
            return [content yo_mapUsingBlock:^(id<YOArchiveContentMapNode> node){
                return node.name;
            }];
        };

        context(@"when initializing with correct zip archive", ^{
            it(@"should be initialized correctly", ^{
                [[zipArchiveContentMap should] beNonNil];
            });

            void(^generateFilesMap)(void) = ^{
                __block NSError *error = [[NSError alloc] init];
                [zipArchiveContentMap generateMapWithPassword:nil completionBlock:^(NSError *e) {
                    error = e;
                }];
                [[expectFutureValue(error) shouldEventuallyBeforeTimingOutAfter(2)] beNil];
            };

            it(@"should correctly generat files map", ^{
                generateFilesMap();
            });

            it(@"should return nil content without generating files map", ^{
                [[[zipArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath]] should] beNil];
            });

            void(^validateFileContent)(id <YOArchiveContentMapNode>, NSString *) =
                    ^(id <YOArchiveContentMapNode> node, NSString *givenContent) {
                        NSError *error = nil;
                        NSData *data = [node.archiveNode dataWithError:&error];
                        [[error should] beNil];
                        NSString *fileContent = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
                        [[fileContent should] equal:givenContent];
                    };

            it(@"should provide correct content with root path", ^{
                generateFilesMap();
                NSArray *content = [zipArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath]];
                NSArray *contentNames = getEntityNamesInContent(content);
                [[contentNames should] beNonNil];
                [[contentNames should] haveCountOf:6];
                [[contentNames should] contain:@"Folder with spaces"];
                [[contentNames should] contain:@"Folder"];
                [[contentNames should] contain:@"Folder / with / slashes"];
                [[contentNames should] contain:@"file.txt"];
                [[contentNames should] contain:@"file with spaces.txt"];
                [[contentNames should] contain:@"__MACOSX"];

                validateFileContent([content yo_findFirst:^BOOL(id<YOArchiveContentMapNode> node) {
                    return [node.name isEqualToString:@"file.txt"];
                }], @"");

                validateFileContent([content yo_findFirst:^BOOL(id<YOArchiveContentMapNode> node) {
                    return [node.name isEqualToString:@"file with spaces.txt"];
                }], @"√-1=i\n\\˚_˚/");
            });

            it(@"should provide correct content at correct path", ^{
                generateFilesMap();
                {
                    NSArray *content = [zipArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath, @"Folder with spaces"]];
                    NSArray *contentNames = getEntityNamesInContent(content);
                    [[contentNames should] haveCountOf:1];
                    [[contentNames should] contain:@"file with spaces.txt"];

                    validateFileContent(content.firstObject, @"√-1=i\n\\˚_˚/");
                }
                {
                    NSArray *content = [zipArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath, @"Folder"]];
                    NSArray *contentNames = getEntityNamesInContent(content);
                    [[contentNames should] haveCountOf:2];
                    [[contentNames should] contain:@"file.txt"];
                    [[contentNames should] contain:@".DS_Store"];

                    validateFileContent([content yo_findFirst:^BOOL(id<YOArchiveContentMapNode> node) {
                        return [node.name isEqualToString:@"file.txt"];
                    }], @"");
                }
                {
                    NSArray *content = [zipArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath, @"Folder / with / slashes"]];
                    NSArray *contentNames = getEntityNamesInContent(content);
                    [[contentNames should] haveCountOf:1];
                    [[contentNames should] contain:@".DS_Store"];
                }
                {
                    NSArray *content = [zipArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath, @"__MACOSX"]];
                    NSArray *contentNames = getEntityNamesInContent(content);
                    [[contentNames should] haveCountOf:4];
                    [[contentNames should] contain:@"._file with spaces.txt"];
                    [[contentNames should] contain:@"Folder"];
                    [[contentNames should] contain:@"Folder / with / slashes"];
                    [[contentNames should] contain:@"Folder with spaces"];
                }
            });

            it(@"should disallow getting content at correct, but non-existing path", ^{
                generateFilesMap();

                [[theBlock(^{
                    [zipArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath, @"qwerty"]];
                }) should] raise];

                [[theBlock(^{
                    [zipArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath, @"Folder : with : slashes"]];
                }) should] raise];
            });

            it(@"should disallow getting content at incorrect path", ^{
                generateFilesMap();

                [[theBlock(^{
                    [zipArchiveContentMap folderContentAtPathComponents:@[@"Folder"]];
                }) should] raise];

                [[theBlock(^{
                    [zipArchiveContentMap folderContentAtPathComponents:nil];
                }) should] raise];
            });
        });

        context(@"when initializing with bad zip archive", ^{
            it(@"should be initialized normal", ^{
                [[badArchiveContentMap should] beNonNil];
            });

            void(^generateFilesMap)(void) = ^{
                __block NSError *error = nil;
                [badArchiveContentMap generateMapWithPassword:nil completionBlock:^(NSError *e){
                    error = e;
                }];
                [[expectFutureValue(error) shouldEventuallyBeforeTimingOutAfter(2)] beNonNil];
            };

            it(@"should return error after generating files map", ^{
                generateFilesMap();
            });

            it(@"should return nil content without generating files map", ^{
                [[[badArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath]] should] beNil];
            });

            it(@"should allow getting content at correct path and return nil", ^{
                generateFilesMap();
                {
                    NSArray *content = [badArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath]];
                    NSArray *contentNames = getEntityNamesInContent(content);
                    [[contentNames should] beNil];
                }
                {
                    NSArray *content = [badArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath, @"qwerty"]];
                    NSArray *contentNames = getEntityNamesInContent(content);
                    [[contentNames should] beNil];
                }
                {
                    NSArray *content = [badArchiveContentMap folderContentAtPathComponents:@[YOArchiveInitialPath, @"Folder : with : slashes"]];
                    NSArray *contentNames = getEntityNamesInContent(content);
                    [[contentNames should] beNil];
                }
            });

            it(@"should disallow getting content at incorrect path", ^{
                [[theBlock(^{
                    [badArchiveContentMap folderContentAtPathComponents:@[@"Folder"]];
                }) should] raise];

                [[theBlock(^{
                    [badArchiveContentMap folderContentAtPathComponents:nil];
                }) should] raise];
            });
        });
    });
SPEC_END
