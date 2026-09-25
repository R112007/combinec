package combinec;

import arc.struct.IntSet;
import arc.struct.ObjectMap;
import arc.util.Log;
import mindustry.type.SectorPreset;

/**
 * 「双份波次」配置：规定某个地图的某一波，在合体巨兽之外，
 * 把该波原版阵容（全部独立单位）再额外刷新一遍 —— 合体和独立单位同时出场。
 *
 * <p>匹配规则与黑名单一致：地图名用 {@code Map.name()}（原版图为英文内部名，
 * 可用 "*" 通配所有地图）；波次 {@code <=0} 表示该图所有波次。
 *
 * <p>优先级：{@link Blacklist#skipWave} 整波豁免优先于本配置
 * （整波都不合体了，自然不存在"合体+独立"双份）。
 */
public final class BothWaves{
    /** map -> 双份出场的波次集合 */
    private static final ObjectMap<String, IntSet> waves = new ObjectMap<>();

    /** 登记：map 地图的第 wave 波（wave<=0 = 所有波）双份出场。 */
    public static void add(String map, int wave){
        waves.get(map.trim(), IntSet::new).add(wave);
    }

    /** 重载：战役扇区常量（key 取预设本地化名，与运行时 state.map.name() 一致）。 */
    public static void add(SectorPreset preset, int wave){
        if(preset == null){
            Log.warn("[u-wavemerge] BothWaves.add 收到 null 扇区预设，已忽略");
            return;
        }
        add(preset.localizedName, wave);
    }

    public static void clear(){
        waves.clear();
    }

    static boolean contains(String map, int wave){
        for(var e : waves){
            if(!matchMap(e.key, map)) continue;
            if(e.value.contains(wave) || e.value.contains(0)) return true;
        }
        return false;
    }

    private static boolean matchMap(String key, String map){
        return key.equals("*") || key.equalsIgnoreCase(map == null ? "" : map.trim());
    }

    /** ===================== 在这里登记双份波次规则 ===================== */
    public static void loadDefaults(){
        // 例：add(SectorPresets.furnace, 10);   // 「熔炉」第 10 波：合体巨兽 + 全部独立单位同时出场
        //     add("*", 0);           // 所有地图每一波都双份（0 = 所有波次，慎用，难度翻倍）
    }
}
