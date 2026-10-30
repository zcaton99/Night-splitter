"""Launch a real dedicated server and two native clients; preserve logs on every failure."""
import json,os,pathlib,subprocess,time,signal,sys,hashlib,uuid
root=pathlib.Path(__file__).resolve().parents[1]
base=root/'build/multiplayer'; launches=json.loads((base/'launch.json').read_text()); processes=[];handles=[]
serverdir=base/'server';(serverdir/'eula.txt').write_text('eula=true\n')
(serverdir/'server.properties').write_text('online-mode=false\nwhite-list=true\nenforce-whitelist=false\npause-when-empty-seconds=-1\nserver-ip=127.0.0.1\nserver-port=25575\nview-distance=3\nsimulation-distance=2\nmax-players=4\nspawn-protection=0\nlevel-type=minecraft:flat\npvp=true\nallow-flight=true\nenforce-secure-profile=false\nmotd=Creeperborn automated multiplayer test\n')
(serverdir/'whitelist.json').write_text(json.dumps([{'uuid':str(uuid.UUID(bytes=hashlib.md5(('OfflinePlayer:'+name).encode()).digest(),version=3)),'name':name} for name in ('LanAlpha','LanBeta')]))
for folder in ('alpha','beta'):
 (base/folder/'options.txt').write_text('maxFps:15\nrenderDistance:2\nsimulationDistance:5\npauseOnLostFocus:false\nshowSubtitles:false\nparticles:2\nsoundCategory_master:0.0\nenableVsync:false\n')
def start(config,headless=False):
 h=open(base/(config['name']+'.log'),'w');handles.append(h)
 cmd=config['command'];cmd=['xvfb-run','-a','-s','-screen 0 800x600x24',*cmd] if headless else cmd
 p=subprocess.Popen(cmd,cwd=config['directory'],stdout=h,stderr=subprocess.STDOUT,env={**os.environ,'LIBGL_ALWAYS_SOFTWARE':'true','SDL_AUDIODRIVER':'dummy','LP_NUM_THREADS':'2'},start_new_session=True);processes.append(p);return p
try:
 server=start(launches[0]);deadline=time.monotonic()+120
 while time.monotonic()<deadline:
  if server.poll() is not None:raise RuntimeError('Dedicated server exited before ready')
  if 'CREEPERBORN_MULTIPLAYER_SERVER_READY' in (base/'runMultiplayerServer.log').read_text():break
  time.sleep(.5)
 else:raise TimeoutError('Dedicated server did not start')
 start(launches[1],True);start(launches[2],True);deadline=time.monotonic()+480
 while time.monotonic()<deadline:
  if all(p.poll() is not None for p in processes):break
  if any(p.poll() not in (None,0) for p in processes):raise RuntimeError('A native multiplayer process failed')
  if server.poll() is not None and 'CREEPERBORN_NATIVE_MULTIPLAYER_PASSED' not in (base/'runMultiplayerServer.log').read_text():raise RuntimeError('Dedicated server failed its multiplayer assertions; inspect server log')
  time.sleep(1)
 else:raise TimeoutError('Native multiplayer scenario exceeded eight minutes')
 for name,marker in [('runMultiplayerServer','CREEPERBORN_NATIVE_MULTIPLAYER_PASSED'),('runMultiplayerAlpha','CREEPERBORN_NATIVE_MULTIPLAYER_CLIENT_PASSED'),('runMultiplayerBeta','CREEPERBORN_NATIVE_MULTIPLAYER_CLIENT_PASSED')]:
  text=(base/(name+'.log')).read_text()
  if marker not in text:raise AssertionError(name+' did not complete its assertions')
 report=json.loads((serverdir/'multiplayer-report.json').read_text());assert report['passed'],report
 print(json.dumps(report,indent=2));print('CREEPERBORN_MULTIPLAYER_CI_PASSED')
finally:
 for p in processes:
  if p.poll() is None:
   try:os.killpg(p.pid,signal.SIGTERM)
   except ProcessLookupError:pass
 for h in handles:h.close()
