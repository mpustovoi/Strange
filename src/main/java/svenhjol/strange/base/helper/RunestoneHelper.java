package svenhjol.strange.base.helper;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import svenhjol.meson.Meson;
import svenhjol.strange.Strange;
import svenhjol.strange.outerlands.module.Outerlands;
import svenhjol.strange.runestones.capability.IRunestonesCapability;
import svenhjol.strange.runestones.module.Runestones;
import svenhjol.strange.traveljournal.Entry;

import java.util.*;

public class RunestoneHelper {
    public final static ResourceLocation SPAWN = new ResourceLocation(Strange.MOD_ID, "spawn_point");

    private static final Map<Character, Character> runeCharMap = new TreeMap<>();

    public static Map<Character, Character> getRuneCharMap() {
        return runeCharMap;
    }

    public static BlockPos addRandomOffset(BlockPos pos, Random rand, int max) {
        int n = rand.nextInt(max);
        int e = rand.nextInt(max);
        int s = rand.nextInt(max);
        int w = rand.nextInt(max);
        pos = pos.north(rand.nextFloat() < 0.5F ? n : -n);
        pos = pos.east(rand.nextFloat() < 0.5F ? e : -e);
        pos = pos.south(rand.nextFloat() < 0.5F ? s : -s);
        pos = pos.west(rand.nextFloat() < 0.5F ? w : -w);
        return pos;
    }

    static {
        runeCharMap.put('0', 'a');
        runeCharMap.put('1', 'b');
        runeCharMap.put('2', 'c');
        runeCharMap.put('3', 'd');
        runeCharMap.put('4', 'e');
        runeCharMap.put('5', 'f');
        runeCharMap.put('6', 'g');
        runeCharMap.put('7', 'h');
        runeCharMap.put('8', 'i');
        runeCharMap.put('9', 'j');
        runeCharMap.put('a', 'k');
        runeCharMap.put('b', 'l');
        runeCharMap.put('c', 'm');
        runeCharMap.put('d', 'n');
        runeCharMap.put('e', 'o');
        runeCharMap.put('f', 'p');
        runeCharMap.put('g', 'q');
        runeCharMap.put('h', 'r');
        runeCharMap.put('i', 's');
        runeCharMap.put('j', 't');
        runeCharMap.put('k', 'u');
        runeCharMap.put('l', 'v');
        runeCharMap.put('m', 'w');
        runeCharMap.put('n', 'x');
        runeCharMap.put('o', 'y');
        runeCharMap.put('p', 'z');
    }

    public static BlockPos normalizeInnerPos(BlockPos pos) {
        if (!Meson.isModuleEnabled("strange:outerlands"))
            return pos;

        int x = pos.getX();
        int z = pos.getZ();
        int nx = x;
        int nz = z;

        if (Math.abs(x) > Math.abs(z)) {
            if (x <= 0 && x <= -Outerlands.threshold) {
                nx = -Outerlands.threshold;
            } else if (x > 0 && x > Outerlands.threshold) {
                nx = Outerlands.threshold;
            }
        } else if (Math.abs(x) < Math.abs(z)) {
            if (z <= 0 && z <= -Outerlands.threshold) {
                nz = -Outerlands.threshold;
            } else if (z > 0 && z > Outerlands.threshold) {
                nz = Outerlands.threshold;
            }
        }

        return new BlockPos(nx, pos.getY(), nz);
    }

    public static BlockPos normalizeOuterPos(BlockPos pos) {
        if (!Meson.isModuleEnabled("strange:outerlands"))
            return pos;

        int x = pos.getX();
        int z = pos.getZ();
        int nx = x;
        int nz = z;

        if (Math.abs(x) > Math.abs(z)) {
            if (x <= 0 && x >= -Outerlands.threshold) {
                nx = -Outerlands.threshold;
            } else if (x > 0 && x < Outerlands.threshold) {
                nx = Outerlands.threshold;
            }
        } else if (Math.abs(x) < Math.abs(z)) {
            if (z <= 0 && z >= -Outerlands.threshold) {
                nz = -Outerlands.threshold;
            } else if (z > 0 && z < Outerlands.threshold) {
                nz = Outerlands.threshold;
            }
        }

        return new BlockPos(nx, pos.getY(), nz);
    }

    public static String calculateKnownRunes(PlayerEntity player, Entry entry) {
        return calculateKnownRunes(player, entry, false);
    }

    public static String calculateKnownRunes(PlayerEntity player, Entry entry, boolean showAll) {
        String known = "";

        IRunestonesCapability cap = Runestones.getCapability(player);
        List<Character> values = new ArrayList<>(RunestoneHelper.getRuneCharMap().values());
        StringBuilder assembled = new StringBuilder();

        char[] chars = entry.posref.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            boolean showRune = false;
            char c = chars[i];
            Character letter = RunestoneHelper.getRuneCharMap().get(c);
            if (values.contains(letter)) {
                int runeValue = values.indexOf(letter);
                if (showAll || cap.getDiscoveredTypes().contains(runeValue)) {
                    showRune = true;
                }
            }

            if (!showRune)
                letter = '?';

            assembled.append(letter);
        }
        known = assembled.toString();
        return known;
    }
}
