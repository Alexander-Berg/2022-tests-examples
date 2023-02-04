package ru.auto.tests.realtyapi.mapper;

import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XlsMapper implements ObjectMapper {

    @Override
    public Object deserialize(ObjectMapperDeserializationContext context) {
        try (InputStream input = context.getDataToDeserialize().asInputStream()) {
            return new XSSFWorkbook(input);
        } catch (IOException e) {
            throw new RuntimeException("Неудалось десериализовать файл .xls");
        }
    }

    @Override
    public Object serialize(ObjectMapperSerializationContext context) {
        return new ByteArrayInputStream(context.getObjectToSerializeAs(byte[].class));
    }

    public static XlsMapper xlsMapper() {
        return new XlsMapper();
    }
}
