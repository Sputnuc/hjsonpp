package hjsonpp.expand.blocks.defense;

import arc.Core;
import arc.audio.Sound;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import hjsonpp.expand.meta.AdditionalStats;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.entities.Mover;
import mindustry.entities.Sized;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.blocks.defense.turrets.ItemTurret;

import java.util.Objects;

import static mindustry.Vars.fogControl;
import static mindustry.Vars.tilesize;

//Extendable class for ItemTurret with different fire modes.
public class ModeTurret extends ItemTurret{
    public static TextureRegion defaultIcon;

    public TurretMode defaultMode = new TurretMode("default");

    //Can be null. In constructor, it's just adding DefaultMode
    public Seq<TurretMode> turretModes = new Seq<>();

    public void addModes(TurretMode...tmodes){
        turretModes.add(tmodes);
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("items");
        addBar("currentMode", (ModeTurretBuild entity) ->
                new Bar(
                        () -> Core.bundle.format("turret.mode." + entity.getMode().name),
                        () -> entity.getMode().barColor,
                        () -> 1f
                )
        );
    }

    @Override
    public void load(){
        super.load();
        for(TurretMode t : turretModes){
            t.load();
        }
    }


    public ModeTurret(String name) {
        super(name);
        sync = true;
        saveConfig = true;
        copyConfig = true;
        configurable = true;
        config(Integer.class, (ModeTurretBuild e, Integer idx) -> {
            e.currentTurretMode = idx;
        });
    }

    public static class TurretMode{
        @Nullable
        public ObjectMap<Item, BulletType> modeAmmoTypes;

        //Additional description for stats (Write a bundle-key)
        @Nullable
        public String description;

        //If true, the stats will display "Base turret stats."
        public boolean basicStats = false;

        //"modname"
        public String path = "hjsonpp";
        public String name = "";
        public Color barColor = Color.white;
        public float accuracyMultiplier = 1;
        public float reloadMultiplier = 1;
        public float rotateSpeedMultiplier = 1;
        public float rangeChange = 0;
        public float targetIntervalMultiplier = 1;
        public float ammoUseMultiplier = 1;
        public int ammoUseChange = 0;
        public Sound modeSound = Sounds.none;

        @Nullable
        public ShootPattern modePattern;

        public TextureRegion icon;
        public void load(){
            icon = Core.atlas.find(path+"-turret-mode-" + this.name);
        }

        public TextureRegion icon(){
            return icon;
        }
        public TurretMode() {}

        public TurretMode(String name){
            this.name = name;
        }

        public TurretMode(String name, float accuracyM, float reloadM, float rotateSpdM){
            this.name = name;
            this.accuracyMultiplier = accuracyM;
            this.reloadMultiplier = reloadM;
            this.rotateSpeedMultiplier = rotateSpdM;
        }

        public TurretMode(String name, float accuracyM, float reloadM, float rotateSpdM, Color barColor){
            this.barColor = barColor;
            this.name = name;
            this.accuracyMultiplier = accuracyM;
            this.reloadMultiplier = reloadM;
            this.rotateSpeedMultiplier = rotateSpdM;
        }
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(AdditionalStats.turretMode, table -> {
            table.row();
            for(TurretMode t : turretModes){
                table.table(Styles.grayPanel, tab->{
                    tab.left();
                    tab.image(t.icon()).left();
                    tab.table(st ->{
                        st.left();
                        createTextStats(st, t);
                    }).grow();
                }).grow().pad(0, 0, 12, 0).row();
            }
        });
    }

