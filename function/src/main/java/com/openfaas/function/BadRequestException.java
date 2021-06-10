package com.openfaas.function;

public class BadRequestException extends Exception {

    public BadRequestException(String msg) {
        super(msg);
    }

    
}
