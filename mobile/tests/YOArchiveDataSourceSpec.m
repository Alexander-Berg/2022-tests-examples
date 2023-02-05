// 
// YOArchiveDataSourceSpec.m 
// Authors:  Timur Turaev 
// Copyright © 2013-2017 Yandex. All rights reserved. 
// 

#import <Kiwi/Kiwi.h>
#import "YOArchiveDataSource.h"
#import "YOArchiveContext.h"
#import "YOArchiveDescriptor.h"
#import "YOArchiveEntryViewModel.h"
#import "YOConstants.h"

SPEC_BEGIN(YOArchiveDataSourceSpec)
    describe(@"YOArchiveDataSource", ^{

        YOArchiveDataSource *(^createDataSourceWithZipFileName)(NSString *, NSArray *, BOOL) = ^(NSString *name, NSArray *pathComponents, BOOL correct) {
            NSURL *zipArchiveURL = [[NSBundle bundleForClass:[self class]] URLForResource:name withExtension:@"zip"];
            [[zipArchiveURL should] beNonNil];

            YOArchiveDescriptor *archiveDescriptor = [[YOArchiveDescriptor alloc] initWithMimeType:[[YOMimeType alloc] initWithMimeType:@"application"
                                                                                                                            mimeSubtype:@"zip"]
                                                                                           fileURL:zipArchiveURL
                                                                                    cacheFolderUrl:[NSURL URLWithString:@""]];

            [[archiveDescriptor should] beNonNil];

            YOArchiveContext *archiveContext = [[YOArchiveContext alloc] initWithArchiveDescriptor:archiveDescriptor];
            [[archiveContext should] beNonNil];

            __block NSError *error = correct ? [[NSError alloc] init] : nil;
            YOArchiveDataSource *dataSource = [[YOArchiveDataSource alloc] initWithArchiveContext:archiveContext pathComponentsInsideArchive:pathComponents];
            [[dataSource should] beNonNil];
            [[theValue(dataSource.hasLoadedContent) should] beNo];

            [archiveContext prepareArchiveContentWithPassword:nil completionBlock:^(NSError *e) {
                error = e;
            }];
            if (correct) {
                [[expectFutureValue(error) shouldEventuallyBeforeTimingOutAfter(2)] beNil];
            } else {
                [[expectFutureValue(error) shouldEventuallyBeforeTimingOutAfter(2)] beNonNil];
            }
            [dataSource retrieveData];
            if (correct) {
                [[theValue(dataSource.hasLoadedContent) should] beYes];
            } else {
                [[theValue(dataSource.hasLoadedContent) should] beNo];
            }
            return dataSource;
        };

        void(^validateEntryWithName)(id <YOArchiveEntryViewModel>, NSString *, BOOL, NSUInteger) = ^(id <YOArchiveEntryViewModel> entry, NSString *name, BOOL isDirectory, NSUInteger size) {
            [[theValue(entry.isDirectory) should] equal:theValue(isDirectory)];
            [[entry.fileName should] equal:name];
            [[theValue(entry.size) should] equal:theValue(size)];
        };

        void(^validateFolderWithName)(id <YOArchiveEntryViewModel>, NSString *) = ^(id <YOArchiveEntryViewModel> entry, NSString *name) {
            validateEntryWithName(entry, name, YES, 0);
        };

        void(^validateFileWithName)(id <YOArchiveEntryViewModel>, NSString *, NSUInteger) = ^(id <YOArchiveEntryViewModel> entry, NSString *name, NSUInteger size) {
            validateEntryWithName(entry, name, NO, size);
        };

        it(@"should provide correct content", ^{
            {
                YOArchiveDataSource *dataSource = createDataSourceWithZipFileName(@"test zip archive", @[YOArchiveInitialPath], YES);

                [[theValue(dataSource.numberOfEntries) should] equal:theValue(5)];

                validateFolderWithName([dataSource entryByIndex:0], @"Folder");
                validateFolderWithName([dataSource entryByIndex:1], @"Folder / with / slashes");
                validateFolderWithName([dataSource entryByIndex:2], @"Folder with spaces");
                validateFileWithName([dataSource entryByIndex:3], @"file with spaces.txt", 15);
                validateFileWithName([dataSource entryByIndex:4], @"file.txt", 0);
            }
            {
                YOArchiveDataSource *dataSource = createDataSourceWithZipFileName(@"test zip archive", @[YOArchiveInitialPath, @"Folder"], YES);
                [[theValue(dataSource.numberOfEntries) should] equal:theValue(1)];
                validateFileWithName([dataSource entryByIndex:0], @"file.txt", 0);
            }
            {
                YOArchiveDataSource *dataSource = createDataSourceWithZipFileName(@"test zip archive", @[YOArchiveInitialPath, @"Folder / with / slashes"], YES);
                [[theValue(dataSource.numberOfEntries) should] equal:theValue(0)];
            }
            {
                YOArchiveDataSource *dataSource = createDataSourceWithZipFileName(@"test zip archive", @[YOArchiveInitialPath, @"Folder with spaces"], YES);
                [[theValue(dataSource.numberOfEntries) should] equal:theValue(1)];
                validateFileWithName([dataSource entryByIndex:0], @"file with spaces.txt", 15);
            }
        });

        it(@"should raised when requesting content at non-existing, incorrect or file-path", ^{
            [[theBlock(^{
                createDataSourceWithZipFileName(@"test zip archive", @[YOArchiveInitialPath, @"Folder", @"file.txt"], YES);
            }) should] raise];

            [[theBlock(^{
                createDataSourceWithZipFileName(@"test zip archive", @[YOArchiveInitialPath, @"1"], YES);
            }) should] raise];

            [[theBlock(^{
                createDataSourceWithZipFileName(@"test zip archive", nil, YES);
            }) should] raise];

            [[theBlock(^{
                createDataSourceWithZipFileName(@"not archive", nil, NO);
            }) should] raise];
        });

        it(@"should provide nil content of incorrect archive", ^{
            YOArchiveDataSource *dataSource = createDataSourceWithZipFileName(@"not archive", @[YOArchiveInitialPath], NO);
            [[theValue(dataSource.numberOfEntries) should] equal:theValue(0)];
        });
    });
SPEC_END
