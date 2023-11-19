package svenhjol.strange.feature.cooking_pots;

import net.minecraft.util.StringRepresentable;

public enum CookingStatus implements StringRepresentable {
    NONE("none"),
    IN_PROGRESS("in_progress"),
    COOKED("cooked");

    private final String name;

    CookingStatus(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
