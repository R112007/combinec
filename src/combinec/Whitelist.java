package combinec;

import arc.struct.ObjectSet;
import arc.util.Log;
import mindustry.type.SectorPreset;

/**
 * 白名单模式：默认关闭 —— 所有原版地图每一波都合体（模组默认行为）。
 * 调用 {@link #enable()} 后反转：只有这里登记的地图才合体，其余地图保持原版波次。
 *
 * <p>登记方式与黑名单一致：名字字符串（自建图用 {@code Map.name()}）、
 * 战役扇区常量（{@code SectorPresets.xxx}），或 "*" 通配所有地图。
 */
public final class Whitelist{
    private static final ObjectSet<String> maps = new ObjectSet<>();
    private static boolean on;

    /** 启用白名单模式（不调用本方法则维持默认：所有原版图都合体）。 */
    public static void enable(){
        on = true;
    }

    public static boolean enabled(){
        return on;
    }

    public static void add(String map){
        maps.add(map.trim());
    }

    public static void add(SectorPreset preset){
        if(preset == null){
            Log.warn("[u-wavemerge] Whitelist.add 收到 null 扇区预设，已忽略");
            return;
        }
        add(preset.localizedName);
    }

    public static void clear(){
        maps.clear();
        on = false;
    }

    static boolean contains(String map){
        for(String key : maps){
            if(key.equals("*") || key.equalsIgnoreCase(map == null ? "" : map.trim())) return true;
        }
        return false;
    }

    /** ===================== 在这里登记白名单 ===================== */
    public static void loadDefaults(){
        // 默认什么都不用写：所有原版地图每一波都合体。
        // 若想让"只有某些图合体、其它图保持原版"：
        //   enable();                     // 先开启白名单模式
        //   add(SectorPresets.furnace);   // 只有「熔炉」合体
        //   add("我的自建图");
    }
}
