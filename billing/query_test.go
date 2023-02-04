package query

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	coreerrors "a.yandex-team.ru/billing/hot/accounts/pkg/core/errors"
	"a.yandex-team.ru/library/go/core/xerrors/multierr"
)

func TestParse(t *testing.T) {
	type given struct {
		queryString string
		required    []string
	}

	type expected struct {
		args Args
		err  error
	}

	testcases := []struct {
		name     string
		given    given
		expected expected
	}{
		{
			name: "empty query",
			given: given{
				queryString: "",
			},
			expected: expected{
				err: multierr.Append(
					coreerrors.InvalidRequest("no required location attribute argument with key 'namespace'"),
					coreerrors.InvalidRequest("no required location attribute argument with key 'type'"),
				),
			},
		},
		{
			name: "namespace and type",
			given: given{
				queryString: "namespace=foo&type=cashless",
			},
			expected: expected{
				args: Args{
					Location: entities.LocationAttributes{
						Namespace: "foo",
						Type:      "cashless",
					},
				},
			},
		},
		{
			name: "namespace with empty modifier",
			given: given{
				queryString: "namespace__empty=1&type=t",
			},
			expected: expected{
				err: coreerrors.InvalidRequest(
					"required location attribute argument with key 'namespace' can't be empty",
				),
			},
		},
		{
			name: "type with empty modifier",
			given: given{
				queryString: "namespace=oplata&type__empty",
			},
			expected: expected{
				err: coreerrors.InvalidRequest(
					"required location attribute argument with key 'type' can't be empty",
				),
			},
		},
		{
			name: "namespace array and one type",
			given: given{
				queryString: "namespace=a&namespace=b&type=t",
			},
			expected: expected{
				err: coreerrors.InvalidRequest(
					"location attribute argument with key 'namespace' must be single",
				),
			},
		},
		{
			name: "namespace array and type array",
			given: given{
				queryString: "namespace=n&namespace=m&type=t&type=p",
			},
			expected: expected{
				err: multierr.Append(
					coreerrors.InvalidRequest(
						"location attribute argument with key 'namespace' must be single",
					),
					coreerrors.InvalidRequest(
						"location attribute argument with key 'type' must be single",
					),
				),
			},
		},
		{
			name: "not required empty arg after equal sign",
			given: given{
				queryString: "namespace=bar&type=some&a=",
			},
			expected: expected{
				args: Args{
					Location: entities.LocationAttributes{
						Namespace: "bar",
						Type:      "some",
						Attributes: map[string]*string{
							"a": new(string),
						},
					},
				},
			},
		},
		{
			name: "required empty arg after equal sign",
			given: given{
				queryString: "namespace=uuu&type=payout&a=",
				required:    []string{"a"},
			},
			expected: expected{
				args: Args{
					Required: map[string][]string{
						"a": {""},
					},
					Location: entities.LocationAttributes{
						Namespace: "uuu",
						Type:      "payout",
					},
				},
			},
		},
		{
			name: "not required array arg",
			given: given{
				queryString: "namespace=www&type=ttt&a=1&a=2&b=3&b=4",
			},
			expected: expected{
				err: multierr.Append(
					coreerrors.InvalidRequest("location attribute argument with key 'a' must be single"),
					coreerrors.InvalidRequest("location attribute argument with key 'b' must be single"),
				),
			},
		},
		{
			name: "required array arg",
			given: given{
				queryString: "namespace=n&type=t&a=1&a=2",
				required:    []string{"a"},
			},
			expected: expected{
				args: Args{
					Required: map[string][]string{
						"a": {"1", "2"},
					},
					Location: entities.LocationAttributes{
						Namespace: "n",
						Type:      "t",
					},
				},
			},
		},
		{
			name: "required arg with empty modifier",
			given: given{
				queryString: "namespace=mail_pro&type=payment&a__empty=1",
				required:    []string{"a"},
			},
			expected: expected{
				err: coreerrors.InvalidRequest("required argument with key 'a' can't be empty"),
			},
		},
		{
			name: "not required arg with empty modifier",
			given: given{
				queryString: "namespace=bnpl&type=cashless&client_id__empty=",
			},
			expected: expected{
				args: Args{
					Location: entities.LocationAttributes{
						Namespace: "bnpl",
						Type:      "cashless",
						Attributes: map[string]*string{
							"client_id": nil,
						},
					},
				},
			},
		},
		{
			name: "required arg with unknown modifier",
			given: given{
				queryString: "namespace=a&type=b&client__bar",
				required:    []string{"client"},
			},
			expected: expected{
				err: coreerrors.InvalidRequest("unknown modifier 'bar' for argument with key 'client'"),
			},
		},
		{
			name: "not required arg with unknown modifier",
			given: given{
				queryString: "namespace=nn&&type=tt&client__foo",
			},
			expected: expected{
				err: coreerrors.InvalidRequest("unknown modifier 'foo' for argument with key 'client'"),
			},
		},
		{
			name: "required arg with invalid format",
			given: given{
				queryString: "namespace=p&type=q&somebody__once__told__me=1",
				required:    []string{"somebody"},
			},
			expected: expected{
				err: coreerrors.InvalidRequest("invalid format of argument with raw key 'somebody__once__told__me'"),
			},
		},
		{
			name: "not required arg with invalid format",
			given: given{
				queryString: "namespace=s&type=x&pam__pam__",
			},
			expected: expected{
				err: coreerrors.InvalidRequest("invalid format of argument with raw key 'pam__pam__'"),
			},
		},
		{
			name: "arg with unknown modifier and required arg with empty modifier",
			given: given{
				queryString: "namespace=bnpl&type=cashless&pam__pam=1&notnull__empty",
				required:    []string{"notnull"},
			},
			expected: expected{
				err: multierr.Append(
					coreerrors.InvalidRequest("unknown modifier 'pam' for argument with key 'pam'"),
					coreerrors.InvalidRequest("required argument with key 'notnull' can't be empty"),
				),
			},
		},
		{
			name: "required namespace and type",
			given: given{
				queryString: "namespace=a&type=b&required=c&non_required=",
				required:    []string{"namespace", "type", "required"},
			},
			expected: expected{
				args: Args{
					Required: map[string][]string{
						"namespace": {"a"},
						"type":      {"b"},
						"required":  {"c"},
					},
					Location: entities.LocationAttributes{
						Namespace: "a",
						Type:      "b",
						Attributes: map[string]*string{
							"non_required": new(string),
						},
					},
				},
			},
		},
		{
			name: "some required args are missing",
			given: given{
				queryString: "namespace=k&type=k&a1=k&a2=k",
				required:    []string{"a1", "a2", "a3", "a4"},
			},
			expected: expected{
				err: multierr.Append(
					coreerrors.InvalidRequest("no required argument with key 'a3'"),
					coreerrors.InvalidRequest("no required argument with key 'a4'"),
				),
			},
		},
		{
			name: "required arg in upper case",
			given: given{
				queryString: "namespace=oplata&type=cashless&UPPER=U",
				required:    []string{"upper"},
			},
			expected: expected{
				args: Args{
					Required: map[string][]string{
						"upper": {"U"},
					},
					Location: entities.LocationAttributes{
						Namespace: "oplata",
						Type:      "cashless",
					},
				},
			},
		},
		{
			name: "required arg with modifier in upper case",
			given: given{
				queryString: "namespace=pampam&type=pam&some__EMPTY=",
				required:    []string{"some"},
			},
			expected: expected{
				err: coreerrors.InvalidRequest("unknown modifier 'EMPTY' for argument with key 'some'"),
			},
		},
		{
			name: "same required keys passed to parse empty arg",
			given: given{
				queryString: "namespace=n&type=t&req__empty",
				required:    []string{"req", "req"},
			},
			expected: expected{
				err: coreerrors.InvalidRequest("required argument with key 'req' can't be empty"),
			},
		},
	}

	for _, c := range testcases {
		t.Run(fmt.Sprintf("%s  %s", c.name, c.given.queryString), func(t *testing.T) {
			r := httptest.NewRequest("GET", createURL(c.given.queryString), strings.NewReader(""))

			args, err := Parse(r, c.given.required...)

			if c.expected.err != nil {
				t.Run("bad request error occurred", func(t *testing.T) {
					require.Error(t, err)
					// Из-за использования мапы, проверить собранные ошибки
					// можно только, если не привязываться к порядку.
					require.ElementsMatch(t, multierr.Errors(c.expected.err), multierr.Errors(err))

					var target coreerrors.CodedError

					require.ErrorAs(t, err, &target)
					require.Equal(t, http.StatusBadRequest, target.HTTPCode())
				})

				return
			}

			t.Run("args parsing succeed", func(t *testing.T) {
				require.NoError(t, err)
				require.Equal(t, c.expected.args, args)
			})
		})
	}
}

func createURL(queryString string) string {
	return fmt.Sprintf("https://localhost:8080?%s", queryString)
}
