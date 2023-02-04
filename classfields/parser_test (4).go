package parser_test

import (
	"testing"

	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/cmd/backend/parser"
	"github.com/stretchr/testify/require"
)

func TestParseAlphabet(t *testing.T) {
	testCases := []string{
		``,
		// variable value: dots and numeric
		`dc=10`,
		`dc=10.2.4`,
		`dc=abcde`,
		`dc=abcde3`,
		`dc=abe3gff`,
		`dc=3gff`,
		// quotes
		`dc="10"`,
		`dc=" 10"`,
		`dc="10"`,
		`dc="11\"0"`,
		`dc='10'`,
		`dc='11\'0'`,
		`dc="1\t2"`,
		//escaping
		`dc=5\ 5`,
		`dc=5\=5`,
		`dc=\"5`,
		`dc=\'5`,
		`dc=5\"5\"`,
		`dc=55\"`,
		`dc="\\"`,
		`rest.m\.=5`,
		// variable field
		`rest.A=1`,
		`rest.3=1`,
		`rest.@version=1`,
		`rest.a-b=1`,
		`rest.a_b=1`,
		`rest.*^%$#@?/|\:;~=1`,
		`rest.b=1`,
		`rest.b3=1`,
		`rest.b3.3a=1`,
		`rest.3b.a3=1`,
		// negative numbers with comparing
		`dc>=-5.5`,
		`dc>=-5`,
		// operator variable
		`dc>=5`,
		`dc<=5`,
		`dc!=5`,
		// special symbol with operators
		`rest.@@@=5`,
		`rest.@@@>5`,
		`rest.@@@!=5`,
		`rest.@@@>=5`,
		`rest.@@@<5`,
		`rest.@@@=@@@`,
		// multiple expressions
		`dc=1 dc=2`,
		`dc=* dc!=*`,
		// real world example
		`rest.req.url=/ping rest.response.statusCode=200`,
	}

	queryParser := parser.NewQueryParser()

	for _, testCase := range testCases {
		_, err := queryParser.Parse(testCase)
		require.NoError(t, err, testCase)
	}
}

func TestFailed(t *testing.T) {
	testCases := []string{
		`rest..t=5`,
		`.rest.t=5`,
		`rest.=5`,
		`rest.t.=5`,
		`dc=5=5`,
		`dc=5=`,
		`dc==`,
		`dc="5`,
		`dc='5`,
		`dc=5"5"`,
		`dc=5\"5"`,
		`dc=55"`,
		`dc="\\\"`,
	}
	queryParser := parser.NewQueryParser()

	for _, testCase := range testCases {
		_, err := queryParser.Parse(testCase)
		require.Error(t, err, testCase)
	}
}

func TestLayer(t *testing.T) {
	queryParser := parser.NewQueryParser()

	valid := []string{"test", "prod"}
	for _, testCase := range valid {
		_, err := queryParser.Parse("layer=" + testCase)
		require.NoError(t, err, testCase)
	}

	invalid := []string{"abc", "' prod'", "test_env"}
	for _, testCase := range invalid {
		_, err := queryParser.Parse("layer=" + testCase)
		require.Error(t, err, testCase)
	}
}

func TestLevel(t *testing.T) {
	queryParser := parser.NewQueryParser()

	valid := []string{"fatal", "error", "warn", "info", "debug", "trace"}
	for _, testCase := range valid {
		_, err := queryParser.Parse("level=" + testCase)
		require.NoError(t, err, testCase)
	}

	invalid := []string{"warning", "' fatal'", "debu"}
	for _, testCase := range invalid {
		_, err := queryParser.Parse("level=" + testCase)
		require.Error(t, err, testCase)
	}
}

func TestCanary(t *testing.T) {
	queryParser := parser.NewQueryParser()

	valid := []string{"1", "0", "t", "f", "true", "false"}
	for _, testCase := range valid {
		_, err := queryParser.Parse("canary=" + testCase)
		require.NoError(t, err, testCase)
	}

	invalid := []string{"tRue", "' 0'", "[", "' 1'", "'1 '", "'0 '"}
	for _, testCase := range invalid {
		_, err := queryParser.Parse("canary=" + testCase)
		require.Error(t, err, testCase)
	}
}

func TestSimple(t *testing.T) {
	testCases := []struct {
		query  string
		result *core.Parenthesis
	}{
		{"dc=a", &core.Parenthesis{
			Conditions: []*core.Condition{
				{Field: "dc", Value: "a"},
			},
		}},
		{"dc=a dc=v", &core.Parenthesis{
			Conditions: []*core.Condition{
				{Field: "dc", Value: "a"},
				{Field: "dc", Value: "v"},
			},
		}},
	}
	testQueryParse(t, testCases)
}

