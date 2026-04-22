package com.kris.mithril;

public sealed interface MithrilResult<T> permits MithrilResult.Ok, MithrilResult.Err {

    record Ok<T>(T value) implements MithrilResult<T> {}

    record Err<T>(String message, int line) implements MithrilResult<T> {}

    static <T> MithrilResult<T> ok(T value){
        return new Ok<>(value);
    }

    static <T> MithrilResult<T> err(String msg, int line){
        return new Err<>(msg, line);
    }

    default boolean isOk() {
        return this instanceof Ok;
    }

    default boolean isErr() {
        return this instanceof Err;
    }

    default T unwrap(){
        return switch (this){
            case Ok<T> ok -> ok.value();
            case Err<T> err -> throw new IllegalStateException("Called unwrap() on an Err: " + err.message());
        };
    }
}
