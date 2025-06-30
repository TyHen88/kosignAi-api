package org.kosign.chatbotapi.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.kosign.chatbotapi.components.AbstractEnumConverter;
import org.kosign.chatbotapi.components.GenericEnum;


public enum StatusUser implements GenericEnum<StatusUser, String> {

    ACTIVE("1"),
    INACTIVE("2"),
    SUSPENDED("3"),
    DELETED("4");

    private final String value;

    StatusUser(String value) {
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
            case INACTIVE -> "Inactive";
            case SUSPENDED -> "Suspended";
            case DELETED -> "Deleted";
        };
    }
    @JsonCreator
    public static StatusUser fromValue(String value) {
        for (StatusUser status : StatusUser.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null; // or throw an exception if preferred
    }

    public static class Converter extends AbstractEnumConverter<StatusUser, String> {
        public Converter() {
            super(StatusUser.class);
        }
    }
}
