import os, pathlib, subprocess, zipfile
out = pathlib.Path("build/damage-diagnostics")
out.mkdir(parents=True, exist_ok=True)
wanted = ["net.minecraft.world.entity.player.Player", "net.minecraft.server.level.ServerPlayer", "net.minecraft.world.entity.Entity", "net.minecraft.world.entity.LivingEntity", "net.minecraft.world.entity.monster.spider.CaveSpider", "net.minecraft.world.entity.Mob"]
for jar in pathlib.Path(os.environ["HOME"] + "/.gradle/caches").rglob("*.jar"):
    try:
        with zipfile.ZipFile(jar) as z:
            if "net/minecraft/world/entity/player/Player.class" not in z.namelist(): continue
        for cls in wanted:
            result = subprocess.run(["javap", "-p", "-c", "-classpath", str(jar), cls], capture_output=True, text=True)
            (out / (cls.rsplit(".", 1)[-1] + ".txt")).write_text(result.stdout + result.stderr)
        print("Inspected Minecraft classes in", jar)
        break
    except zipfile.BadZipFile: pass
else: raise RuntimeError("Minecraft jar not found")
