#include <yandex/maps/wiki/tasks/task_info.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps {
namespace wiki {
namespace tasks {
namespace tests {

Y_UNIT_TEST_SUITE(task_info) {

Y_UNIT_TEST(test_task_info_pending)
{
    const std::string xml = R"(<?xml version='1.0' encoding='utf-8'?>
<tasks xmlns="http://maps.yandex.ru/mapspro/tasks/1.x">
  <task status="pending" uid="224829124" created="2016-03-24T17:19:44.797228+03:00" resumable="false" revocable="false" progress="0.0" type="release" id="9">
    <context>
      <release-context>
        <branch>0</branch>
      </release-context>
    </context>
  </task>
  <internal>
    <token>1458829184:11</token>
  </internal>
</tasks>)";
    auto taskInfo = TaskInfo::fromXml(xml);
    UNIT_ASSERT_VALUES_EQUAL(taskInfo.id(), 9);
    UNIT_ASSERT(taskInfo.status() == TaskStatus::InProgress);
    UNIT_ASSERT_STRINGS_EQUAL(taskInfo.type(), "release");
    UNIT_ASSERT_STRINGS_EQUAL(taskInfo.createdAt(), "2016-03-24T17:19:44.797228+03:00");
    UNIT_ASSERT_STRINGS_EQUAL(taskInfo.token(), "1458829184:11");
}

Y_UNIT_TEST(test_task_info_success)
{
    const std::string xml = R"(<?xml version="1.0" encoding="utf-8"?>
<tasks xmlns="http://maps.yandex.ru/mapspro/tasks/1.x">
  <task status="success" uid="224829124" created="2016-03-24T17:19:44.797228+03:00" resumable="false" revocable="false" progress="0.0" type="release" id="9">
    <context>
      <release-context>
        <branch>0</branch>
      </release-context>
    </context>
  </task>
</tasks>)";
    auto taskInfo = TaskInfo::fromXml(xml);
    UNIT_ASSERT_VALUES_EQUAL(taskInfo.id(), 9);
    UNIT_ASSERT(taskInfo.status() == TaskStatus::Success);
    UNIT_ASSERT_STRINGS_EQUAL(taskInfo.type(), "release");
    UNIT_ASSERT_STRINGS_EQUAL(taskInfo.createdAt(), "2016-03-24T17:19:44.797228+03:00");
    UNIT_ASSERT_STRINGS_EQUAL(taskInfo.token(), "");
}

Y_UNIT_TEST(test_task_info_failed)
{
    const std::string xml = R"(<?xml version="1.0" encoding="utf-8"?>
<tasks xmlns="http://maps.yandex.ru/mapspro/tasks/1.x">
  <task status="failure" uid="224829124" created="2016-03-24T19:21:46.869631+03:00" resumable="false" revocable="false" progress="0.0" type="release" id="12">
    <context>
      <release-context>
        <branch>0</branch>
      </release-context>
    </context>
    <result>
      <error-result status="INTERNAL_ERROR">NodeNotFound /tasks/task
  - /home/dtarakanov/mapscore/libs/common/backtrace.cpp:286: in maps::backtrace() [0x7f0ad423c107]
</error-result>
    </result>
  </task>
</tasks>)";
    auto taskInfo = TaskInfo::fromXml(xml);
    UNIT_ASSERT_VALUES_EQUAL(taskInfo.id(), 12);
    UNIT_ASSERT(taskInfo.status() == TaskStatus::Failed);
    UNIT_ASSERT_STRINGS_EQUAL(taskInfo.type(), "release");
    UNIT_ASSERT_STRINGS_EQUAL(taskInfo.createdAt(), "2016-03-24T19:21:46.869631+03:00");
    UNIT_ASSERT_STRINGS_EQUAL(taskInfo.token(), "");
}

Y_UNIT_TEST(test_task_info_error)
{
    const std::string xml = R"(<?xml version="1.0" encoding="utf-8"?>
<tasks xmlns="http://maps.yandex.ru/mapspro/tasks/1.x">
  <error>Some error</error>
</tasks>)";
    UNIT_CHECK_GENERATED_EXCEPTION(TaskInfo::fromXml(xml), maps::xml3::NodeNotFound);
}

Y_UNIT_TEST(test_task_infos)
{
    const std::string xml = R"(<?xml version="1.0" encoding="utf-8"?>
<tasks xmlns="http://maps.yandex.ru/mapspro/tasks/1.x">
  <task-list total-count="39" page="1" per-page="2">
    <task status="success" uid="224829124" created="2016-03-26T13:09:33.724027+03:00" resumable="false" revocable="false" progress="0.0" type="release" id="79">
      <context>
        <release-context>
          <branch>1</branch>
        </release-context>
      </context>
    </task>
    <task status="failure" uid="224829124" created="2016-03-26T08:42:16.040325+03:00" resumable="false" revocable="false" progress="0.0" type="release" id="38">
      <context>
        <release-context>
          <branch>1</branch>
        </release-context>
      </context>
      <result>
        <error-result status="INTERNAL_ERROR">&lt;?xml version='1.0' encoding='utf-8'?&gt;
&lt;tasks xmlns="http://maps.yandex.ru/mapspro/tasks/1.x"&gt;&lt;error&gt;400: Bad Request&lt;/error&gt;&lt;/tasks&gt;
  - /home/dtarakanov/mapscore/libs/common/backtrace.cpp:286: in maps::backtrace() [0x7fa9d7366107]
</error-result>
      </result>
    </task>
  </task-list>
</tasks>)";
    auto taskInfos = TaskInfo::listFromXml(xml);
    UNIT_ASSERT_VALUES_EQUAL(taskInfos.size(), 2);

    UNIT_ASSERT_VALUES_EQUAL(taskInfos.front().id(), 79);
    UNIT_ASSERT(taskInfos.front().status() == TaskStatus::Success);
    UNIT_ASSERT_STRINGS_EQUAL(taskInfos.front().type(), "release");
    UNIT_ASSERT_STRINGS_EQUAL(taskInfos.front().createdAt(), "2016-03-26T13:09:33.724027+03:00");
    UNIT_ASSERT_STRINGS_EQUAL(taskInfos.front().token(), "");

    UNIT_ASSERT_VALUES_EQUAL(taskInfos.back().id(), 38);
    UNIT_ASSERT(taskInfos.back().status() == TaskStatus::Failed);
    UNIT_ASSERT_STRINGS_EQUAL(taskInfos.back().type(), "release");
    UNIT_ASSERT_STRINGS_EQUAL(taskInfos.back().createdAt(), "2016-03-26T08:42:16.040325+03:00");
    UNIT_ASSERT_STRINGS_EQUAL(taskInfos.back().token(), "");
}

Y_UNIT_TEST(test_task_infos_empty)
{
    const std::string xml = R"(<?xml version="1.0" encoding="utf-8"?>
<tasks xmlns="http://maps.yandex.ru/mapspro/tasks/1.x">
  <task-list>
  </task-list>
</tasks>)";
    auto taskInfos = TaskInfo::listFromXml(xml);
    UNIT_ASSERT(taskInfos.empty());
}

} // Y_UNIT_TEST_SUITE

} // namespace tests
} // namespace tasks
} // namespace wiki
} // namespace maps
