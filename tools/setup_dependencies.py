"""Копирует проверенные зависимости из локальной E2E и готовит Maven-каталог."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def prepare(instance=None):
    deps = json.loads((ROOT/'libs/dependencies.json').read_text(encoding='utf-8'))
    source = None
    if instance:
        base = Path(instance).expanduser().resolve()
        source = next((p for p in [base/'minecraft/mods', base/'mods', base] if (p/deps[0]['file']).is_file()), None)
        if source is None:
            raise SystemExit('Не найдена папка mods с нужными версиями. См. libs/README.md.')
    missing = []
    for entry in deps:
        target = ROOT/'libs'/entry['file']
        if source:
            original = source/entry['file']
            if not original.is_file():
                missing.append(entry['file'])
                continue
            if original.resolve() != target.resolve():
                shutil.copy2(original, target)
        if not target.is_file():
            missing.append(entry['file'])
            continue
        if not zipfile.is_zipfile(target):
            raise SystemExit('Повреждённый jar: '+target.name)
        if hashlib.sha256(target.read_bytes()).hexdigest() != entry['sha256']:
            print('Отличие от проверенной контрольной суммы: '+target.name)
    if missing:
        raise SystemExit('Нужны файлы из libs/README.md:\n'+'\n'.join(missing))
    for name, version in [('appliedenergistics2','rv6-stable-7'),('ExtraCells','1.12.2-2.6.7')]:
        folder = ROOT/'libs/maven/local'/name/version
        folder.mkdir(parents=True,exist_ok=True)
        shutil.copy2(ROOT/'libs'/(name+'-'+version+'.jar'), folder/(name+'-'+version+'.jar'))
    print('Зависимости готовы: '+str(len(deps)))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--instance', help='Папка инстанса E2E, minecraft или mods')
    parser.add_argument('--check', action='store_true', help='Проверить уже подготовленную папку libs')
    args = parser.parse_args()
    prepare(args.instance)
