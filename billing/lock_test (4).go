// nolinter: lll
package handlers

import (
	"context"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/mock/actionsmock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	coreerrors "a.yandex-team.ru/billing/hot/accounts/pkg/core/errors"
)

// Получение активной блокировки
func TestGetActiveLock(t *testing.T) {
	checkLock(t, 10, true)
}

// Получение протухшей блокировки
func TestGetObsoleteLock(t *testing.T) {
	checkLock(t, -10, false)
}

func checkLock(t *testing.T, timeout int64, locked bool) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "Tardis"
	attr2 := "Gallifrey"
	uid := "9cb110d7-9fc8-4a9d-b3fd-f30e73120a80"

	mockActions.EXPECT().GetLock(
		gomock.Eq(ctx),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "n",
			Type:      "t",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		})).Return(
		&entities.Lock{
			UID: uid,
			Dt:  time.Now().UTC().Add(time.Duration(timeout) * time.Second),
		},
		nil)

	req := httptest.NewRequest("GET", fmt.Sprintf("http://localhost:9000?namespace=n&type=t&a1=%s&a2=%s", attr1, attr2), strings.NewReader(""))
	resp := httptest.NewRecorder()
	err := GetLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusOK, resp.Code)
	assert.Equal(t, fmt.Sprintf(`{"status":"ok","data":{"UID":"%s","Locked":%v}}`, uid, locked), resp.Body.String())
}

// Проверка некорректного запроса
func TestGetLockFail(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "Tardis"
	attr2 := "Gallifrey"

	req := httptest.NewRequest("GET",
		fmt.Sprintf("http://localhost:9000?namespace=abc&a1=%s&a2=%s", attr1, attr2), strings.NewReader(""))
	resp := httptest.NewRecorder()
	err := GetLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusBadRequest, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"no required location attribute argument with key 'type'"}}`, resp.Body.String())
}

// Получение блокировки - ситуация, когда блокировка вообще не найдена
func TestGetAbsentLock(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "Tardis"
	attr2 := "Gallifrey"

	mockActions.EXPECT().GetLock(
		gomock.Eq(ctx),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "n",
			Type:      "t",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		})).Return(nil, coreerrors.LockNotFound("lock not found"))

	req := httptest.NewRequest("GET", fmt.Sprintf("http://localhost:9000?namespace=n&type=t&a1=%s&a2=%s", attr1, attr2), strings.NewReader(""))
	resp := httptest.NewRecorder()
	err := GetLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusNotFound, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"NOT_FOUND_LOCK","description":"lock not found"}}`, resp.Body.String())

}

// Выставление активной блокировки
func TestInitLock(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "Tardis"
	attr2 := "Gallifrey"
	uid := "a7432b5f-62b0-48b9-8159-e3e9899a001c"

	mockActions.EXPECT().InitLock(
		gomock.Eq(ctx),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "n",
			Type:      "t",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		}),
		gomock.Eq(int64(10))).Return(uid, nil)

	body := fmt.Sprintf(`{"timeout": 10, "loc": {"a1": "%s", "a2": "%s", "type": "t", "namespace": "n"}}`, attr1, attr2)
	req := httptest.NewRequest("POST", "http://localhost:9000", strings.NewReader(body))
	resp := httptest.NewRecorder()
	err := InitLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusOK, resp.Code)
	assert.Equal(t, fmt.Sprintf(`{"status":"ok","data":{"UID":"%s"}}`, uid), resp.Body.String())
}

// Проверка негативных кейсов получения блокировки - пустой таймаут, негативный таймаут, отсутствие типа
func TestInitLockFail(t *testing.T) {
	attr1 := "Tardis"
	attr2 := "Gallifrey"

	response := getNegativeMessage("timeout should be positive")
	body := fmt.Sprintf(`{"loc": {"namespace": "ns", "a1": "%s", "a2": "%s", "type": "t"}}`, attr1, attr2)
	testInitLockFail(t, body, response)

	body = fmt.Sprintf(`{"timeout":-10,"loc":{"namespace":"ns","a1":"%s","a2":"%s","type":"t"}}`, attr1, attr2)
	testInitLockFail(t, body, response)

	body = fmt.Sprintf(`{"timeout": 10, "loc": {"namespace": "ns", "a1": "%s", "a2": "%s"}}`, attr1, attr2)
	testInitLockFail(t, body, getNegativeMessage("no required 'type' attribute"))

	testInitLockFail(t, "NOT A JSON", getNegativeMessage("invalid character 'N' looking for beginning of value"))
}

