//
// Created by Alexey Zarovny on 08.08.2018.
// Copyright (c) 2018 Yandex. All rights reserved.
//

import Foundation

enum Folders {
    static let testDir = "TestDir"
    static let testDirUpperCase = "TESTDIR"
    static let anotherTestDir = "AnotherTestDir"
    static let sharedDir = "XCUI shared dir"
    static let containerDir = "ContainerDir"
    static let dir1 = "Dir1"
    static let dir2 = "Dir2"
    static let dir3 = "Dir3"
    static let dir4 = "Dir4"
    static let faDir = "FA Dir"
    static let roDir = "RO Dir"
    static let renamedDir = "RenamedDir"
    static let cameraUploadsDir = "Camera Uploads"
    static let incorrectName = "Test/Dir"
    static let onlySpaces = "         "
    static let nonEnglishDir = "Директория ҐґЄєІЇії Türk"
    static let symbolicDir = "Dir_-$%№#*&@+()[];{}?!.,\"><"
    static let digitDir = "987654321"
    static let emojiDir = "😀"
    static let emptyDirName = "Папка с пустой папкой"
    static let notEmptyDirName = "Папка с не пустой папкой"
    static let newdDir = "New folder"
}

enum Files {
    static let simpleFile = "simpleFile"
    static let simpleFileUpperCase = "SIMPLEFILE"
    static let cyrrilicFile = "Киррилический файл"
    static let symbolicFile = "File_-$%№#*&@+()[];{}?!.,\"><"
    static let nonLatinFile = "File Türk ҐґЄєІЇії"
    static let generatedTxtFile = "generated.txt"
    static let generatedTxtFileUpperCaseExtension = "generated.TXT"
    static let generatedFile = "generated"
    static let pngImage = "pngImage.png"
    static let restoredPngImage = "pngImage (1).png"
    static let renamedImage = "renamedImage.png"
    static let renamedImageName = "renamedImage"
    static let renamedFile = "renamedFile"
    static let photo = "photo.jpg"
    static let photo1 = "photo1.jpg"
    static let photo2 = "ocean.jpg"
    static let photo3 = "Sunset.jpg"
    static let renamedPhoto = "renamedPhoto.jpg"
    static let renamedPhotoName = "renamedPhoto"
    static let photoPrefix = "photo"
    static let slash = "/"
    static let cloudPhoto1 = "Cat_1.HEIC"
    static let cloudPhoto2 = "Cat_2.HEIC"
    static let cloudPhoto3 = "Cat_3.HEIC"
    static let cloudPhoto4 = "photo.JPG"
    static let cloudVideo1 = "Cat_1.MOV"
    static let cloudFile = "document.docx"
    static let geoPhoto = "geoPhoto.jpg"
    static let cameraPhoto1 = "1.JPG"
    static let cameraPhoto2 = "2.JPG"
    static let cameraPhoto3 = "3.JPG"
    static let cameraPhoto4 = "4.JPG"
}

enum DefaultFiles {
    static let bears = "Bears.jpg"
    static let moscow = "Moscow.jpg"
    static let mountains = "Mountains.jpg"
    static let sea = "Sea.jpg"
    static let stPetersburg = "St. Petersburg.jpg"
    static let winter = "Winter.jpg"

    static let allCases: [String] = [
        bears,
        moscow,
        mountains,
        sea,
        stPetersburg,
        winter,
    ]
}

enum Sort {
    static let folder1 = "Aaaa"
    static let folder2 = "AAaa"
    static let folder3 = "BBcc"
    static let file1 = "321"
    static let file2 = "1235"
    static let file3 = "BB12"
    static let file4 = "BBaa"
    static let file5 = "BBbc"
    static let file6 = "BBcb"
}

enum PublicLinks {
    static let bigFolder = "https://yadi.sk/d/q_7SPb8Dabj6Dg"
}
