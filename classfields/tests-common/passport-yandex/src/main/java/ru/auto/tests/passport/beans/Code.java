package ru.auto.tests.passport.beans;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

public class Code {

    @Getter
    @NonNull
    @SerializedName("phone_operations")
    private List<PhoneOperation> phoneOperations;

    public class PhoneOperation {

        @Getter
        @NonNull
        @SerializedName("code_value")
        private String codeValue;
    }
}