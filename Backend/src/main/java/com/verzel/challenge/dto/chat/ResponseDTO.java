package com.verzel.challenge.dto.chat;

import com.verzel.challenge.type.ResponseAction;

public record ResponseDTO(ResponseAction action, String message, Object data) {

}