func TestParenthesis(t *testing.T) {
	testCases := []struct {
		query  string
		result *core.Parenthesis
	}{
		{"(dc=a)", &core.Parenthesis{
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
					},
				},
			},
		}},
		{"(dc=a) dc=v", &core.Parenthesis{
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
					},
				},
			},
			Conditions: []*core.Condition{
				{Field: "dc", Value: "v"},
			},
		}},
		{"dc=a (dc=v)", &core.Parenthesis{
			Conditions: []*core.Condition{
				{Field: "dc", Value: "a"},
			},
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "v"},
					},
				},
			},
		}},
		{"(dc=a dc=v)", &core.Parenthesis{
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
						{Field: "dc", Value: "v"},
					},
				},
			},
		}},
		{"(dc=a) (dc=v)", &core.Parenthesis{
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
					},
				},
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "v"},
					},
				},
			},
		}},
	}
	testQueryParse(t, testCases)
}

func TestOr(t *testing.T) {
	testCases := []struct {
		query  string
		result *core.Parenthesis
	}{
		{"dc=a or dc=v", &core.Parenthesis{
			Operator: core.Parenthesis_OR,
			Conditions: []*core.Condition{
				{Field: "dc", Value: "a"},
				{Field: "dc", Value: "v"},
			},
		}},
		{"dc=a or (dc=v)", &core.Parenthesis{
			Operator: core.Parenthesis_OR,
			Conditions: []*core.Condition{
				{Field: "dc", Value: "a"},
			},
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "v"},
					},
				},
			},
		}},
		{"(dc=a) or dc=v", &core.Parenthesis{
			Operator: core.Parenthesis_OR,
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
					},
				},
			},
			Conditions: []*core.Condition{
				{Field: "dc", Value: "v"},
			},
		}},
		{"(dc=a) or (dc=v)", &core.Parenthesis{
			Operator: core.Parenthesis_OR,
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
					},
				},
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "v"},
					},
				},
			},
		}},
		{"(dc=a or dc=v)", &core.Parenthesis{
			Parenthesis: []*core.Parenthesis{
				{
					Operator: core.Parenthesis_OR,
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
						{Field: "dc", Value: "v"},
					},
				},
			},
		}},
	}
	testQueryParse(t, testCases)
}

func TestParenthesisAndConditionAtSameLevel(t *testing.T) {
	testCases := []struct {
		query  string
		result *core.Parenthesis
	}{
		{"dc=a (dc=v or dc=i)", &core.Parenthesis{
			Conditions: []*core.Condition{
				{Field: "dc", Value: "a"},
			},
			Parenthesis: []*core.Parenthesis{
				{
					Operator: core.Parenthesis_OR,
					Conditions: []*core.Condition{
						{Field: "dc", Value: "v"},
						{Field: "dc", Value: "i"},
					},
				},
			},
		}},
		{"dc=a (dc=v dc=i)", &core.Parenthesis{
			Conditions: []*core.Condition{
				{Field: "dc", Value: "a"},
			},
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "v"},
						{Field: "dc", Value: "i"},
					},
				},
			},
		}},
		{"(dc=a or dc=v) dc=i", &core.Parenthesis{
			Parenthesis: []*core.Parenthesis{
				{
					Operator: core.Parenthesis_OR,
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
						{Field: "dc", Value: "v"},
					},
				},
			},
			Conditions: []*core.Condition{
				{Field: "dc", Value: "i"},
			},
		}},
		{"(dc=a dc=v) dc=i", &core.Parenthesis{
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
						{Field: "dc", Value: "v"},
					},
				},
			},
			Conditions: []*core.Condition{
				{Field: "dc", Value: "i"},
			},
		}},
	}
	testQueryParse(t, testCases)
}

