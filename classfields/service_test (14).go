package idm

import (
	"encoding/json"
	"fmt"
	"net/http"
	"testing"

	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/assert"
)

var (
	idmApiResponseF     = `{"objects":[{"fields_data":null,"system_specific":null,"added":"2021-01-27T18:48:10.112186+00:00","updated":"2021-01-27T18:48:15.332688+00:00","expire_at":null,"granted_at":"2021-01-27T18:48:15.332688+00:00","review_at":"2022-01-22T18:48:15.332688+00:00","id":30748411,"is_active":true,"is_public":null,"state":"%s","ttl_date":null,"ttl_days":null,"review_date":null,"review_days":null,"with_inheritance":true,"with_robots":true,"with_external":true,"without_hold":false,"system":{"id":512,"description":"","endpoint_timeout":60,"endpoint_long_timeout":60,"group_policy":"unaware","is_active":true,"is_broken":false,"name":"h2p","is_sox":true,"slug":"h2p","use_mini_form":false,"has_review":true,"roles_review_days":360,"state":"\u0410\u043a\u0442\u0438\u0432\u043d\u0430"},"group":null,"node":{"id":40473861,"state":"active","slug":"monitoring","data":{"service":"h2p","provides":"monitoring"},"is_public":true,"is_auto_updated":false,"is_key":false,"unique_id":"service\/h2p\/monitoring","slug_path":"\/service\/h2p\/provides\/monitoring\/","value_path":"%s","name":"monitoring","description":"","system":{"id":512,"description":"","endpoint_timeout":60,"endpoint_long_timeout":60,"group_policy":"unaware","is_active":true,"is_broken":false,"name":"h2p","is_sox":true,"slug":"h2p","use_mini_form":false,"has_review":true,"roles_review_days":360,"state":"\u0410\u043a\u0442\u0438\u0432\u043d\u0430"},"human":"\u0421\u0435\u0440\u0432\u0438\u0441: h2p, Provides: monitoring","human_short":"h2p \/ monitoring","set":null},"parent":null,"user":{"username":"test","email":"test@yandex-team.ru","sex":"M","is_active":true,"position":"\u0421\u0438\u0441\u0442\u0435\u043c\u043d\u044b\u0439 \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440","date_joined":"2019-10-16","fired_at":"2019-06-07","type":"user","full_name":"\u0420\u043e\u043c\u0430\u043d \u041e\u043f\u044f\u043a\u0438\u043d"},"human":"\u0421\u0435\u0440\u0432\u0438\u0441: h2p, Provides: monitoring","human_short":"h2p \/ monitoring","human_state":"\u0412\u044b\u0434\u0430\u043d\u0430","data":{"service":"h2p","provides":"monitoring"}}],"meta":{"offset":0,"limit":100,"total_count":1,"previous":null,"next":null}}`
	idmApiResponseEmpty = `{"objects":[],"meta":{"offset":0,"limit":100,"total_count":0,"previous":null,"next":null}}`
)

func TestParseFields(t *testing.T) {
	s := New("", "", logrus.New("info"))

	// valid
	parsed, err := s.ParseFields(`{"ticket": "TICKET-1", "sox": true}`)
	assert.NoError(t, err)
	assert.Equal(t, map[string]interface{}{
		"ticket": "TICKET-1",
		"sox":    true,
	}, parsed)

	// invalid
	_, err = s.ParseFields(`{"ticket"}`)
	assert.Error(t, err)
}

func TestGetSoxTicket(t *testing.T) {
	s := New("", "", logrus.New("info"))

	// valid
	sox, ticket, err := s.GetSoxTicket(`{"ticket": "TICKET-1", "sox": true}`)
	assert.NoError(t, err)
	assert.Equal(t, true, sox)
	assert.Equal(t, "TICKET-1", ticket)

	// valid incomplete
	sox, ticket, err = s.GetSoxTicket(`{"ticket": "TICKET-2"}`)
	assert.NoError(t, err)
	assert.Equal(t, false, sox)
	assert.Equal(t, "TICKET-2", ticket)

	// invalid
	_, _, err = s.GetSoxTicket(`{"ticket"}`)
	assert.Error(t, err)
}

