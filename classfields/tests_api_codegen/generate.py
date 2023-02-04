#!/usr/bin/env python3
import sys
import json
from typing import Optional, List


class PathParam(object):
    def __init__(self, json: dict):
        self.json = json


    def generate_class(self) -> str:
        json = self.json

        type = json["type"] if "type" in json else "string"

        if type == "string":
            if "enum" in json:
                return "%sParam" % capitalize_only_first_letter(kebab_and_snake_to_camel_case(self.json["name"]))
            return "String"
        elif type == "integer":
            return "Int"
        elif type == "array":
            list_type = json["items"]["type"]
            if list_type == "string":
                return "[String]"
            elif list_type == "integer":
                return "[Int]"
        else:
            assert False, "Unknown path param type: %s, %s" % (type, json)


    def generate_definition(self, indent: int) -> Optional[str]:
        json = self.json

        type = json["type"] if "type" in json else "string"

        if type == "string":
            if "enum" in json:
                enm = ""
                enm += out("enum %s: CustomStringConvertible {" % self.generate_class(), indent=indent)
                for enum_case in json["enum"]:
                    enm += out('case %s' % enum_case, indent=indent + 1)
                enm += out('case _unknown(String)', indent=indent + 1)

                enm += out("", indent=0)
                enm += out("var description: String {", indent=indent + 1)
                enm += out("switch self {", indent=indent + 2)
                for enum_case in json["enum"]:
                    enm += out('case .%s: return "%s"' % (enum_case, enum_case), indent=indent + 2)
                enm += out('case ._unknown(let value): return value', indent=indent + 2)
                enm += out("}", indent=indent + 2)
                enm += out("}", indent=indent + 1)

                enm += out("}", indent=indent)
                return enm
            return None
        elif type == "integer":
            if "enum" in json:
                enm = ""

                enm += out("enum %s: CustomStringConvertible {" % self.generate_class(), indent=indent)
                for enum_case in json["enum"]:
                    enm += out('case _%s' % enum_case, indent=indent + 1)
                enm += out('case _unknown(Int)', indent=indent + 1)

                enm += out("", indent=0)
                enm += out("\nvar description: String {", indent=indent + 1)
                enm += out("switch self {", indent=indent + 2)
                for enum_case in json["enum"]:
                    enm += out('case ._%s: return "%s"' % (enum_case, enum_case), indent=indent + 2)
                enm += out('case ._unknown(let value): return "\(value)"', indent=indent + 2)
                enm += out("}", indent=indent + 2)
                enm += out("}", indent=indent + 1)

                enm += out("}", indent=indent)
                return enm
            return None
        elif type == "array":
            return None
        else:
            assert False, "Unknown type for definition: %s, %s" % (type, json)


