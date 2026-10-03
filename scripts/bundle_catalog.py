from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app/src/main/assets/catalog-seed.json'
p.with_suffix('.js').write_text('const CATALOG_SEED='+p.read_text()+';\nif(typeof module!=="undefined")module.exports=CATALOG_SEED;\n')
