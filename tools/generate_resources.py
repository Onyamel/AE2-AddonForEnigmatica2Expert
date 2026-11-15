"""Рецепты и модели аддона; источники предметов сверены со скриптами E2E 1.93a."""
from pathlib import Path
import json, zipfile
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT/'src/main/resources/assets/ae2expert'
def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
def item(name, data=None, empty=False):
    value = {'item': name}
    if data is not None: value['data'] = data
    if empty: value['type'] = 'ae2expert:empty_cell'
    return value
def material(meta): return item('appliedenergistics2:material',meta)
def ore(name): return {'type':'forge:ore_dict','ore':name}
def recipe(name, rows, keys, optional=False):
    value={'type':'forge:ore_shaped','group':'ae2expert','pattern':rows,'key':keys,'result':item('ae2expert:'+name,0 if name in ('cable_32','cable_64','cable_128','cable_256') else None)}
    if optional: value['conditions']=[{'type':'forge:mod_loaded','modid':'extracells'}]
    write(ASSETS/'recipes'/f'{name}.json',value)
tiers=['16k','32k','64k','128k','256k','512k','1m','2m','4m','8m','16m','32m','64m','128m','256m','512m']
for prefix,component in [('cell_',material(37)),('fluid_cell_',material(56)),('gas_cell_',item('extracells:storage.component',13))]:
    for n,tier in enumerate(tiers):
        casing=item('extracells:storage.casing',2 if prefix=='gas_cell_' else 0) if n>=4 or prefix=='gas_cell_' else material(39)
        optional=n>=4 or prefix=='gas_cell_'
        if n==0:
            recipe(prefix+tier,['GRG','RCR','PHP'],{'G':item('appliedenergistics2:quartz_glass'),'R':ore('dustRedstone'),'C':component,'P':ore('plateIron'),'H':casing},optional)
        else:
            recipe(prefix+tier,(['GPG','CBC','RHR'] if n<4 else ['G G','CBC','RHR']),{**({'P':ore('plateGold')} if n<4 else {}),'G':item('appliedenergistics2:quartz_glass'),'C':item('ae2expert:'+prefix+tiers[n-1],empty=True),'B':material(23 if n<4 else 24),'R':material(8),'H':casing},optional)
recipe('cell_infinite',['CSC','SQS','CSC'],{'C':item('ae2expert:cell_512m',empty=True),'S':material(47),'Q':item('appliedenergistics2:controller')})
for n,channels in enumerate([32,64,128,256]):
    prev=item('appliedenergistics2:part',76) if n==0 else item('ae2expert:cable_'+str(channels//2),32767)
    recipe('cable_'+str(channels),['PRP','CBC','PRP'],{'P':ore('plateIron' if n<2 else 'plateGold'),'R':ore('dustRedstone'),'C':prev,'B':material(24)})
for kind,meta in [('import',240),('export',260)]:
    for n,speed in enumerate([4,8,16]):
        prev=item('appliedenergistics2:part',meta) if n==0 else item(f'ae2expert:{kind}_bus_{speed//2}')
        recipe(f'{kind}_bus_{speed}',['PSP','SBS','PRP'],{'P':ore('plateIron' if n<2 else 'plateGold'),'S':material(30),'B':prev,'R':material(23)})
for n,speed in enumerate([2,4,8,16,32]):
    prev=item('appliedenergistics2:molecular_assembler') if n==0 else item('ae2expert:assembler_'+str(speed//2))
    recipe('assembler_'+str(speed),['GPG','CAC','GPG'],{'G':item('appliedenergistics2:quartz_glass'),'P':material(24),'C':prev,'A':material(30)})
for n,slots in enumerate([18,27,36,48,64]):
    prev=item('appliedenergistics2:interface') if n==0 else item('ae2expert:interface_'+str([18,27,36,48,64][n-1]))
    recipe('interface_'+str(slots),['GPG','CBC','GHG'],{'G':item('appliedenergistics2:quartz_glass'),'P':material(24),'C':prev,'B':material(27),'H':item('appliedenergistics2:interface')})
recipe('fluix_chamber',['GPG','PMP','GCG'],{'G':item('appliedenergistics2:quartz_glass'),'P':ore('plateIron'),'M':item('appliedenergistics2:charger'),'C':material(24)})
recipe('rf_eu_converter',['PCP','RAR','PCP'],{'P':ore('plateCopper'),'C':item('ic2:casing',3),'R':material(24),'A':item('appliedenergistics2:energy_acceptor')})
write(ASSETS/'recipes/_factories.json',{'ingredients':{'empty_cell':'com.e2e.ae2expert.recipe.EmptyCellIngredientFactory'}})
# Текстуры ускоренных сборщиков подготовлены из отдельных файлов для каждого тира.
for speed in [2,4,8,16,32]:
    name='assembler_'+str(speed)
    write(ASSETS/'models/block'/f'{name}.json',{'parent':'block/cube_all','textures':{'all':'ae2expert:blocks/'+name}})
    write(ASSETS/'blockstates'/f'{name}.json',{'variants':{'powered='+state:{'model':'ae2expert:'+name} for state in ['false','true']}})
for slots in [18,27,36,48,64]:
    write(ASSETS/'blockstates'/f'interface_{slots}.json',{'variants':{'normal':{'model':f'ae2expert:interface_{slots}'}}})
for name in [f'assembler_{n}' for n in [2,4,8,16,32]]+[f'interface_{n}' for n in [18,27,36,48,64]]+['fluix_chamber','rf_eu_converter']:
    write(ASSETS/'models/item'/f'{name}.json',{'parent':'ae2expert:block/'+name})
# Добавление недостающих ключей без повторов.
translations={
 'ru_ru':{'tile.ae2expert.fluix_chamber.name':'Камера изменчивых кристаллов','tile.ae2expert.rf_eu_converter.name':'Конвертер RF ↔ EU','ae2expert.gui.switch':'Сменить направление','ae2expert.gui.progress':'Готовность: %s%%','ae2expert.gui.config':'Настройка','ae2expert.gui.buffer':'Буфер','ae2expert.gui.patterns':'Шаблоны','ae2expert.cell.tooltip.bytes':'Объём: %s байт','ae2expert.cell.tooltip.empty_upgrade':'Улучшение: опустошите ячейку и снимите карты','ae2expert.gui.config_hint':'ЛКМ: количество в руке; ПКМ: 1. Пустой рукой: очистить / уменьшить.'},
 'en_us':{'tile.ae2expert.fluix_chamber.name':'Fluix Crystal Chamber','tile.ae2expert.rf_eu_converter.name':'RF ↔ EU Converter','ae2expert.gui.switch':'Switch direction','ae2expert.gui.progress':'Progress: %s%%','ae2expert.gui.config':'Configure','ae2expert.gui.buffer':'Buffer','ae2expert.gui.patterns':'Patterns','ae2expert.cell.tooltip.bytes':'Capacity: %s bytes','ae2expert.cell.tooltip.empty_upgrade':'Empty the cell and remove upgrades before upgrading','ae2expert.gui.config_hint':'Left: held count; right: 1. Empty hand: clear / decrease.'}}
for lang,addition in translations.items():
    path=ASSETS/'lang'/f'{lang}.lang'
    lines=path.read_text(encoding='utf-8-sig').splitlines()
    lines=[line for line in lines if line.split('=',1)[0] not in addition]
    path.write_text('\n'.join(lines)+'\n\n'+'\n'.join(k+'='+v for k,v in addition.items())+'\n',encoding='utf-8')
from generate_visuals import generate
generate()
print('Рецептов:',len(list((ASSETS/'recipes').glob('*.json')))-1)
