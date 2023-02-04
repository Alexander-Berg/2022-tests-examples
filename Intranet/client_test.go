package idm

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/intranet/legacy/staff-api/internal/database"
)

func TestURLValidation(t *testing.T) {
	for _, testCase := range []struct {
		name    string
		params  string
		isValid bool
	}{
		{
			name:    "no_args",
			params:  "",
			isValid: false,
		},
		{
			name:    "no_role",
			params:  "subject_type=user&uid=123",
			isValid: false,
		},
		{
			name:    "no_uid",
			params:  "subject_type=user&role={\"group\":\"u\"}",
			isValid: false,
		},
		{
			name:    "no_subject_type",
			params:  "uid=123&role={\"group\":\"u\"}",
			isValid: false,
		},
		{
			name:    "multiple_uids",
			params:  "subject_type=user&uid=123&uid=456",
			isValid: false,
		},
		{
			name:    "multiple_subject_types",
			params:  "subject_type=user&subject_type=tvm_app&uid=123",
			isValid: false,
		},
		{
			name:    "user_type_mismatch",
			params:  "subject_type=user&login=777",
			isValid: false,
		},
		{
			name:    "tvm_type_mismatch",
			params:  "subject_type=tvm_app&uid=123",
			isValid: false,
		},
		{
			name:    "invalid_type",
			params:  "subject_type=wow&uid=123",
			isValid: false,
		},
		{
			name:    "single_role",
			params:  "subject_type=user&uid=123&role={\"group\":\"u\"}",
			isValid: true,
		},
		{
			name:    "multiple_roles",
			params:  "subject_type=user&uid=123&role={\"group\":\"u\"}&role={\"group\":\"su\"}",
			isValid: true,
		},
	} {
		t.Run(testCase.name, func(t *testing.T) {
			b, _ := url.ParseQuery(testCase.params)
			_, _, _, err := parseParams(b)
			isValid := err == nil

			assert.Equal(t, testCase.isValid, isValid)
		})
	}
}

func TestInfoBody(t *testing.T) {
	l, _ := zap.NewDeployLogger(log.DebugLevel)

	for _, testCase := range []struct {
		body Response
	}{
		{Response{
			Code: 0,
			Roles: &Group{
				Slug:   "group",
				Name:   "группа",
				Values: RoleSet,
			},
		}},
	} {
		t.Run("", func(t *testing.T) {
			w := httptest.NewRecorder()
			r := httptest.NewRequest("GET", "/idm/info", nil)
			getInfoHandler(l).ServeHTTP(w, r)

			exp, _ := json.Marshal(testCase.body)
			act := w.Body.Bytes()

			assert.Equal(t, http.StatusOK, w.Result().StatusCode)
			assert.Equal(t, exp, act)
		})
	}
}

func TestInfoStatus(t *testing.T) {
	l, _ := zap.NewDeployLogger(log.DebugLevel)

	for _, testCase := range []struct {
		method     string
		statusCode int
	}{
		{
			method:     "GET",
			statusCode: http.StatusOK,
		},
		{
			method:     "POST",
			statusCode: http.StatusMethodNotAllowed,
		},
	} {
		t.Run(testCase.method, func(t *testing.T) {
			w := httptest.NewRecorder()
			r := httptest.NewRequest(testCase.method, "/idm/info", nil)
			getInfoHandler(l).ServeHTTP(w, r)

			assert.Equal(t, testCase.statusCode, w.Result().StatusCode)
		})
	}
}

