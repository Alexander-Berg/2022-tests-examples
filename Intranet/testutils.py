# -*- coding: utf-8 -*-


def parse_multipart(boundary, body):
    response = {}
    body_items = [i.strip().strip(b'--').strip() for i in body.split(boundary)]
    for item in body_items:
        if item:
            headers_string, data = item.split(b'\r\n\r\n')
            name = None
            filename = None
            headers = {}
            for row_header in headers_string.split(b'\r\n'):
                if row_header.startswith(b'Content-Disposition'):
                    header_bits = row_header.split(b';')
                    if len(header_bits) == 2:
                        row_header, name_string = header_bits
                        file_string = None
                    elif len(header_bits) == 3:
                        row_header, name_string, file_string = header_bits
                    name = name_string[7:-1]
                    if file_string:
                        filename = file_string[11:-1]
                header_key, header_value = row_header.split(b': ', 1)
                headers[header_key.decode('utf-8')] = header_value.decode('utf-8')
            if name:
                name = name.decode('utf-8')

            try:
                data = data.decode('utf-8')
            except UnicodeDecodeError:
                pass
            response[name] = {
                'headers': headers,
                'data': data
            }
            if filename:
                response[name]['filename'] = filename.decode('utf-8')
    return response


def get_content_type_and_boundary(response):
    content_type, dirty_boundary = response.headers.get('content-type').split(';')
    return content_type, dirty_boundary.split('=')[1]
