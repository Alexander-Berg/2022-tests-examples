process.title = 'rom-server';
var express = require('express');
var features = [
    { "id": 1, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-22.120486898347735, -154.30312565993518] }, "number": 1, "properties": { "iconContent": 2, "balloonContent": 1, "hintContent": 1, "clusterCaption": 1, "type": "банк"}, "options": {iconLayout: 'default#image',
            // Своё изображение иконки метки.
            iconImageHref: 'https://sandbox.api.maps.yandex.net/examples/ru/2.1/icon_customImage/images/myIcon.gif',
            // Размеры метки.
            iconImageSize: [30, 42],
            // Смещение левого верхнего угла иконки относительно
            // её "ножки" (точки привязки).
            iconImageOffset: [-5, -38]}},
    { "id": 2, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-18.229354323120788, -134.3152914242819] }, "number": 2, "properties": { "iconContent": 2, "balloonContent": 2, "hintContent": 2, "clusterCaption": 2, "type": "2"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 3, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-75.78636634163558, 145.2432746719569] }, "number": 3, "properties": { "iconContent": 2, "balloonContent": 3, "hintContent": 3, "clusterCaption": 3, "type": "банк"}, "options": {"preset": "islands#orangeIcon",
        iconImageClipRect: [
            [14, 15],
            [23, 24]
        ]}},
    { "id": 4, "type": "Feature", "geometry": { "type": "Point", "coordinates": [77.47296266141348, -153.86876072734594] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 4, "hintContent": 4, "clusterCaption": 4, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 5, "type": "Feature", "geometry": { "type": "Point", "coordinates": [63.78259275108576, 35.05624413024634] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 5, "hintContent": 5, "clusterCaption": 5, "type": "5"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 6, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-70.41897314134985, 95.02780637238175] }, "number": 6, "properties": { "iconContent": 2, "balloonContent": 6, "hintContent": 6, "clusterCaption": 6, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 7, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-83.6804704356473, 89.31367107667029] }, "number": 7, "properties": { "iconContent": 2, "balloonContent": 7, "hintContent": 7, "clusterCaption": 7, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 8, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-67.66045948606916, -150.42151655070484] }, "number": 8, "properties": { "iconContent": 2, "balloonContent": 8, "hintContent": 8, "clusterCaption": 8, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 9, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-27.255450647789985, 6.240609213709831] }, "number": 9, "properties": { "iconContent": 2, "balloonContent": 9, "hintContent": 9, "clusterCaption": 9, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 10, "type": "Feature", "geometry": { "type": "Point", "coordinates": [37.09250497864559, 149.86615121830255] }, "number": 10, "properties": { "iconContent": 2, "balloonContent": 10, "hintContent": 10, "clusterCaption": 10, "type": "10"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 11, "type": "Feature", "geometry": { "type": "Point", "coordinates": [75.61742190504447, 21.96357311680913] }, "number": 11, "properties": { "iconContent": 2, "balloonContent": 11, "hintContent": 11, "clusterCaption": 11, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 12, "type": "Feature", "geometry": { "type": "Point", "coordinates": [39.38053983380087, 164.266407340765] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 12, "hintContent": 12, "clusterCaption": 12, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 13, "type": "Feature", "geometry": { "type": "Point", "coordinates": [30.448064191732556, -13.388731861487031] }, "number": 13, "properties": { "iconContent": 2, "balloonContent": 13, "hintContent": 13, "clusterCaption": 13, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 14, "type": "Feature", "geometry": { "type": "Point", "coordinates": [22.7360946952831, -0.6816119654104114] }, "number": 14, "properties": { "iconContent": 2, "balloonContent": 14, "hintContent": 14, "clusterCaption": 14, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 15, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-25.069462325191125, -57.581290137022734] }, "number": 15, "properties": { "iconContent": 2, "balloonContent": 15, "hintContent": 15, "clusterCaption": 15, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 16, "type": "Feature", "geometry": { "type": "Point", "coordinates": [17.536084690364078, 52.63912749476731] }, "number": 16, "properties": { "iconContent": 2, "balloonContent": 16, "hintContent": 16, "clusterCaption": 16, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 17, "type": "Feature", "geometry": { "type": "Point", "coordinates": [65.60983541654423, 120.20627089776099] }, "number": 17, "properties": { "iconContent": 2, "balloonContent": 17, "hintContent": 17, "clusterCaption": 17, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 18, "type": "Feature", "geometry": { "type": "Point", "coordinates": [62.19957719091326, -142.51861450262368] }, "number": 18, "properties": { "iconContent": 2, "balloonContent": 18, "hintContent": 18, "clusterCaption": 18, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 19, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-63.61374536762014, 94.90228577982634] }, "number": 19, "properties": { "iconContent": 2, "balloonContent": 19, "hintContent": 19, "clusterCaption": 19, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 20, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-60.13382524251938, -162.68395288847387] }, "number": 20, "properties": { "iconContent": 2, "balloonContent": 20, "hintContent": 20, "clusterCaption": 20, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 21, "type": "Feature", "geometry": { "type": "Point", "coordinates": [8.547760852379724, -116.13272903021425] }, "number": 21, "properties": { "iconContent": 2, "balloonContent": 21, "hintContent": 21, "clusterCaption": 21, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 22, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-35.4531797056552, 99.25217400770634] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 22, "hintContent": 22, "clusterCaption": 22, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 23, "type": "Feature", "geometry": { "type": "Point", "coordinates": [74.38374527031556, -169.65882918331772] }, "number": 23, "properties": { "iconContent": 2, "balloonContent": 23, "hintContent": 23, "clusterCaption": 23, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 24, "type": "Feature", "geometry": { "type": "Point", "coordinates": [17.27167526492849, -4.07685661688447] }, "number": 24, "properties": { "iconContent": 2, "balloonContent": 24, "hintContent": 24, "clusterCaption": 24, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 25, "type": "Feature", "geometry": { "type": "Point", "coordinates": [57, 11.756812068633735] }, "number": 25, "properties": { "iconContent": 2, "balloonContent": 25, "hintContent": 25, "clusterCaption": 25, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 26, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-8.773308674572036, 162.80902706086636] }, "number": 26, "properties": { "iconContent": 2, "balloonContent": 26, "hintContent": 26, "clusterCaption": 26, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 27, "type": "Feature", "geometry": { "type": "Point", "coordinates": [1.0534691077191383, -113.14323051832616] }, "number": 27, "properties": { "iconContent": 2, "balloonContent": 27, "hintContent": 27, "clusterCaption": 27, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 28, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-70.96698547480628, 59.053600137121975] }, "number": 28, "properties": { "iconContent": 2, "balloonContent": 28, "hintContent": 28, "clusterCaption": 28, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 29, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-10.830404232256114, 42.54226019605994] }, "number": 29, "properties": { "iconContent": 2, "balloonContent": 29, "hintContent": 29, "clusterCaption": 29, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 30, "type": "Feature", "geometry": { "type": "Point", "coordinates": [57, 33.95163950510323] }, "number": 30, "properties": { "iconContent": 2, "balloonContent": 30, "hintContent": 30, "clusterCaption": 30, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 31, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-26.57231908873655, 165.06300032604486] }, "number": 31, "properties": { "iconContent": 2, "balloonContent": 31, "hintContent": 31, "clusterCaption": 31, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 32, "type": "Feature", "geometry": { "type": "Point", "coordinates": [41.87239379272796, 134.97857686132193] }, "number": 32, "properties": { "iconContent": 2, "balloonContent": 32, "hintContent": 32, "clusterCaption": 32, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 33, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-0.9409525140654296, -172.104812823236] }, "number": 33, "properties": { "iconContent": 2, "balloonContent": 33, "hintContent": 33, "clusterCaption": 33, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 34, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-69.79867022135295, -118.01427585538477] }, "number": 34, "properties": { "iconContent": 2, "balloonContent": 34, "hintContent": 34, "clusterCaption": 34, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 35, "type": "Feature", "geometry": { "type": "Point", "coordinates": [9.752417949493974, -141.71413087751716] }, "number": 35, "properties": { "iconContent": 2, "balloonContent": 35, "hintContent": 35, "clusterCaption": 35, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 36, "type": "Feature", "geometry": { "type": "Point", "coordinates": [1.3157028530258685, 25.752516514621675] }, "number": 36, "properties": { "iconContent": 2, "balloonContent": 36, "hintContent": 36, "clusterCaption": 36, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 37, "type": "Feature", "geometry": { "type": "Point", "coordinates": [50.76248971512541, -121.54766491614282] }, "number": 37, "properties": { "iconContent": 2, "balloonContent": 37, "hintContent": 37, "clusterCaption": 37, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 38, "type": "Feature", "geometry": { "type": "Point", "coordinates": [0.5424040916841477, -115.32410679850727] }, "number": 38, "properties": { "iconContent": 2, "balloonContent": 38, "hintContent": 38, "clusterCaption": 38, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 39, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-25.556274054106325, 149.3528720922768] }, "number": 39, "properties": { "iconContent": 2, "balloonContent": 39, "hintContent": 39, "clusterCaption": 39, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 40, "type": "Feature", "geometry": { "type": "Point", "coordinates": [78.97977277636528, 25.392930186353624] }, "number": 40, "properties": { "iconContent": 2, "balloonContent": 40, "hintContent": 40, "clusterCaption": 40, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 41, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-24.170429141959175, -130.237110927701] }, "number": 41, "properties": { "iconContent": 2, "balloonContent": 41, "hintContent": 41, "clusterCaption": 41, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 42, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-76.42195896827616, -22.777860551141202] }, "number": 42, "properties": { "iconContent": 2, "balloonContent": 42, "hintContent": 42, "clusterCaption": 42, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 43, "type": "Feature", "geometry": { "type": "Point", "coordinates": [62.396588049596176, -157.76833971496671] }, "number": 43, "properties": { "iconContent": 2, "balloonContent": 43, "hintContent": 43, "clusterCaption": 43, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 44, "type": "Feature", "geometry": { "type": "Point", "coordinates": [51.07890734565444, -47.90708156302571] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 44, "hintContent": 44, "clusterCaption": 44, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 45, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-46.09802938532084, -174.14002780336887] }, "number": 45, "properties": { "iconContent": 2, "balloonContent": 45, "hintContent": 45, "clusterCaption": 45, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 46, "type": "Feature", "geometry": { "type": "Point", "coordinates": [49.71931027714163, -38.07609736919403] }, "number": 46, "properties": { "iconContent": 2, "balloonContent": 46, "hintContent": 46, "clusterCaption": 46, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 47, "type": "Feature", "geometry": { "type": "Point", "coordinates": [49.566395319998264, -34.53976064454764] }, "number": 47, "properties": { "iconContent": 2, "balloonContent": 47, "hintContent": 47, "clusterCaption": 47, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 48, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-1.2213951593730599, 105.737439324148] }, "number": 48, "properties": { "iconContent": 2, "balloonContent": 48, "hintContent": 48, "clusterCaption": 48, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 49, "type": "Feature", "geometry": { "type": "Point", "coordinates": [39.320936885196716, 119.551798151806] }, "number": 49, "properties": { "iconContent": 2, "balloonContent": 49, "hintContent": 49, "clusterCaption": 49, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 50, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-35.26491253403947, -72.9778978927061] }, "number": 50, "properties": { "iconContent": 2, "balloonContent": 50, "hintContent": 50, "clusterCaption": 50, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 51, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-37.53933424362913, 11.217420129105449] }, "number": 51, "properties": { "iconContent": 2, "balloonContent": 51, "hintContent": 51, "clusterCaption": 51, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 52, "type": "Feature", "geometry": { "type": "Point", "coordinates": [71.42111941240728, -111.81821658276021] }, "number": 52, "properties": { "iconContent": 2, "balloonContent": 52, "hintContent": 52, "clusterCaption": 52, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 53, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-26.42306115827523, -15.782075305469334] }, "number": 53, "properties": { "iconContent": 2, "balloonContent": 53, "hintContent": 53, "clusterCaption": 53, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 54, "type": "Feature", "geometry": { "type": "Point", "coordinates": [33.15064777736552, 49.47859293781221] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 54, "hintContent": 54, "clusterCaption": 54, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 55, "type": "Feature", "geometry": { "type": "Point", "coordinates": [31.912814383395016, 133.64258942194283] }, "number": 55, "properties": { "iconContent": 2, "balloonContent": 55, "hintContent": 55, "clusterCaption": 55, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 56, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-77.27813892182894, 14.036481943912804] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 56, "hintContent": 56, "clusterCaption": 56, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 57, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-10.515316233504564, -162.45998562779278] }, "number": 57, "properties": { "iconContent": 2, "balloonContent": 57, "hintContent": 57, "clusterCaption": 57, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 58, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-35.90748235816136, -57.546950904652476] }, "number": 58, "properties": { "iconContent": 2, "balloonContent": 58, "hintContent": 58, "clusterCaption": 58, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 59, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-78.43777199741453, 91.9158340478316] }, "number": 59, "properties": { "iconContent": 2, "balloonContent": 59, "hintContent": 59, "clusterCaption": 59, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 60, "type": "Feature", "geometry": { "type": "Point", "coordinates": [43.09108052868396, 63.16673350520432] }, "number": 60, "properties": { "iconContent": 2, "balloonContent": 60, "hintContent": 60, "clusterCaption": 60, "type": "60"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 61, "type": "Feature", "geometry": { "type": "Point", "coordinates": [28.955794530920684, -24.866685681045055] }, "number": 61, "properties": { "iconContent": 2, "balloonContent": 61, "hintContent": 61, "clusterCaption": 61, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 62, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-1.7357972473837435, 27.705758409574628] }, "number": 62, "properties": { "iconContent": 2, "balloonContent": 62, "hintContent": 62, "clusterCaption": 62, "type": "62"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 63, "type": "Feature", "geometry": { "type": "Point", "coordinates": [12.903401619987562, -115.66630042623729] }, "number": 63, "properties": { "iconContent": 2, "balloonContent": 63, "hintContent": 63, "clusterCaption": 63, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 64, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-61.0817191330716, -120.49432128667831] }, "number": 64, "properties": { "iconContent": 2, "balloonContent": 64, "hintContent": 64, "clusterCaption": 64, "type": "64"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 65, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-80.43446542229503, -153.42924683354795] }, "number": 65, "properties": { "iconContent": 2, "balloonContent": 65, "hintContent": 65, "clusterCaption": 65, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 66, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-22.724066460505128, -68.04126051720232] }, "number": 66, "properties": { "iconContent": 2, "balloonContent": 66, "hintContent": 66, "clusterCaption": 66, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 67, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-63.23557224357501, 9.714589440263808] }, "number": 67, "properties": { "iconContent": 2, "balloonContent": 67, "hintContent": 67, "clusterCaption": 67, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 68, "type": "Feature", "geometry": { "type": "Point", "coordinates": [69.9369633058086, -69.80254270602018] }, "number": 68, "properties": { "iconContent": 2, "balloonContent": 68, "hintContent": 68, "clusterCaption": 68, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 69, "type": "Feature", "geometry": { "type": "Point", "coordinates": [53.766535685863346, 161.97843158617616] }, "number": 69, "properties": { "iconContent": 2, "balloonContent": 69, "hintContent": 69, "clusterCaption": 69, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 70, "type": "Feature", "geometry": { "type": "Point", "coordinates": [14.242640133015811, -46.179194930009544] }, "number": 70, "properties": { "iconContent": 2, "balloonContent": 70, "hintContent": 70, "clusterCaption": 70, "type": "70"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 71, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-36.19695171597414, -57.49910800717771] }, "number": 71, "properties": { "iconContent": 2, "balloonContent": 71, "hintContent": 71, "clusterCaption": 71, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 72, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-14.966422801371664, -13.977497527375817] }, "number": 72, "properties": { "iconContent": 2, "balloonContent": 72, "hintContent": 72, "clusterCaption": 72, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 73, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-0.029561384581029415, 26.58658544998616] }, "number": 73, "properties": { "iconContent": 2, "balloonContent": 73, "hintContent": 73, "clusterCaption": 73, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 74, "type": "Feature", "geometry": { "type": "Point", "coordinates": [58.56910969829187, 38.42266408726573] }, "number": 74, "properties": { "iconContent": 2, "balloonContent": 74, "hintContent": 74, "clusterCaption": 74, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 75, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-20.64840951235965, 168.4825969208032] }, "number": 75, "properties": { "iconContent": 2, "balloonContent": 75, "hintContent": 75, "clusterCaption": 75, "type": "75"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 76, "type": "Feature", "geometry": { "type": "Point", "coordinates": [11.94336472894065, 16.55747360084206] }, "number": 76, "properties": { "iconContent": 2, "balloonContent": 76, "hintContent": 76, "clusterCaption": 76, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 77, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-58.52935106609948, -14.377918811514974] }, "number": 77, "properties": { "iconContent": 2, "balloonContent": 77, "hintContent": 77, "clusterCaption": 77, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 78, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-61.44946433021687, 86.85873677954078] }, "number": 78, "properties": { "iconContent": 2, "balloonContent": 78, "hintContent": 78, "clusterCaption": 78, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 79, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-17.558601094642654, 97.4793195957318] }, "number": 79, "properties": { "iconContent": 2, "balloonContent": 79, "hintContent": 79, "clusterCaption": 79, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 80, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-37.01097710756585, 12.186556435190141] }, "number": 80, "properties": { "iconContent": 2, "balloonContent": 80, "hintContent": 80, "clusterCaption": 80, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 81, "type": "Feature", "geometry": { "type": "Point", "coordinates": [11.298646298237145, -166.41829614993185] }, "number": 81, "properties": { "iconContent": 2, "balloonContent": 81, "hintContent": 81, "clusterCaption": 81, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 82, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-40.3000547690317, -151.94783148821443] }, "number": 82, "properties": { "iconContent": 2, "balloonContent": 82, "hintContent": 82, "clusterCaption": 82, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 83, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-9.540807799203321, 146.75126903224736] }, "number": 83, "properties": { "iconContent": 2, "balloonContent": 83, "hintContent": 83, "clusterCaption": 83, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 84, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-10.674095720751211, -128.37751043960452] }, "number": 84, "properties": { "iconContent": 2, "balloonContent": 84, "hintContent": 84, "clusterCaption": 84, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 85, "type": "Feature", "geometry": { "type": "Point", "coordinates": [12.177281602052972, 6.496756570413709] }, "number": 85, "properties": { "iconContent": 2, "balloonContent": 85, "hintContent": 85, "clusterCaption": 85, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 86, "type": "Feature", "geometry": { "type": "Point", "coordinates": [49.92404194548726, -153.00348508637398] }, "number": 86, "properties": { "iconContent": 2, "balloonContent": 86, "hintContent": 86, "clusterCaption": 86, "type": "86"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 87, "type": "Feature", "geometry": { "type": "Point", "coordinates": [58.9763172226958, -96.11503883264959] }, "number": 87, "properties": { "iconContent": 2, "balloonContent": 87, "hintContent": 87, "clusterCaption": 87, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 88, "type": "Feature", "geometry": { "type": "Point", "coordinates": [23.336628661490977, 150.47562814783305] }, "number": 88, "properties": { "iconContent": 2, "balloonContent": 88, "hintContent": 88, "clusterCaption": 88, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 89, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-23.009235775098205, -49.845305434428155] }, "number": 89, "properties": { "iconContent": 2, "balloonContent": 89, "hintContent": 89, "clusterCaption": 89, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 90, "type": "Feature", "geometry": { "type": "Point", "coordinates": [69.88022245466709, 147.4206282477826] }, "number": 90, "properties": { "iconContent": 2, "balloonContent": 90, "hintContent": 90, "clusterCaption": 90, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 91, "type": "Feature", "geometry": { "type": "Point", "coordinates": [38.51314887870103, -73.85071779135615] }, "number": 91, "properties": { "iconContent": 2, "balloonContent": 91, "hintContent": 91, "clusterCaption": 91, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 92, "type": "Feature", "geometry": { "type": "Point", "coordinates": [22.684620124055073, -122.85106523428112] }, "number": 92, "properties": { "iconContent": 2, "balloonContent": 92, "hintContent": 92, "clusterCaption": 92, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 93, "type": "Feature", "geometry": { "type": "Point", "coordinates": [43.57195228571072, 135.18798680976033] }, "number": 93, "properties": { "iconContent": 2, "balloonContent": 93, "hintContent": 93, "clusterCaption": 93, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 94, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-55.826469354797155, 132.21472178120166] }, "number": 94, "properties": { "iconContent": 2, "balloonContent": 94, "hintContent": 94, "clusterCaption": 94, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 95, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-11.56536857248284, 3.7418871000409126] }, "number": 95, "properties": { "iconContent": 2, "balloonContent": 95, "hintContent": 95, "clusterCaption": 95, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 96, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-49.951358006801456, -33.176036230288446] }, "number": 96, "properties": { "iconContent": 2, "balloonContent": 96, "hintContent": 96, "clusterCaption": 96, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 97, "type": "Feature", "geometry": { "type": "Point", "coordinates": [54.897886087419465, -123.85704595595598] }, "number": 97, "properties": { "iconContent": 2, "balloonContent": 97, "hintContent": 97, "clusterCaption": 97, "type": "97"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 98, "type": "Feature", "geometry": { "type": "Point", "coordinates": [57.11749534239061, -162.33967887237668] }, "number": 98, "properties": { "iconContent": 2, "balloonContent": 98, "hintContent": 98, "clusterCaption": 98, "type": "98"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 99, "type": "Feature", "geometry": { "type": "Point", "coordinates": [70.8715494594071, 84.40932787489146] }, "number": 99, "properties": { "iconContent": 2, "balloonContent": 99, "hintContent": 99, "clusterCaption": 99, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 100, "type": "Feature", "geometry": { "type": "Point", "coordinates": [27.061156089184806, -97.77701122686267] }, "number": 100, "properties": { "iconContent": 2, "balloonContent": 100, "hintContent": 100, "clusterCaption": 100, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 101, "type": "Feature", "geometry": { "type": "Point", "coordinates": [18.484233523486182, -42.770757246762514] }, "number": 101, "properties": { "iconContent": 2, "balloonContent": 101, "hintContent": 101, "clusterCaption": 101, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 102, "type": "Feature", "geometry": { "type": "Point", "coordinates": [25.048159243306145, -157.36777325626463] }, "number": 102, "properties": { "iconContent": 2, "balloonContent": 102, "hintContent": 102, "clusterCaption": 102, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 103, "type": "Feature", "geometry": { "type": "Point", "coordinates": [3.9508186641614884, 31.6593260737136] }, "number": 103, "properties": { "iconContent": 2, "balloonContent": 103, "hintContent": 103, "clusterCaption": 103, "type": "103"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 104, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-56.0951353830751, -30.83513889927417] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 104, "hintContent": 104, "clusterCaption": 104, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 105, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-40.2892662177328, 36.19856136851013] }, "number": 105, "properties": { "iconContent": 2, "balloonContent": 105, "hintContent": 105, "clusterCaption": 105, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 106, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-55.356467528035864, 96.25819943379611] }, "number": 106, "properties": { "iconContent": 2, "balloonContent": 106, "hintContent": 106, "clusterCaption": 106, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 107, "type": "Feature", "geometry": { "type": "Point", "coordinates": [77.90476181311533, 40.25502917356789] }, "number": 107, "properties": { "iconContent": 2, "balloonContent": 107, "hintContent": 107, "clusterCaption": 107, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 108, "type": "Feature", "geometry": { "type": "Point", "coordinates": [4.914768850430846, -41.67188668157905] }, "number": 108, "properties": { "iconContent": 2, "balloonContent": 108, "hintContent": 108, "clusterCaption": 108, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 109, "type": "Feature", "geometry": { "type": "Point", "coordinates": [65.11277941754088, -45.657912427559495] }, "number": 109, "properties": { "iconContent": 2, "balloonContent": 109, "hintContent": 109, "clusterCaption": 109, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 110, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-3.397750227013603, -157.7899539610371] }, "number": 110, "properties": { "iconContent": 2, "balloonContent": 110, "hintContent": 110, "clusterCaption": 110, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 111, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-10.555053531425074, -80.36464154720306] }, "number": 111, "properties": { "iconContent": 2, "balloonContent": 111, "hintContent": 111, "clusterCaption": 111, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 112, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-11.482795242918655, 36.42327051144093] }, "number": 112, "properties": { "iconContent": 2, "balloonContent": 112, "hintContent": 112, "clusterCaption": 112, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 113, "type": "Feature", "geometry": { "type": "Point", "coordinates": [35.63535877270624, 143.19650712423027] }, "number": 113, "properties": { "iconContent": 2, "balloonContent": 113, "hintContent": 113, "clusterCaption": 113, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 114, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-63.951826645061374, 86.41872458159924] }, "number": 114, "properties": { "iconContent": 2, "balloonContent": 114, "hintContent": 114, "clusterCaption": 114, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 115, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-4.95872596045956, -54.71735391765833] }, "number": 115, "properties": { "iconContent": 2, "balloonContent": 115, "hintContent": 115, "clusterCaption": 115, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 116, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-64.53409892972559, -74.42154035903513] }, "number": 116, "properties": { "iconContent": 2, "balloonContent": 116, "hintContent": 116, "clusterCaption": 116, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 117, "type": "Feature", "geometry": { "type": "Point", "coordinates": [9.223785783397034, 8.722870778292418] }, "number": 117, "properties": { "iconContent": 2, "balloonContent": 117, "hintContent": 117, "clusterCaption": 117, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 118, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-72.34896795358509, 111.33254015352577] }, "number": 118, "properties": { "iconContent": 2, "balloonContent": 118, "hintContent": 118, "clusterCaption": 118, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 119, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-61.71480741701089, 158.54413556400687] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 119, "hintContent": 119, "clusterCaption": 119, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 120, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-80.12071067700163, 154.30816955398768] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 120, "hintContent": 120, "clusterCaption": 120, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 121, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-12.430957217002288, -25.266533349640667] }, "number": 121, "properties": { "iconContent": 2, "balloonContent": 121, "hintContent": 121, "clusterCaption": 121, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 122, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-20.124524261336774, 4.281716952100396] }, "number": 122, "properties": { "iconContent": 2, "balloonContent": 122, "hintContent": 122, "clusterCaption": 122, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 123, "type": "Feature", "geometry": { "type": "Point", "coordinates": [32.00245242449455, 133.1557736825198] }, "number": 123, "properties": { "iconContent": 2, "balloonContent": 123, "hintContent": 123, "clusterCaption": 123, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 124, "type": "Feature", "geometry": { "type": "Point", "coordinates": [22.33459070441313, -82.96388296410441] }, "number": 124, "properties": { "iconContent": 2, "balloonContent": 124, "hintContent": 124, "clusterCaption": 124, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 125, "type": "Feature", "geometry": { "type": "Point", "coordinates": [59.97708885814063, -67.40965683013201] }, "number": 125, "properties": { "iconContent": 2, "balloonContent": 125, "hintContent": 125, "clusterCaption": 125, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 126, "type": "Feature", "geometry": { "type": "Point", "coordinates": [47.49663173686713, -59.2638724995777] }, "number": 126, "properties": { "iconContent": 2, "balloonContent": 126, "hintContent": 126, "clusterCaption": 126, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 127, "type": "Feature", "geometry": { "type": "Point", "coordinates": [60.20985524402931, -162.97119999770075] }, "number": 127, "properties": { "iconContent": 2, "balloonContent": 127, "hintContent": 127, "clusterCaption": 127, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 128, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-67.36311208223924, 6.458219890482724] }, "number": 128, "properties": { "iconContent": 2, "balloonContent": 128, "hintContent": 128, "clusterCaption": 128, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 129, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-12.733528311364353, -103.81693282630295] }, "number": 129, "properties": { "iconContent": 2, "balloonContent": 129, "hintContent": 129, "clusterCaption": 129, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 130, "type": "Feature", "geometry": { "type": "Point", "coordinates": [21.605473122326657, -29.768657349050045] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 130, "hintContent": 130, "clusterCaption": 130, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 131, "type": "Feature", "geometry": { "type": "Point", "coordinates": [1.0485502716619521, 19.825344965793192] }, "number": 131, "properties": { "iconContent": 2, "balloonContent": 131, "hintContent": 131, "clusterCaption": 131, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 132, "type": "Feature", "geometry": { "type": "Point", "coordinates": [64.52318031457253, 15.70801021065563] }, "number": 132, "properties": { "iconContent": 2, "balloonContent": 132, "hintContent": 132, "clusterCaption": 132, "type": "132"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 133, "type": "Feature", "geometry": { "type": "Point", "coordinates": [17.391338008455932, 167.91188176721334] }, "number": 133, "properties": { "iconContent": 2, "balloonContent": 133, "hintContent": 133, "clusterCaption": 133, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 134, "type": "Feature", "geometry": { "type": "Point", "coordinates": [19.30773339001462, 89.66573565267026] }, "number": 134, "properties": { "iconContent": 2, "balloonContent": 134, "hintContent": 134, "clusterCaption": 134, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 135, "type": "Feature", "geometry": { "type": "Point", "coordinates": [81.24418738647364, -103.90756638720632] }, "number": 135, "properties": { "iconContent": 2, "balloonContent": 135, "hintContent": 135, "clusterCaption": 135, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 136, "type": "Feature", "geometry": { "type": "Point", "coordinates": [4.087985089281574, 88.10982798691839] }, "number": 136, "properties": { "iconContent": 2, "balloonContent": 136, "hintContent": 136, "clusterCaption": 136, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 137, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-22.14217185974121, -8.495447994209826] }, "number": 137, "properties": { "iconContent": 2, "balloonContent": 137, "hintContent": 137, "clusterCaption": 137, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 138, "type": "Feature", "geometry": { "type": "Point", "coordinates": [41.756515606539324, 163.60363806132227] }, "number": 138, "properties": { "iconContent": 2, "balloonContent": 138, "hintContent": 138, "clusterCaption": 138, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 139, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-28.627273481106386, 137.2250929987058] }, "number": 139, "properties": { "iconContent": 2, "balloonContent": 139, "hintContent": 139, "clusterCaption": 139, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 140, "type": "Feature", "geometry": { "type": "Point", "coordinates": [32.88234817213379, 125.07769995834678] }, "number": 140, "properties": { "iconContent": 2, "balloonContent": 140, "hintContent": 140, "clusterCaption": 140, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 141, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-81.27380210673437, -70.2273706253618] }, "number": 141, "properties": { "iconContent": 2, "balloonContent": 141, "hintContent": 141, "clusterCaption": 141, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 142, "type": "Feature", "geometry": { "type": "Point", "coordinates": [36.59266176982783, 127.37568320240825] }, "number": 142, "properties": { "iconContent": 2, "balloonContent": 142, "hintContent": 142, "clusterCaption": 142, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 143, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-40.921818611677736, -172.04561290796846] }, "number": 143, "properties": { "iconContent": 2, "balloonContent": 143, "hintContent": 143, "clusterCaption": 143, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 144, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-55.746810706332326, -47.25726077333093] }, "number": 144, "properties": { "iconContent": 2, "balloonContent": 144, "hintContent": 144, "clusterCaption": 144, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 145, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-69.9060550914146, -66.09093770384789] }, "number": 145, "properties": { "iconContent": 2, "balloonContent": 145, "hintContent": 145, "clusterCaption": 145, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 146, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-15.890392819419503, 161.21560954023153] }, "number": 146, "properties": { "iconContent": 2, "balloonContent": 146, "hintContent": 146, "clusterCaption": 146, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 147, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-43.53163355961442, -150.05853965878487] }, "number": 147, "properties": { "iconContent": 2, "balloonContent": 147, "hintContent": 147, "clusterCaption": 147, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 148, "type": "Feature", "geometry": { "type": "Point", "coordinates": [54.28722663433291, 59.603331657126546] }, "number": 148, "properties": { "iconContent": 2, "balloonContent": 148, "hintContent": 148, "clusterCaption": 148, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 149, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-72.00844303122722, -148.3418029686436] }, "number": 149, "properties": { "iconContent": 2, "balloonContent": 149, "hintContent": 149, "clusterCaption": 149, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 150, "type": "Feature", "geometry": { "type": "Point", "coordinates": [19.253128618001938, -109.92633185349405] }, "number": 150, "properties": { "iconContent": 2, "balloonContent": 150, "hintContent": 150, "clusterCaption": 150, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 151, "type": "Feature", "geometry": { "type": "Point", "coordinates": [52.991214324720204, -80.0057320157066] }, "number": 151, "properties": { "iconContent": 2, "balloonContent": 151, "hintContent": 151, "clusterCaption": 151, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 152, "type": "Feature", "geometry": { "type": "Point", "coordinates": [84.15201383177191, -140.3344526840374] }, "number": 152, "properties": { "iconContent": 2, "balloonContent": 152, "hintContent": 152, "clusterCaption": 152, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 153, "type": "Feature", "geometry": { "type": "Point", "coordinates": [82.25031309295446, 11.203574524261057] }, "number": 153, "properties": { "iconContent": 2, "balloonContent": 153, "hintContent": 153, "clusterCaption": 153, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 154, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-49.373807879164815, -71.40175086446106] }, "number": 154, "properties": { "iconContent": 2, "balloonContent": 154, "hintContent": 154, "clusterCaption": 154, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 155, "type": "Feature", "geometry": { "type": "Point", "coordinates": [7.633572162594646, -151.10118022654206] }, "number": 155, "properties": { "iconContent": 2, "balloonContent": 155, "hintContent": 155, "clusterCaption": 155, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 156, "type": "Feature", "geometry": { "type": "Point", "coordinates": [66.32355373119935, 21.646730094216764] }, "number": 156, "properties": { "iconContent": 2, "balloonContent": 156, "hintContent": 156, "clusterCaption": 156, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 157, "type": "Feature", "geometry": { "type": "Point", "coordinates": [29.98725753510371, 93.64715734496713] }, "number": 157, "properties": { "iconContent": 2, "balloonContent": 157, "hintContent": 157, "clusterCaption": 157, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 158, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-0.1254889986012131, 84.02190541382879] }, "number": 158, "properties": { "iconContent": 2, "balloonContent": 158, "hintContent": 158, "clusterCaption": 158, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 159, "type": "Feature", "geometry": { "type": "Point", "coordinates": [50.469389375066385, 122.57804313674569] }, "number": 159, "properties": { "iconContent": 2, "balloonContent": 159, "hintContent": 159, "clusterCaption": 159, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 160, "type": "Feature", "geometry": { "type": "Point", "coordinates": [63.215294691035524, -17.229922427795827] }, "number": 160, "properties": { "iconContent": 2, "balloonContent": 160, "hintContent": 160, "clusterCaption": 160, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 161, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-43.03545256261714, 160.37592791486531] }, "number": 161, "properties": { "iconContent": 2, "balloonContent": 161, "hintContent": 161, "clusterCaption": 161, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 162, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-54.457593876868486, -131.52803184930235] }, "number": 162, "properties": { "iconContent": 2, "balloonContent": 162, "hintContent": 162, "clusterCaption": 162, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 163, "type": "Feature", "geometry": { "type": "Point", "coordinates": [75.56041849195026, 15.265560681000352] }, "number": 163, "properties": { "iconContent": 2, "balloonContent": 163, "hintContent": 163, "clusterCaption": 163, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 164, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-63.70577561785467, 69.23784860875458] }, "number": 164, "properties": { "iconContent": 2, "balloonContent": 164, "hintContent": 164, "clusterCaption": 164, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 165, "type": "Feature", "geometry": { "type": "Point", "coordinates": [68.15533934975974, 115.33917473629117] }, "number": 165, "properties": { "iconContent": 2, "balloonContent": 165, "hintContent": 165, "clusterCaption": 165, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 166, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-48.74603254138492, -59.35590479057282] }, "number": 166, "properties": { "iconContent": 2, "balloonContent": 166, "hintContent": 166, "clusterCaption": 166, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 167, "type": "Feature", "geometry": { "type": "Point", "coordinates": [48.65115177119151, -85.22064974997193] }, "number": 167, "properties": { "iconContent": 2, "balloonContent": 167, "hintContent": 167, "clusterCaption": 167, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 168, "type": "Feature", "geometry": { "type": "Point", "coordinates": [19.917878524865955, 71.8062497349456] }, "number": 168, "properties": { "iconContent": 2, "balloonContent": 168, "hintContent": 168, "clusterCaption": 168, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 169, "type": "Feature", "geometry": { "type": "Point", "coordinates": [61.39625707175583, -73.37840919848531] }, "number": 169, "properties": { "iconContent": 2, "balloonContent": 169, "hintContent": 169, "clusterCaption": 169, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 170, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-43.512132610194385, -124.62416976224631] }, "number": 170, "properties": { "iconContent": 2, "balloonContent": 170, "hintContent": 170, "clusterCaption": 170, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 171, "type": "Feature", "geometry": { "type": "Point", "coordinates": [72.48624237603508, 122.40371107589453] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 171, "hintContent": 171, "clusterCaption": 171, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 172, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-22.932995973387733, -177.37224598880857] }, "number": 172, "properties": { "iconContent": 2, "balloonContent": 172, "hintContent": 172, "clusterCaption": 172, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 173, "type": "Feature", "geometry": { "type": "Point", "coordinates": [12.116425017593428, 94.83686758205295] }, "number": 173, "properties": { "iconContent": 2, "balloonContent": 173, "hintContent": 173, "clusterCaption": 173, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 174, "type": "Feature", "geometry": { "type": "Point", "coordinates": [72.61624165344983, -91.44640212412924] }, "number": 174, "properties": { "iconContent": 2, "balloonContent": 174, "hintContent": 174, "clusterCaption": 174, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 175, "type": "Feature", "geometry": { "type": "Point", "coordinates": [69.82389625627548, 11.31498564966023] }, "number": 175, "properties": { "iconContent": 2, "balloonContent": 175, "hintContent": 175, "clusterCaption": 175, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 176, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-77.5869010714814, 39.595761015079916] }, "number": 176, "properties": { "iconContent": 2, "balloonContent": 176, "hintContent": 176, "clusterCaption": 176, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 177, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-17.5571400730405, -131.9345961883664] }, "number": 177, "properties": { "iconContent": 2, "balloonContent": 177, "hintContent": 177, "clusterCaption": 177, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 178, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-84.94455766980536, 118.27597090043128] }, "number": 178, "properties": { "iconContent": 2, "balloonContent": 178, "hintContent": 178, "clusterCaption": 178, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 179, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-68.76333926222287, -87.09176843985915] }, "number": 179, "properties": { "iconContent": 2, "balloonContent": 179, "hintContent": 179, "clusterCaption": 179, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 180, "type": "Feature", "geometry": { "type": "Point", "coordinates": [31.762105544330552, 153.14279118552804] }, "number": 180, "properties": { "iconContent": 2, "balloonContent": 180, "hintContent": 180, "clusterCaption": 180, "type": "180"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 181, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-64.94425517506897, -120.96068681683391] }, "number": 181, "properties": { "iconContent": 2, "balloonContent": 181, "hintContent": 181, "clusterCaption": 181, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 182, "type": "Feature", "geometry": { "type": "Point", "coordinates": [21.819229365792125, 123.21776641532779] }, "number": 182, "properties": { "iconContent": 2, "balloonContent": 182, "hintContent": 182, "clusterCaption": 182, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 183, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-41.20994248543866, -57.60726368520409] }, "number": 183, "properties": { "iconContent": 2, "balloonContent": 183, "hintContent": 183, "clusterCaption": 183, "type": "183"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 184, "type": "Feature", "geometry": { "type": "Point", "coordinates": [29.8393385973759, 25.310693783685565] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 184, "hintContent": 184, "clusterCaption": 184, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 185, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-66.89627410029061, 125.70918173063546] }, "number": 185, "properties": { "iconContent": 2, "balloonContent": 185, "hintContent": 185, "clusterCaption": 185, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 186, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-11.016561527503654, -109.96611228212714] }, "number": 186, "properties": { "iconContent": 2, "balloonContent": 186, "hintContent": 186, "clusterCaption": 186, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 187, "type": "Feature", "geometry": { "type": "Point", "coordinates": [24.088435063604265, 119.51363211963326] }, "number": 187, "properties": { "iconContent": 2, "balloonContent": 187, "hintContent": 187, "clusterCaption": 187, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 188, "type": "Feature", "geometry": { "type": "Point", "coordinates": [22.29981376323849, 141.7473344039172] }, "number": 188, "properties": { "iconContent": 2, "balloonContent": 188, "hintContent": 188, "clusterCaption": 188, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 189, "type": "Feature", "geometry": { "type": "Point", "coordinates": [77.94091420131736, -93.82783354260027] }, "number": 189, "properties": { "iconContent": 2, "balloonContent": 189, "hintContent": 189, "clusterCaption": 189, "type": "189"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 190, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-23.45601370325312, 110.74938596226275] }, "number": 190, "properties": { "iconContent": 2, "balloonContent": 190, "hintContent": 190, "clusterCaption": 190, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 191, "type": "Feature", "geometry": { "type": "Point", "coordinates": [34.72236446570605, -12.077215346507728] }, "number": 191, "properties": { "iconContent": 2, "balloonContent": 191, "hintContent": 191, "clusterCaption": 191, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 192, "type": "Feature", "geometry": { "type": "Point", "coordinates": [36.34268430643715, 85.22558015305549] }, "number": 192, "properties": { "iconContent": 2, "balloonContent": 192, "hintContent": 192, "clusterCaption": 192, "type": "192"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 193, "type": "Feature", "geometry": { "type": "Point", "coordinates": [1.5158408845309168, 134.40881879534572] }, "number": 193, "properties": { "iconContent": 2, "balloonContent": 193, "hintContent": 193, "clusterCaption": 193, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 194, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-76.69875762891024, -174.6911898208782] }, "number": 194, "properties": { "iconContent": 2, "balloonContent": 194, "hintContent": 194, "clusterCaption": 194, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 195, "type": "Feature", "geometry": { "type": "Point", "coordinates": [33.484697688836604, 31.52675298973918] }, "number": 195, "properties": { "iconContent": 2, "balloonContent": 195, "hintContent": 195, "clusterCaption": 195, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 196, "type": "Feature", "geometry": { "type": "Point", "coordinates": [61.033844373887405, -66.36500582098961] }, "number": 196, "properties": { "iconContent": 2, "balloonContent": 196, "hintContent": 196, "clusterCaption": 196, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 197, "type": "Feature", "geometry": { "type": "Point", "coordinates": [78.9378168724943, 40.7789739780128] }, "number": 197, "properties": { "iconContent": 2, "balloonContent": 197, "hintContent": 197, "clusterCaption": 197, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 198, "type": "Feature", "geometry": { "type": "Point", "coordinates": [64.96929866145365, -65.26401283219457] }, "number": 198, "properties": { "iconContent": 2, "balloonContent": 198, "hintContent": 198, "clusterCaption": 198, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 199, "type": "Feature", "geometry": { "type": "Point", "coordinates": [58.61410373705439, 173.43768156599253] }, "number": 199, "properties": { "iconContent": 2, "balloonContent": 199, "hintContent": 199, "clusterCaption": 199, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 200, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-48.440031834179536, -42.73513843305409] }, "number": 200, "properties": { "iconContent": 2, "balloonContent": 200, "hintContent": 200, "clusterCaption": 200, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 201, "type": "Feature", "geometry": { "type": "Point", "coordinates": [31.18257105234079, -104.421924976632] }, "number": 201, "properties": { "iconContent": 2, "balloonContent": 201, "hintContent": 201, "clusterCaption": 201, "type": "201"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 202, "type": "Feature", "geometry": { "type": "Point", "coordinates": [78.43155711423606, 158.33422504831105] }, "number": 202, "properties": { "iconContent": 2, "balloonContent": 202, "hintContent": 202, "clusterCaption": 202, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 203, "type": "Feature", "geometry": { "type": "Point", "coordinates": [82.4086989252828, -19.810201842337847] }, "number": 203, "properties": { "iconContent": 2, "balloonContent": 203, "hintContent": 203, "clusterCaption": 203, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 204, "type": "Feature", "geometry": { "type": "Point", "coordinates": [75.1478970318567, 104.35231201350689] }, "number": 204, "properties": { "iconContent": 2, "balloonContent": 204, "hintContent": 204, "clusterCaption": 204, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 205, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-47.903366424143314, -149.301019879058] }, "number": 205, "properties": { "iconContent": 2, "balloonContent": 205, "hintContent": 205, "clusterCaption": 205, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 206, "type": "Feature", "geometry": { "type": "Point", "coordinates": [30.617889171699062, 1.766279311850667] }, "number": 206, "properties": { "iconContent": 2, "balloonContent": 206, "hintContent": 206, "clusterCaption": 206, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 207, "type": "Feature", "geometry": { "type": "Point", "coordinates": [56.63918589474633, -139.0611502714455] }, "number": 207, "properties": { "iconContent": 2, "balloonContent": 207, "hintContent": 207, "clusterCaption": 207, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 208, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-62.589977979660034, 169.24241975415498] }, "number": 208, "properties": { "iconContent": 2, "balloonContent": 208, "hintContent": 208, "clusterCaption": 208, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 209, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-33.88436526991427, 179.38573063351214] }, "number": 209, "properties": { "iconContent": 2, "balloonContent": 209, "hintContent": 209, "clusterCaption": 209, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 210, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-32.62833063490689, -57.09868517238647] }, "number": 210, "properties": { "iconContent": 2, "balloonContent": 210, "hintContent": 210, "clusterCaption": 210, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 211, "type": "Feature", "geometry": { "type": "Point", "coordinates": [42.95849481713958, -27.669995008036494] }, "number": 211, "properties": { "iconContent": 2, "balloonContent": 211, "hintContent": 211, "clusterCaption": 211, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 212, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-23.343464395729825, 108.88718402478844] }, "number": 212, "properties": { "iconContent": 2, "balloonContent": 212, "hintContent": 212, "clusterCaption": 212, "type": "212"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 213, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-84.78268180391751, 53.42051776126027] }, "number": 213, "properties": { "iconContent": 2, "balloonContent": 213, "hintContent": 213, "clusterCaption": 213, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 214, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-61.57085821731016, 7.612822777591646] }, "number": 214, "properties": { "iconContent": 2, "balloonContent": 214, "hintContent": 214, "clusterCaption": 214, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 215, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-55.40730971028097, -96.18514888454229] }, "number": 215, "properties": { "iconContent": 2, "balloonContent": 215, "hintContent": 215, "clusterCaption": 215, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 216, "type": "Feature", "geometry": { "type": "Point", "coordinates": [64.1718090325594, -16.9558865390718] }, "number": 216, "properties": { "iconContent": 2, "balloonContent": 216, "hintContent": 216, "clusterCaption": 216, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 217, "type": "Feature", "geometry": { "type": "Point", "coordinates": [31.07665642630309, -72.37327257636935] }, "number": 217, "properties": { "iconContent": 2, "balloonContent": 217, "hintContent": 217, "clusterCaption": 217, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 218, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-77.86491368780844, 151.25647648237646] }, "number": 218, "properties": { "iconContent": 2, "balloonContent": 218, "hintContent": 218, "clusterCaption": 218, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 219, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-53.38183484855108, -95.83320630714297] }, "number": 219, "properties": { "iconContent": 2, "balloonContent": 219, "hintContent": 219, "clusterCaption": 219, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 220, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-12.769395002396777, -88.04045726079494] }, "number": 220, "properties": { "iconContent": 2, "balloonContent": 220, "hintContent": 220, "clusterCaption": 220, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 221, "type": "Feature", "geometry": { "type": "Point", "coordinates": [4.353738019708544, 85.66841369960457] }, "number": 221, "properties": { "iconContent": 2, "balloonContent": 221, "hintContent": 221, "clusterCaption": 221, "type": "221"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 222, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-68.81613040342927, -81.87136877328157] }, "number": 222, "properties": { "iconContent": 2, "balloonContent": 222, "hintContent": 222, "clusterCaption": 222, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 223, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-31.050112436059862, 72.06647019367665] }, "number": 223, "properties": { "iconContent": 2, "balloonContent": 223, "hintContent": 223, "clusterCaption": 223, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 224, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-5.787461589789018, -68.93747292459011] }, "number": 224, "properties": { "iconContent": 2, "balloonContent": 224, "hintContent": 224, "clusterCaption": 224, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 225, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-56.34278396726586, 159.64606967289] }, "number": 225, "properties": { "iconContent": 2, "balloonContent": 225, "hintContent": 225, "clusterCaption": 225, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 226, "type": "Feature", "geometry": { "type": "Point", "coordinates": [66.84029657277279, 63.77824690192938] }, "number": 226, "properties": { "iconContent": 2, "balloonContent": 226, "hintContent": 226, "clusterCaption": 226, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 227, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-75.83317561424337, -81.19322255719453] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 227, "hintContent": 227, "clusterCaption": 227, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 228, "type": "Feature", "geometry": { "type": "Point", "coordinates": [35.78933389275335, 37.47849524952471] }, "number": 228, "properties": { "iconContent": 2, "balloonContent": 228, "hintContent": 228, "clusterCaption": 228, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 229, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-53.5048228292726, 163.31199745181948] }, "number": 229, "properties": { "iconContent": 2, "balloonContent": 229, "hintContent": 229, "clusterCaption": 229, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 230, "type": "Feature", "geometry": { "type": "Point", "coordinates": [59.51123132253997, 60.65758162178099] }, "number": 230, "properties": { "iconContent": 2, "balloonContent": 230, "hintContent": 230, "clusterCaption": 230, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 231, "type": "Feature", "geometry": { "type": "Point", "coordinates": [59.829672107007354, -157.92699983809143] }, "number": 231, "properties": { "iconContent": 2, "balloonContent": 231, "hintContent": 231, "clusterCaption": 231, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 232, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-51.992969514103606, 120.1349446317181] }, "number": 232, "properties": { "iconContent": 2, "balloonContent": 232, "hintContent": 232, "clusterCaption": 232, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 233, "type": "Feature", "geometry": { "type": "Point", "coordinates": [23.29841460683383, 9.217321216128767] }, "number": 233, "properties": { "iconContent": 2, "balloonContent": 233, "hintContent": 233, "clusterCaption": 233, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 234, "type": "Feature", "geometry": { "type": "Point", "coordinates": [63.958196587627754, 124.85599694307894] }, "number": 234, "properties": { "iconContent": 2, "balloonContent": 234, "hintContent": 234, "clusterCaption": 234, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 235, "type": "Feature", "geometry": { "type": "Point", "coordinates": [59.316857918165624, -24.737559193745255] }, "number": 235, "properties": { "iconContent": 2, "balloonContent": 235, "hintContent": 235, "clusterCaption": 235, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 236, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-76.73109300318174, 39.88469799980521] }, "number": 236, "properties": { "iconContent": 2, "balloonContent": 236, "hintContent": 236, "clusterCaption": 236, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 237, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-5.643415455706418, 100.72236749343574] }, "number": 237, "properties": { "iconContent": 2, "balloonContent": 237, "hintContent": 237, "clusterCaption": 237, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 238, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-40.862847161479294, -51.8691679649055] }, "number": 238, "properties": { "iconContent": 2, "balloonContent": 238, "hintContent": 238, "clusterCaption": 238, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 239, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-33.03155767498538, -35.19841751083732] }, "number": 239, "properties": { "iconContent": 2, "balloonContent": 239, "hintContent": 239, "clusterCaption": 239, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 240, "type": "Feature", "geometry": { "type": "Point", "coordinates": [6.031875953776762, -76.06673394329846] }, "number": 240, "properties": { "iconContent": 2, "balloonContent": 240, "hintContent": 240, "clusterCaption": 240, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 241, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-5.494706358294934, 154.39461631234735] }, "number": 241, "properties": { "iconContent": 2, "balloonContent": 241, "hintContent": 241, "clusterCaption": 241, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 242, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-53.5382018215023, 20.988405696116388] }, "number": 242, "properties": { "iconContent": 2, "balloonContent": 242, "hintContent": 242, "clusterCaption": 242, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 243, "type": "Feature", "geometry": { "type": "Point", "coordinates": [45.0614352512639, -55.6084801023826] }, "number": 243, "properties": { "iconContent": 2, "balloonContent": 243, "hintContent": 243, "clusterCaption": 243, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 244, "type": "Feature", "geometry": { "type": "Point", "coordinates": [61.44756803405471, 146.77027981728315] }, "number": 244, "properties": { "iconContent": 2, "balloonContent": 244, "hintContent": 244, "clusterCaption": 244, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 245, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-36.19989610277116, 56.21455112937838] }, "number": 245, "properties": { "iconContent": 2, "balloonContent": 245, "hintContent": 245, "clusterCaption": 245, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 246, "type": "Feature", "geometry": { "type": "Point", "coordinates": [37.83485632040538, 124.43880875129253] }, "number": 246, "properties": { "iconContent": 2, "balloonContent": 246, "hintContent": 246, "clusterCaption": 246, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 247, "type": "Feature", "geometry": { "type": "Point", "coordinates": [46.5626643167343, -45.294704237021506] }, "number": 247, "properties": { "iconContent": 2, "balloonContent": 247, "hintContent": 247, "clusterCaption": 247, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 248, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-74.33219070546329, 43.80191456526518] }, "number": 248, "properties": { "iconContent": 2, "balloonContent": 248, "hintContent": 248, "clusterCaption": 248, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 249, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-76.96370541816577, 173.8588111801073] }, "number": 249, "properties": { "iconContent": 2, "balloonContent": 249, "hintContent": 249, "clusterCaption": 249, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 250, "type": "Feature", "geometry": { "type": "Point", "coordinates": [84.11475267494097, 162.02995493542403] }, "number": 250, "properties": { "iconContent": 2, "balloonContent": 250, "hintContent": 250, "clusterCaption": 250, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 251, "type": "Feature", "geometry": { "type": "Point", "coordinates": [19.094304285245016, 1.5258564474061131] }, "number": 251, "properties": { "iconContent": 2, "balloonContent": 251, "hintContent": 251, "clusterCaption": 251, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 252, "type": "Feature", "geometry": { "type": "Point", "coordinates": [58.49288143450394, -121.37236080598086] }, "number": 252, "properties": { "iconContent": 2, "balloonContent": 252, "hintContent": 252, "clusterCaption": 252, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 253, "type": "Feature", "geometry": { "type": "Point", "coordinates": [65.39195518707857, 11.215678369626403] }, "number": 253, "properties": { "iconContent": 2, "balloonContent": 253, "hintContent": 253, "clusterCaption": 253, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 254, "type": "Feature", "geometry": { "type": "Point", "coordinates": [15.448096434120089, 68.11169411055744] }, "number": 254, "properties": { "iconContent": 2, "balloonContent": 254, "hintContent": 254, "clusterCaption": 254, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 255, "type": "Feature", "geometry": { "type": "Point", "coordinates": [83.12352434615605, 133.32543455064297] }, "number": 255, "properties": { "iconContent": 2, "balloonContent": 255, "hintContent": 255, "clusterCaption": 255, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 256, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-30.77656366978772, -138.9029444474727] }, "number": 256, "properties": { "iconContent": 2, "balloonContent": 256, "hintContent": 256, "clusterCaption": 256, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 257, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-15.606328869471326, -166.8929500412196] }, "number": 257, "properties": { "iconContent": 2, "balloonContent": 257, "hintContent": 257, "clusterCaption": 257, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 258, "type": "Feature", "geometry": { "type": "Point", "coordinates": [42.118278776761144, -168.26158155687153] }, "number": 258, "properties": { "iconContent": 2, "balloonContent": 258, "hintContent": 258, "clusterCaption": 258, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 259, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-20.52817422663793, 97.5186219625175] }, "number": 259, "properties": { "iconContent": 2, "balloonContent": 259, "hintContent": 259, "clusterCaption": 259, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 260, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-55.926732457010075, 177.74382170755416] }, "number": 260, "properties": { "iconContent": 2, "balloonContent": 260, "hintContent": 260, "clusterCaption": 260, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 261, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-20.535043862182647, 51.40693148598075] }, "number": 261, "properties": { "iconContent": 2, "balloonContent": 261, "hintContent": 261, "clusterCaption": 261, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 262, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-2.520473290933296, -89.42791587207466] }, "number": 262, "properties": { "iconContent": 2, "balloonContent": 262, "hintContent": 262, "clusterCaption": 262, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 263, "type": "Feature", "geometry": { "type": "Point", "coordinates": [68.04497729986906, -127.39867882803082] }, "number": 263, "properties": { "iconContent": 2, "balloonContent": 263, "hintContent": 263, "clusterCaption": 263, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 264, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-63.62396701704711, -112.19198324717581] }, "number": 264, "properties": { "iconContent": 2, "balloonContent": 264, "hintContent": 264, "clusterCaption": 264, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 265, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-76.87453603837639, -148.51164375431836] }, "number": 265, "properties": { "iconContent": 2, "balloonContent": 265, "hintContent": 265, "clusterCaption": 265, "type": "265"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 266, "type": "Feature", "geometry": { "type": "Point", "coordinates": [23.12666641548276, -49.205613671801984] }, "number": 266, "properties": { "iconContent": 2, "balloonContent": 266, "hintContent": 266, "clusterCaption": 266, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 267, "type": "Feature", "geometry": { "type": "Point", "coordinates": [45.14781999401748, -122.33589929994196] }, "number": 267, "properties": { "iconContent": 2, "balloonContent": 267, "hintContent": 267, "clusterCaption": 267, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 268, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-32.34913481748663, 126.60908054094762] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 268, "hintContent": 268, "clusterCaption": 268, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 269, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-37.00161124346778, -70.86533898487687] }, "number": 269, "properties": { "iconContent": 2, "balloonContent": 269, "hintContent": 269, "clusterCaption": 269, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 270, "type": "Feature", "geometry": { "type": "Point", "coordinates": [62.12501363363117, 3.591424017213285] }, "number": 270, "properties": { "iconContent": 2, "balloonContent": 270, "hintContent": 270, "clusterCaption": 270, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 271, "type": "Feature", "geometry": { "type": "Point", "coordinates": [76.81717827450484, 30.554481223225594] }, "number": 271, "properties": { "iconContent": 2, "balloonContent": 271, "hintContent": 271, "clusterCaption": 271, "type": "271"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 272, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-69.10196927376091, 70.29111730400473] }, "number": 272, "properties": { "iconContent": 2, "balloonContent": 272, "hintContent": 272, "clusterCaption": 272, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 273, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-1.1506037705112249, 120.38516233675182] }, "number": 273, "properties": { "iconContent": 2, "balloonContent": 273, "hintContent": 273, "clusterCaption": 273, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 274, "type": "Feature", "geometry": { "type": "Point", "coordinates": [55.89287847047672, 131.33480572141707] }, "number": 274, "properties": { "iconContent": 2, "balloonContent": 274, "hintContent": 274, "clusterCaption": 274, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 275, "type": "Feature", "geometry": { "type": "Point", "coordinates": [20.336085568415, 40.098583633080125] }, "number": 275, "properties": { "iconContent": 2, "balloonContent": 275, "hintContent": 275, "clusterCaption": 275, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 276, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-27.78959944495, -35.08016805164516] }, "number": 276, "properties": { "iconContent": 2, "balloonContent": 276, "hintContent": 276, "clusterCaption": 276, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 277, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-84.06636033207178, -108.55469668284059] }, "number": 277, "properties": { "iconContent": 2, "balloonContent": 277, "hintContent": 277, "clusterCaption": 277, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 278, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-56.039765347959474, -136.06771054677665] }, "number": 278, "properties": { "iconContent": 2, "balloonContent": 278, "hintContent": 278, "clusterCaption": 278, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 279, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-74.12859612610191, 165.02721379045397] }, "number": 279, "properties": { "iconContent": 2, "balloonContent": 279, "hintContent": 279, "clusterCaption": 279, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 280, "type": "Feature", "geometry": { "type": "Point", "coordinates": [79.95223939418793, -64.18894754257053] }, "number": 280, "properties": { "iconContent": 2, "balloonContent": 280, "hintContent": 280, "clusterCaption": 280, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 281, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-74.16155225248076, -35.75819176621735] }, "number": 281, "properties": { "iconContent": 2, "balloonContent": 281, "hintContent": 281, "clusterCaption": 281, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 282, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-73.70279378490523, -130.15145391225815] }, "number": 282, "properties": { "iconContent": 2, "balloonContent": 282, "hintContent": 282, "clusterCaption": 282, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 283, "type": "Feature", "geometry": { "type": "Point", "coordinates": [25.815272569889203, 14.962499309331179] }, "number": 283, "properties": { "iconContent": 2, "balloonContent": 283, "hintContent": 283, "clusterCaption": 283, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 284, "type": "Feature", "geometry": { "type": "Point", "coordinates": [49.339292015647516, 65.35397424828261] }, "number": 284, "properties": { "iconContent": 2, "balloonContent": 284, "hintContent": 284, "clusterCaption": 284, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 285, "type": "Feature", "geometry": { "type": "Point", "coordinates": [57.65981961856596, -35.8174490975216] }, "number": 285, "properties": { "iconContent": 2, "balloonContent": 285, "hintContent": 285, "clusterCaption": 285, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 286, "type": "Feature", "geometry": { "type": "Point", "coordinates": [64.79543305351399, 119.50527649372816] }, "number": 286, "properties": { "iconContent": 2, "balloonContent": 286, "hintContent": 286, "clusterCaption": 286, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 287, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-19.948562749195844, -144.75687907543033] }, "number": 287, "properties": { "iconContent": 2, "balloonContent": 287, "hintContent": 287, "clusterCaption": 287, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 288, "type": "Feature", "geometry": { "type": "Point", "coordinates": [3.305255095474422, 177.54556430038065] }, "number": 288, "properties": { "iconContent": 2, "balloonContent": 288, "hintContent": 288, "clusterCaption": 288, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 289, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-0.24257688550278544, 157.77556579560041] }, "number": 289, "properties": { "iconContent": 2, "balloonContent": 289, "hintContent": 289, "clusterCaption": 289, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 290, "type": "Feature", "geometry": { "type": "Point", "coordinates": [55.54469711729325, -41.669906876049936] }, "number": 290, "properties": { "iconContent": 2, "balloonContent": 290, "hintContent": 290, "clusterCaption": 290, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 291, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-39.959551418432966, -88.46969368401915] }, "number": 291, "properties": { "iconContent": 2, "balloonContent": 291, "hintContent": 291, "clusterCaption": 291, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 292, "type": "Feature", "geometry": { "type": "Point", "coordinates": [37.31047214823775, 54.60189064498991] }, "number": 292, "properties": { "iconContent": 2, "balloonContent": 292, "hintContent": 292, "clusterCaption": 292, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 293, "type": "Feature", "geometry": { "type": "Point", "coordinates": [40.2827025053557, -150.7522804895416] }, "number": 293, "properties": { "iconContent": 2, "balloonContent": 293, "hintContent": 293, "clusterCaption": 293, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 294, "type": "Feature", "geometry": { "type": "Point", "coordinates": [50.92534369206987, 150.3360709687695] }, "number": 294, "properties": { "iconContent": 2, "balloonContent": 294, "hintContent": 294, "clusterCaption": 294, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 295, "type": "Feature", "geometry": { "type": "Point", "coordinates": [4.504794639069587, -127.95453328639269] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 295, "hintContent": 295, "clusterCaption": 295, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 296, "type": "Feature", "geometry": { "type": "Point", "coordinates": [34.889970938675106, 8.667070479132235] }, "number": 296, "properties": { "iconContent": 2, "balloonContent": 296, "hintContent": 296, "clusterCaption": 296, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 297, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-44.74113074596971, 125.78015855513513] }, "number": 297, "properties": { "iconContent": 2, "balloonContent": 297, "hintContent": 297, "clusterCaption": 297, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 298, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-47.32118468149565, 81.91589344292879] }, "number": 298, "properties": { "iconContent": 2, "balloonContent": 298, "hintContent": 298, "clusterCaption": 298, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 299, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-73.50225936737843, -156.56746222637594] }, "number": 299, "properties": { "iconContent": 2, "balloonContent": 299, "hintContent": 299, "clusterCaption": 299, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 300, "type": "Feature", "geometry": { "type": "Point", "coordinates": [9.962936430238187, -176.71439261175692] }, "number": 300, "properties": { "iconContent": 2, "balloonContent": 300, "hintContent": 300, "clusterCaption": 300, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 301, "type": "Feature", "geometry": { "type": "Point", "coordinates": [34.998999438248575, -144.38321387395263] }, "number": 301, "properties": { "iconContent": 2, "balloonContent": 301, "hintContent": 301, "clusterCaption": 301, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 302, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-38.54598636040464, 64.96289120055735] }, "number": 302, "properties": { "iconContent": 2, "balloonContent": 302, "hintContent": 302, "clusterCaption": 302, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 303, "type": "Feature", "geometry": { "type": "Point", "coordinates": [6.9263315899297595, 152.6497668866068] }, "number": 303, "properties": { "iconContent": 2, "balloonContent": 303, "hintContent": 303, "clusterCaption": 303, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 304, "type": "Feature", "geometry": { "type": "Point", "coordinates": [21.131569478893653, 176.02837767452002] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 304, "hintContent": 304, "clusterCaption": 304, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 305, "type": "Feature", "geometry": { "type": "Point", "coordinates": [41.068942877463996, 45.08204424753785] }, "number": 305, "properties": { "iconContent": 2, "balloonContent": 305, "hintContent": 305, "clusterCaption": 305, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 306, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-13.524026434170082, -163.37228508666158] }, "number": 306, "properties": { "iconContent": 2, "balloonContent": 306, "hintContent": 306, "clusterCaption": 306, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 307, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-73.56294731609523, 131.41321670729667] }, "number": 307, "properties": { "iconContent": 2, "balloonContent": 307, "hintContent": 307, "clusterCaption": 307, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 308, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-84.67693367623724, 53.554650214500725] }, "number": 308, "properties": { "iconContent": 2, "balloonContent": 308, "hintContent": 308, "clusterCaption": 308, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 309, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-37.420748425647616, -164.67645100783557] }, "number": 309, "properties": { "iconContent": 2, "balloonContent": 309, "hintContent": 309, "clusterCaption": 309, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 310, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-30.528797074221075, 160.709901410155] }, "number": 310, "properties": { "iconContent": 2, "balloonContent": 310, "hintContent": 310, "clusterCaption": 310, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 311, "type": "Feature", "geometry": { "type": "Point", "coordinates": [10.21321926265955, 0.5770146986469626] }, "number": 311, "properties": { "iconContent": 2, "balloonContent": 311, "hintContent": 311, "clusterCaption": 311, "type": "311"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 312, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-67.22154104034416, -107.1865521138534] }, "number": 312, "properties": { "iconContent": 2, "balloonContent": 312, "hintContent": 312, "clusterCaption": 312, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 313, "type": "Feature", "geometry": { "type": "Point", "coordinates": [62.15403938549571, 176.86375121120363] }, "number": 313, "properties": { "iconContent": 2, "balloonContent": 313, "hintContent": 313, "clusterCaption": 313, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 314, "type": "Feature", "geometry": { "type": "Point", "coordinates": [32.08622458972968, 144.32812695857137] }, "number": 314, "properties": { "iconContent": 2, "balloonContent": 314, "hintContent": 314, "clusterCaption": 314, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 315, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-57.86344317137264, 134.12218092475086] }, "number": 315, "properties": { "iconContent": 2, "balloonContent": 315, "hintContent": 315, "clusterCaption": 315, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 316, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-4.466680348850787, -155.69949415046722] }, "number": 316, "properties": { "iconContent": 2, "balloonContent": 316, "hintContent": 316, "clusterCaption": 316, "type": "316"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 317, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-50.20613138680346, -16.38360736425966] }, "number": 317, "properties": { "iconContent": 2, "balloonContent": 317, "hintContent": 317, "clusterCaption": 317, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 318, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-64.08699910738505, 93.0932351667434] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 318, "hintContent": 318, "clusterCaption": 318, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 319, "type": "Feature", "geometry": { "type": "Point", "coordinates": [59.22086899285205, -78.21308105718344] }, "number": 319, "properties": { "iconContent": 2, "balloonContent": 319, "hintContent": 319, "clusterCaption": 319, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 320, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-14.273321903310716, -136.88525372184813] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 320, "hintContent": 320, "clusterCaption": 320, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 321, "type": "Feature", "geometry": { "type": "Point", "coordinates": [43.04181007668376, -132.4410569295287] }, "number": 321, "properties": { "iconContent": 2, "balloonContent": 321, "hintContent": 321, "clusterCaption": 321, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 322, "type": "Feature", "geometry": { "type": "Point", "coordinates": [47.49224938452244, -53.048162776976824] }, "number": 322, "properties": { "iconContent": 2, "balloonContent": 322, "hintContent": 322, "clusterCaption": 322, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 323, "type": "Feature", "geometry": { "type": "Point", "coordinates": [82.17069962644018, 20.92940418049693] }, "number": 323, "properties": { "iconContent": 2, "balloonContent": 323, "hintContent": 323, "clusterCaption": 323, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 324, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-67.33808533870615, 86.96980135515332] }, "number": 324, "properties": { "iconContent": 2, "balloonContent": 324, "hintContent": 324, "clusterCaption": 324, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 325, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-41.931595862843096, -13.329776194877923] }, "number": 325, "properties": { "iconContent": 2, "balloonContent": 325, "hintContent": 325, "clusterCaption": 325, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 326, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-18.1110068236012, 58.92594622448087] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 326, "hintContent": 326, "clusterCaption": 326, "type": "326"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 327, "type": "Feature", "geometry": { "type": "Point", "coordinates": [47.948520054342225, 141.68739956337959] }, "number": 327, "properties": { "iconContent": 2, "balloonContent": 327, "hintContent": 327, "clusterCaption": 327, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 328, "type": "Feature", "geometry": { "type": "Point", "coordinates": [7.682951561873779, 158.23609890416265] }, "number": 328, "properties": { "iconContent": 2, "balloonContent": 328, "hintContent": 328, "clusterCaption": 328, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 329, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-29.94970244821161, -147.18835615552962] }, "number": 329, "properties": { "iconContent": 2, "balloonContent": 329, "hintContent": 329, "clusterCaption": 329, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 330, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-61.60928288358264, -72.70007380284369] }, "number": 330, "properties": { "iconContent": 2, "balloonContent": 330, "hintContent": 330, "clusterCaption": 330, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 331, "type": "Feature", "geometry": { "type": "Point", "coordinates": [25.760756301460788, -8.846806436777115] }, "number": 331, "properties": { "iconContent": 2, "balloonContent": 331, "hintContent": 331, "clusterCaption": 331, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 332, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-0.267212645849213, -132.265294957906] }, "number": 332, "properties": { "iconContent": 2, "balloonContent": 332, "hintContent": 332, "clusterCaption": 332, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 333, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-8.513509886106476, -14.465332296676934] }, "number": 333, "properties": { "iconContent": 2, "balloonContent": 333, "hintContent": 333, "clusterCaption": 333, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 334, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-75.51067441119812, -148.549621864222] }, "number": 334, "properties": { "iconContent": 2, "balloonContent": 334, "hintContent": 334, "clusterCaption": 334, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 335, "type": "Feature", "geometry": { "type": "Point", "coordinates": [11.605496022384614, 2.4870479246601462] }, "number": 335, "properties": { "iconContent": 2, "balloonContent": 335, "hintContent": 335, "clusterCaption": 335, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 336, "type": "Feature", "geometry": { "type": "Point", "coordinates": [44.21851809020154, 82.99830714706331] }, "number": 336, "properties": { "iconContent": 2, "balloonContent": 336, "hintContent": 336, "clusterCaption": 336, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 337, "type": "Feature", "geometry": { "type": "Point", "coordinates": [47.446051008300856, -28.20466303266585] }, "number": 337, "properties": { "iconContent": 2, "balloonContent": 337, "hintContent": 337, "clusterCaption": 337, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 338, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-51.44518473302014, 165.0471784779802] }, "number": 338, "properties": { "iconContent": 2, "balloonContent": 338, "hintContent": 338, "clusterCaption": 338, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 339, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-2.9034123092424124, -118.12379334121943] }, "number": 339, "properties": { "iconContent": 2, "balloonContent": 339, "hintContent": 339, "clusterCaption": 339, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 340, "type": "Feature", "geometry": { "type": "Point", "coordinates": [63.67714498541318, 112.73445960134268] }, "number": 340, "properties": { "iconContent": 2, "balloonContent": 340, "hintContent": 340, "clusterCaption": 340, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 341, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-19.119755873689428, -79.1787191759795] }, "number": 341, "properties": { "iconContent": 2, "balloonContent": 341, "hintContent": 341, "clusterCaption": 341, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 342, "type": "Feature", "geometry": { "type": "Point", "coordinates": [61.95391597924754, 149.64498854242265] }, "number": 342, "properties": { "iconContent": 2, "balloonContent": 342, "hintContent": 342, "clusterCaption": 342, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 343, "type": "Feature", "geometry": { "type": "Point", "coordinates": [38.74282868229784, -40.35986389964819] }, "number": 343, "properties": { "iconContent": 2, "balloonContent": 343, "hintContent": 343, "clusterCaption": 343, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 344, "type": "Feature", "geometry": { "type": "Point", "coordinates": [69.20313783572055, 140.98873004782945] }, "number": 344, "properties": { "iconContent": 2, "balloonContent": 344, "hintContent": 344, "clusterCaption": 344, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 345, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-29.854650251800194, -28.03532608319074] }, "number": 345, "properties": { "iconContent": 2, "balloonContent": 345, "hintContent": 345, "clusterCaption": 345, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 346, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-6.942438945407048, -136.0082748066634] }, "number": 346, "properties": { "iconContent": 2, "balloonContent": 346, "hintContent": 346, "clusterCaption": 346, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 347, "type": "Feature", "geometry": { "type": "Point", "coordinates": [83.77172265783884, 130.11116554029286] }, "number": 347, "properties": { "iconContent": 2, "balloonContent": 347, "hintContent": 347, "clusterCaption": 347, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 348, "type": "Feature", "geometry": { "type": "Point", "coordinates": [55.0104065798223, 64.82251045759767] }, "number": 348, "properties": { "iconContent": 2, "balloonContent": 348, "hintContent": 348, "clusterCaption": 348, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 349, "type": "Feature", "geometry": { "type": "Point", "coordinates": [14.94294615695253, -69.08331686630845] }, "number": 349, "properties": { "iconContent": 2, "balloonContent": 349, "hintContent": 349, "clusterCaption": 349, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 350, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-58.988108484772965, 94.7601426159963] }, "number": 350, "properties": { "iconContent": 2, "balloonContent": 350, "hintContent": 350, "clusterCaption": 350, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 351, "type": "Feature", "geometry": { "type": "Point", "coordinates": [65.79281784361228, 93.67574411910027] }, "number": 351, "properties": { "iconContent": 2, "balloonContent": 351, "hintContent": 351, "clusterCaption": 351, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 352, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-32.27841609972529, 114.65190758462995] }, "number": 352, "properties": { "iconContent": 2, "balloonContent": 352, "hintContent": 352, "clusterCaption": 352, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 353, "type": "Feature", "geometry": { "type": "Point", "coordinates": [28.574171548243612, 51.94018707610667] }, "number": 353, "properties": { "iconContent": 2, "balloonContent": 353, "hintContent": 353, "clusterCaption": 353, "type": "353"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 354, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-66.11847972497344, 173.14038979355246] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 354, "hintContent": 354, "clusterCaption": 354, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 355, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-37.03808677266352, 176.97497185785323] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 355, "hintContent": 355, "clusterCaption": 355, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 356, "type": "Feature", "geometry": { "type": "Point", "coordinates": [14.377771134022623, 138.10243237763643] }, "number": 356, "properties": { "iconContent": 2, "balloonContent": 356, "hintContent": 356, "clusterCaption": 356, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 357, "type": "Feature", "geometry": { "type": "Point", "coordinates": [44.91094258497469, 59.05333107803017] }, "number": 357, "properties": { "iconContent": 2, "balloonContent": 357, "hintContent": 357, "clusterCaption": 357, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 358, "type": "Feature", "geometry": { "type": "Point", "coordinates": [10.935402513714507, 61.909249420277774] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 358, "hintContent": 358, "clusterCaption": 358, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 359, "type": "Feature", "geometry": { "type": "Point", "coordinates": [21.043218282284215, 144.74810665939003] }, "number": 359, "properties": { "iconContent": 2, "balloonContent": 359, "hintContent": 359, "clusterCaption": 359, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 360, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-57.077412084909156, 174.75413577631116] }, "number": 360, "properties": { "iconContent": 2, "balloonContent": 360, "hintContent": 360, "clusterCaption": 360, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 361, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-14.008704122388735, -28.639572956599295] }, "number": 361, "properties": { "iconContent": 2, "balloonContent": 361, "hintContent": 361, "clusterCaption": 361, "type": "361"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 362, "type": "Feature", "geometry": { "type": "Point", "coordinates": [51.135650551877916, -75.3084067767486] }, "number": 362, "properties": { "iconContent": 2, "balloonContent": 362, "hintContent": 362, "clusterCaption": 362, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 363, "type": "Feature", "geometry": { "type": "Point", "coordinates": [73.62079937011003, -116.3154930761084] }, "number": 363, "properties": { "iconContent": 2, "balloonContent": 363, "hintContent": 363, "clusterCaption": 363, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 364, "type": "Feature", "geometry": { "type": "Point", "coordinates": [66.52200861135498, -24.766812454909086] }, "number": 364, "properties": { "iconContent": 2, "balloonContent": 364, "hintContent": 364, "clusterCaption": 364, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 365, "type": "Feature", "geometry": { "type": "Point", "coordinates": [59.480082157533616, 35.013464321382344] }, "number": 365, "properties": { "iconContent": 2, "balloonContent": 365, "hintContent": 365, "clusterCaption": 365, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 366, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-82.92703694896773, -41.53290026821196] }, "number": 366, "properties": { "iconContent": 2, "balloonContent": 366, "hintContent": 366, "clusterCaption": 366, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 367, "type": "Feature", "geometry": { "type": "Point", "coordinates": [17.93536805664189, 163.94926734734327] }, "number": 367, "properties": { "iconContent": 2, "balloonContent": 367, "hintContent": 367, "clusterCaption": 367, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 368, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-79.1730108729098, -118.38798405602574] }, "number": 368, "properties": { "iconContent": 2, "balloonContent": 368, "hintContent": 368, "clusterCaption": 368, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 369, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-40.27588836266659, 47.84823917783797] }, "number": 369, "properties": { "iconContent": 2, "balloonContent": 369, "hintContent": 369, "clusterCaption": 369, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 370, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-38.35379645344801, 155.32247861847281] }, "number": 370, "properties": { "iconContent": 2, "balloonContent": 370, "hintContent": 370, "clusterCaption": 370, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 371, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-14.310031199129298, 72.09465363062918] }, "number": 371, "properties": { "iconContent": 2, "balloonContent": 371, "hintContent": 371, "clusterCaption": 371, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 372, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-75.64100739778951, -76.47344387602061] }, "number": 372, "properties": { "iconContent": 2, "balloonContent": 372, "hintContent": 372, "clusterCaption": 372, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 373, "type": "Feature", "geometry": { "type": "Point", "coordinates": [17.61418197886087, 24.879413852468133] }, "number": 373, "properties": { "iconContent": 2, "balloonContent": 373, "hintContent": 373, "clusterCaption": 373, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 374, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-33.70180644094944, -162.89987612050027] }, "number": 374, "properties": { "iconContent": 2, "balloonContent": 374, "hintContent": 374, "clusterCaption": 374, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 375, "type": "Feature", "geometry": { "type": "Point", "coordinates": [32.69939703401178, 36.27803396433592] }, "number": 375, "properties": { "iconContent": 2, "balloonContent": 375, "hintContent": 375, "clusterCaption": 375, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 376, "type": "Feature", "geometry": { "type": "Point", "coordinates": [73.35609581670724, 58.95256840623915] }, "number": 376, "properties": { "iconContent": 2, "balloonContent": 376, "hintContent": 376, "clusterCaption": 376, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 377, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-69.69067348516546, 135.91577991843224] }, "number": 377, "properties": { "iconContent": 2, "balloonContent": 377, "hintContent": 377, "clusterCaption": 377, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 378, "type": "Feature", "geometry": { "type": "Point", "coordinates": [50.99053305340931, -146.97180708870292] }, "number": 378, "properties": { "iconContent": 2, "balloonContent": 378, "hintContent": 378, "clusterCaption": 378, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 379, "type": "Feature", "geometry": { "type": "Point", "coordinates": [68.56498992536217, -158.86019927915186] }, "number": 379, "properties": { "iconContent": 2, "balloonContent": 379, "hintContent": 379, "clusterCaption": 379, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 380, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-71.99420276097953, 84.47428984567523] }, "number": 380, "properties": { "iconContent": 2, "balloonContent": 380, "hintContent": 380, "clusterCaption": 380, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 381, "type": "Feature", "geometry": { "type": "Point", "coordinates": [59.24746548756957, -86.03476933669299] }, "number": 381, "properties": { "iconContent": 2, "balloonContent": 381, "hintContent": 381, "clusterCaption": 381, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 382, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-41.63803355069831, 73.61240607686341] }, "number": 382, "properties": { "iconContent": 2, "balloonContent": 382, "hintContent": 382, "clusterCaption": 382, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 383, "type": "Feature", "geometry": { "type": "Point", "coordinates": [24.5923756796401, 23.825904512777925] }, "number": 383, "properties": { "iconContent": 2, "balloonContent": 383, "hintContent": 383, "clusterCaption": 383, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 384, "type": "Feature", "geometry": { "type": "Point", "coordinates": [30.59795318520628, -4.984654490835965] }, "number": 384, "properties": { "iconContent": 2, "balloonContent": 384, "hintContent": 384, "clusterCaption": 384, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 385, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-65.440956922248, 144.1795050445944] }, "number": 385, "properties": { "iconContent": 2, "balloonContent": 385, "hintContent": 385, "clusterCaption": 385, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 386, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-46.31300993496552, 170.92776407953352] }, "number": 386, "properties": { "iconContent": 2, "balloonContent": 386, "hintContent": 386, "clusterCaption": 386, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 387, "type": "Feature", "geometry": { "type": "Point", "coordinates": [82.42254057317041, -118.62406807020307] }, "number": 387, "properties": { "iconContent": 2, "balloonContent": 387, "hintContent": 387, "clusterCaption": 387, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 388, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-38.00495910109021, 162.85845547914505] }, "number": 388, "properties": { "iconContent": 2, "balloonContent": 388, "hintContent": 388, "clusterCaption": 388, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 389, "type": "Feature", "geometry": { "type": "Point", "coordinates": [81.66389635880478, -14.701061001978815] }, "number": 389, "properties": { "iconContent": 2, "balloonContent": 389, "hintContent": 389, "clusterCaption": 389, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 390, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-68.14526632777415, -155.85030633956194] }, "number": 390, "properties": { "iconContent": 2, "balloonContent": 390, "hintContent": 390, "clusterCaption": 390, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 391, "type": "Feature", "geometry": { "type": "Point", "coordinates": [82.27166450349614, 33.05555902887136] }, "number": 391, "properties": { "iconContent": 2, "balloonContent": 391, "hintContent": 391, "clusterCaption": 391, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 392, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-55.00186515273526, -116.73326594755054] }, "number": 392, "properties": { "iconContent": 2, "balloonContent": 392, "hintContent": 392, "clusterCaption": 392, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 393, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-69.70095589174889, -104.71691323444247] }, "number": 393, "properties": { "iconContent": 2, "balloonContent": 393, "hintContent": 393, "clusterCaption": 393, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 394, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-19.059799326350912, -174.94236612226814] }, "number": 394, "properties": { "iconContent": 2, "balloonContent": 394, "hintContent": 394, "clusterCaption": 394, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 395, "type": "Feature", "geometry": { "type": "Point", "coordinates": [10.088085070019588, 139.16615682654083] }, "number": 395, "properties": { "iconContent": 2, "balloonContent": 395, "hintContent": 395, "clusterCaption": 395, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 396, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-9.011563925305381, -76.74838538747281] }, "number": 396, "properties": { "iconContent": 2, "balloonContent": 396, "hintContent": 396, "clusterCaption": 396, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 397, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-61.142329938011244, -129.82643149327487] }, "number": 397, "properties": { "iconContent": 2, "balloonContent": 397, "hintContent": 397, "clusterCaption": 397, "type": "397"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 398, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-78.47524433978833, 138.69396817870438] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 398, "hintContent": 398, "clusterCaption": 398, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 399, "type": "Feature", "geometry": { "type": "Point", "coordinates": [48.64548882818781, -44.26233539823443] }, "number": 399, "properties": { "iconContent": 2, "balloonContent": 399, "hintContent": 399, "clusterCaption": 399, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 400, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-16.132477829232812, 120.9331250237301] }, "number": 400, "properties": { "iconContent": 2, "balloonContent": 400, "hintContent": 400, "clusterCaption": 400, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 401, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-55.87615819997154, -100.79260947648436] }, "number": 401, "properties": { "iconContent": 2, "balloonContent": 401, "hintContent": 401, "clusterCaption": 401, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 402, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-54.09085798077285, 127.06349884159863] }, "number": 402, "properties": { "iconContent": 2, "balloonContent": 402, "hintContent": 402, "clusterCaption": 402, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 403, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-6.9684645207598805, -137.76135379448533] }, "number": 403, "properties": { "iconContent": 2, "balloonContent": 403, "hintContent": 403, "clusterCaption": 403, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 404, "type": "Feature", "geometry": { "type": "Point", "coordinates": [20.252418966265395, -169.52251739334315] }, "number": 404, "properties": { "iconContent": 2, "balloonContent": 404, "hintContent": 404, "clusterCaption": 404, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 405, "type": "Feature", "geometry": { "type": "Point", "coordinates": [23.81964405765757, -12.75759399868548] }, "number": 405, "properties": { "iconContent": 2, "balloonContent": 405, "hintContent": 405, "clusterCaption": 405, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 406, "type": "Feature", "geometry": { "type": "Point", "coordinates": [13.986815416719764, 134.7804161394015] }, "number": 406, "properties": { "iconContent": 2, "balloonContent": 406, "hintContent": 406, "clusterCaption": 406, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 407, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-52.78478569118306, 22.694224370643497] }, "number": 407, "properties": { "iconContent": 2, "balloonContent": 407, "hintContent": 407, "clusterCaption": 407, "type": "407"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 408, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-44.79711886146106, 49.68541476409882] }, "number": 408, "properties": { "iconContent": 2, "balloonContent": 408, "hintContent": 408, "clusterCaption": 408, "type": "id"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 409, "type": "Feature", "geometry": { "type": "Point", "coordinates": [14.864975290838629, 37.55443193949759] }, "number": 409, "properties": { "iconContent": 2, "balloonContent": 409, "hintContent": 409, "clusterCaption": 409, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 410, "type": "Feature", "geometry": { "type": "Point", "coordinates": [32.713807424297556, 17.04709677491337] }, "number": 410, "properties": { "iconContent": 2, "balloonContent": 410, "hintContent": 410, "clusterCaption": 410, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 411, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-74.14106486015953, 127.96774970367551] }, "number": 411, "properties": { "iconContent": 2, "balloonContent": 411, "hintContent": 411, "clusterCaption": 411, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 412, "type": "Feature", "geometry": { "type": "Point", "coordinates": [17.061733779264614, 167.1651742234826] }, "number": 412, "properties": { "iconContent": 2, "balloonContent": 412, "hintContent": 412, "clusterCaption": 412, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 413, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-40.089353086659685, -99.63462315034121] }, "number": 413, "properties": { "iconContent": 2, "balloonContent": 413, "hintContent": 413, "clusterCaption": 413, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 414, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-35.03701156936586, 99.97096662875265] }, "number": 414, "properties": { "iconContent": 2, "balloonContent": 414, "hintContent": 414, "clusterCaption": 414, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 415, "type": "Feature", "geometry": { "type": "Point", "coordinates": [57.645318389404565, -71.45529840141535] }, "number": 415, "properties": { "iconContent": 2, "balloonContent": 415, "hintContent": 415, "clusterCaption": 415, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 416, "type": "Feature", "geometry": { "type": "Point", "coordinates": [10.702526012901217, -138.55264480691403] }, "number": 416, "properties": { "iconContent": 2, "balloonContent": 416, "hintContent": 416, "clusterCaption": 416, "type": "416"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 417, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-23.63664841046557, 3.849858879111707] }, "number": 417, "properties": { "iconContent": 2, "balloonContent": 417, "hintContent": 417, "clusterCaption": 417, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 418, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-57.113954407395795, 55.96779996994883] }, "number": 418, "properties": { "iconContent": 2, "balloonContent": 418, "hintContent": 418, "clusterCaption": 418, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 419, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-32.55741155706346, 60.158170186914504] }, "number": 419, "properties": { "iconContent": 2, "balloonContent": 419, "hintContent": 419, "clusterCaption": 419, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 420, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-61.31049711839296, 114.57075550686568] }, "number": 420, "properties": { "iconContent": 2, "balloonContent": 420, "hintContent": 420, "clusterCaption": 420, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 421, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-68.80364878568798, -95.85284280125052] }, "number": 421, "properties": { "iconContent": 2, "balloonContent": 421, "hintContent": 421, "clusterCaption": 421, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 422, "type": "Feature", "geometry": { "type": "Point", "coordinates": [71.48625520057976, -69.02549791149795] }, "number": 422, "properties": { "iconContent": 2, "balloonContent": 422, "hintContent": 422, "clusterCaption": 422, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 423, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-34.62386848637834, 176.41230592969805] }, "number": 423, "properties": { "iconContent": 2, "balloonContent": 423, "hintContent": 423, "clusterCaption": 423, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 424, "type": "Feature", "geometry": { "type": "Point", "coordinates": [72.85780066857114, 172.07386071793735] }, "number": 424, "properties": { "iconContent": 2, "balloonContent": 424, "hintContent": 424, "clusterCaption": 424, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 425, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-56.95290782721713, 6.440724888816476] }, "number": 425, "properties": { "iconContent": 2, "balloonContent": 425, "hintContent": 425, "clusterCaption": 425, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 426, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-59.21190946944989, 118.43992557842284] }, "number": 426, "properties": { "iconContent": 2, "balloonContent": 426, "hintContent": 426, "clusterCaption": 426, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 427, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-9.64402332319878, -44.468566132709384] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 427, "hintContent": 427, "clusterCaption": 427, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 428, "type": "Feature", "geometry": { "type": "Point", "coordinates": [39.747367716627195, -141.9112320849672] }, "number": 428, "properties": { "iconContent": 2, "balloonContent": 428, "hintContent": 428, "clusterCaption": 428, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 429, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-67.79420888517052, 28.035312294960022] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 429, "hintContent": 429, "clusterCaption": 429, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 430, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-79.02868877863511, -134.31349606253207] }, "number": 430, "properties": { "iconContent": 2, "balloonContent": 430, "hintContent": 430, "clusterCaption": 430, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 431, "type": "Feature", "geometry": { "type": "Point", "coordinates": [6.655849595554173, 81.75730930175632] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 431, "hintContent": 431, "clusterCaption": 431, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 432, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-38.94023964763619, 10.133288344368339] }, "number": 432, "properties": { "iconContent": 2, "balloonContent": 432, "hintContent": 432, "clusterCaption": 432, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 433, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-51.0200703330338, 5.684899426996708] }, "number": 433, "properties": { "iconContent": 2, "balloonContent": 433, "hintContent": 433, "clusterCaption": 433, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 434, "type": "Feature", "geometry": { "type": "Point", "coordinates": [53.78625908168033, 169.09137995447963] }, "number": 434, "properties": { "iconContent": 2, "balloonContent": 434, "hintContent": 434, "clusterCaption": 434, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 435, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-2.1074423019308597, -85.44044278096408] }, "number": 435, "properties": { "iconContent": 2, "balloonContent": 435, "hintContent": 435, "clusterCaption": 435, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 436, "type": "Feature", "geometry": { "type": "Point", "coordinates": [76.93866939283907, -178.55155985802412] }, "number": 436, "properties": { "iconContent": 2, "balloonContent": 436, "hintContent": 436, "clusterCaption": 436, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 437, "type": "Feature", "geometry": { "type": "Point", "coordinates": [20.197854705620557, 29.55704585183412] }, "number": 437, "properties": { "iconContent": 2, "balloonContent": 437, "hintContent": 437, "clusterCaption": 437, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 438, "type": "Feature", "geometry": { "type": "Point", "coordinates": [24.02550163329579, -26.716477666050196] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 438, "hintContent": 438, "clusterCaption": 438, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 439, "type": "Feature", "geometry": { "type": "Point", "coordinates": [19.838208872824907, 48.501827172003686] }, "number": 439, "properties": { "iconContent": 2, "balloonContent": 439, "hintContent": 439, "clusterCaption": 439, "type": "439"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 440, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-76.20379058760591, -97.94901177752763] }, "number": 440, "properties": { "iconContent": 2, "balloonContent": 440, "hintContent": 440, "clusterCaption": 440, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 441, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-32.44289138237946, -100.71668112650514] }, "number": 441, "properties": { "iconContent": 2, "balloonContent": 441, "hintContent": 441, "clusterCaption": 441, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 442, "type": "Feature", "geometry": { "type": "Point", "coordinates": [38.65883131977171, -8.54008146096021] }, "number": 442, "properties": { "iconContent": 2, "balloonContent": 442, "hintContent": 442, "clusterCaption": 442, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 443, "type": "Feature", "geometry": { "type": "Point", "coordinates": [40.458181898575276, 3.9111025957390666] }, "number": 443, "properties": { "iconContent": 2, "balloonContent": 443, "hintContent": 443, "clusterCaption": 443, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 444, "type": "Feature", "geometry": { "type": "Point", "coordinates": [66.78748457226902, 179.51635752338916] }, "number": 444, "properties": { "iconContent": 2, "balloonContent": 444, "hintContent": 444, "clusterCaption": 444, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 445, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-51.889701762702316, -74.69121916685253] }, "number": 445, "properties": { "iconContent": 2, "balloonContent": 445, "hintContent": 445, "clusterCaption": 445, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 446, "type": "Feature", "geometry": { "type": "Point", "coordinates": [46.35685786022805, -18.346958924084902] }, "number": 446, "properties": { "iconContent": 2, "balloonContent": 446, "hintContent": 446, "clusterCaption": 446, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 447, "type": "Feature", "geometry": { "type": "Point", "coordinates": [36.20701545616612, 76.58413076307625] }, "number": 447, "properties": { "iconContent": 2, "balloonContent": 447, "hintContent": 447, "clusterCaption": 447, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 448, "type": "Feature", "geometry": { "type": "Point", "coordinates": [46.692976384656504, -107.23750503268093] }, "number": 448, "properties": { "iconContent": 2, "balloonContent": 448, "hintContent": 448, "clusterCaption": 448, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 449, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-7.816810264484957, 156.4436240075156] }, "number": 449, "properties": { "iconContent": 2, "balloonContent": 449, "hintContent": 449, "clusterCaption": 449, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 450, "type": "Feature", "geometry": { "type": "Point", "coordinates": [20.29818477923982, -141.3378570182249] }, "number": 450, "properties": { "iconContent": 2, "balloonContent": 450, "hintContent": 450, "clusterCaption": 450, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 451, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-6.07564133242704, -23.97965891752392] }, "number": 451, "properties": { "iconContent": 2, "balloonContent": 451, "hintContent": 451, "clusterCaption": 451, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 452, "type": "Feature", "geometry": { "type": "Point", "coordinates": [59.050537571311, -155.88697058614343] }, "number": 452, "properties": { "iconContent": 2, "balloonContent": 452, "hintContent": 452, "clusterCaption": 452, "type": "аптека"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 453, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-5.485644675791264, -48.691947469487786] }, "number": 453, "properties": { "iconContent": 2, "balloonContent": 453, "hintContent": 453, "clusterCaption": 453, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 454, "type": "Feature", "geometry": { "type": "Point", "coordinates": [4.186373902484775, 168.57989522628486] }, "number": 454, "properties": { "iconContent": 2, "balloonContent": 454, "hintContent": 454, "clusterCaption": 454, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 455, "type": "Feature", "geometry": { "type": "Point", "coordinates": [36.14676529541612, -164.2345046857372] }, "number": 455, "properties": { "iconContent": 2, "balloonContent": 455, "hintContent": 455, "clusterCaption": 455, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 456, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-81.49799864622764, 37.746648509055376] }, "number": 456, "properties": { "iconContent": 2, "balloonContent": 456, "hintContent": 456, "clusterCaption": 456, "type": "кафе"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 457, "type": "Feature", "geometry": { "type": "Point", "coordinates": [55.708170322468504, 85.05533636547625] }, "number": 457, "properties": { "iconContent": 2, "balloonContent": 457, "hintContent": 457, "clusterCaption": 457, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 458, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-60.51091407891363, 78.90333065763116] }, "number": 458, "properties": { "iconContent": 2, "balloonContent": 458, "hintContent": 458, "clusterCaption": 458, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 459, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-12.435655348235741, 47.69203423988074] }, "number": 459, "properties": { "iconContent": 2, "balloonContent": 459, "hintContent": 459, "clusterCaption": 459, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 460, "type": "Feature", "geometry": { "type": "Point", "coordinates": [81.88735791482031, 154.97410568408668] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 460, "hintContent": 460, "clusterCaption": 460, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 461, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-80.52724270848557, -172.6647268049419] }, "number": 461, "properties": { "iconContent": 2, "balloonContent": 461, "hintContent": 461, "clusterCaption": 461, "type": "банк"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 462, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-20.425695764133707, 160.14844042249024] }, "number": 462, "properties": { "iconContent": 2, "balloonContent": 462, "hintContent": 462, "clusterCaption": 462, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 463, "type": "Feature", "geometry": { "type": "Point", "coordinates": [22.791659351205453, -28.862859313376248] }, "number": 463, "properties": { "iconContent": 2, "balloonContent": 463, "hintContent": 463, "clusterCaption": 463, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 464, "type": "Feature", "geometry": { "type": "Point", "coordinates": [72.50472666230053, -64.48567890096456] }, "number": 464, "properties": { "iconContent": 2, "balloonContent": 464, "hintContent": 464, "clusterCaption": 464, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 465, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-12.076845747651532, -68.97856176830828] }, "number": 465, "properties": { "iconContent": 2, "balloonContent": 465, "hintContent": 465, "clusterCaption": 465, "type": "кафе"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 466, "type": "Feature", "geometry": { "type": "Point", "coordinates": [40.152796956244856, 149.6491736266762] }, "number": 466, "properties": { "iconContent": 2, "balloonContent": 466, "hintContent": 466, "clusterCaption": 466, "type": "466"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 467, "type": "Feature", "geometry": { "type": "Point", "coordinates": [62.93013887363486, 21.30426401272416] }, "number": 467, "properties": { "iconContent": 2, "balloonContent": 467, "hintContent": 467, "clusterCaption": 467, "type": "id"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 468, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-3.625157119240612, 114.10575153306127] }, "number": 468, "properties": { "iconContent": 2, "balloonContent": 468, "hintContent": 468, "clusterCaption": 468, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 469, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-36.92599567701109, -85.77548346016556] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 469, "hintContent": 469, "clusterCaption": 469, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 470, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-4.7789486416149884, -5.756399827077985] }, "number": 470, "properties": { "iconContent": 2, "balloonContent": 470, "hintContent": 470, "clusterCaption": 470, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 471, "type": "Feature", "geometry": { "type": "Point", "coordinates": [6.343737787101418, -21.61121638957411] }, "number": 471, "properties": { "iconContent": 2, "balloonContent": 471, "hintContent": 471, "clusterCaption": 471, "type": "банк"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 472, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-11.929550372296944, -52.67156147863716] }, "number": 472, "properties": { "iconContent": 2, "balloonContent": 472, "hintContent": 472, "clusterCaption": 472, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 473, "type": "Feature", "geometry": { "type": "Point", "coordinates": [73.25797835248522, -102.66647047363222] }, "number": 473, "properties": { "iconContent": 2, "balloonContent": 473, "hintContent": 473, "clusterCaption": 473, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 474, "type": "Feature", "geometry": { "type": "Point", "coordinates": [82.39469519234262, 171.0305817425251] }, "number": 474, "properties": { "iconContent": 2, "balloonContent": 474, "hintContent": 474, "clusterCaption": 474, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 475, "type": "Feature", "geometry": { "type": "Point", "coordinates": [7.330371509306133, -73.2615264505148] }, "number": 475, "properties": { "iconContent": 2, "balloonContent": 475, "hintContent": 475, "clusterCaption": 475, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 476, "type": "Feature", "geometry": { "type": "Point", "coordinates": [26.00656103459187, 116.834526672028] }, "number": 476, "properties": { "iconContent": 2, "balloonContent": 476, "hintContent": 476, "clusterCaption": 476, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 477, "type": "Feature", "geometry": { "type": "Point", "coordinates": [56.73038432607427, 33.794578607194126] }, "number": 477, "properties": { "iconContent": 2, "balloonContent": 477, "hintContent": 477, "clusterCaption": 477, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 478, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-55.53673304151744, 2.096187877468765] }, "number": 478, "properties": { "iconContent": 2, "balloonContent": 478, "hintContent": 478, "clusterCaption": 478, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 479, "type": "Feature", "geometry": { "type": "Point", "coordinates": [72.28977963794023, 156.8402661010623] }, "number": 479, "properties": { "iconContent": 2, "balloonContent": 479, "hintContent": 479, "clusterCaption": 479, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 480, "type": "Feature", "geometry": { "type": "Point", "coordinates": [29.24349706270732, 14.96772647369653] }, "number": 480, "properties": { "iconContent": 2, "balloonContent": 480, "hintContent": 480, "clusterCaption": 480, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 481, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-66.39868286438286, 11.091731772758067] }, "number": 481, "properties": { "iconContent": 2, "balloonContent": 481, "hintContent": 481, "clusterCaption": 481, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 482, "type": "Feature", "geometry": { "type": "Point", "coordinates": [22.960584769025445, 41.87277969438583] }, "number": 482, "properties": { "iconContent": 2, "balloonContent": 482, "hintContent": 482, "clusterCaption": 482, "type": "банк"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 483, "type": "Feature", "geometry": { "type": "Point", "coordinates": [27.7099425974302, -15.929539469070733] }, "number": 483, "properties": { "iconContent": 2, "balloonContent": 483, "hintContent": 483, "clusterCaption": 483, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 484, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-78.22443891665898, 96.74266254063696] }, "number": 484, "properties": { "iconContent": 2, "balloonContent": 484, "hintContent": 484, "clusterCaption": 484, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 485, "type": "Feature", "geometry": { "type": "Point", "coordinates": [77.52308955183253, -13.469015532173216] }, "number": 485, "properties": { "iconContent": 2, "balloonContent": 485, "hintContent": 485, "clusterCaption": 485, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 486, "type": "Feature", "geometry": { "type": "Point", "coordinates": [83.37058255681768, -116.30080488044769] }, "number": 5, "properties": { "iconContent": 2, "balloonContent": 486, "hintContent": 486, "clusterCaption": 486, "type": "486"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 487, "type": "Feature", "geometry": { "type": "Point", "coordinates": [12.190566955832765, 66.21090876404196] }, "number": 487, "properties": { "iconContent": 2, "balloonContent": 487, "hintContent": 487, "clusterCaption": 487, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 488, "type": "Feature", "geometry": { "type": "Point", "coordinates": [22.271220337133855, -125.90884868055582] }, "number": 488, "properties": { "iconContent": 2, "balloonContent": 488, "hintContent": 488, "clusterCaption": 488, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 489, "type": "Feature", "geometry": { "type": "Point", "coordinates": [26.230652861995623, -47.79392612166703] }, "number": 489, "properties": { "iconContent": 2, "balloonContent": 489, "hintContent": 489, "clusterCaption": 489, "type": "школа"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 490, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-5.858523437054828, -106.01516480091959] }, "number": 490, "properties": { "iconContent": 2, "balloonContent": 490, "hintContent": 490, "clusterCaption": 490, "type": "аптека"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 491, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-55.01191286253743, -63.70535871013999] }, "number": 491, "properties": { "iconContent": 2, "balloonContent": 491, "hintContent": 491, "clusterCaption": 491, "type": "кафе"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 492, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-14.566112263128161, -36.84307691641152] }, "number": 492, "properties": { "iconContent": 2, "balloonContent": 492, "hintContent": 492, "clusterCaption": 492, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 493, "type": "Feature", "geometry": { "type": "Point", "coordinates": [29.37523482600227, -116.12255017273128] }, "number": 493, "properties": { "iconContent": 2, "balloonContent": 493, "hintContent": 493, "clusterCaption": 493, "type": "аптека"}, "options": {"preset": "islands#greenIcon"}},
    { "id": 494, "type": "Feature", "geometry": { "type": "Point", "coordinates": [49.49839949258603, 161.8611359037459] }, "number": 494, "properties": { "iconContent": 2, "balloonContent": 494, "hintContent": 494, "clusterCaption": 494, "type": "школа"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 495, "type": "Feature", "geometry": { "type": "Point", "coordinates": [17.847157610813156, -74.08019850030541] }, "number": 495, "properties": { "iconContent": 2, "balloonContent": 495, "hintContent": 495, "clusterCaption": 495, "type": "школа"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 496, "type": "Feature", "geometry": { "type": "Point", "coordinates": [83.04439317667857, 150.8899344317615] }, "number": 496, "properties": { "iconContent": 2, "balloonContent": 496, "hintContent": 496, "clusterCaption": 496, "type": "id"}, "options": {"preset": "islands#orangeIcon"}},
    { "id": 497, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-69.37591864145361, -153.56583325657994] }, "number": 497, "properties": { "iconContent": 2, "balloonContent": 497, "hintContent": 497, "clusterCaption": 497, "type": "банк"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 498, "type": "Feature", "geometry": { "type": "Point", "coordinates": [55.2711235138122, 58.88434407301247] }, "number": 498, "properties": { "iconContent": 2, "balloonContent": 498, "hintContent": 498, "clusterCaption": 498, "type": "кафе"}, "options": {"preset": "islands#yellowIcon"}},
    { "id": 499, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-0.61157779651694, -13.497984311543405] }, "number": 499, "properties": { "iconContent": 2, "balloonContent": 499, "hintContent": 499, "clusterCaption": 499, "type": "аптека"}, "options": {"preset": "islands#pinkIcon"}},
    { "id": 500, "type": "Feature", "geometry": { "type": "Point", "coordinates": [-35.85260404041037, -75.76286149676889] }, "number": 500, "properties": { "iconContent": 2, "balloonContent": 500, "hintContent": 500, "clusterCaption": 500, "type": "школа"}, "options": {"preset": "islands#greenIcon"}},
    {"id": 501, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [61.28246358013712, -82.62861336232163],
            [60.29903147253208, -82.68008847953752],
            [54.952320771990344, -90.83561725099571],
            [52.94217281509191, -83.65286790532991],
            [55.17040379229002, -87.98271695384756]
        ]
    ]}, "coordBBox": [57.73898961720988, -86.32089888677001], "properties": {"hintContent": 501, "balloonContent": 501, "type": "аптека"}, "options": {"fillColor": "#ffffff" }},
    {"id": 502, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-55.27988387038931, -173.56837068218738],
        [-52.92201031581499, -174.63627281365916],
        [-51.77499864134006, -171.4617274189368]
    ]}, "coordBBox": [-52.99151298473589, -173.0667132837698], "properties": {"hintContent": 502, "balloonContent": 502, "type": "кафе"}, "options": {"fillColor": "#ffffff" }},
    {"id": 503, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-26.628059488721192, -110.21834596409462], "radius": 246607.39372484386}, "coordBBox": [-27.803255278849974, -106.10220541711897], "properties": {"hintContent": 503, "balloonContent": 503, "type": "кафе"}, "options": {"fillColor": "#000000" }},
    {"id": 504, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [-61.38442093739286, -117.04480551183224],
        [-64.71959966700524, -118.38748414767906]
    ]}, "coordBBox": [-63.37113038520329, -115.56880011688918], "properties": {"hintContent": 504, "balloonContent": 504, "type": "кафе"}, "options": {"fillColor": "#000000" }},
    {"id": 505, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [-41.16426959284581, -41.45455380901694],
            [-41.566259348765016, -39.34264314593747],
            [-35.35707016475499, -33.24209287879057],
            [-38.66132178925909, -36.18513692403212],
            [-33.10236487071961, -37.067471536574885]
        ]
    ]}, "coordBBox": [-36.632915266091004, -37.534975027665496], "properties": {"hintContent": 505, "balloonContent": 505, "type": "школа"}, "options": {"fillColor": "#ffffff" }},
    {"id": 506, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-9.31838562944904, -77.82554304692894],
        [-10.143935054074973, -78.57491316157393],
        [-5.06146392901428, -72.49339927104302]
    ]}, "coordBBox": [-5.853067642310634, -74.09919725731015], "properties": {"hintContent": 506, "balloonContent": 506, "type": "банк"}, "options": {"fillColor": "#ff0000" }},
    {"id": 507, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [34.39762424211949, 127.31177920126356], "radius": 31285.98164767027}, "coordBBox": [31.039825715124607, 122.66558578703552], "properties": {"hintContent": 507, "balloonContent": 507, "type": "банк"}, "options": {"fillColor": "#000000" }},
    {"id": 508, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [-85.81073367036879, -103.60785713302903],
        [-81.34968911181204, -97.9754385817796]
    ]}, "coordBBox": [-81.31324145710096, -100.2219632640481], "properties": {"hintContent": 508, "balloonContent": 508, "type": "кафе"}, "options": {"fillColor": "#00ff00" }},
    {"id": 509, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [56.488977406406775, -51.80616580066271],
            [64.05202310299501, -54.40180165693164],
            [59.75971671403386, -51.53741756337695],
            [55.33809304004535, -58.594662809045985],
            [62.38185235415585, -52.264135223813355]
        ]
    ]}, "coordBBox": [59.86155920429155, -56.398120508529246], "properties": {"hintContent": 509, "balloonContent": 509, "type": "банк"}, "options": {"fillColor": "#00ff00" }},
    {"id": 510, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-10.838679586304352, 76.18392512085848],
        [-13.35005194763653, 78.04533597780392],
        [-9.679307285696268, 78.05847939336672]
    ]}, "coordBBox": [-11.675671052653342, 76.19380284100771], "properties": {"hintContent": 510, "balloonContent": 510, "type": "школа"}, "options": {"fillColor": "#0000ff" }},
    {"id": 511, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-72.87511242670007, 3.300006063655019], "radius": 593271.3146321476}, "coordBBox": [-71.87832560390234, 5.978441974148154], "properties": {"hintContent": 511, "balloonContent": 511, "type": "банк"}, "options": {"fillColor": "#ff0000" }},
    {"id": 512, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [80.21971073001623, 46.36750903329812],
        [76.69492088025436, 46.95413741050288]
    ]}, "coordBBox": [78.039592273999, 50.085626877844334], "properties": {"hintContent": 512, "balloonContent": 512, "type": "банк"}, "options": {"fillColor": "#ffffff" }},
    {"id": 513, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [-18.541624901117757, 18.99354904424399],
            [-21.491070742486045, 17.117877781856805],
            [-17.877245849231258, 15.002132105873898],
            [-21.781132926698774, 20.632666423916817],
            [-18.76134432037361, 13.052207814762369]
        ]
    ]}, "coordBBox": [-20.667863993439823, 15.662749358452857], "properties": {"hintContent": 513, "balloonContent": 513, "type": "кафе"}, "options": {"fillColor": "#ffffff" }},
    {"id": 514, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-59.36947954352945, -98.5994808585383],
        [-57.9365229641553, -101.36885504703969],
        [-52.29597715428099, -94.49342601234093]
    ]}, "coordBBox": [-57.04551542876288, -97.12049409281462], "properties": {"hintContent": 514, "balloonContent": 514, "type": "школа"}, "options": {"fillColor": "#000000" }},
    {"id": 515, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-62.242152674589306, -27.0433056820184], "radius": 16363.297821953893}, "coordBBox": [-64.05898699071258, -31.72419425100088], "properties": {"hintContent": 515, "balloonContent": 515, "type": "аптека"}, "options": {"fillColor": "#ffffff" }},
    {"id": 516, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [-51.14838108071126, 155.39137856685556],
        [-55.499706387054175, 156.92627054988407]
    ]}, "coordBBox": [-55.1612449681852, 155.35483117215335], "properties": {"hintContent": 516, "balloonContent": 516, "type": "кафе"}, "options": {"fillColor": "#0000ff" }},
    {"id": 517, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [45.88567302795127, 77.15016575530171],
            [39.35727737727575, 73.74023333657533],
            [44.76159205776639, 75.12441524886526],
            [47.904658103361726, 75.76831422164105],
            [41.88943112269044, 75.98913355497643]
        ]
    ]}, "coordBBox": [43.236005682265386, 72.27336263749748], "properties": {"hintContent": 517, "balloonContent": 517, "type": "аптека"}, "options": {"fillColor": "#00ff00" }},
    {"id": 518, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-31.31547880358994, -136.73282692674547],
        [-30.038395145675167, -135.23709596833214],
        [-30.761035601608455, -133.8477242179215]
    ]}, "coordBBox": [-34.65507599408738, -134.67892612796277], "properties": {"hintContent": 518, "balloonContent": 518, "type": "аптека"}, "options": {"fillColor": "#ff0000" }},
    {"id": 519, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [19.43245869479142, -107.23038628697395], "radius": 676177.8555810452}, "coordBBox": [20.150873571401462, -106.03267283644527], "properties": {"hintContent": 519, "balloonContent": 519, "type": "кафе"}, "options": {"fillColor": "#0000ff" }},
    {"id": 520, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [-60.68203442846425, -95.57563296053559],
        [-60.05651009036228, -93.07701748679392]
    ]}, "coordBBox": [-64.70542399794795, -93.39621942490339], "properties": {"hintContent": 520, "balloonContent": 520, "type": "школа"}, "options": {"fillColor": "#0000ff" }},
    {"id": 521, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [-79.26157333888113, 88.16006194683723],
            [-86.23232589568943, 96.43720949534327],
            [-78.96669192356057, 95.34373663365841],
            [-84.17708640103228, 96.06637212913483],
            [-83.84733919403516, 89.80326717020944]
        ]
    ]}, "coordBBox": [-81.64598641567864, 92.34679513145238], "properties": {"hintContent": 521, "balloonContent": 521, "type": "банк"}, "options": {"fillColor": "#000000" }},
    {"id": 522, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-67.32874994981103, 81.50862054433674],
        [-68.85106623754837, 82.71421213983558],
        [-71.30640841438435, 77.9261573322583]
    ]}, "coordBBox": [-67.81455018324777, 80.7454842980951], "properties": {"hintContent": 522, "balloonContent": 522, "type": "аптека"}, "options": {"fillColor": "#00ff00" }},
    {"id": 523, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-21.994493232341483, -133.18326045991853], "radius": 979905.1110167056}, "coordBBox": [-17.370057428488508, -132.68053142353892], "properties": {"hintContent": 523, "balloonContent": 523, "type": "аптека"}, "options": {"fillColor": "#0000ff" }},
    {"id": 524, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [23.26770662679337, -129.57082092529163],
        [22.992716428125277, -132.67866387031972]
    ]}, "coordBBox": [25.922738284571096, -130.017902366817], "properties": {"hintContent": 524, "balloonContent": 524, "type": "кафе"}, "options": {"fillColor": "#0000ff" }},
    {"id": 525, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [17.16174243018031, 130.85273500881158],
            [12.622812669724226, 125.51431709900498],
            [12.753351551946253, 127.8041176777333],
            [16.434159730561078, 124.02878290740773],
            [15.818437311099842, 129.04164750361815]
        ]
    ]}, "coordBBox": [17.343780017690733, 125.89581121690571], "properties": {"hintContent": 525, "balloonContent": 525, "type": "кафе"}, "options": {"fillColor": "#ffffff" }},
    {"id": 526, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [13.262157042045146, 22.290389473782852],
        [13.341517829103395, 19.865028586937115],
        [14.84012086642906, 20.150925419293344]
    ]}, "coordBBox": [13.766504802042618, 23.521032901480794], "properties": {"hintContent": 526, "balloonContent": 526, "type": "школа"}, "options": {"fillColor": "#ff0000" }},
    {"id": 527, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-59.376223797444254, -154.25107416231185], "radius": 246431.12206831574}, "coordBBox": [-62.73844430572353, -153.8982844306156], "properties": {"hintContent": 527, "balloonContent": 527, "type": "школа"}, "options": {"fillColor": "#ffffff" }},
    {"id": 528, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [20.72809083503671, 167.13181123835966],
        [19.547966042300686, 171.03875875473022]
    ]}, "coordBBox": [18.7670586805325, 171.24986154958606], "properties": {"hintContent": 528, "balloonContent": 528, "type": "кафе"}, "options": {"fillColor": "#ffffff" }},
    {"id": 529, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [77.10994354682043, 126.95321121020243],
            [74.408754108008, 129.69288119114935],
            [77.23851770511828, 130.81791857257485],
            [74.93320773122832, 122.26543991244398],
            [71.96686437935568, 122.90139961754903]
        ]
    ]}, "coordBBox": [76.88921625376679, 126.76588771864772], "properties": {"hintContent": 529, "balloonContent": 529, "type": "кафе"}, "options": {"fillColor": "#ff0000" }},
    {"id": 530, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [56.7106903961394, -56.56345448922366],
        [58.312870929948986, -54.47888253023848],
        [57.04843669082038, -48.726016343571246]
    ]}, "coordBBox": [54.99542420147918, -52.10026055574417], "properties": {"hintContent": 530, "balloonContent": 530, "type": "кафе"}, "options": {"fillColor": "#000000" }},
    {"id": 531, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [58.5162921785377, -162.90598258958198], "radius": 197223.2887055725}, "coordBBox": [62.34603433520533, -166.2975954450667], "properties": {"hintContent": 531, "balloonContent": 531, "type": "школа"}, "options": {"fillColor": "#ff0000" }},
    {"id": 532, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [36.78713067434728, -123.23024668963626],
        [29.76040776236914, -127.26305629708804]
    ]}, "coordBBox": [32.31423828168772, -126.86730690300465], "properties": {"hintContent": 532, "balloonContent": 532, "type": "банк"}, "options": {"fillColor": "#0000ff" }},
    {"id": 533, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [25.1937682274729, -9.690797764342278],
            [26.573680610163137, -5.732201787177473],
            [19.026733464561403, -7.973015255993232],
            [27.227945800404996, -4.253298495896161],
            [19.80139878578484, -9.031084096059203]
        ]
    ]}, "coordBBox": [22.243030382087454, -4.996364805847406], "properties": {"hintContent": 533, "balloonContent": 533, "type": "школа"}, "options": {"fillColor": "#ffffff" }},
    {"id": 534, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [32.48451515566558, -120.64124158467166],
        [25.25706330081448, -120.07320116390474],
        [33.37017425452359, -120.18779593752697]
    ]}, "coordBBox": [30.080428931396455, -118.68474688846618], "properties": {"hintContent": 534, "balloonContent": 534, "type": "банк"}, "options": {"fillColor": "#ffffff" }},
    {"id": 535, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [4.018334798747674, -173.11530766775832], "radius": 235865.2635011822}, "coordBBox": [4.714865139685571, -177.2533592954278], "properties": {"hintContent": 535, "balloonContent": 535, "type": "банк"}, "options": {"fillColor": "#0000ff" }},
    {"id": 536, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [-54.6730007964652, -160.60127147240564],
        [-61.89225446083583, -166.9652761996258]
    ]}, "coordBBox": [-56.99666817905381, -162.19121671747416], "properties": {"hintContent": 536, "balloonContent": 536, "type": "аптека"}, "options": {"fillColor": "#000000" }},
    {"id": 537, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [-6.788502929266542, -104.43296606419608],
            [-5.028810555813834, -97.76629400555976],
            [-5.502158822491765, -100.52507031476125],
            [-2.8903470770455897, -102.34200736624189],
            [-1.4007915672846138, -106.44531237659976]
        ]
    ]}, "coordBBox": [-3.249040397349745, -102.58760732598603], "properties": {"hintContent": 537, "balloonContent": 537, "type": "аптека"}, "options": {"fillColor": "#ff0000" }},
    {"id": 538, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-40.07955893059261, -36.56202862155624],
        [-45.10504137724638, -38.346083698561415],
        [-47.54311407916248, -35.57289624004625]
    ]}, "coordBBox": [-44.03481382061727, -38.58432191424072], "properties": {"hintContent": 538, "balloonContent": 538, "type": "аптека"}, "options": {"fillColor": "#00ff00" }},
    {"id": 539, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-38.24506091885269, -158.2825997157488], "radius": 281713.0954936147}, "coordBBox": [-34.33486050693318, -154.39995189663023], "properties": {"hintContent": 539, "balloonContent": 539, "type": "школа"}, "options": {"fillColor": "#00ff00" }},
    {"id": 540, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [72.0554747863207, -9.274784808512777],
        [68.666911739856, -12.274069566046819]
    ]}, "coordBBox": [71.95521295419894, -8.722513960674405], "properties": {"hintContent": 540, "balloonContent": 540, "type": "школа"}, "options": {"fillColor": "#00ff00" }},
    {"id": 541, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [61.7830548749771, 119.86016947077587],
            [62.48480631504208, 117.17198187019676],
            [62.33738258713856, 117.78076101792976],
            [65.19291316973977, 122.24070176947862],
            [65.10230780462734, 117.41068578325212]
        ]
    ]}, "coordBBox": [64.46541581419297, 121.93541393615305], "properties": {"hintContent": 541, "balloonContent": 541, "type": "банк"}, "options": {"fillColor": "#00ff00" }},
    {"id": 542, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [73.26710052904673, -35.696417280705646],
        [75.7078247377649, -35.23457028903067],
        [79.55990039859898, -35.705479303142056]
    ]}, "coordBBox": [75.66193509148434, -32.323598731309175], "properties": {"hintContent": 542, "balloonContent": 542, "type": "школа"}, "options": {"fillColor": "#ffffff" }},
    {"id": 543, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-74.6802986273542, 94.8448315972928], "radius": 167877.4517495185}, "coordBBox": [-72.83169319969602, 90.52317211870104], "properties": {"hintContent": 543, "balloonContent": 543, "type": "школа"}, "options": {"fillColor": "#ff0000" }},
    {"id": 544, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [69.01700807269663, -152.99212809302844],
        [66.56877069384791, -153.49157316144556]
    ]}, "coordBBox": [69.23949210788123, -153.28865826129913], "properties": {"hintContent": 544, "balloonContent": 544, "type": "банк"}, "options": {"fillColor": "#ffffff" }},
    {"id": 545, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [72.39814542233944, -119.21147341374308],
            [75.4704038798809, -117.85976043553092],
            [79.70827044919133, -118.17773063783534],
            [75.49745927681215, -125.30251338845119],
            [75.75553517439403, -120.92359776725061]
        ]
    ]}, "coordBBox": [76.10086048371159, -120.36777680274099], "properties": {"hintContent": 545, "balloonContent": 545, "type": "аптека"}, "options": {"fillColor": "#0000ff" }},
    {"id": 546, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [67.53963205846958, -95.2999452513177],
        [68.06861320277676, -96.8081094219815],
        [71.02806779090315, -96.02936417213641]
    ]}, "coordBBox": [68.48891854169779, -99.101103823632], "properties": {"hintContent": 546, "balloonContent": 546, "type": "аптека"}, "options": {"fillColor": "#ffffff" }},
    {"id": 547, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [7.7430440043099225, -81.99172296677716], "radius": 731866.5680941194}, "coordBBox": [10.76695618685335, -83.60530575737357], "properties": {"hintContent": 547, "balloonContent": 547, "type": "школа"}, "options": {"fillColor": "#ffffff" }},
    {"id": 548, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [30.47145890071988, -84.50646889745258],
        [32.9592655855231, -88.70553301996551]
    ]}, "coordBBox": [30.15080392593518, -84.74744626320899], "properties": {"hintContent": 548, "balloonContent": 548, "type": "кафе"}, "options": {"fillColor": "#ffffff" }},
    {"id": 549, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [10.539059559814632, -174.0867260529194],
            [8.08639366994612, -180.77910215128213],
            [2.712863718625158, -172.03418977675028],
            [9.665007566800341, -177.78339789481834],
            [3.433702504262328, -175.38316698744893]
        ]
    ]}, "coordBBox": [7.162453294731677, -176.50167491286993], "properties": {"hintContent": 549, "balloonContent": 549, "type": "аптека"}, "options": {"fillColor": "#00ff00" }},
    {"id": 550, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-42.86167526151985, 55.40713679394685],
        [-44.62142739677802, 63.70542164542712],
        [-43.618806230369955, 62.43679271894507]
    ]}, "coordBBox": [-43.512903671944514, 59.71263993065804], "properties": {"hintContent": 550, "balloonContent": 550, "type": "аптека"}, "options": {"fillColor": "#000000" }},
    {"id": 551, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-40.72982763056643, 133.62690233625472], "radius": 860355.2302811295}, "coordBBox": [-36.69813156244345, 129.82266529463232], "properties": {"hintContent": 551, "balloonContent": 551, "type": "аптека"}, "options": {"fillColor": "#000000" }},
    {"id": 552, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [-69.54214217956178, -105.13565982109867],
        [-69.4236976839602, -102.78467867872678]
    ]}, "coordBBox": [-68.07870510150678, -101.2949052080512], "properties": {"hintContent": 552, "balloonContent": 552, "type": "аптека"}, "options": {"fillColor": "#0000ff" }},
    {"id": 553, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [33.21958753163926, 96.74775114748627],
            [29.13840271299705, 102.20242297276855],
            [31.038507458288223, 98.98463066667318],
            [26.527050494914874, 98.39105908293277],
            [28.023059375118464, 100.57460169540718]
        ]
    ]}, "coordBBox": [29.266532337060198, 98.9928545197472], "properties": {"hintContent": 553, "balloonContent": 553, "type": "кафе"}, "options": {"fillColor": "#0000ff" }},
    {"id": 554, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [60.17766770441085, -35.285598343471065],
        [59.662799953948706, -30.33076753723435],
        [65.19274436635897, -32.17335217399523]
    ]}, "coordBBox": [60.80959507264197, -34.45668957196176], "properties": {"hintContent": 554, "balloonContent": 554, "type": "аптека"}, "options": {"fillColor": "#ffffff" }},
    {"id": 555, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [5.579663461539894, -169.39981946954504], "radius": 684067.6730498672}, "coordBBox": [1.366394943324849, -168.2402156246826], "properties": {"hintContent": 555, "balloonContent": 555, "type": "555"}, "options": {"fillColor": "#000000" }},
    {"id": 556, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [32.231816618004814, 16.674054272007197],
        [27.674255467718467, 16.914824675768614]
    ]}, "coordBBox": [31.69614987098612, 12.290590270422399], "properties": {"hintContent": 556, "balloonContent": 556, "type": "кафе"}, "options": {"fillColor": "#000000" }},
    {"id": 557, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [31.59004841116257, 103.26492033316754],
            [28.11224594246596, 105.47225365648046],
            [30.573315493529662, 101.63243697723374],
            [31.152342401910573, 105.32532977871597],
            [27.36065265024081, 101.84032914112322]
        ]
    ]}, "coordBBox": [29.40610383520834, 105.55931948125362], "properties": {"hintContent": 557, "balloonContent": 557, "type": "аптека"}, "options": {"fillColor": "#000000" }},
    {"id": 558, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-28.047635193215683, 144.59831923246384],
        [-30.433423578506336, 145.71198975318111],
        [-26.990463676629588, 144.66436662245542]
    ]}, "coordBBox": [-29.325888734310865, 145.74516153428704], "properties": {"hintContent": 558, "balloonContent": 558, "type": "банк"}, "options": {"fillColor": "#000000" }},
    {"id": 559, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [62.456831495510414, -8.059582033893093], "radius": 306887.4238524586}, "coordBBox": [60.84335808176547, -3.3010986959561706], "properties": {"hintContent": 559, "balloonContent": 559, "type": "банк"}, "options": {"fillColor": "#000000" }},
    {"id": 560, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [44.18468199321069, -132.54744944977574],
        [45.05700484733097, -131.02374712005258]
    ]}, "coordBBox": [43.3756236883346, -129.92338542826474], "properties": {"hintContent": 560, "balloonContent": 560, "type": "школа"}, "options": {"fillColor": "#000000" }},
    {"id": 561, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [16.841433267109096, -15.792620469583198],
            [20.333455953514203, -19.78449290851131],
            [13.403582157334313, -13.547434732317924],
            [21.067572525935248, -15.230547257233411],
            [21.422414870467037, -15.808726225513965]
        ]
    ]}, "coordBBox": [16.58238259376958, -15.309569989331067], "properties": {"hintContent": 561, "balloonContent": 561, "type": "кафе"}, "options": {"fillColor": "#ff0000" }},
    {"id": 562, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [26.747059150366113, -14.385953593300655],
        [17.711258701747283, -17.016170454444364],
        [23.885702535044402, -13.86585733271204]
    ]}, "coordBBox": [21.879244671436027, -13.793280143290758], "properties": {"hintContent": 562, "balloonContent": 562, "type": "кафе"}, "options": {"fillColor": "#0000ff" }},
    {"id": 563, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [51.33973926305771, -16.300462293438613], "radius": 622264.7153772414}, "coordBBox": [52.245258955517784, -13.509936737827957], "properties": {"hintContent": 563, "balloonContent": 563, "type": "кафе"}, "options": {"fillColor": "#00ff00" }},
    {"id": 564, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [69.98689963365905, -74.14763250155374],
        [66.88969584996812, -71.63011872442439]
    ]}, "coordBBox": [66.16926930379122, -72.75104688014835], "properties": {"hintContent": 564, "balloonContent": 564, "type": "школа"}, "options": {"fillColor": "#ff0000" }},
    {"id": 565, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [35.94852273352444, -110.42306039831601],
            [33.83934665238485, -115.62588628847152],
            [34.53819771762937, -115.49433081061579],
            [38.5134438239038, -113.28605282586068],
            [40.76078600483015, -109.61917707696557]
        ]
    ]}, "coordBBox": [37.6776164828334, -110.70297414902598], "properties": {"hintContent": 565, "balloonContent": 565, "type": "школа"}, "options": {"fillColor": "#00ff00" }},
    {"id": 566, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [0.6622990674804896, -135.23238739813678],
        [3.4885815693996847, -143.48668697173707],
        [0.1370319223497063, -137.97900617588311]
    ]}, "coordBBox": [1.9935818074736744, -139.96966136153787], "properties": {"hintContent": 566, "balloonContent": 566, "type": "банк"}, "options": {"fillColor": "#000000" }},
    {"id": 567, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-20.190734625793993, -87.48658001539297], "radius": 45243.0488076061}, "coordBBox": [-23.30072804936208, -88.11747903004289], "properties": {"hintContent": 567, "balloonContent": 567, "type": "банк"}, "options": {"fillColor": "#ffffff" }},
    {"id": 568, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [-3.266426146728918, 147.7568298811093],
        [1.423572787316516, 145.95367756905034]
    ]}, "coordBBox": [1.5809853211976588, 143.992088874802], "properties": {"hintContent": 568, "balloonContent": 568, "type": "банк"}, "options": {"fillColor": "#000000" }},
    {"id": 569, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [43.01446956815198, 180.80186589621007],
            [42.29688734863885, 179.59843015414663],
            [49.5515244291164, 183.2771800586488],
            [48.75281017506495, 175.13835726655088],
            [41.25198461697437, 175.602117277449]
        ]
    ]}, "coordBBox": [46.03652163059451, 179.60320342797786], "properties": {"hintContent": 569, "balloonContent": 569, "type": "банк"}, "options": {"fillColor": "#ff0000" }},
    {"id": 570, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-66.85410661622882, -149.4609865930397],
        [-70.98809736431576, -151.62758999271318],
        [-68.13306054333225, -142.09764867438935]
    ]}, "coordBBox": [-68.83706191671081, -146.62965977098793], "properties": {"hintContent": 570, "balloonContent": 570, "type": "кафе"}, "options": {"fillColor": "#0000ff" }},
    {"id": 571, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-67.20181399141438, -68.09344682027586], "radius": 490701.40602998435}, "coordBBox": [-68.64597387146205, -72.01200781390071], "properties": {"hintContent": 571, "balloonContent": 571, "type": "кафе"}, "options": {"fillColor": "#ffffff" }},
    {"id": 572, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [-64.86917094676755, 68.3683924516663],
        [-61.32823085645214, 65.99280698108487]
    ]}, "coordBBox": [-64.00332971476018, 70.0474702147767], "properties": {"hintContent": 572, "balloonContent": 572, "type": "кафе"}, "options": {"fillColor": "#00ff00" }},
    {"id": 573, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [-11.269393514376134, 56.566760311834514],
            [-8.169398022582754, 53.36869200109504],
            [-11.254797923611477, 60.047614582581446],
            [-10.49855284858495, 55.416744579561055],
            [-10.201949964975938, 60.77032905071974]
        ]
    ]}, "coordBBox": [-11.486811528448015, 57.151067038066685], "properties": {"hintContent": 573, "balloonContent": 573, "type": "школа"}, "options": {"fillColor": "#000000" }},
    {"id": 574, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [41.298636324936524, -80.28204043162987],
        [43.48179116845131, -77.57096857763827],
        [40.280435546301305, -73.56969605782069]
    ]}, "coordBBox": [44.87772417371161, -75.90366950724274], "properties": {"hintContent": 574, "balloonContent": 574, "type": "школа"}, "options": {"fillColor": "#ffffff" }},
    {"id": 575, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-36.53139792266302, -145.09856190299615], "radius": 558793.5994844884}, "coordBBox": [-38.12576952157542, -143.37952677626163], "properties": {"hintContent": 575, "balloonContent": 575, "type": "банк"}, "options": {"fillColor": "#ffffff" }},
    {"id": 576, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [-46.76868542213924, -156.76920571248047],
        [-48.294669011374936, -160.04391900612973]
    ]}, "coordBBox": [-43.80492540425621, -160.0049901008606], "properties": {"hintContent": 576, "balloonContent": 576, "type": "школа"}, "options": {"fillColor": "#0000ff" }},
    {"id": 577, "type": "Feature", "geometry": {"type": "Polygon", "coordinates": [
        [
            [5.626820792676881, 27.791636082110927],
            [11.914615494897589, 30.454353378154337],
            [9.786439179442823, 27.481718690833077],
            [13.455754088936374, 30.785936394240707],
            [6.884810073534027, 28.677076066378504]
        ]
    ]}, "coordBBox": [8.487032689154148, 27.97590107191354], "properties": {"hintContent": 577, "balloonContent": 577, "type": "кафе"}, "options": {"fillColor": "#ffffff" }},
    {"id": 578, "type": "Feature", "geometry": {"type": "LineString", "coordinates": [
        [-72.6371570629999, -79.50235419673845],
        [-79.59887700155377, -84.3039024353493],
        [-73.62441384233534, -84.99800703604706]
    ]}, "coordBBox": [-76.81211854913272, -82.65054915100336], "properties": {"hintContent": 578, "balloonContent": 578, "type": "аптека"}, "options": {"fillColor": "#000000" }},
    {"id": 579, "type": "Feature", "geometry": {"type": "Circle", "coordinates": [-66.82778702815995, -170.5746680253651], "radius": 246872.03601934016}, "coordBBox": [-69.33998497901484, -170.6746548693627], "properties": {"hintContent": 579, "balloonContent": 579, "type": "аптека"}, "options": {"fillColor": "#00ff00" }},
    {"id": 580, "type": "Feature", "geometry": {"type": "Rectangle", "coordinates": [
        [3.0735350877512246, 62.011034468887374],
        [6.84710301226005, 60.36351085873321]
    ]}, "coordBBox": [6.709810935426503, 58.815919579938054], "properties": {"hintContent": 580, "balloonContent": 580, "type": "школа"}, "options": {"fillColor": "#ffffff" }},
    {
        "type": "Feature",
        "id": 581,
        "geometry": {
            "type": "Point",
            "coordinates": [55.763338, 37.565466]
        },
        "coordBBox": [55.763338, 37.565466],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "Очень длиннный, но невероятно интересный текс."
        },
        "options": {
            "preset": "islands#blueSportCircleIcon"
        }
    },
    {
        "type": "Feature",
        "id": 582,
        "geometry": {
            "type": "Point",
            "coordinates": [55.763338, 37.565466]
        },
        "coordBBox": [55.773338, 37.565466],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "Очень длиннный, но невероятно интересный текс."
        },
        "options": {
            "preset": "islands#blueSportIcon"
        }
    },
    {
        "type": "Feature",
        "id": 583,
        "geometry": {
            "type": "Point",
            "coordinates": [55.744522, 37.616378]
        },
        "coordBBox": [55.744522, 37.616378],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "22"
        },
        "options": {
            "preset": "islands#blueIcon"
        }
    },
    {
        "type": "Feature",
        "id": 584,
        "geometry": {
            "type": "Point",
            "coordinates": [55.780898, 37.642889]
        },
        "coordBBox": [55.780898, 37.642889],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "Очень длиннный, но невероятно интересный текс."
        },
        "options": {
            "preset": "islands#blueStretchyIcon"
        }
    },
    {
        "type": "Feature",
        "id": 585,
        "geometry": {
            "type": "Point",
            "coordinates": [55.793559, 37.435983]
        },
        "coordBBox": [55.793559, 37.435983],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "22"
        },
        "options": {
            "preset": "islands#blueDotIcon"
        }
    },
    {
        "type": "Feature",
        "id": 586,
        "geometry": {
            "type": "Point",
            "coordinates": [55.800584, 37.675638]
        },
        "coordBBox": [55.800584, 37.675638],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "22"
        },
        "options": {
            "preset": "islands#blueCircleIcon"
        }
    },
    {
        "type": "Feature",
        "id": 587,
        "geometry": {
            "type": "Point",
            "coordinates": [55.716733, 37.589988]
        },
        "coordBBox": [55.716733, 37.589988],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "Очень длиннный, но невероятно интересный текс."
        },
        "options": {
            "preset": "islands#blueCircleDotIcon"
        }
    },
    {
        "type": "Feature",
        "id": 588,
        "geometry": {
            "type": "Point",
            "coordinates": [55.775724, 37.56084]
        },
        "coordBBox": [55.775724, 37.56084],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconCaption": "Очень длиннный, но невероятно интересный текс."
        },
        "options": {
            "preset": "islands#blueCircleDotIconWithCaption"
        }
    },
    {
        "type": "Feature",
        "id": 589,
        "geometry": {
            "type": "Point",
            "coordinates": [55.822144, 37.433781]
        },
        "coordBBox": [55.822144, 37.433781],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconCaption": "Очень длиннный, но невероятно интересный текс."
        },
        "options": {
            "preset": "islands#blueDotIconWithCaption"
        }
    },
    {
        "type": "Feature",
        "id": 590,
        "geometry": {
            "type": "Point",
            "coordinates": [55.87417, 37.669838]
        },
        "coordBBox": [55.87417, 37.669838],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "22"
        }
    },
    {
        "type": "Feature",
        "id": 591,
        "geometry": {
            "type": "Point",
            "coordinates": [55.71677, 37.482338]
        },
        "coordBBox": [55.71677, 37.482338],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "22"
        },
        "options": {
            "preset": "islands#glyphIcon",
            "iconGlyph": "tower",
            "iconGlyphColor": "blue",
            "iconColor": "blue"
        }
    },
    {
        "type": "Feature",
        "id": 592,
        "geometry": {
            "type": "Point",
            "coordinates": [55.78085, 37.75021]
        },
        "coordBBox": [55.78085, 37.75021],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "22"
        },
        "options": {
            "preset": "islands#glyphCircleIcon",
            "iconGlyph": "tower",
            "iconGlyphColor": "blue",
            "iconColor": "blue"

        }},
    {
        "type": "Feature",
        "id": 593,
        "geometry": {
            "type": "Point",
            "coordinates": [55.810906, 37.654142]
        },
        "coordBBox": [55.810906, 37.654142],
        "properties": {
            "balloonContent": "Содержимое балуна",
            "clusterCaption": "Еще одна метка",
            "hintContent": "Текст подсказки",
            "iconContent": "22"
        },
        "options": {
            "preset": "islands#blueGlyphIcon",
            "iconGlyph": "tower",
            "iconGlyphColor": "blue"
        }
    }
];