func TestAllRolesBody(t *testing.T) {
	l, _ := zap.NewDeployLogger(log.DebugLevel)
	db := database.NewFakeClient()

	for _, testCase := range []struct {
		body Response
	}{
		{Response{
			Code: 0,
			Users: []User{
				{
					Login: "cracker",
					SubjectType:  "user",
					Roles: []Role{
						{"roles": "u"},
					},
				},
				{
					Login: "denis-p",
					SubjectType:  "user",
					Roles: []Role{
						{"roles": "su"},
						{"roles": "a"},
					},
				},
				{
					Login: "777",
					SubjectType:  "tvm_app",
					Roles: []Role{
						{"roles": "u"},
						{"roles": "su"},
					},
				},
			},
		}},
	} {
		t.Run("", func(t *testing.T) {
			w := httptest.NewRecorder()
			r := httptest.NewRequest("GET", "/idm/get-all-roles", nil)

			getAllRolesHandler(l, db).ServeHTTP(w, r)

			exp, _ := json.Marshal(testCase.body)
			act := w.Body.Bytes()

			assert.Equal(t, http.StatusOK, w.Result().StatusCode)
			assert.Equal(t, exp, act)
		})
	}
}

func TestAllRolesStatus(t *testing.T) {
	l, _ := zap.NewDeployLogger(log.DebugLevel)
	db := database.NewFakeClient()

	for _, testCase := range []struct {
		method     string
		statusCode int
	}{
		{
			method:     "GET",
			statusCode: http.StatusOK,
		},
		{
			method:     "POST",
			statusCode: http.StatusMethodNotAllowed,
		},
	} {
		t.Run(testCase.method, func(t *testing.T) {
			w := httptest.NewRecorder()
			r := httptest.NewRequest(testCase.method, "/idm/get-all-roles", nil)

			getAllRolesHandler(l, db).ServeHTTP(w, r)

			assert.Equal(t, testCase.statusCode, w.Result().StatusCode)
		})
	}
}

func TestAddRoleBody(t *testing.T) {
	l, _ := zap.NewDeployLogger(log.DebugLevel)
	db := database.NewFakeClient()

	for _, testCase := range []struct {
		name   string
		params string
		body   Response
	}{
		{
			"new_user_role",
			"subject_type=user&uid=456&role={\"group\":\"a\"}",
			Response{Code: 0},
		},
		{
			"new_tvm_role",
			"subject_type=tvm_app&login=777&role={\"group\":\"a\"}",
			Response{Code: 0},
		},
		{
			"existing_user_role",
			"subject_type=user&uid=456&role={\"group\":\"u\"}",
			Response{Code: 0, Warning: "subject user 456 already has role(s) [u]"},
		},
		{
			"existing_tvm_role",
			"subject_type=tvm_app&login=777&role={\"group\":\"u\"}",
			Response{Code: 0, Warning: "subject tvm_app 777 already has role(s) [u]"},
		},
		{
			"non_existent_user_uid",
			"subject_type=user&uid=147&role={\"group\":\"u\"}",
			Response{Code: 1, Fatal: "subject user 147 not found"},
		},
		{
			"non_existent_tvm_app_id",
			"subject_type=tvm_app&login=852&role={\"group\":\"u\"}",
			Response{Code: 1, Fatal: "subject tvm_app 852 not found"},
		},
		{
			"uid_type_mismatch",
			"subject_type=tvm_app&uid=123&role={\"group\":\"u\"}",
			Response{Code: 1, Fatal: "login: parameter not provided"},
		},
		{
			"login_type_mismatch",
			"subject_type=user&login=777&role={\"group\":\"u\"}",
			Response{Code: 1, Fatal: "uid: parameter not provided"},
		},
		{
			"non_existent_user_role",
			"subject_type=user&uid=123&role={\"group\":\"b\"}",
			Response{Code: 1, Fatal: "role b not found"},
		},
		{
			"non_existent_tvm_role",
			"subject_type=tvm_app&login=777&role={\"group\":\"b\"}",
			Response{Code: 1, Fatal: "role b not found"},
		},
	} {
		t.Run(testCase.name, func(t *testing.T) {
			w := httptest.NewRecorder()
			r := httptest.NewRequest("POST", "/idm/add-role", strings.NewReader(testCase.params))

			getAddRoleHandler(l, db).ServeHTTP(w, r)

			exp, _ := json.Marshal(testCase.body)
			act := w.Body.Bytes()

			assert.Equal(t, http.StatusOK, w.Result().StatusCode)
			assert.Equal(t, exp, act)
		})
	}
}

