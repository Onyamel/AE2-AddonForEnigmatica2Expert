"""Геометрия моделей и ссылки на исходные текстуры. Растровые ассеты не меняются."""
from pathlib import Path
import json, zipfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT/'src/main/resources/assets/ae2expert'
AE = 'appliedenergistics2:blocks/'

def write(group, name, data):
    path = ASSETS/group/(name+'.json')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def box(start, end, texture='metal', tint=None, faces=None):
    sides = {}
    for side in faces or ['down','up','north','south','west','east']:
        face = {'texture':'#'+texture}
        if tint is not None: face['tintindex'] = tint
        sides[side] = face
    return {'from':start, 'to':end, 'faces':sides}

def model(textures, elements):
    return {'parent':'block/block', 'ambientocclusion':True, 'textures':textures, 'elements':elements}

def generate():
    with zipfile.ZipFile(ROOT/'libs/appliedenergistics2-rv6-stable-7.jar') as archive:
        original = json.loads(archive.read('assets/appliedenergistics2/models/block/molecular_assembler.json'))
        icons = [n for n in archive.namelist() if n.endswith('.png') and 'dense_smart' in n and 'gray' in n]
        # Имя исходной иконки стабильно в rv6; при смене зависимости не подставляем случайную текстуру.
        if not icons:
            icons = [n for n in archive.namelist() if n.endswith('.png') and 'dense' in n and 'smart' in n]
        if not icons: raise RuntimeError('Не найдена исходная иконка плотного умного кабеля')
        icon = sorted(icons)[0].replace('assets/appliedenergistics2/textures/','appliedenergistics2:')[:-4]
    for rank, speed in enumerate([2,4,8,16,32], 1):
        # Исходная прозрачная рама целиком сохраняется; метки лежат на нижней металлической балке.
        frame = json.loads(json.dumps(original))
        frame['textures']['metal'] = 'minecraft:blocks/iron_block'
        for mark in range(rank):
            x = 3 + mark*2
            frame['elements'].append(box([x,0.5,-0.02],[x+1,1.5,0.02],tint=0,faces=['north']))
            frame['elements'].append(box([x,0.5,15.98],[x+1,1.5,16.02],tint=0,faces=['south']))
        name = 'assembler_'+str(speed)
        write('models/block',name,frame)
        write('blockstates',name,{'forge_marker':1,'defaults':{'model':'ae2expert:'+name},'variants':{'powered':{'false':{},'true':{'submodel':'appliedenergistics2:molecular_assembler_lights'}}}})
    for rank, slots in enumerate([18,27,36,48,64],1):
        elements = [box([0,0,0],[16,16,16],'body')]
        for mark in range(rank):
            x = 3+mark*2
            elements += [box([x,1,-0.02],[x+1,2,0.02],tint=0,faces=['north']),
                         box([x,1,15.98],[x+1,2,16.02],tint=0,faces=['south']),
                         box([-0.02,1,x],[0.02,2,x+1],tint=0,faces=['west']),
                         box([15.98,1,x],[16.02,2,x+1],tint=0,faces=['east'])]
        write('models/block','interface_'+str(slots),model({'particle':AE+'interface','body':AE+'interface','metal':'minecraft:blocks/iron_block'},elements))
    for capacity in [32,64,128,256]:
        write('models/item','cable_'+str(capacity),{'parent':'item/generated','textures':{'layer0':icon}})
    converter = [box([1,0,1],[15,16,15],'body')]
    # Тёмные боковые радиаторы и два цветных гнезда на каждой рабочей стороне.
    for y in [3,5,7,9,11]:
        converter += [box([0,y,2],[1,y+1,14],'dark'),box([15,y,2],[16,y+1,14],'dark')]
    for z, face in [([0,1],'north'),([15,16],'south')]:
        converter += [box([2,4,z[0]],[7,12,z[1]],'dark'),box([9,4,z[0]],[14,12,z[1]],'dark'),
                      box([3,5,z[0]-0.01],[6,11,z[1]+0.01],tint=0,faces=[face]),
                      box([10,5,z[0]-0.01],[13,11,z[1]+0.01],tint=1,faces=[face])]
    converter += [box([2,15.98,3],[6,16.02,13],tint=0,faces=['up']),box([10,15.98,3],[14,16.02,13],tint=1,faces=['up'])]
    write('models/block','rf_eu_converter',model({'particle':AE+'energy_acceptor','body':AE+'energy_acceptor','metal':'minecraft:blocks/iron_block','dark':'minecraft:blocks/coal_block'},converter))
    chamber = [box([0,0,0],[16,3,16],'body'),box([0,13,0],[16,16,16],'body')]
    for x in [0,14]:
        for z in [0,14]: chamber.append(box([x,3,z],[x+2,13,z+2]))
    # Стекло окружает полость, а кристалл читается из любого горизонтального направления.
    chamber += [box([2,3,2],[14,13,14],'glass',faces=['north','south','west','east']),box([4,3,4],[12,4,12],'dark')]
    for start,end,angle in [([6.5,4,6.5],[9.5,10,9.5],22.5),([4.5,4,5],[6.5,7,7],-22.5),([9,4,9],[11,8,11],22.5)]:
        crystal=box(start,end,'crystal',tint=0)
        crystal['rotation']={'origin':[(start[0]+end[0])/2,start[1],(start[2]+end[2])/2],'axis':'z','angle':angle}
        chamber.append(crystal)
    write('models/block','fluix_chamber',model({'particle':AE+'charger','body':AE+'charger','metal':'minecraft:blocks/iron_block','dark':AE+'charger_inside','glass':'minecraft:blocks/glass','crystal':'minecraft:blocks/quartz_block_side'},chamber))

if __name__ == '__main__':
    generate()
    print('Обновлены модели: нейтральные корпуса AE2, метки тиров, отдельные машины.')
