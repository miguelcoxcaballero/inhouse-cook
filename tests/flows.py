"""Integration paths against the packaged UI, using Chromium at a mobile viewport."""
import json
from pathlib import Path
from playwright.sync_api import sync_playwright, expect
BASE='http://127.0.0.1:8765'
OUT=Path('/workspace/inhouse-cook/docs/screenshots');OUT.mkdir(parents=True,exist_ok=True)
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path='/usr/bin/chromium',args=['--no-sandbox'])
 page=browser.new_page(viewport={'width':390,'height':844},device_scale_factor=1)
 errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
 page.goto(BASE);expect(page.locator('h1')).to_have_text('Tu súper, cerca.')
 page.locator('#postal').fill('28001');page.locator('[data-action="wizard-next"]').click()
 page.locator('[data-action="people-plus"]').click();expect(page.locator('.counter strong')).to_have_text('3')
 page.locator('[data-action="wizard-next"]').click();page.locator('#budget').fill('70')
 page.locator('[data-action="days"][data-n="7"]').click();page.locator('[data-action="wizard-next"]').click()
 page.locator('[data-action="style"][data-id="quick"]').click();page.screenshot(path=str(OUT/'01-estilos.png'),full_page=True)
 page.locator('[data-action="wizard-next"]').click();page.locator('[data-action="diet"][data-id="vegetarian"]').click()
 page.locator('[data-action="wizard-next"]').click();page.locator('.pills [data-action="equipment"][data-id="airfryer"]').click()
 page.screenshot(path=str(OUT/'02-cocina.png'),full_page=True);page.locator('[data-action="generate"]').click()
 expect(page.locator('h1')).to_have_text('Tu semana, resuelta.',timeout=15000)
 expect(page.locator('.meal')).to_have_count(7)
 initial=page.evaluate('JSON.parse(localStorage.getItem("inhouse.cook.v2"))')
 assert all(page.evaluate('(id)=>COOK_DATA.recipes.find(r=>r.id===id).ingredients.every(i=>!["meat","fish"].includes(COOK_DATA.ingredients[i.key].animal))',m['recipeId']) for m in initial['plan']['meals'])
 page.screenshot(path=str(OUT/'03-semana.png'),full_page=True)
 page.locator('[data-action="save-plan"]').click();page.locator('#plan-name').fill('Mi semana vegetariana');page.locator('[data-action="confirm-save-plan"]').click()
 page.locator('[data-action="regenerate"]').click();expect(page.locator('.meal')).to_have_count(7,timeout=15000)
 regenerated=page.evaluate('JSON.parse(localStorage.getItem("inhouse.cook.v2")).plan.meals')
 assert regenerated!=initial['plan']['meals'],'Regeneration must change choices'
 page.locator('[data-action="swap"]').first.click();page.locator('[data-action="select-swap"]').first.click()
 page.locator('.meal-body').first.click();expect(page.locator('.ingredient')).not_to_have_count(0)
 amount_before=page.locator('.ingredient .amount').first.inner_text();page.locator('[data-action="servings-plus"]').click()
 assert page.locator('.ingredient .amount').first.inner_text()!=amount_before
 page.locator('[data-action="ingredient-check"]').first.click();expect(page.locator('.ingredient').first).to_have_class('ingredient checked')
 page.locator('[data-action="favorite"]').click();page.locator('[data-action="add-recipe-groceries"]').click()
 page.screenshot(path=str(OUT/'04-receta.png'),full_page=True)
 page.locator('[data-action="cook"]').first.click();expect(page.locator('.cook h1')).to_have_text('Paso 1.')
 if page.locator('[data-action="timer-start"]').count():
  page.locator('[data-action="timer-start"]').click();expect(page.locator('[data-action="timer-stop"]')).to_be_visible()
 while page.locator('[data-action="cook-next"]').count():page.locator('[data-action="cook-next"]').click()
 page.screenshot(path=str(OUT/'05-cocinar.png'),full_page=True)
 page.locator('[data-action="finish-cook"]').click();page.locator('[data-action="rate"][data-n="4"]').click()
 page.locator('[data-action="back"]').click();page.locator('.nav [data-action="nav"][data-screen="groceries"]').click()
 expect(page.locator('.grocery')).not_to_have_count(0);page.locator('[data-action="bought"]').first.click()
 expect(page.locator('.grocery').first).to_have_class('grocery bought')
 if page.locator('[data-action="packs-plus"]').count():
  text=page.locator('.grand-total strong').inner_text();page.locator('[data-action="packs-plus"]').first.click();assert text!=page.locator('.grand-total strong').inner_text()
 page.locator('[data-action="edit-price"]').first.click();page.locator('#price-value').fill('2.45');page.locator('#price-quantity').fill('500');page.locator('[data-action="save-price"]').click()
 page.locator('[data-action="add-item"]').click();page.locator('#extra-item').fill('200 g ingrediente desconocido');page.locator('[data-action="confirm-item"]').click()
 expect(page.locator('.alert')).to_contain_text('sin precio')
 page.locator('[data-action="edit-price"][data-key^="custom:"]').click();page.locator('#price-value').fill('1.50');page.locator('[data-action="save-price"]').click()
 page.screenshot(path=str(OUT/'06-compra.png'),full_page=True)
 page.locator('[data-action="pantry"]').click();page.locator('[data-action="pantry-toggle"][data-key="rice"]').click();page.locator('[data-action="pantry-done"]').click()
 assert page.locator('[data-action="bought"][data-key="rice"]').count()==0
 page.locator('[data-action="nav"][data-screen="discover"]').click();page.locator('[data-action="import-recipe"]').click()
 page.locator('#recipe-name').fill('Mi arroz');page.locator('#recipe-url').fill('https://example.com/receta');page.locator('#recipe-ingredients').fill('200 g arroz\n2 huevos');page.locator('#recipe-steps').fill('Cuece el arroz.\nAñade el huevo cocinado.')
 page.locator('[data-action="import-equipment"][data-id="hob"]').click();page.locator('[data-action="save-recipe"]').click()
 expect(page.locator('h1')).to_have_text('Mi arroz');expect(page.locator('.ingredient')).to_have_count(2)
 page.locator('[data-action="recipe-plan"]').click();page.locator('[data-action="select-swap"]').first.click()
 expect(page.locator('.meal h3').first).to_have_text('Mi arroz')
 page.reload();expect(page.locator('.meal h3').first).to_have_text('Mi arroz')
 page.locator('[data-action="nav"][data-screen="settings"]').click();expect(page.locator('.saved-plan')).to_have_count(1)
 page.locator('[data-action="load-plan"]').click();restored=page.evaluate('JSON.parse(localStorage.getItem("inhouse.cook.v2")).plan.meals')
 assert restored==initial['plan']['meals']
 state=page.evaluate('JSON.parse(localStorage.getItem("inhouse.cook.v2"))')
 assert state['history'][0]['rating']==4 and len(state['imports'])==1 and state['favorites']
 page.locator('[data-action="nav"][data-screen="settings"]').click()
 with page.expect_download() as dl:page.locator('[data-action="export"]').click()
 path=Path('/tmp/cook-backup.json');dl.value.save_as(path);assert json.loads(path.read_text())['state']['saved'][0]['name']=='Mi semana vegetariana'
 page.locator('#file-import').set_input_files(str(path));expect(page.locator('.sheet h2')).to_have_text('Restaurar tu cocina');page.locator('[data-action="confirm-restore"]').click()
 page.locator('[data-action="nav"][data-screen="discover"]').click();page.locator('#search').fill('Mi arroz');expect(page.locator('.recipe-card')).to_have_count(1)
 page.screenshot(path=str(OUT/'07-recetario.png'),full_page=True)
 assert page.evaluate('document.documentElement.scrollWidth<=window.innerWidth'),'No mobile overflow'
 assert not errors,errors
 print('PASS: complete onboarding, diet constraints, regeneration, swap, saving, scaling, ingredients, favorites, cooking/timer/rating, basket math, manual prices, pantry, imports, reload persistence, saved plan restore, JSON round trip, search, mobile layout. No JavaScript errors.')
 browser.close()
