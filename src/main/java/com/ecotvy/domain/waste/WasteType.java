package com.ecotvy.domain.waste;

public enum WasteType {
    PLASTIC("플라스틱"),
    CAN("캔"),
    PAPER("종이"),
    FOOD("음식물"),
    GENERAL("일반쓰레기"),
    GLASS("유리"),
    BATTERY("건전지"),
    LIGHT_BULB("형광등");

    private final String displayName;

    WasteType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

