package com.example.backend.common;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    public String getMessage(String messageCode, Object... args) {
        return messageSource.getMessage(
                messageCode,
                args,
                Locale.getDefault());
    }
}