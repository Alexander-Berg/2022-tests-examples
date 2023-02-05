// 
// YOArchiveRarContentProviderSpec.m 
// Authors:  Timur Turaev, Mariya Kachalova 
// Copyright © 2013-2017 Yandex. All rights reserved. 
// 

#import <Kiwi/Kiwi.h>
#import "YOArchiveRarContentProvider.h"
#import "YOArchiveNode.h"

SPEC_BEGIN(YOArchiveRarContentProviderSpec)
    describe(@"YOArchiveRarContentProvider", ^{
        __block NSURL *documentsURL;
        __block NSURL *archiveURL;
        __block NSURL *notArchiveURL;
        __block NSURL *passArchiveURL;

        beforeAll(^{
            documentsURL = [[[NSFileManager defaultManager] URLsForDirectory:NSDocumentDirectory
                                                                   inDomains:NSUserDomainMask] firstObject];
            [[documentsURL should] beNonNil];

            notArchiveURL = [[NSBundle bundleForClass:[self class]] URLForResource:@"not archive" withExtension:@"rar"];
            [[notArchiveURL should] beNonNil];

            archiveURL = [[NSBundle bundleForClass:[self class]] URLForResource:@"test rar archive" withExtension:@"rar"];
            [[archiveURL should] beNonNil];

            passArchiveURL = [[NSBundle bundleForClass:[self class]] URLForResource:@"rar_with_pass" withExtension:@"rar"];
            [[passArchiveURL should] beNonNil];
        });

        it(@"should be corrently initialized with url of rar archive", ^{
            YOArchiveRarContentProvider *contentProvider = [[YOArchiveRarContentProvider alloc] initWithURL:archiveURL password:nil];
            [[contentProvider should] beNonNil];
            [[contentProvider.archiveInitializationError should] beNil];
            [[contentProvider.content should] beNonNil];
            [[contentProvider.content should] haveCountOf:9];
            NSArray *array = [contentProvider.content yo_mapUsingBlock:^(id <YOArchiveNode> node) {
                        return node.filePathComponentsInsideArchive;
                    }];
            [[array should] containObjectsInArray:@[
                    @[@"content", @"file with spaces.txt"],
                    @[@"content", @"file.txt"],
                    @[@"content", @"Folder"],
                    @[@"content", @"Folder", @".DS_Store"],
                    @[@"content", @"Folder", @"file.txt"],
                    @[@"content", @"Folder / with / slashes"],
                    @[@"content", @"Folder / with / slashes", @".DS_Store"],
                    @[@"content", @"Folder with spaces"],
                    @[@"content", @"Folder with spaces", @"file with spaces.txt"],
            ]];
        });

        it(@"should be initialized with error when initializing with URL of folder", ^{
            BOOL isDirectory;
            [[theValue([[NSFileManager defaultManager] fileExistsAtPath:documentsURL.path isDirectory:&isDirectory]) should] beYes];
            [[theValue(isDirectory) should] beYes];

            YOArchiveRarContentProvider *contentProvider = [[YOArchiveRarContentProvider alloc] initWithURL:documentsURL password:nil];
            [[contentProvider should] beNonNil];
            [[contentProvider.archiveInitializationError should] beNonNil];
        });

        it(@"should be initialized with error when initializing with URL of non existing file", ^{
            NSURL *nonExistingArchiveURL = [documentsURL URLByAppendingPathComponent:@"temp.rar" isDirectory:NO];
            [[theValue([[NSFileManager defaultManager] fileExistsAtPath:nonExistingArchiveURL.path]) should] beNo];

            YOArchiveRarContentProvider *contentProvider = [[YOArchiveRarContentProvider alloc] initWithURL:nonExistingArchiveURL password:nil];
            [[contentProvider should] beNonNil];
            [[contentProvider.archiveInitializationError should] beNonNil];
        });

        it(@"should be initialized with error when initializing with URL of incorrect archive file", ^{
            YOArchiveRarContentProvider *contentProvider = [[YOArchiveRarContentProvider alloc] initWithURL:notArchiveURL password:nil];
            [[contentProvider should] beNonNil];
            [[contentProvider.archiveInitializationError should] beNonNil];
        });

        it(@"should be initialized with error for encrypted archive without password", ^{
            YOArchiveRarContentProvider *contentProvider = [[YOArchiveRarContentProvider alloc] initWithURL:passArchiveURL password:nil];
            [[contentProvider.archiveInitializationError should] beNonNil];
            [[@(contentProvider.archiveInitializationError.code) should] equal:@(YOErrorCodeArchiveWrongPasswordError)];
        });

        it(@"should be initialized with error for encrypted archive with wrong password", ^{
            YOArchiveRarContentProvider *contentProvider = [[YOArchiveRarContentProvider alloc] initWithURL:passArchiveURL password:@"42"];
            [[contentProvider.archiveInitializationError should] beNonNil];
            [[@(contentProvider.archiveInitializationError.code) should] equal:@(YOErrorCodeArchiveWrongPasswordError)];
        });

        it(@"should be initialized without error for encrypted archive with good password", ^{
            YOArchiveRarContentProvider *contentProvider = [[YOArchiveRarContentProvider alloc] initWithURL:passArchiveURL password:@"1111"];
            [[contentProvider.archiveInitializationError should] beNil];
            [[contentProvider.archiveContent should] haveCountOf:1];
        });
    });
SPEC_END