class PathMethod(object):
    def __init__(self, json: dict, method: str):
        self.method = method
        self.process_json(json)


    def process_json(self, json: dict):
        params = json['parameters']

        self.path_params = [x for x in params if x['in'] == 'path']

        query_params_names = set()
        query_params = []
        for x in params:
            if x['in'] == 'query' and x['name'] not in query_params_names:
                query_params_names.add(x['name'])
                query_params.append(x)

        self.query_params = sorted(query_params, key=lambda item: item["name"])

        body_params = [x for x in params if x['in'] == 'body']
        assert len(body_params) <= 1, "Should be only single body param: %s" % body_params

        # definition format: /definitions/xxx.yyy_zzz.Model
        swagger_request_definition = (body_params[0] if len(body_params) > 0 else {}) \
            .get('schema', {}) \
            .get('$ref') or ""
        swagger_response_definition = json.get('responses', {}).get('200', {}).get('schema', {}).get('$ref') or ""

        swagger_request_definition = None if len(swagger_request_definition) == 0 else swagger_request_definition.split('/')[-1]
        swagger_response_definition = None if len(swagger_response_definition) == 0 else swagger_response_definition.split('/')[-1]

        self.has_known_models = is_known(swagger_request_definition) and is_known(swagger_response_definition)
        self.request_type = self.parse_ref_type(swagger_request_definition)
        self.response_type = self.parse_ref_type(swagger_response_definition)


    def should_use_generic_for_request_type(self) -> bool:
        return self.request_type is not None and '_' not in self.request_type


    def parse_ref_type(self, full_type: Optional[str]) -> Optional[str]:
        if full_type is None:
            return None

        # auto.api.auto_parts.AutopartsSuggestApiResponse.Response -> Auto_Api_AutoParts_AutopartsSuggestApiResponse.Response
        parts = full_type.split('.')
        parts = list(
            map(
                lambda x: ''.join(
                    list(
                        map(
                            lambda y: y[1] if y[0] == 0 else capitalize_only_first_letter(y[1]),
                            enumerate(x.replace('-', '_').split('_'))
                        )
                    )
                ),
                parts
            )
        )

        is_inner = False
        for x in enumerate(parts):
            idx = x[0]
            s = x[1]
            if s[0].isupper():
                if not is_inner and idx > 0:
                    is_inner = True
                    parts[idx] = '_' + s
                    continue
                parts[idx] = ('' if idx == 0 else '.') + s
            else:
                parts[idx] = ('' if idx == 0 else '_') + capitalize_only_first_letter(s)

        return ''.join(parts)


    def generate_ok_method(self, indent: str) -> str:
        generic_type_method = []
        if self.should_use_generic_for_request_type():
            generic_type_method += ["Request: SwiftProtobuf.Message"]
        if self.response_type is None:
            generic_type_method += ["Response: SwiftProtobuf.Message"]
        request_type = 'StubProtobufMessage' if self.request_type is None else self.request_type

        cl = ""
        if len(generic_type_method) == 0:
            cl += out("func ok(", indent=indent)
        else:
            cl += out("func ok<%s>(" % ', '.join(generic_type_method), indent=indent)

        if len(self.query_params) > 0:
            query_param_type = "QueryParameter"
        else:
            query_param_type = "StubEndpointQueryParameter"

        cl += out(
            "mock: MockSource<%s, %s>" % (
                'Request' if self.should_use_generic_for_request_type() else request_type,
                'Response' if self.response_type is None else self.response_type
            ),
            indent=indent + 1
        )
        cl += out(") {", indent=indent)

        cl += out(
            "let endpoint = MockedEndpoint<%s, %s, %s>(" % (
                query_param_type,
                'Request' if self.should_use_generic_for_request_type() else request_type,
                'Response' if self.response_type is None else self.response_type
            ),
            indent=indent + 1
        )
        cl += out("responseCode: ._200,", indent=indent + 2)
        cl += out("method: .%s," % self.method, indent=indent + 2)
        cl += out('path: path,', indent=indent + 2)

        if len(self.query_params) > 0:
            cl += out("parameters: parameters,", indent=indent + 2)
        else:
            cl += out("parameters: nil,", indent=indent + 2)

        cl += out("mock: mock", indent=indent + 2)
        cl += out(")", indent=indent + 1)
        cl += out("endpoint.use(with: server)", indent=indent + 1)
        cl += out("}", indent=indent)
        return cl


    def generate_error_method(self, indent: str) -> str:
        request_type = 'StubProtobufMessage' if self.request_type is None else self.request_type

        cl = ""
        if self.should_use_generic_for_request_type():
            cl += out("func error<Request: SwiftProtobuf.Message, Response: SwiftProtobuf.Message>(", indent=indent)
        else:
            cl += out("func error<Response: SwiftProtobuf.Message>(", indent=indent)

        cl += out("status: HTTPResponseStatus = ._400,", indent=indent + 1)

        if len(self.query_params) > 0:
            query_param_type = "QueryParameter"
        else:
            query_param_type = "StubEndpointQueryParameter"

        cl += out(
            "mock: MockSource<%s, Response>" %
                ('Request' if self.should_use_generic_for_request_type() else request_type),
            indent=indent + 1
        )
        cl += out(") {", indent=indent)

        cl += out(
            "let endpoint = MockedEndpoint<%s, %s, Response>(" % (
                query_param_type,
                'Request' if self.should_use_generic_for_request_type() else request_type
            ),
            indent=indent + 1
        )
        cl += out("responseCode: status,", indent=indent + 2)
        cl += out("method: .%s," % self.method, indent=indent + 2)
        cl += out('path: path,', indent=indent + 2)

        if len(self.query_params) > 0:
            cl += out("parameters: parameters,", indent=indent + 2)
        else:
            cl += out("parameters: nil,", indent=indent + 2)

        cl += out("mock: mock", indent=indent + 2)
        cl += out(")", indent=indent + 1)
        cl += out("endpoint.use(with: server)", indent=indent + 1)
        cl += out("}", indent=indent)
        return cl

    def generate_wait_method(self, indent: str, is_inverted: bool = False) -> str:
        cl = ""

        should_use_generic = self.should_use_generic_for_request_type()

        method_name = "notExpect" if is_inverted else "expect"

        if should_use_generic:
            cl += out("func %s<T: SwiftProtobuf.Message>(" % method_name, indent=indent)
        else:
            cl += out("func %s(" % method_name, indent=indent)

        if len(self.query_params) > 0:
            query_param_type = "QueryParameter"
        else:
            query_param_type = "StubEndpointQueryParameter"

        if self.request_type is not None:
            request_type = 'T' if should_use_generic else self.request_type
            cl += out("checker: ((%s, Int) -> ExpectationCheckerVerdict)? = nil" % request_type, indent=indent + 1)
        else:
            request_type = 'StubProtobufMessage'
        cl += out(") -> XCTestExpectation {", indent=indent)

        cl += out(
            "let expectation = MockedEndpointExpectation<%s, %s>(" % (
                query_param_type,
                request_type
            ),
            indent=indent + 1
        )
        cl += out("method: .%s," % self.method, indent=indent + 2)
        cl += out('path: path,', indent=indent + 2)

        if len(self.query_params) > 0:
            cl += out("parameters: parameters,", indent=indent + 2)
        else:
            cl += out("parameters: nil,", indent=indent + 2)

        if self.request_type is not None:
            cl += out("checker: checker,", indent=indent + 2)
        else:
            cl += out("checker: nil,", indent=indent + 2)

        cl += out("isInverted: %s" % ('true' if is_inverted else 'false'), indent=indent + 2)

        cl += out(")", indent=indent + 1)
        cl += out("return expectation.make(with: server)", indent=indent + 1)
        cl += out("}", indent=indent)
        return cl


    def generate_body(self, indent: int) -> str:
        cl = ""

        if len(self.query_params) > 0:
            cl += self.generate_query_parameter_definition(indent=indent)
            cl += out("fileprivate let server: StubServer", indent=indent)
            cl += out("fileprivate let path: String", indent=indent)
            cl += out("fileprivate let parameters: EndpointQueryParametersMatching<QueryParameter>", indent=indent)
            cl += "\n"
            cl += out("fileprivate init(_ server: StubServer, _ path: String, _ parameters: EndpointQueryParametersMatching<QueryParameter>) {", indent=indent)
            cl += out("self.parameters = parameters", indent=indent + 1)
        else:
            cl += out("fileprivate let server: StubServer", indent=indent)
            cl += out("fileprivate let path: String", indent=indent)
            cl += "\n"
            cl += out("fileprivate init(_ server: StubServer, _ path: String) {", indent=indent)

        cl += out("self.server = server", indent=indent + 1)
        cl += out("self.path = path", indent=indent + 1)
        cl += out("}", indent=indent)
        cl += "\n"

        cl += self.generate_ok_method(indent=indent)
        cl += '\n'
        cl += self.generate_error_method(indent=indent)
        cl += '\n'
        cl += self.generate_wait_method(indent=indent)
        cl += '\n'
        cl += self.generate_wait_method(indent=indent, is_inverted=True)

        return cl


    def param_type_to_string(self, json: dict) -> str:
        type = json["type"] if "type" in json else "string"

        if type in ["string", "date"]:
            return "String"
        elif type in ["integer", "int"]:
            return "Int"
        elif type == "boolean":
            return "Bool"
        elif type in ["number", "double"]:
            return "Double"
        elif type == "array":
            if "items" in json:
                return "[" + self.param_type_to_string(json["items"]) + "]"
            else:
                return "[String]"
        else:
            assert False, "Unknown type for definition: %s, %s" % (type, json)


    def generate_query_parameter_definition(self, indent: int) -> str:
        cl = ""
        cl += out("enum QueryParameter: EndpointQueryParameter {", indent=indent)
        for param in self.query_params:
            case_type = self.param_type_to_string(param)
            cl += out("case %s(%s)" % (kebab_and_snake_to_camel_case(param["name"]), case_type), indent=indent + 1)
        cl += out("case _unknown(String, String)", indent=indent + 1)
        cl += "\n"

        cl += out("var queryRepresentation: String {", indent=indent + 1)

        cl += out("switch self {", indent=indent + 2)
        for param in self.query_params:
            case_type = self.param_type_to_string(param)
            if case_type in ["String", "Int", "Bool", "Double"]:
                string_repr = '"%s=\(value)"' % param["name"]
            elif case_type[0] == "[":
                string_repr = 'value.map({ "%s=\($0)" }).joined(separator: "&")' % param["name"]
            else:
                assert False, "Unknown type: %s" % (case_type)

            cl += out(
                'case .%s(let value): return %s' % (
                    kebab_and_snake_to_camel_case(param["name"]),
                    string_repr
                ),
                indent=indent + 2
            )
        cl += out('case ._unknown(let key, let value): return "\(key)=\(value)"', indent=indent + 2)
        cl += out("}", indent=indent + 2)

        cl += out("}", indent=indent + 1)

        cl += out("}\n", indent=indent)
        return cl


