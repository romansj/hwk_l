package org.home.work;

import org.home.work.messages.MessageType;
import org.junit.jupiter.api.Test;

import static org.home.work.messages.MessageType.UNKNOWN;
import static org.junit.jupiter.api.Assertions.*;

class MessageTypeTest {

    @Test
    void undefinedStrReturnsUnknownType() {
        var messageType = MessageType.ofStr("Abracadabra");
        assertEquals(UNKNOWN, messageType);
    }
}