    public void createTextStats(Table t, TurretMode mode){
        t.add("«" + Core.bundle.format("turret.mode." + mode.name) +"»").left().pad(0, 0, 10, 0).color(mode.barColor).row();
        Color negativeColor = Color.valueOf("ff6e6e");
        Color positiveColor = Pal.accent;
        if(mode.basicStats) {
            t.add(Core.bundle.format("stat-tur-mode-basic-stats")).row();
            return;
        }
        if(mode.reloadMultiplier != 1)  t.add(Core.bundle.format("stat-tur-mode-reload-multiplier", mode.reloadMultiplier * 100)).color(mode.reloadMultiplier > 1 ? positiveColor : negativeColor).left().row();
        if(mode.accuracyMultiplier != 1) t.add(Core.bundle.format("stat-tur-mode-accuracy-multiplier", mode.accuracyMultiplier * 100)).color(mode.accuracyMultiplier > 1 ? positiveColor : negativeColor).left().row();
        if(mode.rangeChange != 0) t.add(Core.bundle.format(mode.rangeChange > 0 ? "stat-tur-mode-range-change-positive" : "stat-tur-mode-range-change-negative", mode.rangeChange / tilesize)).color(mode.rangeChange > 0 ? positiveColor : negativeColor).left().row();
        if(mode.rotateSpeedMultiplier != 1) t.add(Core.bundle.format("stat-tur-mode-rotate-speed-multiplier", mode.rotateSpeedMultiplier * 100)).color(mode.rotateSpeedMultiplier > 1 ? positiveColor : negativeColor).left().row();
        if(mode.targetIntervalMultiplier != 1) t.add(Core.bundle.format("stat-tur-mode-target-switch-multiplier", mode.targetIntervalMultiplier)).color(mode.targetIntervalMultiplier < 1 ? positiveColor : negativeColor).left().row();

        if(Core.bundle.has("turret.mode." + mode.name + ".description")) t.add(Core.bundle.get("turret.mode." + mode.name + ".description")).left().padTop(8).row();
    }

    public class ModeTurretBuild extends ItemTurretBuild{

        public int currentTurretMode;

        public TurretMode getMode(){
            return !turretModes.isEmpty() &  turretModes.get(currentTurretMode) != null ? turretModes.get(currentTurretMode) : defaultMode;
        }

        float lastRangeChange;

        @Override
        public float range(){
            if(peekAmmo() != null){
                return range + peekAmmo().rangeChange + getMode().rangeChange;
            }
            return range;
        }

        @Override
        public void updateTile(){
            if(!validateTarget()) target = null;

            if(soundLoop != null){
                soundLoop.update(x, y, shouldActiveSound(), activeSoundVolume());
            }

            float warmupTarget = (isShooting() && canConsume()) || charging() ? 1f : 0f;
            if(warmupTarget > 0 && !isControlled()){
                warmupHold = 1f;
            }
            if(warmupHold > 0f){
                warmupHold -= Time.delta / warmupMaintainTime;
                warmupTarget = 1f;
            }

            if(linearWarmup){
                shootWarmup = Mathf.approachDelta(shootWarmup, warmupTarget, shootWarmupSpeed * (warmupTarget > 0 ? efficiency : 1f));
            }else{
                shootWarmup = Mathf.lerpDelta(shootWarmup, warmupTarget, shootWarmupSpeed * (warmupTarget > 0 ? efficiency : 1f));
            }

            wasShooting = false;

            curRecoil = Mathf.approachDelta(curRecoil, 0, 1 / recoilTime);
            if(recoils > 0){
                if(curRecoils == null) curRecoils = new float[recoils];
                for(int i = 0; i < recoils; i++){
                    curRecoils[i] = Mathf.approachDelta(curRecoils[i], 0, 1 / recoilTime);
                }
            }
            heat = Mathf.approachDelta(heat, 0, 1 / cooldownTime);
            charge = charging() ? Mathf.approachDelta(charge, 1, 1 / shoot.firstShotDelay) : 0;

            unit.tile(this);
            unit.rotation(rotation);
            unit.team(team);
            recoilOffset.trns(rotation, -Mathf.pow(curRecoil, recoilPow) * recoil);

            if(logicControlTime > 0){
                logicControlTime -= Time.delta;
            }

            if(heatRequirement > 0){
                heatReq = calculateHeat(sideHeat);
            }

            if(rotate){
                //sync underlying rotation; 0-3 rotation is a shadowed field
                ((Building)this).rotation = Mathf.mod(Mathf.round(rotation / 90f), 4);
            }

            //turret always reloads regardless of whether it's targeting something
            if(reloadWhileCharging || !charging()){
                updateReload();
                updateCooling();
            }

            if(Vars.state.rules.fog){
                float newRange = hasAmmo() ? peekAmmo().rangeChange : 0f;
                if(newRange != lastRangeChange){
                    lastRangeChange = newRange;
                    fogControl.forceUpdate(team, this);
                }
            }

            if(activationTimer > 0){
                activationTimer -= Time.delta;
                return;
            }

            if(hasAmmo()){
                if(Float.isNaN(reloadCounter)) reloadCounter = 0;

                if(timer(timerTarget, (target != null ? newTargetInterval : targetInterval) * getMode().targetIntervalMultiplier)){
                    findTarget();
                }

                if(validateTarget()){
                    boolean canShoot;

                    if(isControlled()){ //player behavior
                        targetPos.set(unit.aimX(), unit.aimY());
                        canShoot = unit.isShooting();
                    }else if(logicControlled()){ //logic behavior
                        canShoot = logicShooting;
                    }else{ //default AI behavior
                        targetPosition(target);

                        if(Float.isNaN(rotation)) rotation = 0;
                        canShoot = within(target, range() + (target instanceof Sized hb ? hb.hitSize()/1.9f : 0f));
                    }

                    if(!isControlled()){
                        unit.aimX(targetPos.x);
                        unit.aimY(targetPos.y);
                    }

                    float targetRot = angleTo(targetPos);

                    if(shouldTurn()){
                        turnToTarget(targetRot);
                    }

                    if(!alwaysShooting && Angles.angleDist(rotation, targetRot) < shootCone && canShoot){
                        wasShooting = true;
                        updateShooting();
                    }
                }else{
                    target = null;
                }

                if(alwaysShooting){
                    wasShooting = true;
                    updateShooting();
                }
            }
        }

