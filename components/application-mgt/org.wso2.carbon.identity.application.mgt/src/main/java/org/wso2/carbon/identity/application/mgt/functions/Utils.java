package org.wso2.carbon.identity.application.mgt.functions;

import java.util.function.Consumer;

/**
 *
 */
public class Utils {

    public static void setIfNotNull(String value, Consumer<String> consumer) {

        if (value != null) {
            consumer.accept(value);
        }
    }

    public static void setIfNotNull(Boolean value, Consumer<Boolean> consumer) {

        if (value != null) {
            consumer.accept(value);
        }
    }

    public static <T> void setIfNotNull(T value, Consumer<T> consumer) {

        if (value != null) {
            consumer.accept(value);
        }
    }
}
