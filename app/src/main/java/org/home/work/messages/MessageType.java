package org.home.work.messages;

public enum MessageType {
    LAUNCHED("RocketLaunched"),
    SPEED_INCREASED("RocketSpeedIncreased"),
    SPEED_DECREASED("RocketSpeedDecreased"),
    EXPLODED("RocketExploded"),
    MISSION_CHANGED("RocketMissionChanged"),

    UNKNOWN("Unknown")
    ;

    private final String str;

    MessageType(String str) {
        this.str = str;
    }

    public static MessageType ofStr(String str) {
        for (var updateType : values()) {
            if (updateType.str.equalsIgnoreCase(str)) {
                return updateType;
            }
        }
        return UNKNOWN;
    }
}
