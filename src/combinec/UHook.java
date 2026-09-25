package combinec;

import arc.struct.Seq;
import arc.util.Log;
import combineunit.units.UnitComboDamage;
import combineunit.units.UnitComboMerge;
import mindustry.gen.Unit;

/**
 * 直接依赖 u（combineunit）模组（mod.hjson 里已声明 dependencies），编译时把 u 的 jar 加进
 * compileOnly 依赖即可，不用反射。
 *
 * <p>这里只做一层薄封装：UHook.available() 在没装 u / 类缺失时兜底返回 false，
 * 让整个附属模组静默失效而不是把游戏崩掉（正常装了 combineunit 不会走到兜底分支）。
 */
public final class UHook{
    private static boolean checked, ok;

    public static boolean available(){
        if(!checked){
            checked = true;
            try{
                // 触发一次类加载，确认 u 真的在
                Class.forName("combineunit.units.UnitComboMerge");
                ok = true;
            }catch(Throwable t){
                Log.warn("[u-wavemerge] 未检测到 u（combineunit）模组，波次合体功能不可用");
            }
        }
        return ok;
    }

    /** u 的可合体判定（排除核心机、巨兽、不可命中单位等）。 */
    public static boolean groupable(Unit u){
        return UnitComboDamage.groupable(u);
    }

    /** 服务器权威合体；u 内部在客户端会直接返回 null。 */
    public static Unit mergeSelected(Seq<Unit> units){
        return UnitComboMerge.mergeSelected(units);
    }
}