var clusterId = 10001;

var app = express();
app.get('/', function (req, res) {

    var requestBbox = req.query.bbox;
    var arrayBbox = requestBbox.split(',');
    var bbox = [];
    var filteredFeatures = [];
    var sortedFeatures;
    var clusteredFeatures = [];
    var clusterPresets = ["islands#yellowClusterIcons"];
    var tempFeatures = [];
    var types = ['школа', 'кафе', 'аптека', 'банк'];
    for (var i = 0; i < arrayBbox.length; i++) {
        bbox[i] = parseFloat(arrayBbox[i])
    }

    for (i = 0; i < features.length; i++) {
        if (!features[i].coordBBox && features[i].geometry.coordinates[0] >= bbox[0] &&
            features[i].geometry.coordinates[0] <= bbox[2] &&
            features[i].geometry.coordinates[1] >= bbox[1] &&
            features[i].geometry.coordinates[1] <= bbox[3]) {
            filteredFeatures.push(features[i])
        }
    }
    if (filteredFeatures.length >= 15) {
        var clusterCoords = [(bbox[0] + bbox[2]) / 2, (bbox[1] + bbox[3]) / 2];
        clusteredFeatures.push({
            type: 'Cluster',
            id: clusterId,
            geometry: {
                type: 'Point',
                coordinates: clusterCoords
            },
            features: filteredFeatures,
            bbox: [
                [bbox[0], bbox[1]],
                [bbox[2], bbox[3]]
            ],
            number: filteredFeatures.length,
            properties: {type: types[clusterId%types.length]},
            options: {preset: clusterPresets[clusterId%clusterPresets.length]}
        });
        clusterId++;

    }
    else {
        for (i = 0; i < filteredFeatures.length; i++) {
            clusteredFeatures.push(filteredFeatures[i])
        }
    }
    for (i = 0; i < features.length; i++) {
        if (features[i].coordBBox &&
            features[i].coordBBox[0] >= bbox[0] &&
            features[i].coordBBox[0] <= bbox[2] &&
            features[i].coordBBox[1] >= bbox[1] &&
            features[i].coordBBox[1] <= bbox[3]) {
            clusteredFeatures.push(features[i])
        }
    }
    var objects = {
        type: 'FeatureCollection',
        features: clusteredFeatures
    };
    res.jsonp(objects);
});
app.listen(8010, function () {});
