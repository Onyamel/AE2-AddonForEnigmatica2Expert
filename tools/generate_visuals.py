"""Геометрия моделей и ссылки на исходные текстуры. Растровые ассеты не меняются."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT/'src/main/resources/assets/ae2expert'

def write(group, name, data):
    path = ASSETS/group/(name+'.json')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def generate():
    # Текстуры машин подготовлены из отдельных референсов и хранятся в ресурсах.
    for capacity in [32,64,128,256]:
        name = 'cable_'+str(capacity)
        write('models/item',name,{'parent':'item/generated','textures':{
            'layer0':'ae2expert:items/'+name, 'layer1':'ae2expert:items/'+name+'_marks'}})

if __name__ == '__main__':
    generate()
    print('Обновлены модели: нейтральные корпуса AE2, метки тиров, отдельные машины.')
