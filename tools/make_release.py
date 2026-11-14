"""Готовит чистые исходники, игровой релиз и комплект для распространения."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = re.search(r"version\s*=\s*'([^']+)'", (ROOT/'build.gradle').read_text()).group(1)
OUT = ROOT/'dist'/('github-'+VERSION)
SOURCE_NAME = 'ae2expert-'+VERSION+'-sources.zip'
RELEASE_NAME = 'ae2expert-'+VERSION+'-release.zip'
JAR_NAME = 'ae2expert-'+VERSION+'.jar'
FILES = ['.gitignore','.gitattributes','build.gradle','build.ps1','gradle.properties',
         'settings.gradle','gradlew','gradlew.bat','README.md','README-RU.md','LICENSE','NOTICE.md','NOTICE-RU.md',
         'CHANGELOG-'+VERSION+'.md','RELEASE_NOTES.md','libs/README.md','libs/README-RU.md','libs/dependencies.json']
TREES = ['src','gradle','docs','.github']
TOOLS = ['generate_resources.py','generate_visuals.py','validate_resources.py',
         'setup_dependencies.py','launch_runtime_check.py','make_release.py']

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def archive(path, files):
    with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as output:
        for source,name in sorted(files,key=lambda pair:pair[1]):
            info = zipfile.ZipInfo(name,(2026,11,14,0,0,0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (0o755 if name.endswith('/gradlew') else 0o644)<<16
            output.writestr(info,source.read_bytes())

def main():
    jar = ROOT/'build/libs'/JAR_NAME
    if not jar.is_file():raise SystemExit('Сначала соберите проект: '+str(jar))
    with zipfile.ZipFile(jar) as z:
        for required in ['LICENSE','NOTICE.md','licenses/GPL-3.0.txt','licenses/CC-BY-NC-SA-3.0.txt']:
            if required not in z.namelist():raise SystemExit('В jar не включены лицензии: '+required)
        if any('RuntimeIntegrationChecks' in n or 'RegressionChecks' in n for n in z.namelist()):
            raise SystemExit('Основной jar содержит проверочные классы')
        metadata=json.loads(z.read('mcmod.info'))[0]
        if metadata['version']!=VERSION or metadata['mcversion']!='1.12.2':raise SystemExit('Неверные метаданные jar')
    selected = [ROOT/p for p in FILES]
    for name in TREES+['licenses']:
        selected += [p for p in (ROOT/name).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
    selected += [ROOT/'tools'/name for name in TOOLS]
    for p in selected:
        if not p.is_file():raise SystemExit('Отсутствует файл пакета: '+str(p))
    if len(selected)!=len(set(selected)):raise SystemExit('Повтор пути в списке исходников')
    # Обновление пакета не удаляет существующие каталоги: посторонние остатки запрещены.
    repository = OUT/'repository'
    expected={p.relative_to(ROOT).as_posix() for p in selected}
    if repository.exists():
        extra={p.relative_to(repository).as_posix() for p in repository.rglob('*') if p.is_file()}-expected
        if extra:raise SystemExit('В repository обнаружены посторонние файлы: '+', '.join(sorted(extra)))
    for source in selected:
        target=repository/source.relative_to(ROOT)
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(source,target)
    archive(OUT/SOURCE_NAME,[(p,'ae2expert-addon-'+VERSION+'/'+p.relative_to(ROOT).as_posix()) for p in selected])
    release = OUT/'release'
    release.mkdir(parents=True,exist_ok=True)
    shutil.copy2(jar,release/JAR_NAME)
    for name in ['LICENSE','NOTICE.md','NOTICE-RU.md','README.md','README-RU.md','RELEASE_NOTES.md','CHANGELOG-'+VERSION+'.md']:
        shutil.copy2(ROOT/name,release/name)
    shutil.copytree(ROOT/'licenses',release/'licenses',dirs_exist_ok=True)
    (release/'INSTALL-RU.txt').write_text('AE2 Expert Addon '+VERSION+' / Minecraft 1.12.2\n\n'
        'Закройте Minecraft и сохраните резервную копию мира.\n'
        'Положите '+JAR_NAME+' в minecraft/mods установленной E2E, заменив прежний jar аддона.\n'
        'На сервере и у всех клиентов должна быть версия '+VERSION+'.\n'
        'Остальные файлы этого архива в mods копировать не нужно.\n'
        'Исходники опубликованы рядом с jar в релизе и в репозитории проекта.\n',encoding='utf-8')
    (release/'INSTALL-EN.txt').write_text('AE2 Expert Addon '+VERSION+' / Minecraft 1.12.2\n\nClose Minecraft and back up your world. Replace the previous addon jar in minecraft/mods with '+JAR_NAME+'. Install the same version on the server and all clients. Do not copy the other archive files into mods. Source code is included alongside the release.\n',encoding='utf-8')
    checksum=digest(jar)+'  '+JAR_NAME+'\n'
    (release/'SHA256SUMS.txt').write_text(checksum,encoding='utf-8')
    archive(OUT/RELEASE_NAME,[(p,p.relative_to(release).as_posix()) for p in release.rglob('*') if p.is_file()])
    shutil.copy2(jar,OUT/JAR_NAME)
    shutil.copy2(ROOT/'RELEASE_NOTES.md',OUT/'RELEASE_NOTES.md')
    (OUT/'SHA256SUMS.txt').write_text(''.join(digest(OUT/name)+'  '+name+'\n' for name in [JAR_NAME,SOURCE_NAME,RELEASE_NAME]),encoding='utf-8')
    packname='ae2expert-'+VERSION+'-github-pack.zip'
    archive(OUT/packname,[(p,'ae2expert-github-'+VERSION+'/'+p.relative_to(OUT).as_posix()) for p in OUT.rglob('*') if p.is_file() and p.name!=packname])
    print('Пакет готов: '+str(OUT))
    print('Исходных файлов: '+str(len(selected)))
    print('SHA256 jar: '+digest(jar))

if __name__=='__main__':main()
