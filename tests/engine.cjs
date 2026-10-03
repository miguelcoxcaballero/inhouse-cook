const assert=require('node:assert/strict');
const E=require('../app/src/main/assets/engine.js'),D=require('../app/src/main/assets/data.js'),C=require('../app/src/main/assets/catalog-seed.js');
const map=E.products(C[0]);
// Package rounding is shared by the planner and the shopping UI.
const fixture=[{id:'a',baseServings:2,ingredients:[{key:'rice',amount:300},{key:'egg',amount:2},{key:'water',amount:500}],equipment:[],tags:[]}];
const products={rice:{price:2,quantity:1000,unit:'g'},egg:{price:3,quantity:6,unit:'ud'}};
let rows=E.basket([{day:0,recipeId:'a',servings:4},{day:1,recipeId:'a',servings:4}],fixture,products);
assert.equal(rows.find(r=>r.key==='rice').packs,2);assert.equal(rows.find(r=>r.key==='egg').packs,2);assert.deepEqual(E.totals(rows),{cost:10,missing:0});
rows=E.basket([{day:0,recipeId:'a',servings:4}],fixture,products,{rice:true},[{key:'egg',amount:5}]);assert.equal(rows.length,1);assert.equal(rows[0].packs,2);
rows=E.basket([{day:0,recipeId:'a',servings:4}],fixture,{});assert.equal(E.totals(rows).missing,2);assert.equal(E.totals(rows).cost,0);
assert.equal(E.basket([{day:0,recipeId:'a',servings:2}],fixture,products,{},[],{rice:3}).find(r=>r.key==='rice').cost,6);
// Constrained plans always respect dietary exclusions and equipment availability.
for(const diets of [[],['vegan'],['vegetarian'],['pescatarian'],['gluten-free'],['dairy-free'],['vegan','gluten-free']]){
 for(const equipment of [['hob','oven'],[],['airfryer'],['microwave']]){
  const config={equipment,diets,styles:['quick'],days:7,people:2,budget:55};const plan=E.generate(config,D.recipes,map,{oliveOil:true,salt:true},7);
  if(plan.error){assert(!D.recipes.some(r=>E.eligible(r,config)));continue;}
  assert(plan.meals.every(m=>E.eligible(D.recipes.find(r=>r.id===m.recipeId),config)));
  assert.equal(new Set(plan.meals.map(m=>m.recipeId)).size,plan.meals.length);
  assert.deepEqual(plan.total,E.totals(E.basket(plan.meals,D.recipes,map,{oliveOil:true,salt:true})));
 }
}
const budget={equipment:['hob','oven'],diets:[],styles:[],days:5,people:2,budget:55};const first=E.generate(budget,D.recipes,map,{oliveOil:true,salt:true},1),second=E.generate(budget,D.recipes,map,{oliveOil:true,salt:true},2,first.meals);
assert(first.inBudget);assert.notDeepEqual(first.meals,second.meals);
const impossible=E.generate({...budget,budget:0.01},D.recipes,map,{},5);assert(!impossible.inBudget);assert(impossible.total.cost>0.01);
const imported={id:'custom',equipment:[],ingredients:[{key:'custom:unknown',amount:100}],tags:[]};assert(!E.eligible(imported,{equipment:[],diets:[],allergens:['egg']}));
assert.equal(E.parseIngredient('2 huevos').unit,'ud');assert.equal(E.parseIngredient('1/2 kg arroz').amount,500);assert.equal(E.parseIngredient('250 ml leche').unit,'ml');assert.equal(E.parseIngredient('2 dientes de ajo').amount,8);assert(E.parseIngredient('150 g ñame').key.startsWith('custom:'));
assert(map.chickpeas.drained&&map.chickpeas.quantity===400);assert(!/fiambre|crema/.test(map.chicken.name.toLowerCase()));
const dia=E.products(C[1]);assert(!/fiambre/.test(dia.chicken.name.toLowerCase()));assert.equal(dia.carrot.name,'Zanahoria 1 Kg');assert(/bandeja/.test(dia.mushroom.name));assert.equal(dia.yogurt.quantity,480);
console.log('PASS: package accounting, aggregation, pantry/extras, unknown prices, manual pack changes, every diet/equipment combination, realistic budget bounds, regeneration, restricted imports, quantity parsing, retailer product selection and drained/multipack weights.');
