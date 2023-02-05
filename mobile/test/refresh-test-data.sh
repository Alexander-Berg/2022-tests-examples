#!/bin/zsh

scriptDir="${0:A:h}"
arcadiaRoot=$(arc rev-parse --show-toplevel)
mapsPath=${arcadiaRoot}/mobile/geo/maps/maps
resources=${scriptDir}/resources
ktFile=${resources}/ru/yandex/yandexmaps/multiplatform/analytics/GeneratedAppAnalytics.kt

cd "${mapsPath}/tools" && ./gradlew :analytics-generator:run -q --args="\
 --inputFile=${resources}/testmetrics.yaml\
 --markdownOutputFile=${resources}/testmetrics.md\
 --kotlinOutputFile=${ktFile}\
 --kotlinTrackerPackage=ru.yandex.yandexmaps.multiplatform.analytics.tracker\
 --swiftOutputFile=${resources}/GenaMetricsEventTracker+Generated.swift.expected\
 --cppOutputFile=${resources}/cpp/gena_report.cpp\
 --cppRootNamespace=yandex::maps::navikit::report::gena\
 --cppIncludeDir=${resources}/cpp/headers\
 kotlin swift cpp"

rm ${resources}/cpp/headers/yandex/maps/navikit/report/gena/reportable.h
mv ${ktFile} ${ktFile}.expected
