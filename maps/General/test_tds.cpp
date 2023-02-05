#include "common.h"

#include <maps/sprav/callcenter/libs/task/tds.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <sprav/lib/sprav_editor/http_client/http_client.h>
#include <sprav/lib/testing/mock_server/mock_server.h>


namespace maps::sprav::callcenter::task::tests {

class LoadTdsCompanies: public ::testing::Test {
protected:
    static void SetUpTestSuite() {
        spravEditorMock = std::make_unique<NSprav::TMockServerContainer>(
            TFsPath(GetWorkPath()).Child("sprav_editor_mock"),
            GetOutputPath().Child("sprav_editor_mock"),
            "SPRAV_EDITOR_MOCK_PROXY"
        );
        spravEditor = std::make_unique<NSprav::TSpravEditorClient>(
            "localhost", spravEditorMock->GetPort(), 100, "1120000000036585"
        );
    }

    static void TearDownTestSuite() {
        spravEditorMock.reset();
        spravEditor.reset();
    }

protected:
    static std::unique_ptr<NSprav::TMockServerContainer> spravEditorMock;
    static std::unique_ptr<NSprav::TSpravEditorClient> spravEditor;
};

std::unique_ptr<NSprav::TMockServerContainer> LoadTdsCompanies::spravEditorMock = nullptr;
std::unique_ptr<NSprav::TSpravEditorClient> LoadTdsCompanies::spravEditor = nullptr;

TEST_F(LoadTdsCompanies, Single) {
    TaskDataPtr taskData = std::make_shared<TaskData>(createTask(1));
    taskData->setRequests({createRequest(1, 1124715036), createRequest(2, 1210735541), createRequest(3, std::nullopt)});
    taskData->setPermalinkCommits({createTaskPermalinkCommit(1, 1124715036, 1769138700069079515)});

    fillTdsCompanies({taskData}, *spravEditor);

    EXPECT_TRUE(taskData->permalinkCompanies().at(1124715036).tdsCompany().has_export_id());
    EXPECT_TRUE(taskData->permalinkCompanies().at(1210735541).tdsCompany().has_export_id());
}

TEST_F(LoadTdsCompanies, CreationCompaies) {
    TaskDataPtr taskData = std::make_shared<TaskData>(createTask(1));
    taskData->setRequests({createRequest(1, std::nullopt)});
    taskData->setPermalinkCommits({});
    EXPECT_NO_THROW(fillTdsCompanies({taskData}, *spravEditor));
}

TEST_F(LoadTdsCompanies, Multiple) {
    std::vector<TaskDataPtr> taskData;
    taskData.emplace_back(std::make_shared<TaskData>(createTask(1)));
    taskData.emplace_back(std::make_shared<TaskData>(createTask(2)));
    taskData[0]->setRequests({createRequest(1, 1124715036)});
    taskData[0]->setPermalinkCommits({createTaskPermalinkCommit(1, 1124715036, 1769138700069079515)});

    taskData[1]->setRequests({createRequest(2, 1210735541), createRequest(3, std::nullopt)});
    taskData[1]->setPermalinkCommits({});

    fillTdsCompanies(taskData, *spravEditor);

    EXPECT_TRUE(taskData[0]->permalinkCompanies().at(1124715036).tdsCompany().has_export_id());
    EXPECT_TRUE(taskData[1]->permalinkCompanies().at(1210735541).tdsCompany().has_export_id());
}

} // namespace maps::sprav::callcenter::task::tests