class PathNode(object):
    def __init__(self, path_segment: str, current_full_path: str):
        self.child = None
        self.method = None
        self.path_param = None
        self.child_path_param = dict()
        self.path_segment = path_segment
        self.current_full_path = current_full_path

        if is_path_segment_parameter(path_segment):
            self.reference_name = kebab_and_snake_to_camel_case(path_segment[1:][:-1])
            self.reference_class = capitalize_only_first_letter(kebab_and_snake_to_camel_case(path_segment[1:][:-1]))
        else:
            self.reference_name = kebab_and_snake_to_camel_case(path_segment)
            self.reference_class = capitalize_only_first_letter(kebab_and_snake_to_camel_case(path_segment))


    def __str__(self) -> str:
        return "PathNode <path_segment=%s, is_leaf=%s, child_len=%s>" % (
            self.path_segment,
            self.is_leaf(),
            len(self.child) if self.child is not None else None
        )


    def append_child(self, path_component: str, node: PathMethod):
        if self.child is None:
            self.child = dict()
        assert(self.method is None)
        self.child[path_component] = node

        if node.path_param is not None:
            self.child_path_param[path_component] = node.path_param


    def assign_method(self, method: json):
        assert(self.child is None)
        assert(self.method is None)
        self.method = method


    def assign_path_param(self, param: PathParam):
        self.path_param = param


    def is_leaf(self) -> bool:
        return self.method is not None


    def generate_reference_class(self) -> str:
        return "%s" % self.reference_class


    def generate_reference_class_declaration(self) -> str:
        if self.is_leaf():
            return "final class %s {" % self.generate_reference_class()
        return "final class %s: EndpointBuilder {" % self.generate_reference_class()


    def generate_chain_method_definition(self, indent: int) -> str:
        mt = ""
        if self.path_param is None:
            if self.is_leaf():
                mt += out("/// method: %s %s" % (self.path_segment.upper(), self.current_full_path), indent=indent)

                assert self.method is not None, "Method should be assigned"
                if len(self.method.query_params) > 0:
                    ctor = '.init(server, path, parameters)'
                    mt += out(
                        'func %s(parameters: EndpointQueryParametersMatching<%s.QueryParameter>) -> %s { %s }' % (
                            escape_swift_keyword_if_needed(self.reference_name),
                            self.generate_reference_class(),
                            self.generate_reference_class(),
                            ctor
                        ),
                        indent=indent
                    )
                else:
                    ctor = '.init(server, path)'
                    mt += out(
                        'var %s: %s { %s }' % (
                            escape_swift_keyword_if_needed(self.reference_name),
                            self.generate_reference_class(),
                            ctor
                        ),
                        indent=indent
                    )
            else:
                ctor = '.init(server, path + "/%s")' % self.path_segment
                mt += out("/// path: %s" % self.current_full_path, indent=indent)

                mt += out(
                    'var %s: %s { %s }' % (
                        escape_swift_keyword_if_needed(self.reference_name),
                        self.generate_reference_class(),
                        ctor
                    ),
                    indent=indent
                )
        else:
            assert not self.is_leaf(), 'Should not be a leaf'

            arg_class = self.path_param.generate_class()
            append_path = self.reference_name
            if arg_class[0] == '[': # swift array:
                append_path = '%s.joined(separator: ",")' % self.reference_name

            ctor = '.init(server, path + "/\(%s)")' % append_path
            mt += out("/// path: %s" % self.current_full_path, indent=indent)

            mt += out(
                'func %s(_ %s: %s) -> %s { %s }' % (
                    escape_swift_keyword_if_needed(self.reference_name),
                    escape_swift_keyword_if_needed(self.reference_name),
                    arg_class,
                    self.generate_reference_class(),
                    ctor
                ),
                indent=indent
            )
        return mt


    def generate_child_path_param_definitions(self, indent: int) -> List[int]:
        df = []
        for cpp in self.child_path_param:
            cdf = self.child_path_param[cpp].generate_definition(indent=indent)
            if cdf is not None:
                df.append(cdf)
        return df


