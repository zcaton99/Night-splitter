import pathlib, os, zipfile, subprocess
p=pathlib.Path("build/api-inspect");p.mkdir(parents=True,exist_ok=True)
for j in pathlib.Path(os.environ["HOME"]+"/.gradle/caches").rglob("*.jar"):
 try:
  with zipfile.ZipFile(j) as z:
   if "net/minecraft/world/entity/monster/creeper/Creeper.class" not in z.namelist(): continue
  for cls in ["net.minecraft.world.entity.monster.creeper.Creeper","net.minecraft.world.entity.player.Player","net.minecraft.world.entity.LivingEntity"]:
   s=subprocess.run(["javap","-p","-c","-classpath",str(j),cls],capture_output=True,text=True)
   (p/(cls.split(".")[-1]+".txt")).write_text(s.stdout+s.stderr)
  break
 except zipfile.BadZipFile: pass
