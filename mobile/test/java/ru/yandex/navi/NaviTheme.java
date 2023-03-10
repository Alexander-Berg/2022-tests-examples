package ru.yandex.navi;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public enum NaviTheme {
    DAY,
    NIGHT;

    public static NaviTheme oppositeOf(NaviTheme value) {
        return value == DAY ? NIGHT : DAY;
    }

    private static final int[] rise = new int[]{
        // Jan
        359, 359, 358, 358, 358, 357, 356, 356, 355, 354,
        353, 353, 352, 351, 349, 348, 347, 346, 345, 343,
        342, 340, 339, 337, 336, 334, 333, 331, 329, 327, 326,
        // Feb
        324, 322, 320, 318, 316, 314, 312, 310, 308, 306,
        303, 301, 299, 297, 295, 292, 290, 288, 285, 283,
        281, 278, 276, 273, 271, 269, 266, 264,
        // Mar
        261, 259, 256, 254, 251, 249, 246, 243, 241, 238,
        236, 233, 231, 228, 225, 223, 220, 218, 215, 212,
        210, 207, 205, 202, 199, 197, 194, 191, 189, 186, 184,
        // Apr
        181, 178, 176, 173, 171, 168, 165, 163, 160, 158,
        155, 153, 150, 148, 145, 143, 140, 138, 135, 133,
        130, 128, 126, 123, 121, 118, 116, 114, 112, 109,
        // May
        107, 105, 103, 100, 98, 96, 94, 92, 90, 88,
        86, 84, 82, 80, 78, 77, 75, 73, 71, 70,
        68, 67, 65, 64, 62, 61, 59, 58, 57, 56, 55,
        // Jun
        53, 52, 51, 51, 50, 49, 48, 48, 47, 46,
        46, 46, 45, 45, 45, 44, 44, 44, 44, 45,
        45, 45, 45, 46, 46, 46, 47, 48, 48, 49,
        // Jul
        50, 51, 52, 52, 53, 55, 56, 57, 58, 59,
        60, 62, 63, 65, 66, 67, 69, 71, 72, 74,
        75, 77, 79, 80, 82, 84, 86, 87, 89, 91, 93,
        // Aug
        95, 96, 98, 100, 102, 104, 106, 108, 110, 111,
        113, 115, 117, 119, 121, 123, 125, 127, 129, 131,
        133, 135, 137, 139, 140, 142, 144, 146, 148, 150, 152,
        // Sep
        154, 156, 158, 160, 162, 164, 166, 167, 169, 171,
        173, 175, 177, 179, 181, 183, 185, 187, 189, 191,
        192, 194, 196, 198, 200, 202, 204, 206, 208, 210,
        // Oct
        212, 214, 216, 218, 220, 222, 224, 226, 228, 230,
        232, 234, 236, 238, 240, 242, 244, 246, 248, 250,
        252, 254, 256, 258, 260, 262, 265, 267, 269, 271, 273,
        // Nov
        275, 277, 279, 281, 283, 286, 288, 290, 292, 294,
        296, 298, 300, 302, 304, 306, 308, 310, 312, 314,
        316, 318, 320, 322, 324, 326, 327, 329, 331, 333,
        // Dec
        334, 336, 337, 339, 340, 342, 343, 345, 346, 347,
        348, 350, 351, 352, 353, 354, 354, 355, 356, 357,
        357, 358, 358, 358, 359, 359, 359, 359, 359, 359, 359,
    };

    private static final int[] set = new int[] {
        // Jan
        787, 788, 790, 791, 792, 794, 795, 797, 798, 800,
        801, 803, 805, 807, 808, 810, 812, 814, 816, 818,
        820, 822, 824, 826, 828, 830, 832, 834, 836, 838, 841,
        // Feb
        843, 845, 847, 849, 851, 853, 856, 858, 860, 862,
        864, 866, 869, 871, 873, 875, 877, 879, 882, 884,
        886, 888, 890, 892, 894, 897, 899, 901,
        // Mar
        903, 905, 907, 909, 911, 913, 915, 918, 920, 922,
        924, 926, 928, 930, 932, 934, 936, 938, 940, 942,
        944, 946, 948, 950, 952, 954, 956, 958, 960, 962, 964,
        // Apr
        966, 968, 970, 972, 974, 976, 978, 980, 982, 984,
        986, 988, 990, 992, 994, 996, 998, 1000, 1002, 1005,
        1007, 1009, 1011, 1013, 1015, 1017, 1019, 1021, 1023, 1025,
        // May
        1027, 1029, 1031, 1033, 1035, 1036, 1038, 1040, 1042, 1044,
        1046, 1048, 1050, 1052, 1054, 1055, 1057, 1059, 1061, 1063,
        1064, 1066, 1068, 1069, 1071, 1073, 1074, 1076, 1077, 1079, 1080,
        // Jun
        1081, 1083, 1084, 1085, 1086, 1088, 1089, 1090, 1091, 1092,
        1093, 1093, 1094, 1095, 1096, 1096, 1097, 1097, 1097, 1098,
        1098, 1098, 1098, 1098, 1098, 1098, 1098, 1098, 1098, 1098,
        // Jul
        1097, 1097, 1096, 1096, 1095, 1094, 1093, 1093, 1092, 1091,
        1090, 1089, 1088, 1086, 1085, 1084, 1083, 1081, 1080, 1078,
        1077, 1075, 1074, 1072, 1070, 1069, 1067, 1065, 1063, 1061, 1059,
        // Aug
        1057, 1056, 1054, 1051, 1049, 1047, 1045, 1043, 1041, 1039,
        1036, 1034, 1032, 1030, 1027, 1025, 1023, 1020, 1018, 1015,
        1013, 1011, 1008, 1006, 1003, 1001, 998, 996, 993, 991, 988,
        // Sep
        986, 983, 980, 978, 975, 973, 970, 967, 965, 962,
        960, 957, 954, 952, 949, 946, 944, 941, 939, 936,
        933, 931, 928, 925, 923, 920, 917, 915, 912, 910,
        // Oct
        907, 904, 902, 899, 897, 894, 891, 889, 886, 884,
        881, 879, 876, 874, 871, 869, 866, 864, 861, 859,
        857, 854, 852, 849, 847, 845, 843, 840, 838, 836, 834,
        // Nov
        831, 829, 827, 825, 823, 821, 819, 817, 815, 813,
        811, 809, 808, 806, 804, 802, 801, 799, 798, 796,
        795, 793, 792, 791, 789, 788, 787, 786, 785, 784,
        // Dec
        783, 782, 781, 780, 780, 779, 779, 778, 778, 777,
        777, 777, 777, 777, 777, 777, 777, 777, 777, 778,
        778, 778, 779, 780, 780, 781, 782, 783, 784, 785, 786,
    };

    public static NaviTheme forNow() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        final int dayOfYear = (now.getDayOfYear() - 1) % 365;
        int riseTime = rise[dayOfYear];
        int setTime = set[dayOfYear];
        int nowMinutes = now.getHour() * 60 + now.getMinute();
        if (nowMinutes < riseTime - 5 || nowMinutes > setTime + 5)
            return NaviTheme.NIGHT;
        if (nowMinutes > riseTime + 5 && nowMinutes < setTime - 5)
            return NaviTheme.DAY;
        return null;
    }
}