def out(str: str, indent: int) -> str:
    return (' ' * 4) * indent + str + "\n"


def capitalize_only_first_letter(str: str) -> str:
    return str[:1].upper() + str[1:]


def snake_to_camel_case(str: str) -> str:
    parts = str.split('_')
    return ''.join([parts[0]] + list(map(lambda x: x.capitalize(), parts[1:])))


def kebab_to_camel_case(str: str) -> str:
    parts = str.split('-')
    return ''.join([parts[0]] + list(map(lambda x: x.capitalize(), parts[1:])))


def kebab_and_snake_to_camel_case(str: str) -> str:
    return snake_to_camel_case(kebab_to_camel_case(str))


def is_path_segment_parameter(str: str) -> str:
    if len(str) == 0:
        return False
    return str[0] == '{'


def escape_swift_keyword_if_needed(str: str) -> str:
    if str in ['init', 'default', 'do']:
        return "`%s`" % str
    return str


def build_path_tree(node: PathNode, path: str, method: PathMethod):
    parts = path.split("/")

    is_path_segment_param = is_path_segment_parameter(parts[0])
    raw_current = parts[0]
    current = parts[0][1:][:-1] if is_path_segment_param else parts[0]
    tail = parts[1:]

    if len(tail) == 0:
        method_node = PathNode(
            path_segment=current,
            current_full_path=node.current_full_path
        )
        method_node.assign_method(method)

        node.append_child(current, method_node)
    else:
        child_node = PathNode(path_segment=current, current_full_path=node.current_full_path + '/' + raw_current)
        if node.child is not None and current in node.child:
            child_node = node.child[current]

        if is_path_segment_param:
            for p in method.path_params:
                if p["name"] == current:
                    child_node.assign_path_param(PathParam(p))

        build_path_tree(child_node, '/'.join(tail), method)
        node.append_child(current, child_node)


