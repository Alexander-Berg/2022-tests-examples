# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import urllib
import pytest
import mock
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
)

from lxml import etree
from yb_snout_api.resources.muzzle.bridge.utils.execution_state import ExecutionState
from yb_snout_api.resources.muzzle.bridge.utils.files import load_xml_file

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.resources.muzzle_bridge.helpers import (
    create_content_for_xml,
    create_xslt_tree,
    get_file_loader_mock,
    XMLMatcher,
)


class TestMuzzle(TestCaseApiAppBase):
    BASE_API = '/muzzle/bridge/balance-admin/test_file.xml'

    test_http_parameters = {
        'test-parm1': 'test-parm1-value',
        'test-parm2': '666',
    }

    test_http_cookies = {'test_cookie1': 'value1', 'test_cookie2': 'value2'}

    test_http_headers = {
        'Test-Header1': 'test-header1-value',
        'Test-Header2': '666',
        'Cookie': '; '.join('{}={}'.format(k, v) for k, v in test_http_cookies.items()),
        'Is-Admin': 'False',
    }

    test_content_data = '159e22ec4edaa8'

    def test_get_xml(self):
        """Запрос xml файла с контентом и наложенным xslt.
        Проверяем:
            - Вызывается метод загружающий содержимое файлов xml и xsl;
            - Накладывется xslt;
            - В ответе приходит xml в котором есть даные из xml файла выбраные xslt.
        """

        content = create_content_for_xml('<content test_attr="{}"/>'.format(self.test_content_data))

        xslt = create_xslt_tree(
            '''
                <content_tag>
                    <data>
                        <value>
                            <xsl:value-of select="page/content/@test_attr"/>
                        </value>
                    </data>
                </content_tag>
            ''',
        )

        with get_file_loader_mock(content, xslt):

            response = self.test_client.get(self.BASE_API, is_admin=False)
            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

            # Пришел xml документ с наложенным xsl, в документе есть данные.
            assert_that(
                etree.XML(response.data), XMLMatcher([
                    {'value': self.test_content_data},
                ]),
            )

    def test_send_parameters(self):
        """Передаём параметры, куки и хедеры. После вызова конструктора, объект
        ExecutionState должен содержать данные из запроса.
        """

        class ExecutionStateData(object):
            parameters = None
            cookies = None
            headers = None

        def execution_state_fabric(request, subdir, file_name):
            """Мок конструктора объекта состояния."""
            execution_state = ExecutionState(request, subdir, file_name)
            ExecutionStateData.parameters = execution_state.in_params
            ExecutionStateData.cookies = execution_state.in_cookies
            ExecutionStateData.headers = execution_state.in_headers
            return execution_state

        with get_file_loader_mock(content=None), mock.patch(
                'yb_snout_api.resources.muzzle.bridge.logic.ExecutionState',
                new=execution_state_fabric):

            self.test_client.get(
                self.BASE_API,
                self.test_http_parameters,
                is_admin=False,
                headers=self.test_http_headers,
            )
            atalon_headers = dict(self.test_http_headers)
            del atalon_headers['Cookie']
            assert_that(ExecutionStateData.parameters, has_entries(self.test_http_parameters))
            assert_that(ExecutionStateData.cookies, has_entries(self.test_http_cookies))
            assert_that(ExecutionStateData.headers, has_entries(atalon_headers))

    def test_mist_execution(self):
        """Выполняем xml с mist блоками,
        объект ExecutionState должен содержать данные установленные mist блоками и данные из запроса.
        Проверяем извлекая данные из объекта состояния в xslt.
        """
        mist_string_name = 'test_string_variable1'

        parameters = [
            {'name': key, 'value': value}
            for key, value in self.test_http_parameters.items()
        ][:2]

        content = create_content_for_xml(
            '''
                <mist>
                    <method>set_state_string</method>
                    <param type='String'>{name}</param>
                    <param type='String'>{value}</param>
                </mist>
                <mist>
                    <method>set_state_by_request</method>
                    <param type='String'></param>
                </mist>
            '''.format(name=mist_string_name, value=self.test_content_data),
        )

        xslt = create_xslt_tree(
            '''
                <content_tag>
                    <variable>
                        <name>{mist_str_name}</name>
                        <value>
                            <xsl:value-of select="snout:get-state-arg('{mist_str_name}')"/>
                        </value>
                    </variable>
                    <variable>
                        <name>{parametr_name0}</name>
                        <value>
                            <xsl:value-of select="snout:get-state-arg('{parametr_name0}')"/>
                        </value>
                    </variable>
                    <variable>
                        <name>{parametr_name1}</name>
                        <value>
                            <xsl:value-of select="snout:get-state-arg('{parametr_name1}')"/>
                        </value>
                    </variable>
                </content_tag>
            '''.format(
                mist_str_name=mist_string_name,
                mist_str_val=self.test_content_data,
                parametr_name0=parameters[0]['name'],
                parametr_name1=parameters[1]['name']),
        )

        with get_file_loader_mock(content, xslt):
            response = self.test_client.get(
                self.BASE_API,
                {par['name']: par['value']for par in parameters},
                is_admin=False,
            )

            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
            content_tag = etree.XML(response.data)
            assert_that(content_tag is not None, 'response should contain content_tag')
            assert_that(
                content_tag, XMLMatcher(
                    [
                        {'name': mist_string_name, 'value': self.test_content_data},
                        parameters[0],
                        parameters[1],
                    ],
                ),
            )

    def test_file_tag_execution(self):
        """В документ должен быть вставлен файл описанный в теге <file>.
        Мокаем метод загрузки содержимого файла, метод возвращает содержимое файла с даными,
        добавляем в ответ данные в xslt, проверяем, что в ответе есть переданные данные.
        """
        content = create_content_for_xml(
            '''
                <file>
                    <method>include</method>
                    <param type='String'>test_include.xml</param>
                </file>
            ''',
        )

        content_for_include = etree.XML(
            '<included_data>data_from_included_file</included_data>',
        )

        xslt = create_xslt_tree(
            '''
                <content_tag>
                    <data_from_included_file>
                            <data><xsl:value-of select='*/included_data'/></data>
                    </data_from_included_file>
                </content_tag>
            ''',
        )

        def xml_file_loader_mock(path):
            '''Мок загрузки xml файла.'''
            if 'test_include.xml' in path:
                return etree.ElementTree(content_for_include)
            else:
                return load_xml_file(path)

        with get_file_loader_mock(content, xslt), mock.patch(
                'yb_snout_api.resources.muzzle.bridge.utils.node_executors.file_block.files.load_xml_file',
                new=xml_file_loader_mock):

            response = self.test_client.get(self.BASE_API, is_admin=False)
            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
            content_tag = etree.XML(response.data)
            assert_that(content_tag is not None, 'response should contain content_tag')
            assert_that(
                content_tag, XMLMatcher([
                    {'data': 'data_from_included_file'},
                ]),
            )

    @staticmethod
    def create_muzzle_content_block():
        parameters = [
            {'name': key, 'value': value}
            for key, value in TestMuzzle.test_http_parameters.items()
        ][:2]

        content = create_content_for_xml(
            '''
                <mist>
                    <method>set_state_string</method>
                    <param type='String'>{name0}</param>
                    <param type='String'>{value0}</param>
                </mist>

                <block threaded='no' timeout='350000'>
                    <nameref>MuzzleServantRef</nameref>
                    <method>servant_test_method</method>
                    <param type='StateArg' as='String'>{name0}</param>
                    <param type='StateArg' as='String' default='{value1}'>{name1}</param>
                </block>
            '''.format(
                name0=parameters[0]['name'],
                name1=parameters[1]['name'],
                value0=parameters[0]['value'],
                value1=parameters[1]['value'],
            ),
        )
        return content

    @staticmethod
    def create_paysys_content_block():
        state_variables = {
            'variable1': 'value1',
            'variable2': 'value2',
        }

        block_parameters = ['block_par1', 'block_par2']

        content = create_content_for_xml(
            ''.join([
                '''
                    <mist>
                        <method>set_state_string</method>
                        <param type='String'>{name}</param>
                        <param type='String'>{value}</param>
                    </mist>
                '''.format(name=name, value=value)
                for name, value in state_variables.items()
            ]) +
            '''
                    <mist>
                        <method>set_state_by_request</method>
                        <param type='String'></param>
                    </mist>
                    <block threaded='no' timeout='350000'>
                        <nameref>PaysysServantRef</nameref>
                        <method>servant_test_method</method>
                        <param type='State'/>
                        <param type='Request'/>
                        <param type='String'>{param1}</param>
                        <param type='String'>{param2}</param>
                    </block>
            '''.format(
                param1=block_parameters[0],
                param2=block_parameters[1],
            ),
        )
        return content

    @pytest.mark.parametrize("content_func", ['create_muzzle_content_block'])
    def test_block_execution(self, content_func):
        """Выполняем вызовы описанные в теге <block> xml файла, для серванта Muzzle и Paysys.
        Проверяем:
            - Вызов метода должен быть передан в сервант;
            - Параметры извлечены из объекта состояния и переданны
            (если параметра нет в объекте состояния передать значение по умолчанию);
            - Ответ вставлен в тело документа.
        """

        servant_content = getattr(self, content_func)()

        xslt = create_xslt_tree(
            '''
                <content_tag>
                    <data_from_block>
                            <data><xsl:value-of select='*/block_data'/></data>
                    </data_from_block>
                </content_tag>
            ''',
        )

        class BlockExecutionData(object):
            parameters = None
            method_name = None

        def servant_call_mock(*args):
            """Мок вызова метода серванта.
                сигнатуры мокаемых функций
                call_muzzle_method(muzzle_proxy, method_name, parameters)
                call_paysys_method(method_name, parameters)
            """
            method_name = args[-2]
            parameters = args[-1]
            BlockExecutionData.method_name = method_name
            BlockExecutionData.parameters = parameters
            return etree.fromstring('<block_data>{}</block_data>'.format(self.test_content_data))

        with get_file_loader_mock(servant_content, xslt), mock.patch(
                'yb_snout_api.resources.muzzle.bridge.utils.node_executors.block.call_muzzle_method',
                new=servant_call_mock), mock.patch(
                'yb_snout_api.resources.muzzle.bridge.utils.node_executors.block.call_paysys_method',
                new=servant_call_mock):

            response = self.test_client.get(self.BASE_API, is_admin=False)
            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
            content_tag = etree.XML(response.data)
            assert_that(content_tag is not None, 'response should contain content_tag')

            # Имя метода было передано
            assert_that('servant_test_method', BlockExecutionData.method_name)

            # Получили данные из вызванного метода
            assert_that(
                content_tag, XMLMatcher([
                    {'data': self.test_content_data},
                ]),
            )

    def test_http_block(self):
        """Проверяем:
            - Мокаем методы которые выполняют get, post запросы;
            - Проверяем, что в методы передаются правильные url;
            - В метод post передатся тело запроса(проксируется пекущий запрос).
        """
        http_host = 'http://testhost.ru/'
        test_path = 'path/resource/index.xml'

        parameters = urllib.parse.urlencode(self.test_http_parameters)

        test_url = '{}{}?{}'.format(http_host, test_path, parameters)

        content = create_content_for_xml(
            '''
                <http>
                    <method>getHttp</method>
                    <param type='String'>{host}</param>
                    <param type='String'>{path}</param>
                    <param type='String'>?{parameters}</param>
                </http>
                <http proxy='yes' parse='xml'>
                    <method>post_by_request</method>
                    <param type='String'>{host}</param>
                    <param type='String'>{path}</param>
                    <param type='String'>?{parameters}</param>
                </http>
            '''.format(
                host=http_host,
                path=test_path,
                parameters=parameters.replace('&', '&amp;'),
            ),
        )

        xslt = create_xslt_tree(
            '''
                <content_tag>
                    <xsl:for-each select='*/http_response'><http_request>
                        <method>
                            <xsl:value-of select='method'/>
                        </method>
                        <url>
                            <xsl:value-of select='url'/>
                        </url>
                    </http_request></xsl:for-each>
                </content_tag>
            ''',
        )

        def get_mock(url):
            return ('''<http_response><method>get</method><url>{}</url></http_response>'''
                    .format(url.replace('&', '&amp;')))

        def post_mock(url, data=None):
            return ('''<http_response><method>post</method><url>{}</url></http_response>'''
                    .format(url.replace('&', '&amp;')))

        with get_file_loader_mock(content, xslt), mock.patch(
                'yb_snout_api.resources.muzzle.bridge.utils.node_executors.http.post_request',
                new=post_mock), mock.patch(
                'yb_snout_api.resources.muzzle.bridge.utils.node_executors.http.get_request',
                new=get_mock):

            response = self.test_client.get(self.BASE_API, is_admin=False)

            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
            content_tag = etree.XML(response.data)
            assert_that(content_tag is not None, 'response should contain content_tag')
            assert_that(
                content_tag, XMLMatcher([
                    {'method': 'post', 'url': test_url},
                    {'method': 'get', 'url': test_url},
                ]),
            )

    def test_xslt_extensions_xml2txt(self):
        """"""

        content = create_content_for_xml(
            '''
                <data>
                </data>
            ''',
        )

        xslt = create_xslt_tree(
            '''
                <content_tag>
                    <xsl:variable name="m">
                        <html>
                            <head></head>
                            <body>
                                 <br/>
                                 <wbr/>
                                 <script></script>
                                 <div></div>
                                 <div/>
                            </body>
                        </html>
                    </xsl:variable>
                    <xsl:value-of select="x:xml2text($m)" disable-output-escaping="yes" />
                </content_tag>
            ''',
        )

        with get_file_loader_mock(content, xslt):
            response = self.test_client.get(self.BASE_API, is_admin=False)
            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

    def test_xslt_extensions_if(self):
        """В xslt преобразованиях вызываем кастомные расширения,
        вызовы верно меняют ответ: редиректят, ставят и читают куки, статус ответа и хедеры."""

        content = create_content_for_xml(
            '''
                <data>
                    <field1>include</field1>
                    <field2>
                        <field3>0</field3>
                    </field2>
                    <field4>some text</field4>
                </data>
            ''',
        )

        xslt = create_xslt_tree(
            '''
                <content_tag>
                <data>
                    <true_condition_check>
                        <xsl:value-of select="x:if(1 = 1, 'true', 'false')"/>
                    </true_condition_check>
                </data>
                <data>
                    <false_condition_check>
                        <xsl:value-of select="x:if(1 = 0, 'true', 'false')"/>
                    </false_condition_check>
                </data>
                <data>
                    <exists_in_tree>
                        <xsl:value-of select="x:if(*/data/field2, 'true', 'false')"/>
                    </exists_in_tree>
                </data>
                <data>
                    <not_exists_in_tree>
                        <xsl:value-of select="x:if(*/data/field2/not_exist, 'true', 'false')"/>
                    </not_exists_in_tree>
                </data>
                </content_tag>
            ''',
        )

        with get_file_loader_mock(content, xslt):
            response = self.test_client.get(self.BASE_API, is_admin=False)
            assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
            content_tag = etree.XML(response.data)
            assert_that(
                content_tag, XMLMatcher([
                    {'true_condition_check': 'true'},
                    {'false_condition_check': 'false'},
                    {'exists_in_tree': 'true'},
                    {'not_exists_in_tree': 'false'},
                ]),
            )
