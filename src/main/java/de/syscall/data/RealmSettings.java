package de.syscall.data;

public class RealmSettings {

    private boolean publicRealm;
    private boolean pvpEnabled;
    private boolean mobSpawning;
    private boolean animalSpawning;
    private boolean fireSpread;
    private boolean explosions;
    private boolean leafDecay;
    private boolean weatherChanges;
    private boolean dayNightCycle;
    private int timeOfDay;
    private String biome;

    public RealmSettings() {
        this.publicRealm = false;
        this.pvpEnabled = false;
        this.mobSpawning = true;
        this.animalSpawning = true;
        this.fireSpread = false;
        this.explosions = false;
        this.leafDecay = true;
        this.weatherChanges = true;
        this.dayNightCycle = true;
        this.timeOfDay = 6000;
        this.biome = "PLAINS";
    }

    public boolean isPublic() {
        return publicRealm;
    }

    public void setPublic(boolean publicRealm) {
        this.publicRealm = publicRealm;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public boolean isMobSpawning() {
        return mobSpawning;
    }

    public void setMobSpawning(boolean mobSpawning) {
        this.mobSpawning = mobSpawning;
    }

    public boolean isAnimalSpawning() {
        return animalSpawning;
    }

    public void setAnimalSpawning(boolean animalSpawning) {
        this.animalSpawning = animalSpawning;
    }

    public boolean isFireSpread() {
        return fireSpread;
    }

    public void setFireSpread(boolean fireSpread) {
        this.fireSpread = fireSpread;
    }

    public boolean isExplosions() {
        return explosions;
    }

    public void setExplosions(boolean explosions) {
        this.explosions = explosions;
    }

    public boolean isLeafDecay() {
        return leafDecay;
    }

    public void setLeafDecay(boolean leafDecay) {
        this.leafDecay = leafDecay;
    }

    public boolean isWeatherChanges() {
        return weatherChanges;
    }

    public void setWeatherChanges(boolean weatherChanges) {
        this.weatherChanges = weatherChanges;
    }

    public boolean isDayNightCycle() {
        return dayNightCycle;
    }

    public void setDayNightCycle(boolean dayNightCycle) {
        this.dayNightCycle = dayNightCycle;
    }

    public int getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(int timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public String getBiome() {
        return biome;
    }

    public void setBiome(String biome) {
        this.biome = biome;
    }
}