def print_path_tree(node, level: int):
    if node is None or node.child is None:
        return

    for x in node.child:
        print(level * " → ", x)
        print_path_tree(node.child[x], level + 1)


def generate_path_builders(node: PathNode, name, level: int) -> Optional[str]:
    class_definition = []

    if node.is_leaf():
        class_definition = [node.method.generate_body(indent=level + 1)]
    else:
        class_definition += node.generate_child_path_param_definitions(level + 1)

        sorted_nodes = sorted(node.child)
        for x in sorted_nodes:
            class_definition += [generate_path_builders(node.child[x], x, level + 1)]

        for x in sorted_nodes:
            class_definition += [node.child[x].generate_chain_method_definition(indent=level + 1)]

    cl = ""
    cl += out(node.generate_reference_class_declaration(), indent=level)
    for (idx, inner) in enumerate(class_definition):
        cl += inner + ('' if idx == len(class_definition) - 1 else "\n")
    cl += out("}", indent=level)

    return cl


def generate_static_code() -> str:
    cl = ""

    cl += "// Весь код ниже генерируется автоматически, не вносите изменения вручную\n"

    cl += out("import XCTest", indent=0)
    cl += out("import Foundation", indent=0)
    cl += out("import SwiftProtobuf", indent=0)
    cl += out("import AutoRuProtoModels\n", indent=0)

    cl += out("class EndpointBuilder {", indent=0)
    cl += out("fileprivate let server: StubServer", indent=1)
    cl += out("fileprivate let path: String\n", indent=1)
    cl += out("fileprivate init(_ server: StubServer, _ path: String) {", indent=1)
    cl += out("self.server = server", indent=2)
    cl += out("self.path = path", indent=2)
    cl += out("}", indent=1)
    cl += out("}", indent=0)

    cl += '\n'

    cl += out("struct StubEndpointQueryParameter: EndpointQueryParameter {", indent=0)
    cl += out('var queryRepresentation: String { fatalError("Unimplemented") }', indent=1)
    cl += out("}", indent=0)

    cl += '\n'

    cl += out("struct StubProtobufMessage: SwiftProtobuf.Message {", indent=0)
    cl += out('static var protoMessageName: String { fatalError("Unimplemented") }\n', indent=1)
    cl += out('var unknownFields: UnknownStorage\n', indent=1)
    cl += out('init() { unknownFields = .init() }\n', indent=1)
    cl += out('mutating func decodeMessage<D>(decoder: inout D) throws where D: Decoder { fatalError("Unimplemented") }\n', indent=1)
    cl += out('func traverse<V>(visitor: inout V) throws where V: Visitor { fatalError("Unimplemented") }\n', indent=1)
    cl += out('func isEqualTo(message: Message) -> Bool { fatalError("Unimplemented") }', indent=1)
    cl += out("}", indent=0)

    return cl