func TestAddRoleStatus(t *testing.T) {
	l, _ := zap.NewDeployLogger(log.DebugLevel)
	db := database.NewFakeClient()

	for _, testCase := range []struct {
		method     string
		statusCode int
	}{
		{"GET", http.StatusMethodNotAllowed},
		{"POST", http.StatusOK},
	} {
		t.Run(testCase.method, func(t *testing.T) {
			w := httptest.NewRecorder()
			r := httptest.NewRequest(
				testCase.method, "/idm/add-role",
				strings.NewReader("subject_type=user&uid=123&role={\"group\":\"su\"}"),
			)

			getAddRoleHandler(l, db).ServeHTTP(w, r)

			assert.Equal(t, testCase.statusCode, w.Result().StatusCode)
		})
	}
}

func TestRemoveRoleBody(t *testing.T) {
	l, _ := zap.NewDeployLogger(log.DebugLevel)
	db := database.NewFakeClient()

	for _, testCase := range []struct {
		name   string
		params string
		body   Response
	}{
		{
			"missing_user_role",
			"subject_type=user&uid=456&role={\"group\":\"a\"}",
			Response{Code: 0},
		},
		{
			"missing_tvm_role",
			"subject_type=tvm_app&login=777&role={\"group\":\"a\"}",
			Response{Code: 0},
		},
		{
			"existing_user_role",
			"subject_type=user&uid=456&role={\"group\":\"u\"}",
			Response{Code: 0},
		},
		{
			"existing_tvm_role",
			"subject_type=tvm_app&login=777&role={\"group\":\"u\"}",
			Response{Code: 0},
		},
		{
			"non_existent_user_uid",
			"subject_type=user&uid=147&role={\"group\":\"u\"}",
			Response{Code: 1, Fatal: "subject user 147 not found"},
		},
		{
			"non_existent_tvm_app_id",
			"subject_type=tvm_app&login=852&role={\"group\":\"u\"}",
			Response{Code: 1, Fatal: "subject tvm_app 852 not found"},
		},
		{
			"uid_type_mismatch",
			"subject_type=tvm_app&uid=123&role={\"group\":\"u\"}",
			Response{Code: 1, Fatal: "login: parameter not provided"},
		},
		{
			"login_type_mismatch",
			"subject_type=user&login=777&role={\"group\":\"u\"}",
			Response{Code: 1, Fatal: "uid: parameter not provided"},
		},
		{
			"non_existent_user_role",
			"subject_type=user&uid=123&role={\"group\":\"b\"}",
			Response{Code: 1, Fatal: "role b not found"},
		},
		{
			"non_existent_tvm_role",
			"subject_type=tvm_app&login=777&role={\"group\":\"b\"}",
			Response{Code: 1, Fatal: "role b not found"},
		},
	} {
		t.Run(testCase.name, func(t *testing.T) {
			w := httptest.NewRecorder()
			r := httptest.NewRequest("POST", "/idm/remove-role", strings.NewReader(testCase.params))

			getRemoveRoleHandler(l, db).ServeHTTP(w, r)

			assert.Equal(t, http.StatusOK, w.Result().StatusCode)
		})
	}
}

func TestRemoveRoleStatus(t *testing.T) {
	l, _ := zap.NewDeployLogger(log.DebugLevel)
	db := database.NewFakeClient()

	for _, testCase := range []struct {
		method     string
		statusCode int
	}{
		{"GET", http.StatusMethodNotAllowed},
		{"POST", http.StatusOK},
	} {
		t.Run(testCase.method, func(t *testing.T) {
			w := httptest.NewRecorder()
			r := httptest.NewRequest(
				testCase.method, "/idm/remove-role",
				strings.NewReader("subject_type=user&uid=123&role={\"group\":\"su\"}"),
			)

			getRemoveRoleHandler(l, db).ServeHTTP(w, r)

			assert.Equal(t, testCase.statusCode, w.Result().StatusCode)
		})
	}
}