        @Override
        protected void turnToTarget(float targetRot){
            rotation = Angles.moveToward(rotation, targetRot, rotateSpeed * getMode().rotateSpeedMultiplier * delta() * potentialEfficiency);
        }

        @Override
        protected void updateShooting(){

            if(reloadCounter >= reload && !charging() && shootWarmup >= minWarmup){
                BulletType type = peekAmmo();

                shoot(type);

                reloadCounter %= reload;
            }
        }

        @Override
        protected void updateReload(){
            reloadCounter += delta() * ammoReloadMultiplier() * baseReloadSpeed() * getMode().reloadMultiplier;

            //cap reload for visual reasons
            reloadCounter = Math.min(reloadCounter, reload);
        }

        @Override
        protected void bullet(BulletType type, float xOffset, float yOffset, float angleOffset, Mover mover){
            queuedBullets --;

            if(dead || (!consumeAmmoOnce && !hasAmmo())) return;

            float
                    xSpread = Mathf.range(xRand),
                    bulletX = x + Angles.trnsx(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
                    bulletY = y + Angles.trnsy(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
                    shootAngle = rotation + angleOffset + Mathf.range(inaccuracy / getMode().accuracyMultiplier + type.inaccuracy);

            float lifeScl = type.scaleLife ? Mathf.clamp((1 + scaleLifetimeOffset) * Mathf.dst(bulletX, bulletY, targetPos.x, targetPos.y) / type.range, minRange() / type.range, range() / type.range) : range() / type.range;


            //TODO aimX / aimY for multi shot turrets?
            handleBullet(type.create(this, team, bulletX, bulletY, shootAngle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd), lifeScl, null, mover, targetPos.x, targetPos.y), xOffset, yOffset, shootAngle - rotation);

            (shootEffect == null ? type.shootEffect : shootEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
            (smokeEffect == null ? type.smokeEffect : smokeEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
            (getMode().modeSound != Sounds.none ? getMode().modeSound :  type.shootSound != Sounds.none ? type.shootSound : shootSound).at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax), shootSoundVolume);

            ammoUseEffect.at(
                    x - Angles.trnsx(rotation, ammoEjectBack),
                    y - Angles.trnsy(rotation, ammoEjectBack),
                    rotation * Mathf.sign(xOffset)
            );

            if(shake > 0){
                Effect.shake(shake, shake, this);
            }

            curRecoil = 1f;
            if(recoils > 0){
                curRecoils[barrelCounter % recoils] = 1f;
            }
            heat = 1f;
            totalShots++;

            if(!consumeAmmoOnce){
                useAmmo();
            }
        }

        @Override
        public BulletType useAmmo(){
            if(cheating()) return peekAmmo();
            TurretMode mode = getMode();
            AmmoEntry entry = ammo.peek();

            if(mode == null) {
                entry.amount -= ammoPerShot;
            } else if(mode.ammoUseChange == 0) {
                entry.amount -= Math.round(ammoPerShot * mode.ammoUseMultiplier);
            } else entry.amount -= ammoPerShot + mode.ammoUseChange;

            if(entry.amount <= 0) ammo.pop();
            if(mode == null) {
                totalAmmo -= ammoPerShot;
            } else if(mode.ammoUseChange == 0) {
                totalAmmo -= Math.round(ammoPerShot * mode.ammoUseMultiplier);
            } else totalAmmo -= ammoPerShot + mode.ammoUseChange;

            totalAmmo = Math.max(totalAmmo, 0);
            return entry.type();
        }

        @Override
        public boolean hasAmmo(){
            TurretMode mode = getMode();
            int ammoUseNow;
            if(mode == null) {
                ammoUseNow = ammoPerShot;
            } else if(mode.ammoUseChange == 0) {
                ammoUseNow = Math.round(ammoPerShot * mode.ammoUseMultiplier);
            } else ammoUseNow = ammoPerShot + mode.ammoUseChange;

            if(ammo.size >= 2 && ammo.peek().amount < ammoUseNow){
                for(int i = 0; i < ammo.size; i ++){
                    if(ammo.get(i).amount >= ammoUseNow){
                        ammo.swap(ammo.size - 1, i);
                        break;
                    }
                }
            }
            if(!canConsume()) return false;
            return ammo.size > 0 && (ammo.peek().amount >= ammoUseNow || cheating());
        }

        @Override
        public BulletType peekAmmo(){
            TurretMode mode = getMode();
            if(mode.modeAmmoTypes != null && !ammo.isEmpty()){
                Item item = ((ItemEntry)ammo.peek()).item;
                BulletType custom = mode.modeAmmoTypes.get(item);
                if(custom != null) return custom;
            }
            return super.peekAmmo();
        }

        @Override
        protected void shoot(BulletType type){
            float
                    bulletX = x + Angles.trnsx(rotation - 90, shootX, shootY),
                    bulletY = y + Angles.trnsy(rotation - 90, shootX, shootY);

            if(shoot.firstShotDelay > 0){
                chargeSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax));
                type.chargeEffect.at(bulletX, bulletY, rotation);
            }