// Проверка того, что дубликат уже есть при взятии блокировки
func TestInitLockAlreadyActive(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "Tardis"
	attr2 := "Gallifrey"

	mockActions.EXPECT().InitLock(
		gomock.Eq(ctx),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "n",
			Type:      "t",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		}),
		gomock.Eq(int64(10))).Return("", coreerrors.NewCodedError(409, "ACTIVE_LOCKS", "oopsies"))

	body := fmt.Sprintf(`{"timeout": 10, "loc": {"a1": "%s", "a2": "%s", "type": "t", "namespace": "n"}}`, attr1, attr2)
	req := httptest.NewRequest("POST", "http://localhost:9000", strings.NewReader(body))
	resp := httptest.NewRecorder()
	err := InitLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusConflict, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"ACTIVE_LOCKS","description":"oopsies"}}`, resp.Body.String())
}

func testInitLockFail(t *testing.T, body string, response string) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	req := httptest.NewRequest("POST", "http://localhost:9000", strings.NewReader(body))
	resp := httptest.NewRecorder()
	err := InitLock(ctx, resp, req, mockActions)

	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusBadRequest, resp.Code)
	assert.Equal(t, response, resp.Body.String())
}

// Проверка пинга активной блокировки
func TestPingLock(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "Tardis"
	attr2 := "Gallifrey"
	uid := "42d81886-43b1-11eb-b378-0242ac130002"

	mockActions.EXPECT().PingLock(
		gomock.Eq(ctx),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "n",
			Type:      "t",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		}),
		gomock.Eq(int64(10)),
		gomock.Eq(uid)).Return(nil)

	body := fmt.Sprintf(`{"timeout": 10, "loc": {"a1": "%s", "a2": "%s", "type": "t", "namespace": "n"}, "uid": "%s"}`, attr1, attr2, uid)
	req := httptest.NewRequest("POST", "http://localhost:9000", strings.NewReader(body))
	resp := httptest.NewRecorder()
	err := PingLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusOK, resp.Code)
}

// Проверка негативных кейсов пинга - пустой таймаут, негативный таймаут, пустой uid, отсутствие типа в адресации, некорректный json
func TestPingLockFail(t *testing.T) {
	attr1 := "Tardis"
	attr2 := "Gallifrey"
	uid := "42d81886-43b1-11eb-b378-0242ac130002"

	response := getNegativeMessage("timeout should be positive")
	body := fmt.Sprintf(
		`{"loc": {"namespace": "ns", "a1": "%s", "a2": "%s", "type": "t"}, "uid": "%s"}`, attr1, attr2, uid)
	testPingLockFail(t, body, response)

	body = fmt.Sprintf(
		`{"timeout": -10, "loc": {"namespace": "ns", "a1": "%s", "a2": "%s", "type": "t"}, "uid": "%s"}`,
		attr1, attr2, uid,
	)
	testPingLockFail(t, body, response)

	body = fmt.Sprintf(
		`{"timeout": 10, "loc": {"namespace": "ns", "a1": "%s", "a2": "%s", "type": "t"}}`, attr1, attr2)
	testPingLockFail(t, body, getNegativeMessage("uid should not be empty"))

	body = fmt.Sprintf(
		`{"timeout":10,"loc":{"namespace":"ns","a1":"%s","a2":"%s"},"uid":"%s"}`, attr1, attr2, uid)
	testInitLockFail(t, body, getNegativeMessage("no required 'type' attribute"))

	testPingLockFail(t, "NOT A JSON", getNegativeMessage("invalid character 'N' looking for beginning of value"))
}

func testPingLockFail(t *testing.T, body string, response string) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	req := httptest.NewRequest("POST", "http://localhost:9000", strings.NewReader(body))
	resp := httptest.NewRecorder()
	err := PingLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusBadRequest, resp.Code)
	assert.Equal(t, response, resp.Body.String())
}

// Проверка сбоя пинга для устаревшей блокировки
func TestPingObsoleteLock(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "Tardis"
	attr2 := "Gallifrey"
	uid := "42d81886-43b1-11eb-b378-0242ac130002"

	mockActions.EXPECT().PingLock(
		gomock.Eq(ctx),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "m",
			Type:      "t",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		}),
		gomock.Eq(int64(10)),
		gomock.Eq(uid)).Return(coreerrors.LockUpdateError("0 rows were updated"))

	body := fmt.Sprintf(`{"timeout": 10, "loc": {"a1": "%s", "a2": "%s", "type": "t", "namespace": "m"}, "uid": "%s"}`, attr1, attr2, uid)
	req := httptest.NewRequest("POST", "http://localhost:9000", strings.NewReader(body))
	resp := httptest.NewRecorder()
	err := PingLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusConflict, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"LOCK_UPDATE_FAIL","description":"0 rows were updated"}}`, resp.Body.String())
}

