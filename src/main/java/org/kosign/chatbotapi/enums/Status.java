package org.kosign.chatbotapi.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.kosign.chatbotapi.components.AbstractEnumConverter;
import org.kosign.chatbotapi.components.GenericEnum;


public enum Status implements GenericEnum<Status, String> {

    ACTIVE("1"),
    DELETED("9");

    private final String value;

    Status(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return switch (this) {
            case ACTIVE -> "Active";
            case DELETED -> "Deleted";
        };
    }
    @JsonCreator
    public static Status fromValue(String value) {
        for (Status status : Status.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null; // or throw an exception if preferred
    }

    public static class Converter extends AbstractEnumConverter<Status, String> {
        public Converter() {
            super(Status.class);
        }
    }
}
