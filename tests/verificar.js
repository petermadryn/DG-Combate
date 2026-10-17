// Pruebas de regresión para creación e importación. Sin material privado.
const fs = require('fs'), vm=require('vm'), assert=require('node:assert/strict');
const path = require('path');
const html=fs.readFileSync(path.resolve(__dirname,'../app/src/main/assets/index.html'),'utf8');
const match=html.match(/<script>([\s\S]+?)<\/script>/);
assert(match,'Falta el JS principal');
new Function(match[1]);
const ctx={window:{},console,Date,Math,localStorage:{getItem:()=>null,setItem:()=>{}},
  confirm:()=>true,prompt:()=>null};vm.createContext(ctx);vm.runInContext(match[1],ctx);
const samples=[];for(let k=0;k<30;k++)samples.push(JSON.stringify(ctx.mkChar('humano').stats));
assert(new Set(samples).size>=6,'Error: los PNJ generados resultan idénticos');
assert.equal(ctx.mkChar('agente').name,'','Un PJ nuevo debe pedir nombre, no llevar un nombre falso');
const sheet=`NAME AND RANK\nLieutenant Colonel Michael Spay\nStrength (STR) 10 50%\nConstitution (CON) 14 70%\nDexterity (DEX) 14 70%\nIntelligence (INT) 14 70%\nPower (POW) 16 80%\nCharisma (CHA) 11 55%\nHit Points (HP) 12\nWillpower Points (WP) 16\nSanity Points (SAN) 80\nBreaking Point (BP) 64\nAlertness 60%`;
const c=ctx.parseSheet(sheet);
assert.equal(c.name,'Lieutenant Colonel Michael Spay');
for(const [k,v] of Object.entries({fue:10,con:14,des:14,int:14,pod:16,car:11}))assert.equal(c.stats[k],v,k);
for(const [k,v] of Object.entries({hp:12,wp:16,san:80,bp:64}))assert.equal(c[k],v,k);
const stats=`STR 11 CON 11 DEX 11 INT 11 POW 11 CHA 11\nHP 11 WP 11 SAN 0\nSKILLS: Alertness 40%, Firearms 40%\nATTACKS: Shotgun 40%, damage 2D6.\nKnife 40%, damage 1D4, Armor Piercing 3.`;
const monster=ctx.parseSheet(stats);
assert.equal(monster.foundStats,6);assert.equal(monster.san,0);assert.equal(monster.attacks.length,2);
assert.equal(monster.attacks[1].ap,3);
const missing=ctx.parseSheet('Personaje sin datos\nCON 14\nDodge 40%');
assert.equal(missing.foundStats,1);assert.equal(missing.stats.fue,undefined,'No inventar FUE 10');
const layout=[{text:'STATIC Killer',x:.05,y:.3,page:0},{text:'STR 11 CON 11 DEX 11 INT 11 POW 11 CHA 11',x:.05,y:.38,page:0},
{text:'HP 11 WP 11 SAN 0',x:.05,y:.42,page:0},{text:'Delta Green Zip Team Agent',x:.55,y:.51,page:0},
{text:'STR 12 CON 11 DEX 12 INT 11 POW 11 CHA 11',x:.55,y:.58,page:0},{text:'HP 12 WP 11 SAN 50 BREAKING POINT 44',x:.55,y:.61,page:0}];
const found=ctx.findImportCandidates(layout.map(x=>x.text).join('\n'),layout);
assert.equal(found.length,2,'No se separan 2 personajes en la misma página');
assert.equal(ctx.parseSheet(found[0].text).stats.fue,11);
assert.equal(ctx.parseSheet(found[1].text).stats.fue,12);
const fields=[{text:'NAME AND RANK',x:.05,y:.05,page:0},{text:'Lieutenant Colonel Michael Spay',x:.05,y:.072,page:0}];
let y=.10;for(const [label,value] of Object.entries({'Strength (STR)':10,'Constitution (CON)':14,'Dexterity (DEX)':14,'Intelligence (INT)':14,'Power (POW)':16,'Charisma (CHA)':11})){
 fields.push({text:label,x:.62,y,page:0},{text:''+value,x:.79,y,page:0},{text:''+(value*5)+'%',x:.87,y,page:0});y+=.025;
}
let positioned=ctx.parseSheet(fields.map(x=>x.text).join('\n'),false,fields);
assert.equal(positioned.name,'Lieutenant Colonel Michael Spay');assert.equal(positioned.stats.pod,16);
assert.equal(positioned.stats.car,11);
console.log('OK: nombres, PNJ variados, ficha bilingüe, casilleros, dos perfiles y valores pendientes');
