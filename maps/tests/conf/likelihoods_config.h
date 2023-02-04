#pragma once

#include <maps/analyzer/libs/mapmatching_likelihoods/core/include/config.h>


const maps::analyzer::mapmatching_likelihoods::LikelihoodsConfig likelihoodsConfig{
    .common = {
        .break_ = 1e-6,
    },
    .geometric = {
        .enabled = true,
        .norm1 = 20.0,
        .norm2 = 400.0,
        .mixRatio = 0.8,
    },
    .directional = {
        .enabled = false,
        .k = 0.0,
    },
    .skipped = {
        .enabled = true,
        .layer = 0.75,
    },
    .temporal = {
        .enabled = true,
        .speedLimit = 60.0,
        .maxGraphSpeedExceedance = 40.0,
        .minDrivingTime = 1.0,
        .minSignalInterval = 1.0,
    },
    .speed = {
        .enabled = true,
        .minSignalInterval = 1.0,
        .minVehicleSpeed = 3.0,
        .maxVehicleSpeed = 70.0,
        .norm = 180.0,
        .min = 0.05,
    },
    .curvative = {
        .enabled = true,
        .minSignalInterval = 5.0,
        .minSpeedFactor = 1.0,
        .norm = 1.0,
    },
    .transmission = {
        .enabled = true,
        .minSignalDistance = 1.0,
        .norm = 1.0,
    },
    .jump = {
        .enabled = true,
        .norm = 30.0,
        .min = 0.4,
    },
};