            TurretMode mode = getMode();

            ShootPattern pattern = mode.modePattern != null ? mode.modePattern : shoot;

            pattern.shoot(barrelCounter, (xOffset, yOffset, angle, delay, mover) -> {
                queuedBullets++;
                int barrel = barrelCounter;

                if(delay > 0f){
                    Time.run(delay, () -> {
                        //hack: make sure the barrel is the same as what it was when the bullet was queued to fire
                        int prev = barrelCounter;
                        barrelCounter = barrel;
                        bullet(type, xOffset, yOffset, angle, mover);
                        barrelCounter = prev;
                    });
                }else{
                    bullet(type, xOffset, yOffset, angle, mover);
                }
            }, () -> barrelCounter++);

            if(consumeAmmoOnce){
                useAmmo();
            }
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.image(Tex.whiteui, Pal.gray).height(4f).growX().row();
            table.table(buttons ->{
                for(int i = 0; i < turretModes.size; i++){
                    TurretMode tm = turretModes.get(i);
                    int bid = i;
                    boolean isCurrent = currentTurretMode == i;
                    if(i % 5 == 0) table.row();
                    table.button(b ->{
                        b.center();
                        b.image(tm.icon());
                        b.update(() -> b.setChecked(isCurrent));
                    }, Styles.clearNoneTogglei, ()->{
                        configure(bid);
                        deselect();
                    }).growX().size(47.5f).tooltip(Core.bundle.format("fire.turret.mode."  + tm.name));
                }}).grow();
        }

        @Override
        public void write(Writes write) {
            super.write(write);

            write.i(currentTurretMode);

        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);

            currentTurretMode = read.i();
        }

        @Override
        public Object config(){
            return currentTurretMode;
        }

    }
}