const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync(require('node:path').join(__dirname, '../app.js'), 'utf8');
const helper = source.slice(source.indexOf('function groupExpanded('), source.indexOf('function render(){'));
function fixture(search = '', stock = 'all') {
  const context = { opened: new Set(), groupOverrides: new Map(), $: selector => ({ value: selector === '#search' ? search : stock }) };
  vm.createContext(context);
  vm.runInContext(helper, context);
  return context;
}
for (const [name, search, stock] of [['search', 'CN2', 'all'], ['in stock', '', 'IN_STOCK'], ['out of stock', '', 'OUT_OF_STOCK']]) {
  test(`manual collapse overrides automatic expansion during ${name}`, () => {
    const c = fixture(search, stock);
    assert.equal(c.groupExpanded(1), true);
    c.groupOverrides.set(1, false);
    assert.equal(c.groupExpanded(1), false);
    assert.equal(c.groupExpanded(2), true);
    c.groupOverrides.set(1, true);
    assert.equal(c.groupExpanded(1), true);
  });
}
test('ordinary group uses opened state unless explicitly overridden', () => {
  const c = fixture();
  assert.equal(c.groupExpanded(1), false);
  c.opened.add(1);
  assert.equal(c.groupExpanded(1), true);
  c.groupOverrides.set(1, false);
  assert.equal(c.groupExpanded(1), false);
});
