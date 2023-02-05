// 
// YOArchiveZipContentProviderSpec.m 
// Authors:  Timur Turaev, Mariya Kachalova 
// Copyright © 2013-2017 Yandex. All rights reserved. 
// 

#import <Kiwi/Kiwi.h>
#import "YOArchiveZipContentProvider.h"
#import "YOArchiveNode.h"

SPEC_BEGIN(YOArchiveZipContentProviderSpec)
    describe(@"YOArchiveZipContentProvider", ^{
        __block NSURL *documentsURL;
        __block NSURL *zipArchiveURL;
        __block NSURL *notArchiveURL;
        __block NSURL *cyrillicZipArchive;
        __block NSURL *passArchiveURL;
        __block NSURL *dosNameZipArchive;

        beforeAll(^{
            documentsURL = [[[NSFileManager defaultManager] URLsForDirectory:NSDocumentDirectory
                                                                   inDomains:NSUserDomainMask] firstObject];
            [[documentsURL should] beNonNil];

            notArchiveURL = [[NSBundle bundleForClass:[self class]] URLForResource:@"not archive" withExtension:@"zip"];
            [[notArchiveURL should] beNonNil];

            zipArchiveURL = [[NSBundle bundleForClass:[self class]] URLForResource:@"test zip archive" withExtension:@"zip"];
            [[zipArchiveURL should] beNonNil];

            cyrillicZipArchive = [[NSBundle bundleForClass:[self class]] URLForResource:@"zip cyrillic" withExtension:@"zip"];
            [[cyrillicZipArchive should] beNonNil];

            passArchiveURL = [[NSBundle bundleForClass:[self class]] URLForResource:@"zip_with_pass" withExtension:@"zip"];
            [[passArchiveURL should] beNonNil];

            dosNameZipArchive = [[NSBundle bundleForClass:[self class]] URLForResource:@"cp866_dos" withExtension:@"zip"];
            [[dosNameZipArchive should] beNonNil];
        });

        it(@"should be corrently initialized with url of zip archive", ^{
            YOArchiveZipContentProvider *zipContentProvider = [[YOArchiveZipContentProvider alloc] initWithURL:zipArchiveURL password:nil];
            [[zipContentProvider should] beNonNil];
            [[zipContentProvider.archiveInitializationError should] beNil];
            [[zipContentProvider.content should] beNonNil];
            [[zipContentProvider.content should] haveCountOf:17];
            [[[zipContentProvider.content yo_mapUsingBlock:^(id <YOArchiveNode> node) {
                return node.filePathComponentsInsideArchive;
            }] should] containObjectsInArray:@[
                    @[@"file with spaces.txt"],
                    @[@"__MACOSX"],
                    @[@"__MACOSX", @"._file with spaces.txt"],
                    @[@"file.txt"],
                    @[@"Folder"],
                    @[@"Folder", @".DS_Store"],
                    @[@"__MACOSX", @"Folder"],
                    @[@"__MACOSX", @"Folder", @"._.DS_Store"],
                    @[@"Folder", @"file.txt"],
                    @[@"Folder / with / slashes"],
                    @[@"Folder / with / slashes", @".DS_Store"],
                    @[@"__MACOSX", @"Folder / with / slashes"],
                    @[@"__MACOSX", @"Folder / with / slashes", @"._.DS_Store"],
                    @[@"Folder with spaces"],
                    @[@"Folder with spaces", @"file with spaces.txt"],
                    @[@"__MACOSX", @"Folder with spaces"],
                    @[@"__MACOSX", @"Folder with spaces", @"._file with spaces.txt"],
            ]];
        });


        it(@"should be initialized with error when initializing with URL of folder", ^{
            BOOL isDirectory;
            [[theValue([[NSFileManager defaultManager] fileExistsAtPath:documentsURL.path isDirectory:&isDirectory]) should] beYes];
            [[theValue(isDirectory) should] beYes];

            YOArchiveZipContentProvider *zipContentProvider = [[YOArchiveZipContentProvider alloc] initWithURL:documentsURL password:nil];
            [[zipContentProvider should] beNonNil];
            [[zipContentProvider.archiveInitializationError should] beNonNil];
        });

        it(@"should be initialized with error when initializing with URL of non existing file", ^{
            NSURL *nonExistingArchiveURL = [documentsURL URLByAppendingPathComponent:@"temp.zip" isDirectory:NO];
            [[theValue([[NSFileManager defaultManager] fileExistsAtPath:nonExistingArchiveURL.path]) should] beNo];

            YOArchiveZipContentProvider *zipContentProvider = [[YOArchiveZipContentProvider alloc] initWithURL:nonExistingArchiveURL password:nil];
            [[zipContentProvider should] beNonNil];
            [[zipContentProvider.archiveInitializationError should] beNonNil];
        });

        it(@"should be initialized with error when initializing with URL of incorrect zip-archive file", ^{
            YOArchiveZipContentProvider *zipContentProvider = [[YOArchiveZipContentProvider alloc] initWithURL:notArchiveURL password:nil];
            [[zipContentProvider should] beNonNil];
            [[zipContentProvider.archiveInitializationError should] beNonNil];
        });

        it(@"should be initialized without error when opening archive with cyrillic filenames", ^{
            YOArchiveZipContentProvider *zipContentProvider = [[YOArchiveZipContentProvider alloc] initWithURL:cyrillicZipArchive password:nil];
            [[zipContentProvider should] beNonNil];
            [[zipContentProvider.archiveInitializationError should] beNil];
            [[zipContentProvider.archiveContent should] haveCountOf:3];
        });

        it(@"should be initialized without error when opening archive with cyrillic cp866 filenames", ^{
            YOArchiveZipContentProvider *zipContentProvider = [[YOArchiveZipContentProvider alloc] initWithURL:dosNameZipArchive password:nil];
            [[zipContentProvider should] beNonNil];
            [[zipContentProvider.archiveInitializationError should] beNil];
            [[zipContentProvider.archiveContent should] haveCountOf:4];
        });

        it(@"should be initialized with error for encrypted archive without password", ^{
            YOArchiveZipContentProvider *contentProvider = [[YOArchiveZipContentProvider alloc] initWithURL:passArchiveURL password:nil];
            [[contentProvider.archiveInitializationError should] beNonNil];
            [[@(contentProvider.archiveInitializationError.code) should] equal:@(YOErrorCodeArchiveWrongPasswordError)];
        });

        it(@"should be initialized with error for encrypted archive with wrong password", ^{
            YOArchiveZipContentProvider *contentProvider = [[YOArchiveZipContentProvider alloc] initWithURL:passArchiveURL password:@"42"];
            [[contentProvider.archiveInitializationError should] beNonNil];
            [[@(contentProvider.archiveInitializationError.code) should] equal:@(YOErrorCodeArchiveWrongPasswordError)];
        });

        it(@"should be initialized without error for encrypted archive with good password", ^{
            YOArchiveZipContentProvider *contentProvider = [[YOArchiveZipContentProvider alloc] initWithURL:passArchiveURL password:@"1111"];
            [[contentProvider.archiveInitializationError should] beNil];
            [[contentProvider.archiveContent should] haveCountOf:1];
        });
    });
SPEC_END
