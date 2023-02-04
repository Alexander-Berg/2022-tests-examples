package grafana

import (
	"bytes"
	"encoding/json"
	"errors"
	"io/ioutil"
	"net/http"
	"strconv"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	board2 "github.com/YandexClassifieds/shiva/pkg/grafana/dashboard/board"
	"github.com/YandexClassifieds/shiva/pkg/grafana/dashboard/panel"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func getMillisecond(nsec int64) int64 {
	millisec := nsec / 1000000

	return millisec
}

type RequestCreate struct {
	Name          string `json:"name"`
	Role          string `json:"role"`
	SecondsToLive int    `json:"secondsToLive"`
}

type ResponseCreate struct {
	Name string `json:"name"`
	Key  string `json:"key"`
}

func grafanaCreateToken(url, username, passwd string) (string, error) {
	urlAuthKey := url + "/api/auth/keys"
	nowStr := strconv.Itoa(int(getMillisecond(time.Now().UnixNano())))

	requestBody := &RequestCreate{
		Name:          nowStr,
		Role:          "Editor",
		SecondsToLive: 86400,
	}

	requestBodyJson, err := json.Marshal(requestBody)
	if err != nil {
		return "", errors.New("marshal request")
	}

	req, err := http.NewRequest("POST", urlAuthKey, bytes.NewBuffer(requestBodyJson))
	if err != nil {
		return "", err
	}
	req.SetBasicAuth(username, passwd)
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		return "", errors.New("response code: " + strconv.Itoa(resp.StatusCode))
	}

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	response := ResponseCreate{}
	err = json.Unmarshal(body, &response)
	if err != nil {
		return "", errors.New("unmarshaled response")
	}

	return response.Key, nil

}

func TestApi(t *testing.T) {
	tags := []string{"tag1", "tag2"}
	text := "test_text"

	api := prepareApi(t)

	id, err := api.Start(text, tags, time.Now())
	require.NoError(t, err)
	err = api.Stop(text, tags, time.Now().Add(30*time.Second), id)
	require.NoError(t, err)
	id, err = api.Point(text, tags, time.Now().Add(-30*time.Second))
	require.NoError(t, err)
}

func TestSaveDashboard(t *testing.T) {
	api := prepareApi(t)

	sCtx := panel.NewServiceContext(&proto.ServiceMap{
		Name: t.Name(),
	}, nil)
	board := board2.MakeSimpleDashboard(sCtx)

	resp, err := api.SaveDashboard(board, 0, true)
	require.NoError(t, err)

	require.NoError(t, api.DeleteDashboard(*resp.UID))

}

func TestGetDashboardByUid(t *testing.T) {
	api := prepareApi(t)

	sCtx := panel.NewServiceContext(&proto.ServiceMap{
		Name: t.Name(),
	}, nil)
	resp, err := api.SaveDashboard(board2.MakeSimpleDashboard(sCtx), 0, true)
	require.NoError(t, err)

	board, err := api.GetDashboardByUid(*resp.UID)
	require.NoError(t, err)
	assert.Equal(t, t.Name(), board.GetTitle())

	require.NoError(t, api.DeleteDashboard(board.GetUID()))

}

func TestGetDashboardByUidNotFound(t *testing.T) {
	api := prepareApi(t)

	board, err := api.GetDashboardByUid("test")
	require.Error(t, err)
	require.Nil(t, board)
}

func TestDashboardSearch(t *testing.T) {
	api := prepareApi(t)

	sCtx := panel.NewServiceContext(&proto.ServiceMap{
		Name: t.Name(),
	}, nil)
	resp, err := api.SaveDashboard(board2.MakeSimpleDashboard(sCtx), 0, true)
	require.NoError(t, err)

	board, err := api.GetDashboardByFolderIdAndTitle(0, t.Name())
	require.NoError(t, err)

	assert.Equal(t, t.Name(), board.GetTitle())
	assert.Equal(t, *resp.UID, board.GetUID())

	require.NoError(t, api.DeleteDashboard(*resp.UID))
}

func TestDashboardSearchNotFound(t *testing.T) {
	api := prepareApi(t)

	sCtx := panel.NewServiceContext(&proto.ServiceMap{
		Name: t.Name() + "test",
	}, nil)
	resp, err := api.SaveDashboard(board2.MakeSimpleDashboard(sCtx), 0, true)
	require.NoError(t, err)

	board, err := api.GetDashboardByFolderIdAndTitle(0, t.Name())
	require.Nil(t, board)
	require.ErrorIs(t, err, common.ErrNotFound)

	require.NoError(t, api.DeleteDashboard(*resp.UID))

}
func TestCreateFolder(t *testing.T) {
	api := prepareApi(t)

	folder, err := api.CreateFolder(&panel.Folder{Title: t.Name()})

	require.NoError(t, err)
	assert.Equal(t, folder.Title, t.Name())

	require.NoError(t, api.DeleteFolder(folder.UID))
}

func TestGetFolderByTitle(t *testing.T) {
	api := prepareApi(t)

	folder, err := api.CreateFolder(&panel.Folder{Title: t.Name()})
	require.NoError(t, err)

	foundFolder, err := api.GetFolderByTitle(t.Name())

	require.NoError(t, err)
	assert.Equal(t, t.Name(), foundFolder.Title)
	assert.Equal(t, folder.UID, foundFolder.UID)
	assert.Equal(t, folder.ID, foundFolder.ID)

	require.NoError(t, api.DeleteFolder(folder.UID))
}

func TestGetFolderByTitleNotFound(t *testing.T) {
	api := prepareApi(t)

	folder, err := api.CreateFolder(&panel.Folder{Title: t.Name() + "test"})
	require.NoError(t, err)

	foundFolder, err := api.GetFolderByTitle(t.Name())

	require.Nil(t, foundFolder)
	require.ErrorIs(t, err, common.ErrNotFound)

	require.NoError(t, api.DeleteFolder(folder.UID))

}

func TestDeleteDashboard(t *testing.T) {
	api := prepareApi(t)

	sCtx := panel.NewServiceContext(&proto.ServiceMap{
		Name: t.Name(),
	}, nil)
	resp, err := api.SaveDashboard(board2.MakeSimpleDashboard(sCtx), 0, true)
	require.NoError(t, err)

	require.NoError(t, api.DeleteDashboard(*resp.UID))

	_, err = api.GetDashboardByUid(*resp.UID)
	require.Error(t, err)

}

func prepareApi(t *testing.T) *Client {
	test.InitTestEnv()

	token, err := grafanaCreateToken(config.Str("GRAFANA_URL"), config.Str("GRAFANA_USERNAME"), config.Str("GRAFANA_PASSWORD"))
	require.NoError(t, err)

	return NewClient(ApiConf{token: token, url: config.Str("GRAFANA_URL")}, test.NewLogger(t))
}
