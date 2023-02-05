#pragma once

#include <maps/wikimap/feedback/api/src/synctool/lib/need_info_email_settings.h>
#include <maps/wikimap/feedback/api/src/synctool/lib/types.h>
#include <util/stream/output.h>

using maps::wiki::feedback::api::sync::PushTypeToTranslations;
using maps::wiki::feedback::api::sync::SubjectTranslations;

template <>
void Out<PushTypeToTranslations>(
    IOutputStream& os,
    const PushTypeToTranslations& pushTypeToTranslations);

template <>
void Out<SubjectTranslations>(
    IOutputStream& os,
    const SubjectTranslations& subjectTranslations);
