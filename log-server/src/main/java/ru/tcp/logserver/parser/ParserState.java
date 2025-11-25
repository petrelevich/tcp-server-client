package ru.tcp.logserver.parser;

public enum ParserState {
    HEADER,
    LENGTH,
    BEGIN,
    MSG,
}
