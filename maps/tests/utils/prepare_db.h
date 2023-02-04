#pragma once

#include "indoor_model.h"

#include <maps/indoor/libs/db/include/static_transmitter.h>
#include <maps/indoor/libs/db/include/task.h>
#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/include/types.h>
#include <maps/libs/pgpool/include/pgpool3.h>

#include <maps/libs/geolib/include/polyline.h>
#include <yandex/maps/proto/offline-mrc/indoor.sproto.h>

#include <string>
#include <unordered_map>
#include <vector>

namespace maps::mirc::radiomap_evaluator::tests {

namespace pindoor = yandex::maps::sproto::offline::mrc::indoor;

using TaskPathTrack = std::string;
using TaskPathTrackId = std::string;
using TaskPathTracksById = std::unordered_map<TaskPathTrackId, TaskPathTrack>;

std::vector<geolib3::Polyline2> createTaskPaths(const IndoorModel&, const size_t count);

pindoor::IndoorTrack makeTrack(const IndoorModel&, const db::ugc::TaskPath&);

db::ugc::Task createIndoorTaskWithTaskPaths(const IndoorModel&, pgpool3::Pool&);

db::TId assignTask(db::ugc::Task& task, const db::UserId&, pgpool3::Pool&);

TaskPathTracksById createTracksForAssignment(
    const IndoorModel&,
    const db::TId assignmentId,
    pgpool3::Pool&);

TaskPathTracksById prepareDataBase(IndoorModel, pgpool3::Pool&);
TaskPathTracksById prepareDataBase(const std::vector<IndoorModel>&, pgpool3::Pool&);

db::ugc::StaticTransmitter makeStaticTxAndWriteToStaticTxsTable(
    const Transmitter&,
    pgpool3::Pool&);

} // namespace maps::mirc::radiomap_evaluator::tests