func TestCheckRole(t *testing.T) {
	tests := map[string]struct {
		valuePath            string
		user                 string
		activeIDMResponse    string
		requestedIDMResponse string
		expectedError        error
	}{
		"non-existent": {
			valuePath:            "/non-existent/",
			user:                 "t",
			activeIDMResponse:    idmApiResponseEmpty,
			requestedIDMResponse: idmApiResponseEmpty,
			expectedError:        ErrUserHasNotRole,
		},
		"invalid": {
			valuePath:            "/invalid/",
			user:                 "t",
			activeIDMResponse:    fmt.Sprintf(idmApiResponseF, "granted", "/not-invalid/"),
			requestedIDMResponse: idmApiResponseEmpty,
			expectedError:        ErrUserHasNotRole,
		},
		"granted": {
			valuePath:            "/granted/",
			user:                 "t",
			activeIDMResponse:    fmt.Sprintf(idmApiResponseF, "granted", "/granted/"),
			requestedIDMResponse: idmApiResponseEmpty,
			expectedError:        nil,
		},
		"requested": {
			valuePath:            "/requested/",
			user:                 "t",
			activeIDMResponse:    idmApiResponseEmpty,
			requestedIDMResponse: fmt.Sprintf(idmApiResponseF, "requested", "/requested/"),
			expectedError:        ErrRoleRequested,
		},
		"notJSON": {
			valuePath:         "/test/",
			user:              "t",
			activeIDMResponse: "",
			expectedError:     ErrIDMApi,
		},
	}

	httpmock.Activate()
	defer httpmock.DeactivateAndReset()

	s := New("https://127.0.0.1", "", logrus.New("info"))

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			httpmock.Reset()
			httpmock.RegisterResponder("GET", fmt.Sprintf(getActiveRoleUrl, "https://127.0.0.1", tc.valuePath, fmt.Sprintf("user=%s", tc.user)),
				httpmock.NewStringResponder(200, tc.activeIDMResponse))
			httpmock.RegisterResponder("GET", fmt.Sprintf(getRequestedRoleUrl, "https://127.0.0.1", tc.valuePath, fmt.Sprintf("user=%s", tc.user)),
				httpmock.NewStringResponder(200, tc.requestedIDMResponse))

			err := s.CheckRole(tc.user, tc.valuePath)
			assert.Equal(t, tc.expectedError, err)
		})
	}
}

func TestService_AddNodeRecursive(t *testing.T) {
	httpmock.Activate()
	defer httpmock.DeactivateAndReset()

	var requests []string

	httpmock.RegisterResponder("POST", "https://mock/api/v1/rolenodes/",
		func(req *http.Request) (*http.Response, error) {
			apiNode := APINode{}
			if err := json.NewDecoder(req.Body).Decode(&apiNode); err != nil {
				return httpmock.NewStringResponse(400, ""), nil
			}

			requests = append(requests, apiNode.Parent)

			switch apiNode.Parent {
			case "/service/":
				assert.Equal(t, "test", apiNode.Slug)
				assert.Equal(t, "test", apiNode.Name)
				assert.Equal(t, "", apiNode.UniqueId)
				assert.Nil(t, apiNode.Fields)
				assert.Equal(t, true, apiNode.Visibility)
			case "/service/test/":
				assert.Equal(t, "provides", apiNode.Slug)
				assert.Equal(t, "provides", apiNode.Name)
				assert.Equal(t, "", apiNode.UniqueId)
				assert.Nil(t, apiNode.Fields)
				assert.Equal(t, true, apiNode.Visibility)
			case "/service/test/provides/":
				assert.Equal(t, "test-provides", apiNode.Slug)
				assert.Equal(t, "test-provides", apiNode.Name)
				assert.Equal(t, "uniq", apiNode.UniqueId)
				assert.Equal(t, &[]RoleField{{Type: BooleanField}}, apiNode.Fields)
				assert.Equal(t, false, apiNode.Visibility)
			default:
				t.Fail()
			}
			return httpmock.NewStringResponse(201, ""), nil
		})

	s := New("https://mock", "", logrus.New("info"))
	s.AddNodeRecursive(
		"/service/",
		[]string{"test", "provides", "test-provides"},
		"uniq",
		&[]RoleField{{Type: BooleanField}},
		false,
	)

	assert.EqualValues(t, []string{"/service/", "/service/test/", "/service/test/provides/"}, requests)
}

func TestService_DeleteNode(t *testing.T) {
	httpmock.Activate()
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder(
		"DELETE",
		"https://mock/api/v1/rolenodes/h2p/service/test/provides/test-provides/",
		httpmock.NewStringResponder(204, ""),
	)

	httpmock.RegisterResponder(
		"DELETE",
		`=~^https://mock/api/v1/rolenodes/h2p/.*`,
		func(req *http.Request) (*http.Response, error) {
			t.Fail()
			return httpmock.NewStringResponse(400, ""), nil
		},
	)

	s := New("https://mock", "", logrus.New("info"))
	s.DeleteNode("service/test/provides/test-provides/")
}
