package combinec;

import arc.struct.IntMap;
import arc.struct.ObjectMap;
import arc.util.Log;
import mindustry.type.SectorPreset;

/**
 * 「部分合体」配置：规定某个地图的某一波，从该波单位中<b>随机抽取 n 个</b>合体为组合巨兽，
 * 该波<b>其余单位保持原版独立出场</b>。
 *
 * <p>匹配规则与黑名单/双份配置一致：地图名用 {@code Map.name()}
 * （原版图为英文内部名，可用 "*" 通配所有地图）。
 *
 * <p>优先级：{@link Blacklist#skipWave} 整波豁免 > 本配置 > 整波合体 / {@link BothWaves} 双份
 * （同一波登记了部分合体时，整波合体与双份配置不再生效；n ≥ 该波单位数时等同整波合体，
 *  但仍不触发双份）。
 */
public final class PartialWaves{
    /** map -> (wave -> 合体单位数 n) */
    private static final ObjectMap<String, IntMap<Integer>> waves = new ObjectMap<>();

    /** 登记：map 地图的第 wave 波随机抽 n 个单位合体（n<2 时无意义，等于不合体）。 */
    public static void add(String map, int wave, int n){
        waves.get(map.trim(), IntMap::new).put(wave, n);
    }

    /** 重载：战役扇区常量（key 取预设本地化名，与运行时 state.map.name() 一致）。 */
    public static void add(SectorPreset preset, int wave, int n){
        if(preset == null){
            Log.warn("[u-wavemerge] PartialWaves.add 收到 null 扇区预设，已忽略");
            return;
        }
        add(preset.localizedName, wave, n);
    }

    public static void clear(){
        waves.clear();
    }

    /** 该波要抽几个合体；<=0 表示不启用部分合体。 */
    static int count(String map, int wave){
        int result = -1;
        for(var e : waves){
            if(!matchMap(e.key, map)) continue;
            for(var g : e.value){
                if(g.key == wave || g.key == 0){
                    result = Math.max(result, g.value);
                }
            }
        }
        return result;
    }

    private static boolean matchMap(String key, String map){
        return key.equals("*") || key.equalsIgnoreCase(map == null ? "" : map.trim());
    }

    /** ===================== 在这里登记部分合体规则 ===================== */
    public static void loadDefaults(){
        // 例：add(SectorPresets.furnace, 10, 3);   // 「熔炉」第 10 波：随机抽 3 个合体，其余独立出场
        //     add("*", 5, 4);           // 所有地图第 5 波都抽 4 个合体
    }
}