def generate_top_level(root: PathNode) -> str:
    cl = ""

    cl += out("enum PublicAPI {", indent=0)

    for x in root.child:
        cl += generate_path_builders(root.child[x], x, level=1)
        cl += '\n'
    cl += out("}", indent=0)

    cl += '\n'

    cl += out("final class API {", indent=0)

    cl += out("fileprivate let server: StubServer\n", indent=1)
    cl += out("init(server: StubServer) { self.server = server }\n", indent=1)

    for x in root.child:
        node = root.child[x]
        cl += out('/// path: /%s' % node.path_segment, indent=1)
        cl += out(
            'var %s: PublicAPI.%s { PublicAPI.%s(server, "/%s") }' % (
                kebab_and_snake_to_camel_case(node.path_segment),
                node.generate_reference_class(),
                node.generate_reference_class(),
                node.path_segment
            ),
            indent=1
        )
        cl += '\n'

    cl += out("}", indent=0)

    return cl


known_protomodels = []
if sys.argv[1] is not None:
    known_protomodels = open(sys.argv[1], 'r').read().splitlines()


def is_known(model: Optional[str]) -> bool:
    if model is None:
        return True

    if len(known_protomodels) == 0:
        return True

    return model in known_protomodels


input = sys.stdin.read().strip()
schema = json.loads(input)

root = PathNode(path_segment='', current_full_path='')

for path in schema["paths"]:
    path_without_root = path[1:] if path[0] == '/' else path
    for http_method in schema["paths"][path]:
        method = PathMethod(schema["paths"][path][http_method], http_method)

        if method.has_known_models:
            print(f'{http_method.upper()} {path}', file=sys.stderr)
        else:
            continue

        build_path_tree(
            root,
            '/'.join([path_without_root, http_method]),
            method
        )

print(generate_static_code())
print(generate_top_level(root), end='')
