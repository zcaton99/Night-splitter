import pathlib, os, zipfile, subprocess
p=pathlib.Path("build/api-inspect");p.mkdir(parents=True,exist_ok=True)
for j in pathlib.Path(os.environ["HOME"]+"/.gradle/caches").rglob("*.jar"):
 try:
  with zipfile.ZipFile(j) as z:
   if "net/minecraft/world/entity/player/Player.class" not in z.namelist(): continue
   names=z.namelist()
  wanted=[n for n in names if n.endswith(("/Creeper.class","/EntityTypes.class","/Player.class","/LivingEntity.class","/ServerLevel.class")) and n.startswith("net/minecraft/")]
  for n in wanted:
   cls=n[:-6].replace("/",".")
   s=subprocess.run(["javap","-p","-c","-classpath",str(j),cls],capture_output=True,text=True)
   (p/(cls.split(".")[-1]+".txt")).write_text(s.stdout+s.stderr)
  (p/"classes.txt").write_text("\n".join(wanted))
  break
 except zipfile.BadZipFile: pass
else: raise RuntimeError("Minecraft classes not found")
