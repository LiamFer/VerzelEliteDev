package com.verzel.challenge.dto;

public record APIMessage <T>(int code,T message) {
}
