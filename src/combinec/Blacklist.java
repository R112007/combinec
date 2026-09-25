package combinec;

import arc.struct.IntMap;
import arc.struct.IntSet;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Log;
import mindustry.Vars;
import mindustry.type.SectorPreset;
import mindustry.type.UnitType;

/**
 * 黑名单：规定「某个地图的某波（的某个兵种组）不参与合体」。
 *
 * <p>匹配规则：
 * <ul>
 *   <li>地图名用 {@code Map.name()} —— 原版图是英文内部名（如 "Ground Zero"、"Furnace"），可用 "*" 通配所有地图；
 *       （地图没有可引用的常量对象，只能按名字匹配；单位类型则用对象引用匹配）</li>
 *   <li>波组按兵种识别，存的是 {@link UnitType} 对象，匹配是引用比较（==）：
 *       原版单位直接传 {@code UnitTypes.xxx}，模组单位用
 *       {@code Vars.content.units().find(u -> u.name.equals("..."))} 取出来再传；
 *   <li>波次传 {@code <=0} 表示「该图所有波次」。</li>
 * </ul>
 * 被豁免的单位照常以原版形态出场，其余单位照常合体。
 */
public final class Blacklist{
    /** map -> 完全不组合的波次集合 */
    private static final ObjectMap<String, IntSet> skipWaves = new ObjectMap<>();
    /** map -> (wave -> 不合体的兵种集合)（UnitType 引用比较，不受同名/本地化名影响） */
    private static final ObjectMap<String, IntMap<ObjectSet<UnitType>>> skipGroups = new ObjectMap<>();

    /** 整张地图的某一波（或所有波）完全不组合。 */
    public static void skipWave(String map, int wave){
        skipWaves.get(map.trim(), IntSet::new).add(wave);
    }

    /**
     * 重载：直接传战役扇区常量（{@code SectorPresets.groundZero} 等）。
     * 战役进图时 {@code state.map.name()} 就是预设的本地化名（World.java: 重建 map 时取
     * {@code sector.preset.localizedName}），所以登记时解析成本地化名做 key，
     * 与运行时地图名自然对上。
     */
    public static void skipWave(SectorPreset preset, int wave){
        if(preset == null){
            Log.warn("[u-wavemerge] 黑名单 skipWave 收到 null 扇区预设，已忽略");
            return;
        }
        skipWave(preset.localizedName, wave);
    }

    /** 重载：扇区常量 + 兵种对象。 */
    public static void skipGroup(SectorPreset preset, int wave, UnitType type){
        if(preset == null){
            Log.warn("[u-wavemerge] 黑名单 skipGroup 收到 null 扇区预设，已忽略");
            return;
        }
        skipGroup(preset.localizedName, wave, type);
    }

    /** 某一波里某个兵种组不组合，同波其余兵种照常合体。 */
    public static void skipGroup(String map, int wave, UnitType type){
        if(type == null){
            Log.warn("[u-wavemerge] 黑名单 skipGroup 收到 null 单位类型（地图 @ 第@波），已忽略", map, wave);
            return;
        }
        skipGroups.get(map.trim(), IntMap::new).get(wave, ObjectSet::new).add(type);
    }

    /**
     * 字符串重载：登记时按内部名/本地化名解析成 {@link UnitType} 对象。
     * 找不到会直接打警告并忽略 —— 宁可登记时暴露拼写错误，
     * 也不要像纯字符串匹配那样到游戏里静默失效。
     */
    public static void skipGroup(String map, int wave, String typeName){
        UnitType t = resolve(typeName);
        if(t == null){
            Log.warn("[u-wavemerge] 黑名单：找不到单位 '@'（地图 @ 第@波），已忽略；请检查拼写或该单位所属模组是否已安装", typeName, map, wave);
            return;
        }
        skipGroup(map, wave, t);
    }

    private static UnitType resolve(String name){
        String key = name.trim();
        for(UnitType t : Vars.content.units()){
            if(t.name.equalsIgnoreCase(key)) return t;
            if(t.localizedName != null && t.localizedName.equalsIgnoreCase(key)) return t;
        }
        return null;
    }

    public static void clear(){
        skipWaves.clear();
        skipGroups.clear();
    }

    /** 整波跳过？ */
    static boolean waveSkipped(String map, int wave){
        for(var e : skipWaves){
            if(!matchMap(e.key, map)) continue;
            if(e.value.contains(wave) || e.value.contains(0)) return true;
        }
        return false;
    }

    /** 该单位所在波组是否允许合体？ */
    static boolean groupAllowed(String map, int wave, UnitType type){
        if(type == null) return true;
        for(var e : skipGroups){
            if(!matchMap(e.key, map)) continue;
            for(var g : e.value){
                if(g.key != wave && g.key != 0) continue;
                if(g.value.contains(type)) return false; // 引用比较
            }
        }
        return true;
    }

    private static boolean matchMap(String key, String map){
        return key.equals("*") || key.equalsIgnoreCase(map == null ? "" : map.trim());
    }

    /** ===================== 在这里登记黑名单规则 ===================== */
    public static void loadDefaults(){
        // 整张图某一波不组合（地图可用扇区常量对象，也可用名字字符串）：
        //   skipWave(SectorPresets.furnace, 5);   // 「熔炉」第 5 波不组合
        //   skipWave("Ground Zero", 0);           // 波次 <=0 = 该图所有波都不组合
        //   skipWave(SectorPresets.saltFlats, 0); // 盐滩所有波都不组合

        // 某一波的某个兵种组不组合（传 UnitType 对象，引用匹配）：
        //   skipGroup("Glaciate", 8, UnitTypes.takikiwa);                    // 「冰川」第 8 波的重生组不合体
        //   skipGroup("*", 0, UnitTypes.eclipse);                            // 所有地图每一波的日蚀都不合体
        // 模组单位（登记时所有模组的内容已加载完，可以直接查）：
        //   UnitType myUnit = Vars.content.units().find(u -> u.name.equals("my-mod-unit"));
        //   skipGroup("Furnace", 10, myUnit);
    }
}
