package combinec;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.gen.Unit;

/**
 * 每波合体：刷怪口每出生一只波次单位就缓存进 buffer；
 * 原版 runWave() 的顺序是「刷兵 → wave++ → 发 WaveEvent」，
 * 所以 WaveEvent 触发时 buffer 里恰好是本波全体成员 —— 过滤黑名单后直接合体。
 *
 * <p>合体走 u 的 {@link combineunit.units.UnitComboMerge#mergeSelected(Seq)}，服务器权威，
 * 联机时客户端通过 u 自己的 Call.unitDespawn 通道同步，天然一致。
 */
public final class WaveMerger{
    /** 总开关。 */
    public static boolean enabled = true;
    /** 只对原版地图生效（map.mod == null 的图：官方图 + 玩家自建图；关掉则所有图生效）。 */
    public static boolean vanillaMapsOnly = true;
    /** 一波里至少多少个可合体单位才合体，不足则该波保持原版行为。 */
    public static int minMembers = 2;

    /** 本波（上一个 WaveEvent 以来）刷怪口出生的单位。 */
    private static final Seq<Unit> buffer = new Seq<>();
    /** 去重，防同一单位进两次。 */
    private static final ObjectSet<Unit> inBuffer = new ObjectSet<>();

    public static void register(){
        Events.on(WorldLoadEvent.class, e -> clearBuffer());
        Events.on(GameOverEvent.class, e -> clearBuffer());

        Events.on(UnitSpawnEvent.class, e -> {
            if(!canWork()) return;
            Unit u = e.unit;
            if(u != null && u.isAdded() && inBuffer.add(u)) buffer.add(u);
        });

        Events.on(WaveEvent.class, e -> onWave());
    }

    private static void onWave(){
        // 无论是否处理，先取出并清空：buffer 生命周期严格限定在两个 WaveEvent 之间
        Seq<Unit> pending = new Seq<>(buffer);
        clearBuffer();

        // 客户端不做任何结算（u 的合体是服务器权威的）
        if(Vars.net.client() || !canWork()) return;
        if(!Vars.state.rules.waves) return;

        int wave = Vars.state.wave;
        String map = mapName();

        // 规则①：整张图某一波不组合
        if(Blacklist.waveSkipped(map, wave)) return;

        // 规则②：某波组（兵种）不组合；同时过一遍 u 的可合体判定
        Seq<Unit> members = new Seq<>();
        for(Unit u : pending){
            if(u == null || !u.isAdded() || u.dead()) continue;
            if(!UHook.groupable(u)) continue;
            if(!Blacklist.groupAllowed(map, wave, u.type)) continue;
            members.add(u);
        }

        if(members.size < minMembers) return;

        // 配置项：随机抽 n 个合体，其余独立出场（部分合体优先于整波合体/双份）
        int pick = PartialWaves.count(map, wave);
        if(pick > 0){
            // Fisher–Yates 洗牌（仅服务器执行，结果经 u 的服务器权威合体同步给客户端）
            for(int i = members.size - 1; i > 0; i--){
                members.swap(i, Mathf.random(i));
            }
            Seq<Unit> chosen = new Seq<>();
            for(int i = 0; i < Math.min(pick, members.size); i++){
                chosen.add(members.get(i));
            }
            Unit mega = UHook.mergeSelected(chosen);
            if(mega != null){
                Log.info("[u-wavemerge] @ 第@波：随机抽取 @/@ 个单位合体，其余 @ 个独立出场",
                    map, wave, chosen.size, members.size, members.size - chosen.size);
            }
            // 没被抽中的单位保持原版独立状态，直接结束本波处理
            return;
        }

        Unit mega = UHook.mergeSelected(members);
        if(mega != null){
            Log.info("[u-wavemerge] @ 第@波：@ 个单位合体为组合巨兽（血量 @/@）",
                map, wave, members.size, (int)mega.health, (int)mega.maxHealth);

            // 配置项：该波"双份"——在合体之外，把该波原版阵容再刷新一遍（独立单位同时出场）
            if(BothWaves.contains(map, wave)){
                int spawned = 0;
                for(Unit u : members){
                    if(u == null || u.type == null) continue;
                    // 成员此刻已收进巨兽体内，坐标/类型数据仍有效；复制体作为独立单位重新出场
                    Unit copy = u.type.spawn(u.team(), u.x + Mathf.range(6f), u.y + Mathf.range(6f));
                    if(copy != null) spawned++;
                }
                Log.info("[u-wavemerge] @ 第@波：双份出场，额外刷新 @ 个独立单位", map, wave, spawned);
            }
        }
    }

    public static boolean canWork(){
        return enabled && UHook.available() && Vars.state.isGame() && Vars.state.map != null
            && (!vanillaMapsOnly || Vars.state.map.mod == null)
            && (!Whitelist.enabled() || Whitelist.contains(mapName()));
    }

    public static String mapName(){
        return Vars.state.map == null ? "" : Vars.state.map.name();
    }

    private static void clearBuffer(){
        buffer.clear();
        inBuffer.clear();
    }
}
