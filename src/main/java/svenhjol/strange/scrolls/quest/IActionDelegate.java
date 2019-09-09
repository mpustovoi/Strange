package svenhjol.strange.scrolls.quest;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraftforge.eventbus.api.Event;
import svenhjol.strange.scrolls.quest.action.Action;

public interface IActionDelegate
{
    Action.Type getType();

    boolean respondTo(Event event);

    boolean isCompleted();

    float getCompletion();

    CompoundNBT toNBT();

    void setQuest(IQuest quest);

    void fromNBT(INBT nbt);
}
