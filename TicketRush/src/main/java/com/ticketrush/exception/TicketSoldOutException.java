package com.ticketrush.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TicketSoldOutException extends RuntimeException{

    public TicketSoldOutException(String message){
        super(message);
    }

}
