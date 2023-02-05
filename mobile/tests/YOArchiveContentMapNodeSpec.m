// 
// YOArchiveContentMapNodeSpec.m 
// Authors:  Timur Turaev 
// Copyright © 2013-2017 Yandex. All rights reserved. 
// 

#import <Kiwi/Kiwi.h>
#import "YOArchiveContentMapNode.h"
#import "YOArchiveNode.h"

SPEC_BEGIN(YOArchiveContentMapNodeSpec)
    describe(@"YOArchiveContentMapNode", ^{
        __block YOArchiveContentMapNode *rootContentMapNode;
        __block id <YOArchiveNode> directoryNode;
        __block id <YOArchiveNode> fileNode;

        beforeAll(^{
            id <YOArchiveNode>(^createArchiveNode)(BOOL isDirectory) = ^id <YOArchiveNode>(BOOL isDirectory) {
                KWMock *mockForArchiveNode = [KWMock mockForProtocol:@protocol(YOArchiveNode)];
                [mockForArchiveNode stub:@selector(isDirectory) andReturn:theValue(isDirectory)];
                return (id <YOArchiveNode>) mockForArchiveNode;
            };

            directoryNode = createArchiveNode(YES);
            fileNode = createArchiveNode(NO);
        });

        NSString *innerName = @"name";
        NSString *rootFolderName = @"root";

        beforeEach(^{
            rootContentMapNode = [[YOArchiveContentMapNode alloc] initWithName:rootFolderName node:nil];
            [rootContentMapNode appendChildNodeWithName:innerName archiveNode:directoryNode];
            [[rootContentMapNode should] beNonNil];
        });

        it(@"should return self as child archiveNode when path components is empty", ^{
            [[[rootContentMapNode getNodeAtPathComponents:@[]] should] equal:rootContentMapNode];
        });

        it(@"should return nil child archiveNode when path doesn't exist", ^{
            [[[rootContentMapNode getNodeAtPathComponents:@[[innerName stringByAppendingString:@"1"]]] should] beNil];
        });

        it(@"should return existing archiveNode when path does exist", ^{
            YOArchiveContentMapNode *existingNode = [rootContentMapNode getNodeAtPathComponents:@[innerName]];
            YOArchiveContentMapNode *newNode = [rootContentMapNode getOrAppendChildNodeWithName:innerName];
            [[existingNode should] equal:newNode];
        });

        it(@"should allow creating folder-archiveNode when folder-archiveNode exists", ^{
            YOArchiveContentMapNode *existingNode = [rootContentMapNode getNodeAtPathComponents:@[innerName]];
            [rootContentMapNode appendChildNodeWithName:innerName archiveNode:directoryNode];
            YOArchiveContentMapNode *newNode = [rootContentMapNode getNodeAtPathComponents:@[innerName]];
            [[existingNode should] equal:newNode];
        });

        it(@"should disallow creating file-archiveNode when folder-archiveNode exists", ^{
            [[theBlock(^{
                [rootContentMapNode appendChildNodeWithName:innerName archiveNode:fileNode];
            }) should] raise];
        });

        it(@"should disallow appending file-archiveNode wothout name and/or archiveNode", ^{
            [[theBlock(^{
                [rootContentMapNode appendChildNodeWithName:nil archiveNode:nil];
            }) should] raise];

            [[theBlock(^{
                [rootContentMapNode appendChildNodeWithName:@"name" archiveNode:nil];
            }) should] raise];

            [[theBlock(^{
                [rootContentMapNode appendChildNodeWithName:nil archiveNode:directoryNode];
            }) should] raise];
        });
    });
SPEC_END