func TestDifferentOperatorsAtSameLevel(t *testing.T) {
	testCases := []struct {
		query  string
		result *core.Parenthesis
	}{
		{"dc=a or dc=v dc=i", &core.Parenthesis{
			Operator: core.Parenthesis_OR,
			Conditions: []*core.Condition{
				{Field: "dc", Value: "a"},
			},
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "v"},
						{Field: "dc", Value: "i"},
					},
				},
			},
		}},
		{"dc=a dc=v or dc=i", &core.Parenthesis{
			Operator: core.Parenthesis_OR,
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
						{Field: "dc", Value: "v"},
					},
				},
			},
			Conditions: []*core.Condition{
				{Field: "dc", Value: "i"},
			},
		}},
		{"dc=a or dc=v (dc=i)", &core.Parenthesis{
			Operator: core.Parenthesis_OR,
			Conditions: []*core.Condition{
				{Field: "dc", Value: "a"},
			},
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "v"},
					},
					Parenthesis: []*core.Parenthesis{
						{
							Conditions: []*core.Condition{
								{Field: "dc", Value: "i"},
							},
						},
					},
				},
			},
		}},
		{"dc=a or (dc=v) (dc=i)", &core.Parenthesis{
			Operator: core.Parenthesis_OR,
			Conditions: []*core.Condition{
				{Field: "dc", Value: "a"},
			},
			Parenthesis: []*core.Parenthesis{
				{
					Parenthesis: []*core.Parenthesis{
						{
							Conditions: []*core.Condition{
								{Field: "dc", Value: "v"},
							},
						},
						{
							Conditions: []*core.Condition{
								{Field: "dc", Value: "i"},
							},
						},
					},
				},
			},
		}},
		{"(dc=a) or (dc=v) (dc=i)", &core.Parenthesis{
			Operator: core.Parenthesis_OR,
			Parenthesis: []*core.Parenthesis{
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "a"},
					},
				},
				{
					Parenthesis: []*core.Parenthesis{
						{
							Conditions: []*core.Condition{
								{Field: "dc", Value: "v"},
							},
						},
						{
							Conditions: []*core.Condition{
								{Field: "dc", Value: "i"},
							},
						},
					},
				},
			},
		}},
	}
	testQueryParse(t, testCases)
}

func TestDoubleParenthesis(t *testing.T) {
	testCases := []struct {
		query  string
		result *core.Parenthesis
	}{
		{"((dc=a) or (dc=v)) (dc=i)", &core.Parenthesis{
			Parenthesis: []*core.Parenthesis{
				{
					Operator: core.Parenthesis_OR,
					Parenthesis: []*core.Parenthesis{
						{
							Conditions: []*core.Condition{
								{Field: "dc", Value: "a"},
							},
						},
						{
							Conditions: []*core.Condition{
								{Field: "dc", Value: "v"},
							},
						},
					},
				},
				{
					Conditions: []*core.Condition{
						{Field: "dc", Value: "i"},
					},
				},
			},
		}},
		{"((dc=a))", &core.Parenthesis{
			Parenthesis: []*core.Parenthesis{
				{
					Parenthesis: []*core.Parenthesis{
						{
							Conditions: []*core.Condition{
								{Field: "dc", Value: "a"},
							},
						},
					},
				},
			},
		}},
	}
	testQueryParse(t, testCases)
}

func testQueryParse(t *testing.T, testCases []struct {
	query  string
	result *core.Parenthesis
}) {
	queryParser := parser.NewQueryParser()

	for _, testCase := range testCases {
		result, err := queryParser.Parse(testCase.query)
		require.NoError(t, err)

		require.Equalf(t, testCase.result, result, testCase.query)
	}
}

func TestGlob(t *testing.T) {
	testCases := []struct {
		query  string
		result *core.Parenthesis
	}{
		{`rest.m=a*b`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `a*b`, Operator: core.Condition_GLOB},
		}}},
		{`rest.m=a\*b`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `a\*b`},
		}}},
		{`rest.m=*b`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `*b`, Operator: core.Condition_GLOB},
		}}},
		{`rest.m=\*b`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `\*b`},
		}}},
		{`rest.m=b*`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `b*`, Operator: core.Condition_GLOB},
		}}},
		{`rest.m=b\*`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `b\*`},
		}}},
		{`rest.m=*b*`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `*b*`, Operator: core.Condition_GLOB},
		}}},
		{`rest.m=\*b\*`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `\*b\*`},
		}}},
		{`rest.m=*`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `*`, Operator: core.Condition_GLOB},
		}}},
		{`rest.m=\*`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `\*`},
		}}},
		{`rest.m=*b\*`, &core.Parenthesis{Conditions: []*core.Condition{
			{Field: "rest.m", Value: `*b\*`, Operator: core.Condition_GLOB},
		}}},
	}
	testQueryParse(t, testCases)
}

func TestDotEscape(t *testing.T) {
	testCases := []struct {
		query  string
		result *core.Parenthesis
	}{
		{`rest.m\.a=5`, &core.Parenthesis{
			Conditions: []*core.Condition{
				{Field: `rest.m\.a`, Value: "5"},
			},
		}},
	}
	testQueryParse(t, testCases)
}