// Удаление блокировки
func TestRemoveLock(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "Tardis"
	attr2 := "Gallifrey"
	uid := "0746947e-43b5-11eb-b378-0242ac130002"

	mockActions.EXPECT().RemoveLock(
		gomock.Eq(ctx),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "l",
			Type:      "t",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		}),
		gomock.Eq(uid)).Return(nil)

	body := fmt.Sprintf(`{"loc": {"a1": "%s", "a2": "%s", "type": "t", "namespace": "l"}, "uid": "%s"}`, attr1, attr2, uid)
	req := httptest.NewRequest("POST", "http://localhost:9000", strings.NewReader(body))
	resp := httptest.NewRecorder()
	err := RemoveLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusOK, resp.Code)
}

// Проверка негативных кейсов на снятие блокировки - отсутствие uid, отсутствие адресации, неверный json
func TestRemoveLockFail(t *testing.T) {
	attr1 := "Tardis"
	attr2 := "Gallifrey"
	uid := "0746947e-43b5-11eb-b378-0242ac130002"

	body := fmt.Sprintf(`{"loc": {"namespace": "ns", "a1": "%s", "a2": "%s", "type": "t"}}`, attr1, attr2)
	testRemoveLockFail(t, body, getNegativeMessage("uid should not be empty"))

	body = fmt.Sprintf(`{"loc": {"namespace": "ns", "a1": "%s", "a2": "%s"}, "uid": "%s"}`, attr1, attr2, uid)
	testRemoveLockFail(t, body, getNegativeMessage("no required 'type' attribute"))

	testRemoveLockFail(t, "NOT A JSON", getNegativeMessage("invalid character 'N' looking for beginning of value"))
}

func testRemoveLockFail(t *testing.T, body string, response string) {
	t.Helper()

	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	req := httptest.NewRequest("POST", "http://localhost:9000", strings.NewReader(body))
	resp := httptest.NewRecorder()
	err := RemoveLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusBadRequest, resp.Code)
	assert.Equal(t, response, resp.Body.String())
}

// Удаление устаревшей блокировки
func TestRemoveObsoleteLock(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "Tardis"
	attr2 := "Gallifrey"
	uid := "0746947e-43b5-11eb-b378-0242ac130002"

	mockActions.EXPECT().RemoveLock(
		gomock.Eq(ctx),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "whatdidyouexpecthere",
			Type:      "t",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		}),
		gomock.Eq(uid)).Return(coreerrors.LockUpdateError("0 rows were updated"))

	body := fmt.Sprintf(`{"loc": {"a1": "%s", "a2": "%s", "type": "t", "namespace": "whatdidyouexpecthere"}, "uid": "%s"}`, attr1, attr2, uid)
	req := httptest.NewRequest("POST", "http://localhost:9000", strings.NewReader(body))
	resp := httptest.NewRecorder()
	err := RemoveLock(ctx, resp, req, mockActions)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, http.StatusConflict, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"LOCK_UPDATE_FAIL","description":"0 rows were updated"}}`, resp.Body.String())
}

func getNegativeMessage(msg string) string {
	return fmt.Sprintf(`{"status":"error","data":{"code":"INVALID_REQUEST","description":"failed to parse request: %s"}}`, msg)
}
