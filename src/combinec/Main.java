package combinec;

import arc.util.Log;
import mindustry.mod.Mod;

/** u（combineunit）附属：波次自动合体。 */
public class Main extends Mod{

    @Override
    public void init(){
        // 代码规定的"某地图某波组不组合"黑名单，在这里登记
        Blacklist.loadDefaults();
        // "某波同时刷合体+全部独立单位"的双份配置，在这里登记
        BothWaves.loadDefaults();
        // "随机抽 n 个合体、其余独立出场"的部分合体配置，在这里登记
        PartialWaves.loadDefaults();
        // 白名单模式（默认关闭：所有原版图都合体；enable() 后只有登记的图合体）
        Whitelist.loadDefaults();

        WaveMerger.register();

        if(UHook.available()){
            Log.info("[u-wavemerge] 已加载：每波进攻单位将自动合体为组合巨兽（黑名单见 Blacklist.loadDefaults）");
        }
    }
}
