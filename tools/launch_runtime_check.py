from pathlib import Path
import argparse,json,zipfile,shutil,subprocess,os,re
parser=argparse.ArgumentParser(description='Отдельный игровой профиль для интеграционных проверок (Windows/Prism).')
parser.add_argument('--prism',required=True,help='Папка данных Prism с libraries, assets и meta')
parser.add_argument('--instance',required=True,help='Папка инстанса E2E или его minecraft')
parser.add_argument('--java-home',default=os.environ.get('JAVA_HOME'),help='JDK 8 для запуска Forge/передачи управления Cleanroom')
parser.add_argument('--memory',default='7G',help='Память для тестового экземпляра')
args_cli=parser.parse_args()
ROOT=Path(__file__).resolve().parents[1]
BASE=Path(args_cli.prism).expanduser().resolve()
candidate=Path(args_cli.instance).expanduser().resolve()
SOURCE=candidate if (candidate/'mods').is_dir() else candidate/'minecraft'
if not (SOURCE/'mods').is_dir():raise SystemExit('В инстансе не найдена папка mods')
if not args_cli.java_home:raise SystemExit('Укажите --java-home или JAVA_HOME (JDK 8)')
java=Path(args_cli.java_home)/'bin/java.exe'
if not java.is_file():raise SystemExit('Не найден java.exe: '+str(java))
if not re.fullmatch(r'[1-9][0-9]*[MG]',args_cli.memory):raise SystemExit('Память задаётся как 7G или 4096M')
RUN=ROOT/'run-release'
RUN.mkdir(exist_ok=True)
(RUN/'.ae2expert-runtime-check').write_text('Отдельный профиль для интеграционных проверок',encoding='utf-8')
for name in ['mods','config','scripts','resources','structures']:
 if (SOURCE/name).is_dir(): shutil.copytree(SOURCE/name,RUN/name,dirs_exist_ok=True)
version=re.search(r"version\s*=\s*'([^']+)'",(ROOT/'build.gradle').read_text()).group(1)
retired=RUN/'previous-addon'
retired.mkdir(exist_ok=True)
for old in (RUN/'mods').glob('ae2expert-*.jar'):
 shutil.move(str(old),str(retired/old.name))
shutil.copy2(ROOT/f'build/libs/ae2expert-{version}.jar',RUN/f'mods/ae2expert-{version}.jar')
helper=ROOT/f'build/libs/ae2expert-{version}-runtime-checks.jar'
if helper.exists(): shutil.copy2(helper,RUN/'mods'/helper.name)
(RUN/'options.txt').write_text('lang:ru_ru\nguiScale:4\nrenderDistance:4\nmaxFps:60\nfboEnable:true\nfullscreen:false\npauseOnLostFocus:false\nsoundCategory_master:0.0\n',encoding='utf-8')
natives=RUN/'natives'
natives.mkdir(exist_ok=True)
cp=[]
def maven_path(name,classifier=None):
 parts=name.split(':'); group,artifact,version=parts[:3]
 classifier=classifier or (parts[3] if len(parts)>3 else None)
 return BASE/'libraries'/group.replace('.','/')/artifact/version/(artifact+'-'+version+('-'+classifier if classifier else '')+'.jar')
def allowed(lib):
 result=not bool(lib.get('rules'))
 for rule in lib.get('rules',[]):
  system=rule.get('os',{}).get('name')
  if system is None or system=='windows': result=rule['action']=='allow'
 return result
for rel in ['net.minecraft/1.12.2','org.lwjgl/2.9.4-nightly-20150209','net.minecraftforge/14.23.5.2860']:
 meta=json.loads((BASE/'meta'/(rel+'.json')).read_text())
 for lib in meta.get('libraries',[])+([meta['mainJar']] if 'mainJar' in meta else []):
  if not allowed(lib): continue
  if 'artifact' in lib.get('downloads',{}) and 'natives' not in lib:
   path=maven_path(lib['name'])
   if not path.exists(): raise FileNotFoundError(path)
   if str(path) not in cp: cp.append(str(path))
  if 'windows' in lib.get('natives',{}):
   path=maven_path(lib['name'],lib['natives']['windows'])
   with zipfile.ZipFile(path) as z:
    for entry in z.namelist():
     if entry.lower().endswith('.dll'): (natives/Path(entry).name).write_bytes(z.read(entry))
args=[str(java),'-Xms1G','-Xmx'+args_cli.memory,'-Dfile.encoding=UTF-8','-Dae2expert.runtimecheck='+str(RUN),'-Djava.library.path='+str(natives),'-cp',';'.join(cp),'net.minecraft.launchwrapper.Launch','--tweakClass','net.minecraftforge.fml.common.launcher.FMLTweaker','--username','AE2ExpertTest','--uuid','8750d48ce758389aa097321242bf6c29','--accessToken','0','--userType','mojang','--version','1.12.2','--versionType','release','--gameDir',str(RUN),'--assetsDir',str(BASE/'assets'),'--assetIndex','1.12','--width','1920','--height','1080']
(ROOT/'tools/runtime-command.json').write_text(json.dumps(args,ensure_ascii=False,indent=2),encoding='utf-8')
with (RUN/'launch-output.log').open('wb') as out:
 p=subprocess.Popen(args,cwd=RUN,stdout=out,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
(RUN/'process.json').write_text(json.dumps({'pid':p.pid}),encoding='utf-8')
print('Изолированный запуск:',p.pid,'модов:',len(list((RUN/'mods').glob('*.jar'))))
