"""Статическая проверка JSON, локализации и ссылок на собственные ресурсы."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ae2expert'
count=0
errors=[]
def own_resource(ref,kind):
    if not isinstance(ref,str) or not ref.startswith('ae2expert:'): return
    target=root/kind/(ref.split(':',1)[1]+('.png' if kind=='textures' else '.json'))
    if not target.exists(): errors.append('Отсутствует '+str(target))
def walk(node,kind):
    if isinstance(node,dict):
        for key,value in node.items():
            if key=='parent' and kind=='model': own_resource(value,'models')
            if key=='textures' and isinstance(value,dict):
                for ref in value.values(): own_resource(ref,'textures')
            if kind=='state' and key in ('model','submodel') and isinstance(value,str) and value.startswith('ae2expert:'):
                path=value.split(':',1)[1]
                if path.startswith('block/'): errors.append('Лишний block/ в blockstate: '+value)
                own_resource('ae2expert:block/'+path,'models')
            walk(value,kind)
    elif isinstance(node,list):
        for value in node: walk(value,kind)
for path in root.rglob('*.json'):
    try:
        data=json.loads(path.read_text(encoding='utf-8-sig'))
        walk(data,'state' if 'blockstates' in path.parts else 'model')
        count+=1
    except Exception as exc: errors.append(str(path)+': '+str(exc))
langs={}
for path in (root/'lang').glob('*.lang'):
    keys=[]
    for line in path.read_text(encoding='utf-8-sig').splitlines():
        if '=' in line and not line.startswith('#'): keys.append(line.split('=',1)[0])
    if len(keys)!=len(set(keys)): errors.append('Повтор ключей в '+path.name)
    langs[path.stem]=set(keys)
if langs.get('ru_ru')!=langs.get('en_us'): errors.append('Наборы ключей RU и EN различаются')
recipes=[p for p in (root/'recipes').glob('*.json') if not p.name.startswith('_')]
if len(recipes)!=94: errors.append('Ожидалось 94 рецепта, найдено '+str(len(recipes)))
if errors: raise SystemExit('\n'.join(errors))
print(f'Проверено JSON: {count}; рецептов: {len(recipes)}; ключей в каждой локализации: {len(langs["ru_ru"])}. Ошибок нет.')
